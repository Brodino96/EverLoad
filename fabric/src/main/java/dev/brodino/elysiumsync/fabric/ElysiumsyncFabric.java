package dev.brodino.elysiumsync.fabric;

import dev.brodino.elysiumsync.ElysiumSync;
import net.fabricmc.api.ModInitializer;

public final class ElysiumsyncFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		ElysiumSync.initialize();
	}
}
