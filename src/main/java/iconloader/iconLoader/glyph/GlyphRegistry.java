package iconloader.iconLoader.glyph;

import iconloader.iconLoader.util.FileUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GlyphRegistry {
    private static final int PUA_START = 0xE000;
    private static final int PUA_END = 0xF8FF;
    private static final int FIRST_CUSTOM_MODEL_DATA = 10000;
    private static final String FALLBACK_TEXTURE_DIRECTORY = "icons";
    // Written into glyph.yml by older releases for every auto-registered icon.
    private static final List<String> LEGACY_PLACEHOLDER_ALIASES = List.of("example1", "example2");

    private final Map<String, GlyphDefinition> glyphs = new ConcurrentHashMap<>();

    public void load(JavaPlugin plugin, FileConfiguration config, File glyphFile) {
        glyphs.clear();
        ConfigurationSection section = config.getConfigurationSection("glyphs");
        if (section == null) section = config.createSection("glyphs");

        String textureDirectory = resolveTextureDirectory(plugin);
        long maxBytes = (long) (plugin.getConfig().getDouble("textures.max-file-size-mb", 4) * 1024 * 1024);
        int maxWidth = plugin.getConfig().getInt("textures.max-width", 256);
        int maxHeight = plugin.getConfig().getInt("textures.max-height", 256);
        boolean changed = false;

        for (String key : section.getKeys(false)) {
            ConfigurationSection glyphSection = section.getConfigurationSection(key);
            if (glyphSection == null) continue;

            String id = key.toLowerCase(Locale.ROOT);
            String texture = glyphSection.getString("texture", textureDirectory + "/" + key + ".png");
            if (texture.contains("..")) {
                plugin.getLogger().warning("Skipping glyph '" + key + "': texture path must not contain '..'.");
                continue;
            }

            if (!new File(plugin.getDataFolder(), texture).isFile()) {
                plugin.getLogger().warning("Glyph '" + key + "' points at '" + texture + "', which is missing from disk.");
            }

            List<String> aliases = readAliases(glyphSection);
            if (removePlaceholderAliases(glyphSection, aliases)) changed = true;

            String character = decodeCharacter(glyphSection.getString("character", ""));
            int customModelData = glyphSection.getInt("custom-model-data", 0);
            if (customModelData <= 0 || isCustomModelDataTaken(customModelData)) {
                customModelData = nextFreeCustomModelData();
                glyphSection.set("custom-model-data", customModelData);
                changed = true;
            }

            glyphs.put(id, new GlyphDefinition(
                    key,
                    texture,
                    character,
                    glyphSection.getString("permission", ""),
                    glyphSection.getInt("ascent", 8),
                    glyphSection.getInt("height", 8),
                    customModelData,
                    aliases,
                    glyphSection.getBoolean("chat-enabled", true)));
        }

        File iconsDir = new File(plugin.getDataFolder(), textureDirectory);
        File[] diskFiles = iconsDir.isDirectory()
                ? iconsDir.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".png"))
                : null;

        if (diskFiles != null) {
            Arrays.sort(diskFiles, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
            for (File file : diskFiles) {
                String reason = unusableTexture(file, maxBytes, maxWidth, maxHeight);
                if (reason != null) {
                    plugin.getLogger().warning("Skipping '" + file.getName() + "': " + reason);
                    continue;
                }

                String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
                if (glyphs.containsKey(id)) continue;

                String character = findNextAvailableCharacter();
                if (character == null) {
                    plugin.getLogger().warning("No free character left in the Private Use Area; skipping '" + file.getName() + "'.");
                    continue;
                }

                String texture = textureDirectory + "/" + file.getName();
                int customModelData = nextFreeCustomModelData();
                glyphs.put(id, new GlyphDefinition(id, texture, character, "", 8, 8, customModelData, new ArrayList<>(), true));

                section.set(id + ".texture", texture);
                section.set(id + ".character", String.format("\\u%04X", (int) character.charAt(0)));
                section.set(id + ".custom-model-data", customModelData);
                changed = true;
            }
        }

        if (changed) {
            try {
                ((YamlConfiguration) config).save(glyphFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not write the new glyph entries back to glyph.yml: " + e.getMessage());
            }
        }
    }

    /** Registry ids are lower-cased so that chat lookups and declared names always line up. */
    public String canonicalId(String name) {
        return name == null ? null : name.toLowerCase(Locale.ROOT);
    }

    /** Case-insensitive lookup by glyph name or alias. */
    public GlyphDefinition getByToken(String token) {
        if (token == null || token.isEmpty()) return null;
        GlyphDefinition byName = glyphs.get(canonicalId(token));
        if (byName != null) return byName;
        for (GlyphDefinition glyph : glyphs.values()) {
            for (String alias : glyph.getAliases()) {
                if (alias.equalsIgnoreCase(token)) return glyph;
            }
        }
        return null;
    }

    /**
     * Lookup that honours {@code chat.case-insensitive}. When matching is case sensitive the token has to
     * equal the declared name exactly, so ':HEART:' no longer resolves a glyph registered as 'heart'.
     */
    public GlyphDefinition findByToken(String token, boolean caseInsensitive) {
        if (token == null || token.isEmpty()) return null;
        if (caseInsensitive) return getByToken(token);
        for (GlyphDefinition glyph : glyphs.values()) {
            if (glyph.getName().equals(token)) return glyph;
            for (String alias : glyph.getAliases()) {
                if (alias.equals(token)) return glyph;
            }
        }
        return null;
    }

    public GlyphDefinition getByCharacter(String character) {
        for (GlyphDefinition glyph : glyphs.values()) {
            if (glyph.getCharacter().equals(character)) return glyph;
        }
        return null;
    }

    public java.util.List<GlyphDefinition> chatEnabledGlyphs() {
        List<GlyphDefinition> usable = new ArrayList<>();
        for (GlyphDefinition glyph : glyphs.values()) {
            if (glyph.isChatEnabled()) usable.add(glyph);
        }
        return usable;
    }

    public Map<String, GlyphDefinition> getGlyphs() { return glyphs; }

    public static String resolveTextureDirectory(JavaPlugin plugin) {
        String configured = plugin.getConfig().getString("textures.directory", FALLBACK_TEXTURE_DIRECTORY);
        if (configured == null || configured.isBlank()) return FALLBACK_TEXTURE_DIRECTORY;
        String folder = configured.trim().replace('\\', '/').replaceAll("/+", "/");
        if (folder.startsWith("/") || folder.contains("..")) {
            plugin.getLogger().warning("Ignoring textures.directory '" + configured + "'; falling back to '"
                    + FALLBACK_TEXTURE_DIRECTORY + "'.");
            return FALLBACK_TEXTURE_DIRECTORY;
        }
        return folder;
    }

    private String unusableTexture(File file, long maxBytes, int maxWidth, int maxHeight) {
        if (file.length() > maxBytes) {
            return file.length() / 1024 + " KB exceeds the " + maxBytes / 1024 / 1024 + " MB limit.";
        }
        int[] dimensions = FileUtils.readPngDimensions(file);
        if (dimensions == null) return "is not a readable PNG.";
        if (dimensions[0] > maxWidth || dimensions[1] > maxHeight) {
            return dimensions[0] + "x" + dimensions[1] + " exceeds the " + maxWidth + "x" + maxHeight + " limit.";
        }
        return null;
    }

    private List<String> readAliases(ConfigurationSection section) {
        List<String> aliases = new ArrayList<>();
        for (String alias : section.getStringList("aliases")) {
            if (alias == null) continue;
            String trimmed = alias.trim();
            if (trimmed.isEmpty() || LEGACY_PLACEHOLDER_ALIASES.contains(trimmed.toLowerCase(Locale.ROOT))) continue;
            aliases.add(trimmed);
        }
        return aliases;
    }

    private boolean removePlaceholderAliases(ConfigurationSection section, List<String> kept) {
        if (kept.size() == section.getStringList("aliases").size()) return false;
        section.set("aliases", kept.isEmpty() ? null : kept);
        return true;
    }

    private String decodeCharacter(String raw) {
        if (raw != null && raw.startsWith("\\u") && raw.length() > 2) {
            try {
                return String.valueOf((char) Integer.parseInt(raw.substring(2), 16));
            } catch (NumberFormatException ignored) {
                return raw;
            }
        }
        return raw == null || raw.isEmpty() ? String.valueOf((char) PUA_START) : raw;
    }

    private String findNextAvailableCharacter() {
        for (int code = PUA_START; code <= PUA_END; code++) {
            String candidate = String.valueOf((char) code);
            boolean taken = false;
            for (GlyphDefinition glyph : glyphs.values()) {
                if (glyph.getCharacter().equals(candidate)) {
                    taken = true;
                    break;
                }
            }
            if (!taken) return candidate;
        }
        return null;
    }

    private boolean isCustomModelDataTaken(int customModelData) {
        for (GlyphDefinition glyph : glyphs.values()) {
            if (glyph.getCustomModelData() == customModelData) return true;
        }
        return false;
    }

    private int nextFreeCustomModelData() {
        int candidate = FIRST_CUSTOM_MODEL_DATA;
        while (isCustomModelDataTaken(candidate)) candidate++;
        return candidate;
    }
}
