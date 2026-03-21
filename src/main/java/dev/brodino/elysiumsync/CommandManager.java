package dev.brodino.elysiumsync;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.brodino.elysiumsync.screen.SyncProgressScreen;
import dev.brodino.elysiumsync.sync.SyncContext;
import dev.brodino.elysiumsync.sync.SyncScheduler;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class CommandManager {

    public static void initialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            register(dispatcher);
        });
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ElysiumSync.LOGGER.info("Registering ElysiumSync commands");
        
        dispatcher.register(Commands.literal("elysiumsync")
            .then(getRefreshCommand())
            .then(getStatusCommand())
            .then(getReloadCommand())
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> getRefreshCommand() {
        return Commands.literal("refresh").executes(context -> {
            Minecraft mc = Minecraft.getInstance();

            // Check if already syncing
            if (SyncScheduler.isSyncInProgress()) {
                sendMessage(context, "Sync already in progress!", true);
                return 0;
            }

            if (!ElysiumSync.CONFIG.hasRepository() || ElysiumSync.CONFIG.isDisabled()) {
                sendMessage(context, "No repository configured! Edit config/elysiumsync.json", true);
                return 0;
            }

            sendMessage(context, "Starting manual sync...", false);

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

    private static LiteralArgumentBuilder<CommandSourceStack> getStatusCommand() {
        return Commands.literal("status").executes(context -> {
            SyncContext syncContext = SyncScheduler.getCurrentContext();

            sendMessage(context, "State: " + SyncScheduler.getCurrentState(), false);

            String repoUrl = ElysiumSync.CONFIG.getRepositoryUrl();
            String branch = ElysiumSync.CONFIG.getBranch();

            if (repoUrl != null && !repoUrl.isEmpty()) {
                sendMessage(context, "Repository: " + repoUrl, false);
                sendMessage(context, "Branch: " + branch, false);
            } else {
                sendMessage(context, "Repository: Not configured", false);
            }

            if (syncContext != null) {
                sendMessage(context, "Progress: " + syncContext.getProgressPercentage() + "%", false);
                sendMessage(context, "Files: " + syncContext.getFilesCopied() + "/" + syncContext.getTotalFiles(), false);
                sendMessage(context, "Elapsed: " + syncContext.getElapsedSeconds() + "s", false);
            }

            return 1;
        });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> getReloadCommand() {
        return Commands.literal("reload").executes(context -> {
            if (ElysiumSync.CONFIG.reload()) {
                sendMessage(context, "Config successfully reloaded", false);
                return 1;
            } else {
                sendMessage(context, "Failed to reload config", true);
                return 0;
            }
        });
    }

    public static void sendMessage(CommandContext<CommandSourceStack> context, String message, boolean isError) {
        Component component = Component.literal(message);
        context.getSource().sendSuccess(component, false);
    }
}
