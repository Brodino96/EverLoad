package dev.brodino.elysiumsync.util;

import dev.architectury.platform.Platform;
import dev.brodino.elysiumsync.ElysiumSync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PathUtil {
    
    /**
     * Get the Minecraft game directory
     */
    public static Path getGameDirectory() {
        Path gameFolder = Platform.getGameFolder();
        if (gameFolder == null) {
            throw new IllegalStateException("Game folder is not yet initialized");
        }
        return gameFolder;
    }
    
    /**
     * Get the ElysiumSync repository directory
     */
    public static Path getRepositoryDirectory() {
        return getGameDirectory()
            .resolve(ElysiumSync.MOD_ID)
            .resolve("repo");
    }

    /**
     * Get the config directory
     */
    public static Path getConfigDirectory() {
        return getGameDirectory()
            .resolve("config");
    }
    
    /**
     * Ensure a directory exists, creating it if necessary
     * @param directory The directory
     */
    public static void ensureDirectoryExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
    }

    /**
     * Validate that a Git URL is somewhat reasonable
     */
    public static boolean isValidGitUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        
        // Basic validation - check if it looks like a git URL
        String lower = url.toLowerCase();
        return lower.startsWith("http://") 
            || lower.startsWith("https://") 
            || lower.startsWith("git://")
            || lower.startsWith("ssh://")
            || lower.startsWith("git@");
    }
}
