package dev.brodino.elysiumsync;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.brodino.elysiumsync.command.CommandManager;
import dev.brodino.elysiumsync.screen.SyncProgressScreen;
import dev.brodino.elysiumsync.sync.SyncContext;
import dev.brodino.elysiumsync.sync.SyncScheduler;
import net.minecraft.client.Minecraft;

public final class ElysiumSyncClient {
    
    public static void initialize() {
        ElysiumSync.LOGGER.info("Initializing ElysiumSync client");

        ClientLifecycleEvent.CLIENT_STOPPING.register((minecraft) -> {
            ElysiumSync.shutdown();
        });

        CommandManager.initialize();

        startupSync();
    }

    private static void startupSync() {
        Minecraft.getInstance().execute(() -> {
            try {
                if (ElysiumSync.CONFIG.isDisabled()) {
                    ElysiumSync.LOGGER.info("Sync is disabled, skipping startup sync");
                    return;
                }
                
                if (!ElysiumSync.CONFIG.hasRepository()) {
                    ElysiumSync.LOGGER.info("No repository configured, skipping startup sync");
                    return;
                }

                String repoUrl = ElysiumSync.CONFIG.getRepositoryUrl();
                String branch = ElysiumSync.CONFIG.getBranch();
                
                // Validate repository URL
                if (repoUrl == null || repoUrl.trim().isEmpty()) {
                    ElysiumSync.LOGGER.info("Repository URL is empty, skipping startup sync");
                    return;
                }
                
                ElysiumSync.LOGGER.info("Starting startup sync: {} (branch: {})", repoUrl, branch);

                Minecraft.getInstance().setScreen(new SyncProgressScreen());

                SyncScheduler.startSync(repoUrl, branch, SyncContext.Type.STARTUP, () -> {
                    ElysiumSync.LOGGER.info("Startup sync completed successfully");
                    Minecraft.getInstance().setScreen(null);
                }, (error) -> {
                    ElysiumSync.LOGGER.error("Startup sync failed, allowing game to continue", error);
                    Minecraft.getInstance().setScreen(null);
                });
            } catch (Exception e) {
                ElysiumSync.LOGGER.error("Failed to initiate startup sync", e);
            }
        });
    }
}
