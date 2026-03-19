package dev.brodino.elysiumsync.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.brodino.elysiumsync.ElysiumSync;
import dev.brodino.elysiumsync.sync.SyncContext;
import dev.brodino.elysiumsync.sync.SyncScheduler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class Status {

	public static LiteralArgumentBuilder<CommandSourceStack> getCommand() {
		return Commands.literal("status").executes(context -> {
			SyncContext syncContext = SyncScheduler.getCurrentContext();

			CommandManager.sendMessage(context, "State: " + SyncScheduler.getCurrentState(), false);

			String repoUrl = ElysiumSync.CONFIG.getRepositoryUrl();
			String branch = ElysiumSync.CONFIG.getBranch();

			if (repoUrl != null && !repoUrl.isEmpty()) {
				CommandManager.sendMessage(context, "Repository: " + repoUrl, false);
				CommandManager.sendMessage(context, "Branch: " + branch, false);
			} else {
				CommandManager.sendMessage(context, "Repository: Not configured", false);
			}

			if (syncContext != null) {
				CommandManager.sendMessage(context, "Progress: " + syncContext.getProgressPercentage() + "%", false);
				CommandManager.sendMessage(context, "Files: " + syncContext.getFilesCopied() + "/" + syncContext.getTotalFiles(), false);
				CommandManager.sendMessage(context, "Elapsed: " + syncContext.getElapsedSeconds() + "s", false);
			}

			return 1;
		});
	}
}
