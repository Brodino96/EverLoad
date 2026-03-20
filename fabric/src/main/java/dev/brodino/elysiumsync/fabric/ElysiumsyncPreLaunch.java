package dev.brodino.elysiumsync.fabric;

import dev.brodino.elysiumsync.Config;
import dev.brodino.elysiumsync.sync.FileSyncService;
import dev.brodino.elysiumsync.sync.GitSyncManager;
import dev.brodino.elysiumsync.sync.SyncContext;
import dev.brodino.elysiumsync.util.AsyncExecutor;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PreLaunch entrypoint that runs BEFORE any other mod initialization.
 * This ensures that files are synced from the repository before KubeJS
 * or any other mod has a chance to load their scripts.
 */
public class ElysiumsyncPreLaunch implements PreLaunchEntrypoint {

    private static final Logger LOGGER = LoggerFactory.getLogger("elysiumsync");

    private static boolean preLaunchSyncPerformed = false;
    private static boolean preLaunchSyncSuccessful = false;
    
    @Override
    public void onPreLaunch() {
        LOGGER.info("ElysiumSync PreLaunch: Starting early sync to ensure files are ready before other mods");
        
        try {
            this.performPreLaunchSync();
        } catch (Exception e) {
            LOGGER.error("ElysiumSync PreLaunch: Failed to perform sync", e);
        }
    }
    
    private void performPreLaunchSync() {
        // Load config directly (can't use ElysiumSync.CONFIG as it may not be initialized yet)
        Config config = new Config();
        
        if (config.isDisabled()) {
            LOGGER.info("ElysiumSync PreLaunch: Sync is disabled, skipping");
            preLaunchSyncPerformed = true;
            return;
        }
        
        if (!config.hasRepository()) {
            LOGGER.info("ElysiumSync PreLaunch: No repository configured, skipping");
            preLaunchSyncPerformed = true;
            return;
        }
        
        String repoUrl = config.getRepositoryUrl();
        String branch = config.getBranch();
        
        if (repoUrl == null || repoUrl.trim().isEmpty()) {
            LOGGER.info("ElysiumSync PreLaunch: Repository URL is empty, skipping");
            preLaunchSyncPerformed = true;
            return;
        }
        
        LOGGER.info("ElysiumSync PreLaunch: Syncing from {} (branch: {})", repoUrl, branch);

        AsyncExecutor.initialize();
        
        GitSyncManager gitManager = null;
        try {
            gitManager = new GitSyncManager();
            FileSyncService fileService = new FileSyncService();

            SyncContext context = new SyncContext(repoUrl, branch, SyncContext.Type.STARTUP);
            
            // Step 1: Git operations (clone or pull)
            LOGGER.info("ElysiumSync PreLaunch: Performing git sync...");
            gitManager.sync(repoUrl, branch, context);
            
            // Step 2: File copying
            LOGGER.info("ElysiumSync PreLaunch: Copying files to instance...");
            fileService.syncFiles(context);
            
            LOGGER.info("ElysiumSync PreLaunch: Sync completed successfully in {}s", context.getElapsedSeconds());
            preLaunchSyncPerformed = true;
            preLaunchSyncSuccessful = true;
            
        } catch (Exception e) {
            LOGGER.error("ElysiumSync PreLaunch: Sync failed", e);
            preLaunchSyncPerformed = true;
            preLaunchSyncSuccessful = false;
        } finally {
            if (gitManager != null) {
                gitManager.close();
            }
        }
    }
    
    public static boolean wasPreLaunchSyncPerformed() {
        return preLaunchSyncPerformed;
    }
    
    public static boolean wasPreLaunchSyncSuccessful() {
        return preLaunchSyncSuccessful;
    }
}
