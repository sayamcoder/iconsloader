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
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public Component getMessage(String key) {
        String prefix = langConfig.getString("prefix", fallbackConfig.getString("prefix", ""));
        String msg = langConfig.getString(key, fallbackConfig.getString(key, "<red>Missing: " + key));

        // FIX: Replace <prefix> literal string before parsing MiniMessage
        msg = msg.replace("<prefix>", prefix);

        return MiniMessage.miniMessage().deserialize(msg);
    }
}