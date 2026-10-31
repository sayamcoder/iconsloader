package iconloader.iconLoader.pack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import iconloader.iconLoader.IconLoader;
import iconloader.iconLoader.glyph.GlyphDefinition;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ResourcePackGenerator {
    private final IconLoader plugin;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public ResourcePackGenerator(IconLoader plugin) {
        this.plugin = plugin;
    }

    public void generate(CommandSender sender) {
        sender.sendMessage(plugin.getLangManager().getMessage("pack-generating"));

        CompletableFuture.runAsync(() -> {
            File outputDir = new File(plugin.getDataFolder(), "output");
            if (!outputDir.exists()) outputDir.mkdirs();

            File tempPackFile = new File(outputDir, "IconsLoader-ResourcePack.tmp");
            File finalPackFile = new File(outputDir, "IconsLoader-ResourcePack.zip");

            File iconsDir = new File(plugin.getDataFolder(), "icons");
            File[] diskIcons = iconsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));

            if (diskIcons == null || diskIcons.length == 0) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage("§cNo PNG files found in /icons/ folder!"));
                return;
            }

            Map<String, GlyphDefinition> yamlGlyphs = plugin.getConfigManager().getGlyphRegistry().getGlyphs();

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

                    // 1. Font JSON
                    JsonObject provider = new JsonObject();
                    provider.addProperty("type", "bitmap");
                    String fontPath = "font/icons/" + icon.getName();
                    provider.addProperty("file", "minecraft:" + fontPath);
                    provider.addProperty("ascent", ascent);
                    provider.addProperty("height", height);
                    JsonArray chars = new JsonArray();
                    chars.add(character);
                    provider.add("chars", chars);
                    providerArray.add(provider);

                    // 2. Copy to font folder
                    zos.putNextEntry(new ZipEntry("assets/minecraft/textures/" + fontPath));
                    Files.copy(icon.toPath(), zos);
                    zos.closeEntry();

                    // 3. Copy to item folder
                    String itemTexPath = "item/icons/" + icon.getName();
                    zos.putNextEntry(new ZipEntry("assets/minecraft/textures/" + itemTexPath));
                    Files.copy(icon.toPath(), zos);
                    zos.closeEntry();

                    // 4. Create Item Model JSON (FIXED: Removed .png from layer0)
                    JsonObject itemModel = new JsonObject();
                    itemModel.addProperty("parent", "item/generated");
                    JsonObject itemTex = new JsonObject();
                    // REMOVED ".png" HERE
                    itemTex.addProperty("layer0", "minecraft:item/icons/" + key);
                    itemModel.add("textures", itemTex);

                    zos.putNextEntry(new ZipEntry("assets/minecraft/models/item/icons/" + key + ".json"));
                    zos.write(gson.toJson(itemModel).getBytes());
                    zos.closeEntry();
                }

                root.add("providers", providerArray);
                zos.putNextEntry(new ZipEntry("assets/iconsloader/font/default.json"));
                zos.write(gson.toJson(root).getBytes());
                zos.closeEntry();

                zos.finish();
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage("§cFailed: " + e.getMessage()));
                return;
            }

            try {
                Files.move(tempPackFile.toPath(), finalPackFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                String sha1 = getFileChecksum(finalPackFile);
                FileConfiguration config = plugin.getConfig();
                config.set("resource-pack.sha1", sha1);
                plugin.saveConfig();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("§aPack generated async! Size: " + (finalPackFile.length() / 1024) + " KB (" + diskIcons.length + " textures)");
                    sender.sendMessage("§aNew SHA-1 automatically saved to config.yml.");
                });
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage("§cFailed to save ZIP or SHA-1."));
            }
        });
    }

    private String getFileChecksum(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] byteArray = new byte[1024];
            int bytesCount;
            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        }
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

        private int getPackFormat(String version) {
        try {
            if (version.startsWith("1.8")) return 1;
            if (version.startsWith("1.9") || version.startsWith("1.10")) return 2;
            if (version.startsWith("1.11") || version.startsWith("1.12")) return 3;
            if (version.startsWith("1.13") || version.startsWith("1.14")) return 4;
            if (version.startsWith("1.15") || version.startsWith("1.16.1")) return 5;
            if (version.startsWith("1.16")) return 6;
            if (version.startsWith("1.17")) return 7;
            if (version.startsWith("1.18")) return 8;
            if (version.startsWith("1.19.1") || version.startsWith("1.19.2")) return 9;
            if (version.startsWith("1.19.3")) return 12;
            if (version.startsWith("1.19.4")) return 13;
            if (version.startsWith("1.20.1") || version.startsWith("1.20")) return 15;
            if (version.startsWith("1.20.2")) return 18;
            if (version.startsWith("1.20.3") || version.startsWith("1.20.4")) return 22;
            if (version.startsWith("1.20.5") || version.startsWith("1.20.6")) return 32;
            if (version.startsWith("1.21.1") || version.startsWith("1.21")) return 34;
            if (version.startsWith("1.21.2") || version.startsWith("1.21.3")) return 42;
            if (version.startsWith("1.21.4")) return 46;
            if (version.startsWith("1.21.5")) return 55;
            if (version.startsWith("1.21.6")) return 63;
            if (version.startsWith("1.21.7") || version.startsWith("1.21.8")) return 64;

            // Future Versions (1.21.9, 1.22, 26.3 etc.)
            // We return a high number so it won't break.
            return 64;
        } catch (Exception ignored) {}
        return 34; // Default fallback
    }
}