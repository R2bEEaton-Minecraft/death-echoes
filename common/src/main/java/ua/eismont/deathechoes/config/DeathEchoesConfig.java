package ua.eismont.deathechoes.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ua.eismont.deathechoes.Constants;
import ua.eismont.deathechoes.platform.Services;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class DeathEchoesConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static DeathEchoesConfig INSTANCE;

    /**
     * How long ghost replays last in seconds (default 10).
     */
    public int replayDurationSeconds = 10;

    /**
     * Whether player names in death messages should be obfuscated with magic text (default true).
     */
    public boolean obfuscateDeathMessages = true;

    /**
     * Whether weapon/item names in death messages should also be obfuscated (default true).
     */
    public boolean obfuscateWeaponNames = true;

    /**
     * Whether mob names in death messages should also be obfuscated (default false).
     */
    public boolean obfuscateMobNames = false;

    public static DeathEchoesConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        Path configPath = Services.PLATFORM.getConfigDir().resolve("deathechoes.json");
        if (Files.exists(configPath)) {
            try (Reader reader = Files.newBufferedReader(configPath)) {
                INSTANCE = GSON.fromJson(reader, DeathEchoesConfig.class);
            } catch (Exception e) {
                Constants.LOG.error("Failed to load config, falling back to defaults", e);
            }
        }
        if (INSTANCE == null) {
            INSTANCE = new DeathEchoesConfig();
        }
        INSTANCE.sanitize();
        save();
    }

    public static void save() {
        if (INSTANCE == null) return;
        Path configPath = Services.PLATFORM.getConfigDir().resolve("deathechoes.json");
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (Exception e) {
            Constants.LOG.error("Failed to save config", e);
        }
    }

    public void sanitize() {
        if (replayDurationSeconds < 1) {
            replayDurationSeconds = 1;
        } else if (replayDurationSeconds > 120) {
            replayDurationSeconds = 120;
        }
    }

    public int getMaxFrames() {
        return Math.max(20, replayDurationSeconds * 20);
    }
}
