package iconloader.iconLoader.util;

import iconloader.iconLoader.IconLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;

public class LangManager {
    private final IconLoader plugin;
    private FileConfiguration langConfig;
    private FileConfiguration fallbackConfig;

    public LangManager(IconLoader plugin) {
        this.plugin = plugin;
    }

    public void load() {
        String langName = plugin.getConfig().getString("language", "en");
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) langFolder.mkdirs();

        String[] defaultLangs = {"en", "es", "fr", "de", "pt", "ru", "zh", "hi", "ar", "tr", "id", "ja"};
        for (String lang : defaultLangs) {
            File f = new File(langFolder, lang + ".yml");
            if (!f.exists()) {
                try (InputStream in = plugin.getResource("lang/" + lang + ".yml")) {
                    if (in != null) java.nio.file.Files.copy(in, f.toPath());
                } catch (Exception ignored) {}
            }
        }

        File fallbackFile = new File(langFolder, "en.yml");
        fallbackConfig = YamlConfiguration.loadConfiguration(fallbackFile);

        File langFile = new File(langFolder, langName + ".yml");
        if (!langFile.isFile()) {
            plugin.getLogger().warning("language: '" + langName + "' has no matching file in lang/, falling back to English.");
            langName = "en";
            langFile = fallbackFile;
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    /** Raw translated template with {@code <prefix>} resolved, for places that need plain text such as GUI titles. */
    public String getString(String key) {
        String prefix = langConfig.getString("prefix", fallbackConfig.getString("prefix", ""));
        String msg = langConfig.getString(key, fallbackConfig.getString(key, "<red>Missing: " + key + "</red>"));
        return msg.replace("<prefix>", prefix);
    }

    public Component getMessage(String key) {
        return MiniMessage.miniMessage().deserialize(getString(key));
    }

    /**
     * @param placeholders alternating placeholder/value pairs, e.g. {@code "<size>", "42"}.
     */
    public Component getMessage(String key, String... placeholders) {
        String msg = getString(key);
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            msg = msg.replace(placeholders[i], placeholders[i + 1]);
        }
        return MiniMessage.miniMessage().deserialize(msg);
    }
}