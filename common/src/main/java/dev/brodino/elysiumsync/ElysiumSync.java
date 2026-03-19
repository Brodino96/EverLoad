package dev.brodino.elysiumsync;

import dev.brodino.elysiumsync.util.AsyncExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ElysiumSync {
	public static final String MOD_ID = "elysiumsync";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final Config CONFIG = new Config();

	public static void initialize() {
		LOGGER.info("Initializing ElysiumSync");
		AsyncExecutor.initialize();
	}
	
	public static void shutdown() {
		LOGGER.info("Shutting down ElysiumSync");
		AsyncExecutor.shutdown();
	}
}
