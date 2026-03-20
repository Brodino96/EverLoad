package dev.brodino.elysiumsync.forge;

import dev.architectury.platform.forge.EventBuses;
import dev.brodino.elysiumsync.ElysiumSync;
import dev.brodino.elysiumsync.ElysiumSyncClient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(ElysiumSync.MOD_ID)
public final class ElysiumsyncForge {
	public ElysiumsyncForge() {
		EventBuses.registerModEventBus(ElysiumSync.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
		
		// Common initialization
		ElysiumSync.initialize();
		
		// Perform blocking sync during mod initialization
		// This ensures files are synced BEFORE KubeJS or other mods load their scripts
		ElysiumSync.performEarlySync();
		
		// Client-side initialization
		if (FMLEnvironment.dist == Dist.CLIENT) {
			ElysiumSyncClient.initialize();
		}
	}
}
