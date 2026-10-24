package iconloader.iconLoader.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import iconloader.iconLoader.IconLoader;
import iconloader.iconLoader.glyph.GlyphDefinition;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ResourcePackGenerator {
    private final IconLoader plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ResourcePackGenerator(IconLoader plugin) {
        this.plugin = plugin;
    }

    public void generate(CommandSender sender) {
        File outputDir = new File(plugin.getDataFolder(), "output");
        if (!outputDir.exists()) outputDir.mkdirs();

        File tempPackFile = new File(outputDir, "IconsLoader-ResourcePack.tmp");
        File finalPackFile = new File(outputDir, "IconsLoader-ResourcePack.zip");
        File iconsDir = new File(plugin.getDataFolder(), "textures/icons");
        File[] diskIcons = iconsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));

        if (diskIcons == null || diskIcons.length == 0) {
            sender.sendMessage("§cNo PNG files found in plugins/IconsLoader/textures/icons/!");
            return;
        }

        Map<String, GlyphDefinition> yamlGlyphs = new HashMap<>();
        for (GlyphDefinition def : plugin.getConfigManager().getGlyphRegistry().getGlyphs().values()) {
            yamlGlyphs.put(def.getName(), def);
        }

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempPackFile))) {
            int packFormat = getPackFormat(Bukkit.getMinecraftVersion());

            zos.putNextEntry(new ZipEntry("pack.mcmeta"));
            zos.write(("{\n  \"pack\": {\n    \"pack_format\": " + packFormat + ",\n    \"description\": \"IconsLoader Emojis\"\n  }\n}").getBytes());
            zos.closeEntry();

            JsonObject root = new JsonObject();
            JsonArray providerArray = new JsonArray();
            int charCode = 0xE000;

            for (File icon : diskIcons) {
                String key = icon.getName().replace(".png", "");
                GlyphDefinition def = yamlGlyphs.get(key);

                String character = def != null ? def.getCharacter() : String.valueOf((char) charCode++);
                int ascent = def != null ? def.getAscent() : 8;
                int height = def != null ? def.getHeight() : 8;

                JsonObject provider = new JsonObject();
                provider.addProperty("type", "bitmap");
                String zipPath = "font/icons/" + icon.getName();
                // Keep texture in minecraft namespace so other plugins can see it
                provider.addProperty("file", "minecraft:" + zipPath);
                provider.addProperty("ascent", ascent);
                provider.addProperty("height", height);

                JsonArray chars = new JsonArray();
                chars.add(character);
                provider.add("chars", chars);
                providerArray.add(provider);

                zos.putNextEntry(new ZipEntry("assets/minecraft/textures/" + zipPath));
                Files.copy(icon.toPath(), zos);
                zos.closeEntry();
            }

            root.add("providers", providerArray);

            // *** THE FIX: Write to assets/iconsloader/font/default.json ***
            zos.putNextEntry(new ZipEntry("assets/iconsloader/font/default.json"));
            zos.write(gson.toJson(root).getBytes());
            zos.closeEntry();

            zos.finish();
        } catch (Exception e) {
            sender.sendMessage("§cFailed to generate pack: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        try {
            Files.move(tempPackFile.toPath(), finalPackFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            sender.sendMessage("§aPack generated! Size: " + (finalPackFile.length() / 1024) + " KB (" + diskIcons.length + " textures packed)");
        } catch (IOException e) {
            sender.sendMessage("§cFailed to save final ZIP.");
        }
    }

    private int getPackFormat(String version) {
        if (version.startsWith("1.21.8")) return 55;
        if (version.startsWith("1.21.7")) return 54;
        if (version.startsWith("1.21.6")) return 53;
        if (version.startsWith("1.21.5")) return 46;
        if (version.startsWith("1.21.4")) return 45;
        return 34;
    }
}