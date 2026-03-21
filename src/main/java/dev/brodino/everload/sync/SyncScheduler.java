package dev.brodino.everload.sync;

import dev.brodino.everload.EverLoad;
import dev.brodino.everload.util.AsyncExecutor;
import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class SyncScheduler {
    
    private static SyncContext currentContext;
    private static GitSyncManager gitManager;
    private static FileSyncService fileService;
    private static CompletableFuture<Void> currentSync;
    
    /**
     * Start a sync operation with callbacks.
     * @param repositoryUrl The repository URL to sync from
     * @param branch The branch to sync
     * @param type The type of sync operation
     * @param onSuccess Callback on successful completion
     * @param onError Callback on error
     */
    public static void startSync(String repositoryUrl, String branch, SyncContext.Type type, Runnable onSuccess, Consumer<Exception> onError) {
        // Check if sync is already in progress
        if (currentContext != null && currentContext.getState().isActive()) {
            EverLoad.LOGGER.warn("Sync already in progress, ignoring new sync request");
            CompletableFuture.completedFuture(null);
            return;
        }
        
        EverLoad.LOGGER.info("Starting {} sync: {} (branch: {})", type, repositoryUrl, branch);

        currentContext = new SyncContext(repositoryUrl, branch, type);
        currentContext.setState(SyncState.IN_PROGRESS);

        try {
            gitManager = new GitSyncManager();
            fileService = new FileSyncService();
        } catch (Exception e) {
            EverLoad.LOGGER.error("Failed to initialize sync services", e);
            currentContext.setState(SyncState.FAILED);
            currentContext.setLastError(e);
            if (onError != null) {
                Minecraft.getInstance().execute(() -> onError.accept(e));
            }
            CompletableFuture.completedFuture(null);
            return;
        }

        currentSync = AsyncExecutor.execute(() -> {
            try {
                executeSync();

                currentContext.setState(SyncState.COMPLETED);
                EverLoad.LOGGER.info("Sync completed successfully in {}s",
                    currentContext.getElapsedSeconds());

                if (onSuccess != null) {
                    Minecraft.getInstance().execute(onSuccess);
                }
                
            } catch (Exception e) {
                currentContext.setState(SyncState.FAILED);
                currentContext.setLastError(e);
                EverLoad.LOGGER.error("Sync failed after {}s: {}",
                    currentContext.getElapsedSeconds(), e.getMessage(), e);

                if (onError != null) {
                    Minecraft.getInstance().execute(() -> onError.accept(e));
                }
            } finally {
                if (gitManager != null) {
                    gitManager.close();
                }
            }
        });

    }

    private static void executeSync() throws Exception {
        // Step 1: Git operations (clone or pull)
        currentContext.setStatusMessage("Syncing with repository...");
        gitManager.sync(
            currentContext.getRepositoryUrl(), 
            currentContext.getBranch(), 
            currentContext
        );
        
        // Check if cancelled
        if (currentContext.getState() == SyncState.CANCELLED) {
            EverLoad.LOGGER.info("Sync cancelled after git operation");
            return;
        }
        
        // Step 2: File copying
        currentContext.setStatusMessage("Copying files to instance...");
        fileService.syncFiles(currentContext);
        
        // Check if cancelled
        if (currentContext.getState() == SyncState.CANCELLED) {
            EverLoad.LOGGER.info("Sync cancelled after file copy");
            return;
        }
        
        currentContext.setStatusMessage("Sync completed");
    }
    
    /**
     * Cancel the current sync operation
     */
    public static void cancelSync() {
        if (currentContext != null && currentContext.getState().isActive()) {
            EverLoad.LOGGER.info("Cancelling sync operation");
            currentContext.setState(SyncState.CANCELLED);
            currentContext.setStatusMessage("Cancelled by user");
            
            if (currentSync != null) {
                currentSync.cancel(true);
            }
        }
    }

    public static SyncContext getCurrentContext() {
        return currentContext;
    }

    public static boolean isSyncInProgress() {
        return currentContext != null && currentContext.getState().isActive();
    }

    public static SyncState getCurrentState() {
        return currentContext != null ? currentContext.getState() : SyncState.IDLE;
    }

    /**
     * Perform a blocking sync operation. This method will block the calling thread
     * until the sync completes or fails. Use this for early startup sync before
     * other mods have a chance to load their scripts.
     * @param repositoryUrl The repository URL to sync from
     * @param branch The branch to sync
     * @param timeoutSeconds Maximum time to wait for sync to complete
     * @return true if sync completed successfully, false otherwise
     */
    public static boolean startBlockingSync(String repositoryUrl, String branch, int timeoutSeconds) {
        // Check if sync is already in progress
        if (currentContext != null && currentContext.getState().isActive()) {
            EverLoad.LOGGER.warn("Sync already in progress, ignoring blocking sync request");
            return false;
        }

        EverLoad.LOGGER.info("Starting BLOCKING sync: {} (branch: {})", repositoryUrl, branch);

        currentContext = new SyncContext(repositoryUrl, branch, SyncContext.Type.STARTUP);
        currentContext.setState(SyncState.IN_PROGRESS);

        try {
            gitManager = new GitSyncManager();
            fileService = new FileSyncService();
        } catch (Exception e) {
            EverLoad.LOGGER.error("Failed to initialize sync services", e);
            currentContext.setState(SyncState.FAILED);
            currentContext.setLastError(e);
            return false;
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> success = new AtomicReference<>(false);

        currentSync = AsyncExecutor.execute(() -> {
            try {
                executeSync();
                currentContext.setState(SyncState.COMPLETED);
                EverLoad.LOGGER.info("Blocking sync completed successfully in {}s", currentContext.getElapsedSeconds());
                success.set(true);
            } catch (Exception e) {
                currentContext.setState(SyncState.FAILED);
                currentContext.setLastError(e);
                EverLoad.LOGGER.error("Blocking sync failed after {}s: {}", currentContext.getElapsedSeconds(), e.getMessage(), e);
                success.set(false);
            } finally {
                if (gitManager != null) {
                    gitManager.close();
                }
                latch.countDown();
            }
        });

        try {
            boolean completed = latch.await(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                EverLoad.LOGGER.error("Blocking sync timed out after {}s", timeoutSeconds);
                cancelSync();
                return false;
            }
            return success.get();
        } catch (InterruptedException e) {
            EverLoad.LOGGER.error("Blocking sync interrupted", e);
            Thread.currentThread().interrupt();
            cancelSync();
            return false;
        }
    }
}
