package dev.brodino.elysiumsync.client;

import dev.brodino.elysiumsync.CommandManager;
import dev.brodino.elysiumsync.ElysiumSync;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public final class ElysiumSyncClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        initialize();
    }
    
    public static void initialize() {
        ElysiumSync.LOGGER.info("Initializing ElysiumSync client");

        ClientLifecycleEvents.CLIENT_STOPPING.register((client) -> {
            ElysiumSync.shutdown();
        });

        CommandManager.initialize();
    }
}
