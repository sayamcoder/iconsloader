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
            File iconsDir = new File(plugin.getDataFolder(), "textures/icons");
            File[] diskIcons = iconsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));

            if (diskIcons == null || diskIcons.length == 0) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage("§cNo PNG files found!"));
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

                JsonObject paperModel = new JsonObject();
                paperModel.addProperty("parent", "item/generated");
                JsonObject textures = new JsonObject();
                textures.addProperty("layer0", "item/paper");
                paperModel.add("textures", textures);
                JsonArray overrides = new JsonArray();

                int charCode = 0xE000;

                for (File icon : diskIcons) {
                    String key = icon.getName().replace(".png", "");
                    GlyphDefinition def = yamlGlyphs.get(key);

                    String character = def != null ? def.getCharacter() : String.valueOf((char) charCode++);
                    int ascent = def != null ? def.getAscent() : 8;
                    int height = def != null ? def.getHeight() : 8;
                    int cmd = def != null ? def.getCustomModelData() : 10000;

                    // Font JSON
                    JsonObject provider = new JsonObject();
                    provider.addProperty("type", "bitmap");
                    String zipPath = "font/icons/" + icon.getName();
                    provider.addProperty("file", "minecraft:" + zipPath);
                    provider.addProperty("ascent", ascent);
                    provider.addProperty("height", height);
                    JsonArray chars = new JsonArray();
                    chars.add(character);
                    provider.add("chars", chars);
                    providerArray.add(provider);

                    // Copy to font folder
                    zos.putNextEntry(new ZipEntry("assets/minecraft/textures/" + zipPath));
                    Files.copy(icon.toPath(), zos);
                    zos.closeEntry();

                    // Copy to item folder (for GUI)
                    String itemZipPath = "item/icons/" + icon.getName();
                    zos.putNextEntry(new ZipEntry("assets/minecraft/textures/" + itemZipPath));
                    Files.copy(icon.toPath(), zos);
                    zos.closeEntry();

                    // Create Item Model JSON
                    JsonObject itemModel = new JsonObject();
                    itemModel.addProperty("parent", "item/generated");
                    JsonObject itemTex = new JsonObject();
                    itemTex.addProperty("layer0", "minecraft:" + itemZipPath);
                    itemModel.add("textures", itemTex);

                    zos.putNextEntry(new ZipEntry("assets/minecraft/models/item/icons/" + key + ".json"));
                    zos.write(gson.toJson(itemModel).getBytes());
                    zos.closeEntry();

                    // Add to paper.json overrides
                    JsonObject override = new JsonObject();
                    JsonObject predicate = new JsonObject();
                    predicate.addProperty("custom_model_data", cmd);
                    override.add("predicate", predicate);
                    override.addProperty("model", "minecraft:item/icons/" + key);
                    overrides.add(override);
                }

                // Save Font JSON
                root.add("providers", providerArray);
                zos.putNextEntry(new ZipEntry("assets/iconsloader/font/default.json"));
                zos.write(gson.toJson(root).getBytes());
                zos.closeEntry();

                // Save Paper Model JSON
                paperModel.add("overrides", overrides);
                zos.putNextEntry(new ZipEntry("assets/minecraft/models/item/paper.json"));
                zos.write(gson.toJson(paperModel).getBytes());
                zos.closeEntry();

                zos.finish();
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage("§cFailed: " + e.getMessage()));
                return;
            }

            try {
                Files.move(tempPackFile.toPath(), finalPackFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                // AUTO-SHA GENERATION
                String sha1 = getFileChecksum(finalPackFile);
                FileConfiguration config = plugin.getConfig();
                config.set("resource-pack.sha1", sha1);
                plugin.saveConfig();

                Bukkit.getScheduler().runTask(plugin, () -> {
                    sender.sendMessage("§aPack generated async! Size: " + (finalPackFile.length() / 1024) + " KB (" + diskIcons.length + " textures)");
                    sender.sendMessage("§aNew SHA-1 automatically saved to config.yml: §f" + sha1);
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
        if (version.startsWith("1.21.8")) return 55;
        if (version.startsWith("1.21.7")) return 54;
        if (version.startsWith("1.21.6")) return 53;
        if (version.startsWith("1.21.5")) return 46;
        if (version.startsWith("1.21.4")) return 45;
        return 34;
    }
}