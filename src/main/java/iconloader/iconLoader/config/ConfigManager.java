package iconloader.iconLoader.config;

import iconloader.iconLoader.IconLoader;
import iconloader.iconLoader.glyph.GlyphRegistry;
import iconloader.iconLoader.util.FileUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {
    private static final String DEFAULT_TEXTURE_DIRECTORY = "icons";
    private static final String DEFAULT_OUTPUT_DIRECTORY = "output";
    private static final String DEFAULT_PACK_FILE_NAME = "IconsLoader-ResourcePack.zip";

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

        // 2. Unpack the bundled icons before the registry scans the folder
        FileUtils.extractDefaultTextures(plugin, getTextureDirectory());

        // 3. Reload glyph.yml
        glyphFile = new File(plugin.getDataFolder(), "glyph.yml");
        if (!glyphFile.exists()) {
            plugin.saveResource("glyph.yml", false);
        }
        glyphConfig = YamlConfiguration.loadConfiguration(glyphFile);

        // 4. Load glyphs
        glyphRegistry.load(plugin, glyphConfig, glyphFile);

        // 5. PRINT TO CONSOLE so you can see it actually reloaded!
        plugin.getLogger().info("Config Reloaded! URL: " + plugin.getConfig().getString("resource-pack.url"));
        plugin.getLogger().info("Pack Required: " + plugin.getConfig().getBoolean("resource-pack.required"));
        plugin.getLogger().info("Loaded " + glyphRegistry.getGlyphs().size() + " glyphs.");
    }

    public GlyphRegistry getGlyphRegistry() {
        return glyphRegistry;
    }

    public String getTextureDirectory() {
        return GlyphRegistry.resolveTextureDirectory(plugin);
    }

    public String getPackOutputDirectory() {
        String configured = plugin.getConfig().getString("pack-generation.output-directory", DEFAULT_OUTPUT_DIRECTORY);
        if (configured == null || configured.isBlank()) return DEFAULT_OUTPUT_DIRECTORY;
        String folder = configured.trim().replace('\\', '/').replaceAll("/+", "/");
        while (folder.startsWith("/")) folder = folder.substring(1);
        if (folder.isEmpty() || folder.contains("..")) {
            plugin.getLogger().warning("Ignoring pack-generation.output-directory '" + configured
                    + "'; falling back to '" + DEFAULT_OUTPUT_DIRECTORY + "'.");
            return DEFAULT_OUTPUT_DIRECTORY;
        }
        return folder;
    }

    public String getPackFileName() {
        String configured = plugin.getConfig().getString("pack-generation.file-name", DEFAULT_PACK_FILE_NAME);
        if (configured == null || configured.isBlank() || configured.contains("/") || configured.contains("\\")
                || configured.contains("..")) {
            return DEFAULT_PACK_FILE_NAME;
        }
        return configured.trim();
    }

    /** The single source of truth for where the built pack lives, shared by the generator and the HTTP server. */
    public File getPackFile() {
        return new File(new File(plugin.getDataFolder(), getPackOutputDirectory()), getPackFileName());
    }
}