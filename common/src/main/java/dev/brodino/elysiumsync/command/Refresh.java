package dev.brodino.elysiumsync.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.brodino.elysiumsync.ElysiumSync;
import dev.brodino.elysiumsync.screen.SyncProgressScreen;
import dev.brodino.elysiumsync.sync.SyncContext;
import dev.brodino.elysiumsync.sync.SyncScheduler;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class Refresh {
	
	public static LiteralArgumentBuilder<CommandSourceStack> getCommand() {
		return Commands.literal("refresh").executes(context -> {
			Minecraft mc = Minecraft.getInstance();

			// Check if already syncing
			if (SyncScheduler.isSyncInProgress()) {
				CommandManager.sendMessage(context, "Sync already in progress!", true);
				return 0;
			}

			if (!ElysiumSync.CONFIG.hasRepository() || ElysiumSync.CONFIG.isDisabled()) {
				CommandManager.sendMessage(context, "No repository configured! Edit config/elysiumsync.json", true);
				return 0;
			}

			CommandManager.sendMessage(context, "Starting manual sync...", false);

			// Disconnect from world/server if connected
			if (mc.level != null) {
				mc.level.disconnect();
				mc.clearLevel();
			}

			String repoUrl = ElysiumSync.CONFIG.getRepositoryUrl();
			String branch = ElysiumSync.CONFIG.getBranch();

			ElysiumSync.LOGGER.info("Manual refresh triggered: {} (branch: {})", repoUrl, branch);

			mc.execute(() -> {
				mc.setScreen(new SyncProgressScreen());

				SyncScheduler.startSync(repoUrl, branch, SyncContext.Type.MANUAL, () -> {
					ElysiumSync.LOGGER.info("Manual refresh completed successfully");
					mc.setScreen(null);
				}, (error) -> {
					ElysiumSync.LOGGER.error("Manual refresh failed", error);
				});
			});

			return 1;
		});
	}
}
