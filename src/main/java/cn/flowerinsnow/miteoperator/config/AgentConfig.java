package cn.flowerinsnow.miteoperator.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class AgentConfig {
    private static final String CONFIG_NAME = "miteoperator.properties";
    private static final AgentConfig INSTANCE = new AgentConfig();

    private boolean keepInventoryOnDeath = true;
    private boolean keepExperienceOnDeath = true;
    private boolean keepHungerOnDeath = true;
    private boolean dropExperienceOrbsOnDeath = false;
    private int respawnCountdownSeconds = 0;
    private boolean reconnectPenaltyEnabled = false;

    private AgentConfig() {
    }

    public static AgentConfig get() {
        return INSTANCE;
    }

    public static void load() {
        INSTANCE.loadInternal();
    }

    public boolean isKeepInventoryOnDeath() {
        return keepInventoryOnDeath;
    }

    public boolean isKeepExperienceOnDeath() {
        return keepExperienceOnDeath;
    }

    public boolean isKeepHungerOnDeath() {
        return keepHungerOnDeath;
    }

    public boolean isDropExperienceOrbsOnDeath() {
        return dropExperienceOrbsOnDeath;
    }

    public int getRespawnCountdownSeconds() {
        return respawnCountdownSeconds;
    }

    public boolean isReconnectPenaltyEnabled() {
        return reconnectPenaltyEnabled;
    }

    private void loadInternal() {
        Path path = Paths.get(System.getProperty("user.dir"), CONFIG_NAME);
        Properties props = new Properties();

        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                props.load(in);
            } catch (IOException ignored) {
                return;
            }
        }

        keepInventoryOnDeath = getBoolean(props, "death.keep_inventory", true);
        keepExperienceOnDeath = getBoolean(props, "death.keep_experience", true);
        keepHungerOnDeath = getBoolean(props, "death.keep_hunger", true);
        dropExperienceOrbsOnDeath = getBoolean(props, "death.drop_experience_orbs", false);
        respawnCountdownSeconds = getInt(props, "death.respawn_countdown_seconds", 0);
        reconnectPenaltyEnabled = getBoolean(props, "reconnect.penalty_enabled", false);

        if (!Files.exists(path)) {
            props.setProperty("death.keep_inventory", Boolean.toString(keepInventoryOnDeath));
            props.setProperty("death.keep_experience", Boolean.toString(keepExperienceOnDeath));
            props.setProperty("death.keep_hunger", Boolean.toString(keepHungerOnDeath));
            props.setProperty("death.drop_experience_orbs", Boolean.toString(dropExperienceOrbsOnDeath));
            props.setProperty("death.respawn_countdown_seconds", Integer.toString(respawnCountdownSeconds));
            props.setProperty("reconnect.penalty_enabled", Boolean.toString(reconnectPenaltyEnabled));
            try (OutputStream out = Files.newOutputStream(path)) {
                props.store(out, "MITEOperator settings");
            } catch (IOException ignored) {
                // Ignore config write failures to avoid breaking server start.
            }
        }
    }

    private static boolean getBoolean(Properties props, String key, boolean defaultValue) {
        String value = props.getProperty(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value.trim());
    }

    private static int getInt(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }
}
