package iconloader.iconLoader.config;

import iconloader.iconLoader.IconLoader;
import iconloader.iconLoader.glyph.GlyphRegistry;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {
    private final IconLoader plugin;
    private final GlyphRegistry glyphRegistry;
    private FileConfiguration glyphConfig;
    private File glyphFile;

    public ConfigManager(IconLoader plugin) {
        this.plugin = plugin;
        this.glyphRegistry = new GlyphRegistry();
    }

    public void load() {
        // 1. Force reload config.yml from disk
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        // 2. Reload glyph.yml
        glyphFile = new File(plugin.getDataFolder(), "glyph.yml");
        if (!glyphFile.exists()) {
            plugin.saveResource("glyph.yml", false);
        }
        glyphConfig = YamlConfiguration.loadConfiguration(glyphFile);

        // 3. Load glyphs
        glyphRegistry.load(plugin, glyphConfig, glyphFile);

        // 4. PRINT TO CONSOLE so you can see it actually reloaded!
        plugin.getLogger().info("Config Reloaded! URL: " + plugin.getConfig().getString("resource-pack.url"));
        plugin.getLogger().info("Pack Required: " + plugin.getConfig().getBoolean("resource-pack.required"));
        plugin.getLogger().info("Loaded " + glyphRegistry.getGlyphs().size() + " glyphs.");
    }

    public GlyphRegistry getGlyphRegistry() {
        return glyphRegistry;
    }
}