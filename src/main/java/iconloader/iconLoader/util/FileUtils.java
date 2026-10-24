package iconloader.iconLoader.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils {

    public static void extractDefaultTextures(JavaPlugin plugin) {
        File texturesDir = new File(plugin.getDataFolder(), "textures/icons");
        if (!texturesDir.exists()) texturesDir.mkdirs();

        try {
            URI uri = plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            File jarFile = new File(uri);

            // 1. Handle Dev Environment (IntelliJ)
            if (jarFile.isDirectory()) {
                File devIcons = new File(jarFile, "icons");
                if (devIcons.exists()) {
                    copyFolder(devIcons, texturesDir);
                    plugin.getLogger().info("Extracted icons from development folder.");
                }
                return;
            }

            // 2. Handle Production Environment (.jar file)
            // ZipInputStream is much safer on Windows than FileSystems
            int extracted = 0;
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(jarFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName();

                    // Look specifically inside the 'icons/' folder for .png files
                    if (name.startsWith("icons/") && name.endsWith(".png") && !entry.isDirectory()) {
                        // Only extract the file name, not the full path
                        File outFile = new File(texturesDir, new File(name).getName());

                        if (!outFile.exists()) {
                            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                                byte[] buffer = new byte[1024];
                                int len;
                                while ((len = zis.read(buffer)) > 0) {
                                    fos.write(buffer, 0, len);
                                }
                            }
                            extracted++;
                        }
                    }
                }
            }

            if (extracted > 0) {
                plugin.getLogger().info("Extracted " + extracted + " default icons from JAR.");
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Could not extract icons from JAR: " + e.getMessage());
        }
    }

    private static void copyFolder(File source, File target) {
        if (source.isDirectory()) {
            if (!target.exists()) target.mkdirs();
            String[] files = source.list();
            if (files != null) {
                for (String file : files) {
                    copyFolder(new File(source, file), new File(target, file));
                }
            }
        } else {
            try {
                Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {}
        }
    }
}