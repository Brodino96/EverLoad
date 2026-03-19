package dev.brodino.elysiumsync.fabric.client;

import dev.brodino.elysiumsync.ElysiumSyncClient;
import net.fabricmc.api.ClientModInitializer;

public final class ElysiumsyncFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ElysiumSyncClient.initialize();
	}
}
