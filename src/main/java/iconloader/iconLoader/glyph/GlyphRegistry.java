package iconloader.iconLoader.glyph;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GlyphRegistry {
    private final Map<String, GlyphDefinition> glyphs = new HashMap<>();

    public void load(JavaPlugin plugin, FileConfiguration config, File glyphFile) {
        glyphs.clear();
        ConfigurationSection section = config.getConfigurationSection("glyphs");
        if (section == null) {
            section = config.createSection("glyphs");
        }

        // 1. Load existing from YAML
        for (String key : section.getKeys(false)) {
            ConfigurationSection glyphSection = section.getConfigurationSection(key);
            if (glyphSection == null) continue;

            String texture = glyphSection.getString("texture", "icons/" + key + ".png");
            String character = glyphSection.getString("character", "\uE000");
            String permission = glyphSection.getString("permission", "");
            int ascent = glyphSection.getInt("ascent", 8);
            int height = glyphSection.getInt("height", 8);

            if (character.startsWith("\\u")) {
                try {
                    character = String.valueOf((char) Integer.parseInt(character.substring(2), 16));
                } catch (NumberFormatException ignored) {}
            }

            glyphs.put(key, new GlyphDefinition(key, texture, character, permission, ascent, height));
        }

        // 2. Auto-discover new files in textures/icons/
        File texturesDir = new File(plugin.getDataFolder(), "textures/icons");
        if (texturesDir.exists() && texturesDir.isDirectory()) {
            File[] files = texturesDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            if (files != null) {
                boolean changed = false;
                for (File file : files) {
                    String key = file.getName().replace(".png", "");
                    if (!glyphs.containsKey(key)) {
                        // Auto-register new icon
                        String character = findNextAvailableChar();
                        String path = "icons/" + file.getName();

                        GlyphDefinition def = new GlyphDefinition(key, path, character, "", 8, 8);
                        glyphs.put(key, def);

                        // Save to YAML
                        section.set(key + ".texture", path);
                        section.set(key + ".character", String.format("\\u%04X", (int) character.charAt(0)));
                        section.set(key + ".ascent", 8);
                        section.set(key + ".height", 8);
                        changed = true;
                        plugin.getLogger().info("Auto-registered new glyph: " + key + " -> " + character);
                    }
                }
                if (changed) {
                    try {
                        ((YamlConfiguration) config).save(glyphFile);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Could not save new glyphs to glyph.yml");
                    }
                }
            }
        }
    }

    private String findNextAvailableChar() {
        // Private Use Area starts at E000 and goes up to F8FF
        for (int i = 0xE000; i <= 0xF8FF; i++) {
            String c = String.valueOf((char) i);
            boolean used = glyphs.values().stream().anyMatch(g -> g.getCharacter().equals(c));
            if (!used) return c;
        }
        return "?"; // Out of space
    }

    public Map<String, GlyphDefinition> getGlyphs() {
        return glyphs;
    }
}