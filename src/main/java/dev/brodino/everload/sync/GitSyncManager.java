package dev.brodino.everload.sync;

import dev.brodino.everload.EverLoad;
import dev.brodino.everload.util.PathUtil;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GitSyncManager {
    
    private Git git;
    private final Path repositoryDirectory;
    private ObjectId preSyncHead;
    private boolean wasFreshClone;
    
    public GitSyncManager() {
        this.repositoryDirectory = PathUtil.getRepositoryDirectory();
    }
    
    /**
     * Sync a repository (clone if new, pull if exists)
     * @param repositoryUrl The Git repository URL
     * @param branch The branch to sync
     * @param context The sync context for progress tracking
     * @throws IOException if file operations fail
     * @throws GitAPIException if git operations fail
     */
    public void sync(String repositoryUrl, String branch, SyncContext context) throws IOException, GitAPIException {
        EverLoad.LOGGER.info("Starting Git sync: {} (branch: {})", repositoryUrl, branch);

        if (!PathUtil.isValidGitUrl(repositoryUrl)) {
            throw new IllegalArgumentException("Invalid Git URL: " + repositoryUrl);
        }

        PathUtil.ensureDirectoryExists(this.repositoryDirectory.getParent());
        
        File repoDir = this.repositoryDirectory.toFile();
        
        // Reset state tracking
        this.preSyncHead = null;
        this.wasFreshClone = false;
        
        if (this.isRepositoryInitialized()) {
            // Open the repository once and reuse the handle for all read operations,
            // so we can close it explicitly before any cleanup (critical on Windows
            // where open pack-file handles block deletion).
            Git existingGit = null;
            try {
                existingGit = Git.open(repoDir);

                // Capture HEAD before pull for change detection
                this.preSyncHead = this.captureHeadCommit(existingGit);

                String existingUrl = this.readRemoteUrl(existingGit);
                boolean urlMatches = this.isUrlMatching(repositoryUrl, existingUrl);

                if (urlMatches) {
                    // Keep the handle open for the pull
                    this.git = existingGit;
                    existingGit = null; // ownership transferred – don't close in finally
                    context.setStatusMessage("Pulling latest changes...");
                    EverLoad.LOGGER.info("Repository exists, pulling updates from: {}", repoDir);
                    this.pullRepository(branch, context);
                } else {
                    EverLoad.LOGGER.info("Repository URL mismatch. Existing: {}, Configured: {}. Re-cloning...", existingUrl, repositoryUrl);
                    context.setStatusMessage("Repository changed, re-cloning...");
                    // Close ALL handles before attempting to delete on Windows
                    existingGit.close();
                    existingGit = null;
                    this.closeGit();
                    this.cleanupRepository();
                    this.preSyncHead = null; // No previous state for re-clone
                    this.wasFreshClone = true;
                    this.cloneRepository(repositoryUrl, branch, context);
                }
            } finally {
                if (existingGit != null) {
                    existingGit.close();
                }
            }
        } else {
            // Repository doesn't exist, clone it
            context.setStatusMessage("Cloning repository...");
            EverLoad.LOGGER.info("Cloning repository to: {}", repoDir);
            this.wasFreshClone = true;
            this.cloneRepository(repositoryUrl, branch, context);
        }
        
        EverLoad.LOGGER.info("Git sync completed successfully");
    }

    /** Close the cached {@link #git} instance and null it out. */
    private void closeGit() {
        if (this.git != null) {
            this.git.close();
            this.git = null;
        }
    }

    private void cloneRepository(String repositoryUrl, String branch, SyncContext context) throws IOException, GitAPIException {
        File repoDir = this.repositoryDirectory.toFile();
        
        // Create progress monitor
        JGitProgressMonitor progressMonitor = new JGitProgressMonitor(context);
        
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(repositoryUrl)
                .setDirectory(repoDir)
                .setBranch(branch)
                .setCloneAllBranches(false) // Only specified branch
                .setProgressMonitor(progressMonitor)
                .setDepth(1);
        
        EverLoad.LOGGER.info("Executing clone command: {} -> {}", repositoryUrl, repoDir);
        
        try {
            git = cloneCommand.call();
            EverLoad.LOGGER.info("Clone completed: {} files in repository", this.countFiles(repositoryDirectory));
        } catch (GitAPIException e) {
            EverLoad.LOGGER.error("Clone failed: {}", e.getMessage(), e);
            try {
                this.cleanupRepository();
            } catch (IOException cleanupEx) {
                EverLoad.LOGGER.error("Failed to cleanup after failed clone: {}", cleanupEx.getMessage(), cleanupEx);
            }
            throw e;
        }
    }

    private void pullRepository(String branch, SyncContext context) throws IOException, GitAPIException {
        // git is already open and assigned by the caller (sync)
        
        // Create progress monitor
        JGitProgressMonitor progressMonitor = new JGitProgressMonitor(context);
        
        context.setStatusMessage("Fetching updates from remote...");
        
        PullCommand pullCommand = git.pull()
                .setProgressMonitor(progressMonitor)
                .setRemote("origin")
                .setRemoteBranchName(branch);
        
        EverLoad.LOGGER.info("Executing pull command for branch: {}", branch);
        
        try {
            var result = pullCommand.call();
            
            if (result.isSuccessful()) {
                EverLoad.LOGGER.info("Pull completed successfully");
                if (result.getFetchResult() != null) {
                    this.logFetchResult(result.getFetchResult());
                }
            } else {
                EverLoad.LOGGER.warn("Pull completed with issues: {}", result);
            }
        } catch (GitAPIException e) {
            EverLoad.LOGGER.error("Pull failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    private boolean isRepositoryInitialized() {
        File repoDir = repositoryDirectory.toFile();
        File gitDir = new File(repoDir, ".git");
		return repoDir.exists() && gitDir.exists() && gitDir.isDirectory();
    }

    /** Read the remote "origin" URL from an already-open Git instance. */
    private String readRemoteUrl(Git openGit) {
        try {
            StoredConfig config = openGit.getRepository().getConfig();
            return config.getString("remote", "origin", "url");
        } catch (Exception e) {
            EverLoad.LOGGER.warn("Failed to read remote URL from existing repository: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Check if the configured repository URL matches an existing remote URL.
     */
    private boolean isUrlMatching(String repositoryUrl, String existingUrl) {
        if (existingUrl == null) {
            return false;
        }
        return this.normalizeGitUrl(repositoryUrl).equalsIgnoreCase(this.normalizeGitUrl(existingUrl));
    }

    /**
     * Normalize a Git URL for comparison, removing trailing slashes and .git suffix
     * @param url The URL to normalize
     * @return The normalized URL
     */
    private String normalizeGitUrl(String url) {
        if (url == null) {
            return "";
        }
        String normalized = url.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith(".git")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized;
    }
    
    /**
     * Clean up repository directory. Throws if the directory still exists after deletion
     * (e.g. due to lingering Windows file locks), so callers can fail fast instead of
     * attempting a clone into a non-empty directory.
     */
    private void cleanupRepository() throws IOException {
        if (Files.exists(repositoryDirectory)) {
            EverLoad.LOGGER.info("Cleaning up failed repository at: {}", repositoryDirectory);
            this.deleteRecursively(repositoryDirectory);
            if (Files.exists(repositoryDirectory)) {
                throw new IOException(
                    "Failed to fully remove repository directory (file handles still open?): "
                    + repositoryDirectory);
            }
        }
    }

    private void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        if (!Files.isDirectory(path)) {
            deleteWithRetry(path);
            return;
        }
        try (var stream = Files.list(path)) {
            stream.forEach(child -> {
                try {
                    deleteRecursively(child);
                } catch (IOException e) {
                    EverLoad.LOGGER.error("Failed to delete: {}", child, e);
                }
            });
        }

        Files.delete(path); // Deletes the directory after emptying
    }

    /**
     * Delete a single file, retrying a few times on Windows where JGit may keep
     * memory-mapped pack files open briefly after {@link Git#close()}.
     * Also attempts to mark the file writable before deletion, since JGit sometimes
     * creates read-only pack files.
     */
    private void deleteWithRetry(Path path) throws IOException {
        final int MAX_RETRIES = 5;
        final long RETRY_DELAY_MS = 200;

        path.toFile().setWritable(true);

        IOException lastException = null;
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                Files.delete(path);
                return;
            } catch (java.nio.file.AccessDeniedException e) {
                lastException = e;
                EverLoad.LOGGER.warn("Delete attempt {}/{} failed (access denied), retrying: {}",
                        attempt + 1, MAX_RETRIES, path);
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
                path.toFile().setWritable(true);
            }
        }
        throw lastException;
    }

    private int countFiles(Path directory) {
        try (var stream = Files.walk(directory)) {
            return (int) stream
                .filter(Files::isRegularFile)
                .filter(p -> !p.toString().contains(".git"))
                .count();
        } catch (IOException e) {
            return 0;
        }
    }

    private void logFetchResult(FetchResult result) {
        EverLoad.LOGGER.info("Fetch result: {}", result.getMessages());
        result.getTrackingRefUpdates().forEach(update -> {
            EverLoad.LOGGER.info("  Updated ref: {} -> {}",
                update.getRemoteName(), 
                update.getResult());
        });
    }
    
    /**
     * Capture the current HEAD commit ID from an already-open Git instance.
     * @return The ObjectId of HEAD, or null if unavailable
     */
    private ObjectId captureHeadCommit(Git openGit) {
        try {
            ObjectId head = openGit.getRepository().resolve("HEAD");
            if (head != null) {
                EverLoad.LOGGER.info("Captured pre-sync HEAD: {}", head.getName());
            }
            return head;
        } catch (IOException e) {
            EverLoad.LOGGER.warn("Failed to capture HEAD commit: {}", e.getMessage());
            return null;
        }
    }

    public FileChanges getChangedFiles() {
        if (this.wasFreshClone) {
            // For fresh clones, list all files as "added"
            List<String> allFiles = this.collectAllRepositoryFiles();
            EverLoad.LOGGER.info("Fresh clone detected, {} files will be added", allFiles.size());
            return FileChanges.forFreshClone(allFiles);
        }
        
        if (this.preSyncHead == null || this.git == null) {
            EverLoad.LOGGER.info("No previous state available, returning empty changes");
            return FileChanges.empty();
        }
        
        try {
            ObjectId currentHead = this.git.getRepository().resolve("HEAD");
            
            if (currentHead == null) {
                EverLoad.LOGGER.warn("Current HEAD is null, returning empty changes");
                return FileChanges.empty();
            }
            
            if (this.preSyncHead.equals(currentHead)) {
                EverLoad.LOGGER.info("No changes detected (HEAD unchanged: {})", currentHead.getName());
                return FileChanges.empty();
            }
            
            EverLoad.LOGGER.info("Detecting changes between {} and {}", 
                    this.preSyncHead.getName(), currentHead.getName());
            
            List<DiffEntry> diffs = this.getDiffEntries(this.preSyncHead, currentHead);
            return this.categorizeDiffs(diffs);
            
        } catch (IOException | GitAPIException e) {
            EverLoad.LOGGER.error("Failed to detect changes: {}", e.getMessage(), e);
            return FileChanges.empty();
        }
    }

    public void revertToPreSyncState() throws IOException, GitAPIException {
        if (this.wasFreshClone) {
            // For fresh clones, we delete the entire repository
            EverLoad.LOGGER.info("Reverting fresh clone by deleting repository");
            this.cleanupRepository();
            return;
        }
        
        if (this.preSyncHead == null) {
            throw new IllegalStateException("No pre-sync state available to revert to");
        }
        
        if (this.git == null) {
            throw new IllegalStateException("Git repository is not open");
        }
        
        EverLoad.LOGGER.info("Reverting to pre-sync state: {}", this.preSyncHead.getName());
        
        this.git.reset()
            .setMode(ResetCommand.ResetType.HARD)
            .setRef(this.preSyncHead.getName())
            .call();
        
        EverLoad.LOGGER.info("Successfully reverted to commit: {}", this.preSyncHead.getName());
    }

    private List<DiffEntry> getDiffEntries(ObjectId oldHead, ObjectId newHead) 
            throws IOException, GitAPIException {
        
        Repository repository = this.git.getRepository();
        
        AbstractTreeIterator oldTreeParser = this.prepareTreeParser(repository, oldHead);
        AbstractTreeIterator newTreeParser = this.prepareTreeParser(repository, newHead);
        
        return this.git.diff()
            .setOldTree(oldTreeParser)
            .setNewTree(newTreeParser)
            .setShowNameAndStatusOnly(true)
            .call();
    }

    private AbstractTreeIterator prepareTreeParser(Repository repository, ObjectId commitId) 
            throws IOException {
        
        try (RevWalk walk = new RevWalk(repository)) {
            RevCommit commit = walk.parseCommit(commitId);
            ObjectId treeId = commit.getTree().getId();
            
            try (ObjectReader reader = repository.newObjectReader()) {
                CanonicalTreeParser treeParser = new CanonicalTreeParser();
                treeParser.reset(reader, treeId);
                return treeParser;
            }
        }
    }

    private FileChanges categorizeDiffs(List<DiffEntry> diffs) {
        List<String> added = new ArrayList<>();
        List<String> modified = new ArrayList<>();
        List<String> deleted = new ArrayList<>();
        List<String> renamed = new ArrayList<>();
        
        for (DiffEntry diff : diffs) {
            switch (diff.getChangeType()) {
                case ADD -> added.add(diff.getNewPath());
                case MODIFY -> modified.add(diff.getNewPath());
                case DELETE -> deleted.add(diff.getOldPath());
                case RENAME -> renamed.add(diff.getOldPath() + " -> " + diff.getNewPath());
                case COPY -> added.add(diff.getNewPath()); // Treat copies as additions
            }
        }
        
        EverLoad.LOGGER.info("Categorized changes: {} added, {} modified, {} deleted, {} renamed",
                added.size(), modified.size(), deleted.size(), renamed.size());
        
        return new FileChanges(added, modified, deleted, renamed, false);
    }

    private List<String> collectAllRepositoryFiles() {
        List<String> files = new ArrayList<>();
        
        try (var stream = Files.walk(this.repositoryDirectory)) {
            stream.filter(Files::isRegularFile)
              .filter(p -> !p.toString().contains(".git"))
              .forEach(p -> {
                  String relativePath = this.repositoryDirectory.relativize(p).toString();
                  // Normalize path separators for consistency
                  files.add(relativePath.replace('\\', '/'));
              });
        } catch (IOException e) {
            EverLoad.LOGGER.error("Failed to collect repository files: {}", e.getMessage(), e);
        }
        
        return files;
    }

    public void close() {
        if (git != null) {
            git.close();
            git = null;
        }
    }
    
    /**
     * JGit ProgressMonitor implementation that updates SyncContext
     */
    private static class JGitProgressMonitor implements ProgressMonitor {
        private final SyncContext context;
        private String taskName;
        private int totalWork;
        private int completed;
        
        public JGitProgressMonitor(SyncContext context) {
            this.context = context;
        }
        
        @Override
        public void start(int totalTasks) {
            EverLoad.LOGGER.info("Git operation started: {} tasks", totalTasks);
        }
        
        @Override
        public void beginTask(String title, int totalWork) {
            this.taskName = title;
            this.totalWork = totalWork;
            this.completed = 0;
            context.setStatusMessage(title);
            context.setProgress(0, totalWork > 0 ? totalWork : 100);
            EverLoad.LOGGER.info("Task started: {} (total work: {})", title, totalWork);
        }
        
        @Override
        public void update(int completed) {
            this.completed += completed;
            if (totalWork > 0) {
                context.setProgress(this.completed, totalWork);
            }
        }
        
        @Override
        public void endTask() {
            context.setProgress(totalWork, totalWork);
            EverLoad.LOGGER.info("Task completed: {}", taskName);
        }
        
        @Override
        public boolean isCancelled() {
            return context.getState() == SyncState.CANCELLED;
        }

        @Override
        public void showDuration(boolean b) {}
    }
}
