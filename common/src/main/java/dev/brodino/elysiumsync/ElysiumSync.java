package dev.brodino.elysiumsync;

import dev.brodino.elysiumsync.sync.SyncScheduler;
import dev.brodino.elysiumsync.util.AsyncExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ElysiumSync {
	private static final int BLOCKING_SYNC_TIMEOUT_SECONDS = 300; // 5 minutes
	public static final String MOD_ID = "elysiumsync";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Config CONFIG = new Config();

	public static void initialize() {
		LOGGER.info("Initializing ElysiumSync");
		AsyncExecutor.initialize();
	}
	
	/**
	 * Perform an early blocking sync during mod initialization
	 * This ensures that files are synced BEFORE other mods (like KubeJS) have a chance to load their scripts
	 * This method should be called from the mod's main initialization, not from client initialization, to ensure it runs early enough
	 */
	public static void performEarlySync() {
		if (CONFIG.isDisabled()) {
			LOGGER.info("Sync is disabled, skipping early sync");
			return;
		}
		
		if (!CONFIG.hasRepository()) {
			LOGGER.info("No repository configured, skipping early sync");
			return;
		}

		String repoUrl = CONFIG.getRepositoryUrl();
		String branch = CONFIG.getBranch();
		
		if (repoUrl == null || repoUrl.trim().isEmpty()) {
			LOGGER.info("Repository URL is empty, skipping early sync");
			return;
		}
		
		LOGGER.info("Performing early blocking sync to ensure files are ready before other mods load");
		LOGGER.info("Repository: {} (branch: {})", repoUrl, branch);
		
		boolean success = SyncScheduler.startBlockingSync(repoUrl, branch, BLOCKING_SYNC_TIMEOUT_SECONDS);
		
		if (success) {
			LOGGER.info("Early sync completed successfully - files are ready for other mods");
		} else {
			LOGGER.warn("Early sync failed or timed out - other mods may not have the latest files");
		}
	}
	
	public static void shutdown() {
		LOGGER.info("Shutting down ElysiumSync");
		AsyncExecutor.shutdown();
	}
}
