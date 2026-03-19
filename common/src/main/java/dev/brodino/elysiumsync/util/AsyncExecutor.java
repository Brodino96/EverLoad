package dev.brodino.elysiumsync.util;

import dev.brodino.elysiumsync.ElysiumSync;

import java.util.concurrent.*;

public class AsyncExecutor {
    
    private static final int THREAD_POOL_SIZE = 2;
    private static ExecutorService executorService;
    private static ScheduledExecutorService scheduledExecutor;

    public static void initialize() {
        executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE, r -> {
            Thread thread = new Thread(r, "ElysiumSync-Worker");
            thread.setDaemon(true);
            return thread;
        });

        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "ElysiumSync-Scheduler");
            thread.setDaemon(true);
            return thread;
        });

        ElysiumSync.LOGGER.info("AsyncExecutor initialized with {} worker threads", THREAD_POOL_SIZE);
    }
    
    /**
     * Execute an async task
     * @param task The task to execute
     */
    public static CompletableFuture<Void> execute(Runnable task) {
        ensureInitialized();
        return CompletableFuture.runAsync(task, executorService);
    }

    /**
     * Shutdown the executor service gracefully
     */
    public static void shutdown() {
        if (executorService == null) {
            return;
        }

		ElysiumSync.LOGGER.info("Shutting down AsyncExecutor");
		executorService.shutdown();
		scheduledExecutor.shutdown();

		try {
			if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
				executorService.shutdownNow();
			}
			if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
				scheduledExecutor.shutdownNow();
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
			scheduledExecutor.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
    
    /**
     * Check if the executor is initialized
     */
    private static void ensureInitialized() {
        if (executorService == null) {
            initialize();
        }
    }
}
