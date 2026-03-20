package dev.brodino.elysiumsync.fabric;

import dev.brodino.elysiumsync.ElysiumSync;
import net.fabricmc.api.ModInitializer;

public final class ElysiumsyncFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		ElysiumSync.initialize();
		
		// Check if pre-launch sync was already performed
		if (ElysiumsyncPreLaunch.wasPreLaunchSyncPerformed()) {
			if (ElysiumsyncPreLaunch.wasPreLaunchSyncSuccessful()) {
				ElysiumSync.LOGGER.info("Pre-launch sync was successful, skipping initialization sync");
			} else {
				ElysiumSync.LOGGER.warn("Pre-launch sync failed, files may not be up to date");
			}
		} else {
			// Fallback: perform blocking sync during mod initialization if pre-launch didn't run
			// This ensures files are synced BEFORE KubeJS or other mods load their scripts
			ElysiumSync.LOGGER.info("Pre-launch sync was not performed, running sync now");
			ElysiumSync.performEarlySync();
		}
	}
}
