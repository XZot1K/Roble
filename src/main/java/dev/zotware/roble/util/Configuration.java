package dev.zotware.roble.util;

import dev.zotware.roble.RoblePlugin;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Set;

public class Configuration extends YamlConfiguration {

    private final String configurationName;
    private File file;

    /**
     * Creates a configuration file in the YAML format.
     *
     * @param configurationName the configuration file name excluding the extension.
     */
    public Configuration(String configurationName) {
        this.configurationName = configurationName;
        setupFiles();
    }

    /**
     * Attempts to update the passed configuration (If an identical copy is found within the JAR, they will be compared).
     *
     * @param file        The file to update.
     * @param ignoredKeys The keys to ignore found in the configuration.
     */
    public static void update(File file, String... ignoredKeys) {
        if (file == null || !file.exists()) return;

        FileConfiguration resourceYaml = null;
        URL resourceURL = Configuration.class.getResource(file.getName());
        if (resourceURL != null)
            resourceYaml = YamlConfiguration.loadConfiguration(new File(resourceURL.getFile()))
                    .options().copyDefaults(true).copyHeader(true).configuration();

        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file).options().copyDefaults(true).copyHeader(true).configuration();
        ConfigurationSection currentSection = yaml.getConfigurationSection(""),
                resourceSection = (resourceYaml != null ? resourceYaml.getConfigurationSection("") : null);

        Set<String> resourceKeys = (resourceSection != null ? resourceSection.getKeys(true) : null),
                currentKeys = (currentSection != null ? currentSection.getKeys(true) : null);
        if (currentKeys != null) {
            currentKeys.forEach(key -> {
                if (resourceKeys != null && !resourceKeys.contains(key)) {
                    yaml.set(key, null);
                    return;
                }

                if (key.toLowerCase().contains("material")) {
                    final String materialString = currentSection.getString(key);
                    if (materialString != null && !materialString.isEmpty())
                        yaml.set(key, RoblePlugin.INSTANCE.predictCorrectValue(Material.class, materialString));
                } else if (key.toLowerCase().contains("sound")) {
                    final String soundString = currentSection.getString(key);
                    if (soundString != null && !soundString.isEmpty())
                        yaml.set(key, RoblePlugin.INSTANCE.predictCorrectValue(Sound.class, soundString));
                } else if (key.toLowerCase().contains("effect") || key.toLowerCase().contains("particle")) {
                    final String particleString = currentSection.getString(key);
                    if (particleString != null && !particleString.isEmpty())
                        yaml.set(key, RoblePlugin.INSTANCE.predictCorrectValue(Particle.class, particleString));
                }
            });

            if (resourceKeys != null)
                resourceKeys.forEach(key -> {
                    if (!currentKeys.contains(key) && Arrays.stream(ignoredKeys).noneMatch(ignoredKey ->
                            (key.toLowerCase().contains(ignoredKey.toLowerCase()))))
                        yaml.set(key, resourceSection.get(key));
                });
        }
    }

    private void setupFiles() {
        file = new File(RoblePlugin.INSTANCE.getDataFolder(), configurationName + ".yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();

            try {
                RoblePlugin.INSTANCE.saveResource(configurationName + ".yml", false);
            } catch (IllegalArgumentException e) {
                RoblePlugin.INSTANCE.getServer().getLogger().info(e.getMessage());
                try {
                    file.createNewFile();
                } catch (IOException e2) {
                    RoblePlugin.INSTANCE.getServer().getLogger().info("The file \"" + file.getName() + "\" was unable to be created.");
                    return;
                }
            }
        }

        try {load(file);} catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            RoblePlugin.INSTANCE.getServer().getLogger().warning(e.getMessage());
        }
    }

    /**
     * Saves the configuration file to the disk.
     */
    public void save() {
        if (file == null) setupFiles();

        try {
            save(file);
        } catch (IOException e) {
            e.printStackTrace();
            RoblePlugin.INSTANCE.getServer().getLogger().warning(e.getMessage());
        }
    }

    /**
     * Reloads the configuration file from disk.
     */
    public void reload() {
        try {load(file);} catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
            RoblePlugin.INSTANCE.getServer().getLogger().warning(e.getMessage());
        }

        loadConfiguration(file);
    }

    // getters & setters
    public File getFile() {
        return file;
    }

    public String getConfigurationName() {
        return configurationName;
    }
}