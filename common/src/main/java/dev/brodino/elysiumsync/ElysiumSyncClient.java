package dev.brodino.elysiumsync;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.brodino.elysiumsync.command.CommandManager;
import dev.brodino.elysiumsync.sync.SyncScheduler;
import dev.brodino.elysiumsync.sync.SyncState;

public final class ElysiumSyncClient {
    
    public static void initialize() {
        ElysiumSync.LOGGER.info("Initializing ElysiumSync client");

        ClientLifecycleEvent.CLIENT_STOPPING.register((minecraft) -> {
            ElysiumSync.shutdown();
        });

        CommandManager.initialize();
    }
}
