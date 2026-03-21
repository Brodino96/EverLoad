package dev.brodino.everload;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private Path configPath;
	private Type data;

	public Config() {
		Path dataDirectory = Path.of("config");

		try {
			if (!Files.exists(dataDirectory)) {
				Files.createDirectories(dataDirectory);
			}
			this.configPath = dataDirectory.resolve(EverLoad.MOD_ID + ".json");
			this.load();
		} catch (IOException e) {
			EverLoad.LOGGER.error("Failed to load {}.json", EverLoad.MOD_ID);
		}
	}

	private void load() throws IOException {
		if (!Files.exists(this.configPath)) {
			this.data = this.getDefaults();
			this.save();
			return;
		}

		try (Reader reader = Files.newBufferedReader(this.configPath)) {
			this.data = GSON.fromJson(reader, Type.class);
			if (data == null) {
				this.data = this.getDefaults();
				this.save();
			}
		}
	}

	public boolean reload() {
		try {
			this.load();
			return true;
		} catch (IOException ignored) {
			return false;
		}
	}

	private void save() throws IOException {
		try (Writer writer = Files.newBufferedWriter(this.configPath)) {
			GSON.toJson(this.data, writer);
		}
	}

	private Type getDefaults() {
		return new Type();
	}

	private static class Type {
		public String repositoryUrl = "";
		public String branch = "main";
		public boolean enabled = true;
	}

	public String getRepositoryUrl() {
		return this.data.repositoryUrl;
	}

	public boolean hasRepository() {
		return this.data.repositoryUrl != null && !this.data.repositoryUrl.trim().isEmpty();
	}

	public String getBranch() {
		return this.data.branch;
	}

	public boolean isDisabled() {
		return !this.data.enabled;
	}
}
