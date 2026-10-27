package iconloader.iconLoader.glyph;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlyphRegistry {
    private final Map<String, GlyphDefinition> glyphs = new ConcurrentHashMap<>();

    public void load(JavaPlugin plugin, FileConfiguration config, File glyphFile) {
        glyphs.clear();
        ConfigurationSection section = config.getConfigurationSection("glyphs");
        if (section == null) section = config.createSection("glyphs");

        int cmdCounter = 10000;

        for (String key : section.getKeys(false)) {
            ConfigurationSection glyphSection = section.getConfigurationSection(key);
            if (glyphSection == null) continue;

            String texture = glyphSection.getString("texture", key + ".png");
            if (texture.contains("..")) continue;

            String character = glyphSection.getString("character", "\\uE000");
            String permission = glyphSection.getString("permission", "");
            int ascent = glyphSection.getInt("ascent", 8);
            int height = glyphSection.getInt("height", 8);

            int cmd = glyphSection.getInt("custom-model-data", cmdCounter++);
            glyphSection.set("custom-model-data", cmd);

            if (character.startsWith("\\u")) {
                try { character = String.valueOf((char) Integer.parseInt(character.substring(2), 16)); } catch (Exception ignored) {}
            }

            glyphs.put(key, new GlyphDefinition(key, texture, character, permission, ascent, height, cmd));
        }

        // Scan the NEW /icons folder
        File iconsDir = new File(plugin.getDataFolder(), "icons");
        if (iconsDir.exists() && iconsDir.isDirectory()) {
            File[] files = iconsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            if (files != null) {
                boolean changed = false;
                for (File file : files) {
                    String key = file.getName().replace(".png", "");
                    if (!glyphs.containsKey(key)) {
                        String character = findNextAvailableChar();
                        String path = file.getName(); // Just the filename now
                        int cmd = cmdCounter++;

                        glyphs.put(key, new GlyphDefinition(key, path, character, "", 8, 8, cmd));

                        section.set(key + ".texture", path);
                        section.set(key + ".character", String.format("\\u%04X", (int) character.charAt(0)));
                        section.set(key + ".custom-model-data", cmd);
                        changed = true;
                    }
                }
                if (changed) {
                    try { ((YamlConfiguration) config).save(glyphFile); } catch (Exception ignored) {}
                }
            }
        }
    }

    private String findNextAvailableChar() {
        for (int i = 0xE000; i <= 0xF8FF; i++) {
            String c = String.valueOf((char) i);
            if (glyphs.values().stream().noneMatch(g -> g.getCharacter().equals(c))) return c;
        }
        return "?";
    }

    public Map<String, GlyphDefinition> getGlyphs() { return glyphs; }
}