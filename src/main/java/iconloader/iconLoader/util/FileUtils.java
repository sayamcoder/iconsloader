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
        // NEW simple folder: plugins/IconsLoader/icons/
        File iconsDir = new File(plugin.getDataFolder(), "icons");
        if (!iconsDir.exists()) iconsDir.mkdirs();

        // MIGRATION: Move old files from /textures/icons/ to /icons/
        File oldTexturesDir = new File(plugin.getDataFolder(), "textures/icons");
        if (oldTexturesDir.exists() && oldTexturesDir.isDirectory()) {
            File[] oldFiles = oldTexturesDir.listFiles();
            if (oldFiles != null) {
                for (File f : oldFiles) {
                    try {
                        Files.move(f.toPath(), new File(iconsDir, f.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException ignored) {}
                }
            }
            oldTexturesDir.delete();
            File parent = oldTexturesDir.getParentFile();
            if (parent != null) parent.delete();
            plugin.getLogger().info("Migrated icons from /textures/icons/ to /icons/");
        }

        try {
            URI uri = plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            File jarFile = new File(uri);

            // Dev Environment
            if (jarFile.isDirectory()) {
                File devIcons = new File(jarFile, "icons");
                if (devIcons.exists()) {
                    copyFolder(devIcons, iconsDir);
                }
                return;
            }

            // JAR Environment
            int extracted = 0;
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(jarFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (name.startsWith("icons/") && name.endsWith(".png") && !entry.isDirectory()) {
                        File outFile = new File(iconsDir, new File(name).getName());
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
                plugin.getLogger().info("Extracted " + extracted + " default icons to /icons/ folder.");
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Could not extract icons: " + e.getMessage());
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