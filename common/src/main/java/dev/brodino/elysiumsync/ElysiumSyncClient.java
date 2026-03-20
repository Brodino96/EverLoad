package dev.brodino.elysiumsync;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.brodino.elysiumsync.command.CommandManager;

public final class ElysiumSyncClient {
    
    public static void initialize() {
        ElysiumSync.LOGGER.info("Initializing ElysiumSync client");

        ClientLifecycleEvent.CLIENT_STOPPING.register((minecraft) -> {
            ElysiumSync.shutdown();
        });

        CommandManager.initialize();
    }
}
