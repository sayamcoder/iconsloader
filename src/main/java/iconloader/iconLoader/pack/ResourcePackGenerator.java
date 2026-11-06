package iconloader.iconLoader.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import iconloader.iconLoader.IconLoader;
import iconloader.iconLoader.glyph.GlyphDefinition;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ResourcePackGenerator {
    private static final String FONT_TEXTURE_FOLDER = "font/icons";
    private static final String ITEM_MODEL_FOLDER = "item/icons";
    private static final Pattern VERSION_NUMBERS = Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

    /**
     * The first Minecraft release to use each resource pack format, from
     * https://minecraft.wiki/w/Pack_format. Looked up by floor, so a patch release inherits the
     * format that introduced it and a whole release line no longer collapses onto one entry the way
     * startsWith("1.21") collapsed 1.21.4, 1.21.11 and 1.21 onto format 34.
     */
    private static final NavigableMap<Long, Integer> PACK_FORMATS = buildPackFormats();

    private final IconLoader plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ResourcePackGenerator(IconLoader plugin) {
        this.plugin = plugin;
    }

    private record Result(int textures, int itemOverrides, String overriddenItem) {}

    public void generate(CommandSender sender) {
        sender.sendMessage(plugin.getLangManager().getMessage("pack-generating"));

        File packFile = plugin.getConfigManager().getPackFile();
        File tempFile = new File(packFile.getParentFile(), packFile.getName() + ".tmp");

        CompletableFuture.runAsync(() -> {
            try {
                if (!packFile.getParentFile().isDirectory() && !packFile.getParentFile().mkdirs()) {
                    throw new IOException("Could not create " + packFile.getParentFile());
                }

                Result result = writeZip(tempFile);
                Files.move(tempFile.toPath(), packFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                String sha1 = getFileChecksum(packFile);
                long sizeKb = packFile.length() / 1024;

                // Config is not thread safe, so the hash write and the feedback run on the main thread.
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getConfig().set("resource-pack.sha1", sha1);
                    plugin.saveConfig();

                    sender.sendMessage(plugin.getLangManager().getMessage("pack-success",
                            "<size>", String.valueOf(sizeKb),
                            "<count>", String.valueOf(result.textures())));
                    sender.sendMessage("§7Saved to §f" + packFile.getAbsolutePath());

                    if (result.overriddenItem() != null) {
                        sender.sendMessage("§aBound " + result.itemOverrides() + " custom model data values to"
                                + " item/" + result.overriddenItem() + ".json");
                    } else {
                        sender.sendMessage("§7No item overrides written, so custom-model-data stays unused."
                                + " Set pack-generation.override-item to enable it.");
                    }
                });
            } catch (Exception e) {
                if (tempFile.exists() && !tempFile.delete()) {
                    plugin.getLogger().warning("Could not delete the leftover temp pack " + tempFile.getName() + ".");
                }
                String reason = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                plugin.getLogger().severe("Resource pack generation failed: " + reason);
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(
                        plugin.getLangManager().getMessage("pack-fail").append(Component.text(" " + reason))));
            }
        });
    }

    private Result writeZip(File tempFile) throws IOException {
        List<GlyphDefinition> glyphs = new ArrayList<>(plugin.getConfigManager().getGlyphRegistry().getGlyphs().values());
        glyphs.sort(Comparator.comparing(GlyphDefinition::getName, String.CASE_INSENSITIVE_ORDER));

        File textureDirectory = new File(plugin.getDataFolder(), plugin.getConfigManager().getTextureDirectory());
        String overrideItem = plugin.getConfig().getString("pack-generation.override-item", "paper").trim();

        JsonArray fontProviders = new JsonArray();
        JsonArray itemOverrides = new JsonArray();
        Set<String> usedCharacters = new HashSet<>();
        int writtenTextures = 0;

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile))) {
            zos.putNextEntry(new ZipEntry("pack.mcmeta"));
            zos.write(packMeta().getBytes());
            zos.closeEntry();

            for (GlyphDefinition glyph : glyphs) {
                File texture = resolveTextureFile(glyph, textureDirectory);
                if (texture == null) {
                    plugin.getLogger().warning("Glyph '" + glyph.getName()
                            + "' has no usable PNG on disk, so it is missing from the pack.");
                    continue;
                }
                if (!usedCharacters.add(glyph.getCharacter())) {
                    plugin.getLogger().warning("Character " + describe(glyph.getCharacter())
                            + " is shared by more than one glyph; only '" + glyph.getName()
                            + "' shows up in game.");
                }

                String fontTexturePath = FONT_TEXTURE_FOLDER + "/" + texture.getName();

                JsonObject provider = new JsonObject();
                provider.addProperty("type", "bitmap");
                provider.addProperty("file", "minecraft:" + fontTexturePath);
                provider.addProperty("ascent", glyph.getAscent());
                provider.addProperty("height", glyph.getHeight());
                JsonArray chars = new JsonArray();
                chars.add(glyph.getCharacter());
                provider.add("chars", chars);
                fontProviders.add(provider);

                // The PNG is written once, then referenced by both the font provider and the item model.
                zos.putNextEntry(new ZipEntry("assets/minecraft/textures/" + fontTexturePath));
                Files.copy(texture.toPath(), zos);
                zos.closeEntry();
                writtenTextures++;

                JsonObject itemModel = new JsonObject();
                itemModel.addProperty("parent", "item/generated");
                JsonObject itemTextures = new JsonObject();
                itemTextures.addProperty("layer0", "minecraft:" + stripExtension(fontTexturePath));
                itemModel.add("textures", itemTextures);

                String modelKey = glyph.getName().toLowerCase(Locale.ROOT);
                zos.putNextEntry(new ZipEntry("assets/minecraft/models/" + ITEM_MODEL_FOLDER + "/" + modelKey + ".json"));
                zos.write(gson.toJson(itemModel).getBytes());
                zos.closeEntry();

                if (!overrideItem.isEmpty()) {
                    JsonObject predicate = new JsonObject();
                    predicate.addProperty("custom_model_data", (double) glyph.getCustomModelData());
                    JsonObject override = new JsonObject();
                    override.add("predicate", predicate);
                    override.addProperty("model", "minecraft:" + ITEM_MODEL_FOLDER + "/" + modelKey);
                    itemOverrides.add(override);
                }
            }

            if (writtenTextures == 0) {
                throw new IOException("No usable PNG textures were found in " + textureDirectory);
            }

            JsonObject fontRoot = new JsonObject();
            fontRoot.add("providers", fontProviders);
            zos.putNextEntry(new ZipEntry("assets/iconsloader/font/default.json"));
            zos.write(gson.toJson(fontRoot).getBytes());
            zos.closeEntry();

            if (!itemOverrides.isEmpty()) {
                JsonObject baseModel = new JsonObject();
                baseModel.addProperty("parent", "minecraft:item/generated");
                JsonObject baseTextures = new JsonObject();
                baseTextures.addProperty("layer0", "minecraft:item/" + overrideItem);
                baseModel.add("textures", baseTextures);
                baseModel.add("overrides", itemOverrides);

                zos.putNextEntry(new ZipEntry("assets/minecraft/models/item/" + overrideItem + ".json"));
                zos.write(gson.toJson(baseModel).getBytes());
                zos.closeEntry();
            }

            zos.finish();
        }

        return new Result(writtenTextures, itemOverrides.size(), itemOverrides.isEmpty() ? null : overrideItem);
    }

    /**
     * glyph.yml has historically held both {@code icons/name.png} and a bare {@code name.png}, so both
     * shapes are tried before giving up on the texture.
     */
    private File resolveTextureFile(GlyphDefinition glyph, File textureDirectory) {
        String declared = glyph.getTexturePath();
        if (declared != null && !declared.isBlank()) {
            File relativeToDataFolder = new File(textureDirectory.getParentFile(), declared);
            if (relativeToDataFolder.isFile()) return relativeToDataFolder;

            File byFileName = new File(textureDirectory, new File(declared).getName());
            if (byFileName.isFile()) return byFileName;
        }

        File conventional = new File(textureDirectory, glyph.getName() + ".png");
        return conventional.isFile() ? conventional : null;
    }

    private String packMeta() {
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", getPackFormat(Bukkit.getMinecraftVersion()));
        pack.addProperty("description", "IconsLoader Emojis");
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        return gson.toJson(root);
    }

    private String stripExtension(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? path : path.substring(0, dot);
    }

    private String describe(String character) {
        return character.isEmpty() ? "(empty)" : String.format("\\u%04X", (int) character.charAt(0));
    }

    private String getFileChecksum(File file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[8192];
            try (FileInputStream fis = new FileInputStream(file)) {
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-1 is unavailable on this JVM", e);
        }
    }

    private static NavigableMap<Long, Integer> buildPackFormats() {
        NavigableMap<Long, Integer> formats = new TreeMap<>();
        formats.put(versionKey(1, 6, 1), 1);
        formats.put(versionKey(1, 9, 0), 2);
        formats.put(versionKey(1, 11, 0), 3);
        formats.put(versionKey(1, 13, 0), 4);
        formats.put(versionKey(1, 15, 0), 5);
        formats.put(versionKey(1, 16, 2), 6);
        formats.put(versionKey(1, 17, 0), 7);
        formats.put(versionKey(1, 18, 0), 8);
        formats.put(versionKey(1, 19, 0), 9);
        formats.put(versionKey(1, 19, 3), 12);
        formats.put(versionKey(1, 19, 4), 13);
        formats.put(versionKey(1, 20, 0), 15);
        formats.put(versionKey(1, 20, 2), 18);
        formats.put(versionKey(1, 20, 3), 22);
        formats.put(versionKey(1, 20, 5), 32);
        formats.put(versionKey(1, 21, 0), 34);
        formats.put(versionKey(1, 21, 2), 42);
        formats.put(versionKey(1, 21, 4), 46);
        formats.put(versionKey(1, 21, 5), 55);
        formats.put(versionKey(1, 21, 6), 63);
        formats.put(versionKey(1, 21, 7), 64);
        formats.put(versionKey(1, 21, 9), 69);
        formats.put(versionKey(1, 21, 11), 75);
        return formats;
    }

    private static long versionKey(int major, int minor, int patch) {
        return (long) major * 1_000_000L + minor * 1_000L + patch;
    }

    private int getPackFormat(String version) {
        Matcher matcher = VERSION_NUMBERS.matcher(version);
        if (!matcher.find()) {
            plugin.getLogger().warning("Could not read a version out of '" + version
                    + "', so the pack format may be wrong for this server.");
            return PACK_FORMATS.lastEntry().getValue();
        }
        long key = versionKey(Integer.parseInt(matcher.group(1)),
                parseGroup(matcher, 2), parseGroup(matcher, 3));

        Map.Entry<Long, Integer> match = PACK_FORMATS.floorEntry(key);
        if (match == null) {
            // Older than the first numbered format; format 1 is as far back as this goes.
            return PACK_FORMATS.firstEntry().getValue();
        }
        if (key > PACK_FORMATS.lastKey()) {
            plugin.getLogger().warning("Minecraft " + version + " is newer than the last pack format "
                    + "IconsLoader knows about, so it is labelled as format " + match.getValue()
                    + ". If players are told the pack is incompatible, update the table in ResourcePackGenerator.");
        }
        return match.getValue();
    }

    private static int parseGroup(Matcher matcher, int group) {
        String raw = matcher.group(group);
        return raw == null ? 0 : Integer.parseInt(raw);
    }
}