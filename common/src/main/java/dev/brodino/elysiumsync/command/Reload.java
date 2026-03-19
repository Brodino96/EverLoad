package dev.brodino.elysiumsync.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.brodino.elysiumsync.ElysiumSync;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class Reload {

	public static LiteralArgumentBuilder<CommandSourceStack> getCommand() {
		return Commands.literal("status").executes(context -> {
			if (ElysiumSync.CONFIG.reload()) {
				CommandManager.sendMessage(context, "Config successfully reloaded", false);
				return 1;
			} else {
				CommandManager.sendMessage(context, "Failed to reload config", true);
				return 0;
			}
		});
	}
}
