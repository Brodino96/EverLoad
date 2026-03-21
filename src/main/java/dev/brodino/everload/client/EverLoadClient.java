package dev.brodino.everload.client;

import dev.brodino.everload.CommandManager;
import dev.brodino.everload.EverLoad;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public final class EverLoadClient implements ClientModInitializer {
    
    @Override
    public void onInitializeClient() {
        initialize();
    }
    
    public static void initialize() {
        EverLoad.LOGGER.info("Initializing EverLoad client");

        ClientLifecycleEvents.CLIENT_STOPPING.register((client) -> {
            EverLoad.shutdown();
        });

        CommandManager.initialize();
    }
}
