package dev.brodino.elysiumsync.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.brodino.elysiumsync.ElysiumSync;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class CommandManager {

    public static void initialize() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> {
            register(dispatcher);
        });
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ElysiumSync.LOGGER.info("Registering ElysiumSync commands");
        
        dispatcher.register(Commands.literal("elysiumsync")
            .then(Refresh.getCommand())
            .then(Status.getCommand())
            .then(Reload.getCommand())
        );
    }

    public static void sendMessage(CommandContext<CommandSourceStack> context, String message, boolean isError) {
        Component component = Component.literal(message);
        context.getSource().sendSuccess(component, false);
    }
}
