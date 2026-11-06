package iconloader.iconLoader.util;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtils {
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    /** Reads width/height straight from the IHDR chunk so no image decoding is needed at startup. */
    public static int[] readPngDimensions(File file) {
        byte[] header = new byte[24];
        try (InputStream in = new FileInputStream(file)) {
            if (in.read(header) < header.length) return null;
        } catch (IOException e) {
            return null;
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (header[i] != PNG_SIGNATURE[i]) return null;
        }
        if (header[12] != 'I' || header[13] != 'H' || header[14] != 'D' || header[15] != 'R') return null;
        int width = readBigInteger(header, 16);
        int height = readBigInteger(header, 20);
        if (width <= 0 || height <= 0) return null;
        return new int[]{width, height};
    }

    private static int readBigInteger(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24) | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8) | (bytes[offset + 3] & 0xFF);
    }

    public static void extractDefaultTextures(JavaPlugin plugin, String textureDirectory) {
        File iconsDir = new File(plugin.getDataFolder(), textureDirectory);
        if (!iconsDir.exists()) iconsDir.mkdirs();

        // MIGRATION: Move old files from /textures/icons/ to the configured folder
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
            plugin.getLogger().info("Migrated icons from /textures/icons/ to /" + textureDirectory + "/");
        }

        try {
            URI uri = plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            File jarFile = new File(uri);

            // Dev Environment
            if (jarFile.isDirectory()) {
                File devIcons = new File(jarFile, textureDirectory);
                if (devIcons.exists()) {
                    copyFolder(devIcons, iconsDir);
                }
                return;
            }

            // JAR Environment
            int extracted = 0;
            String prefix = textureDirectory + "/";
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(jarFile))) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (name.startsWith(prefix) && name.endsWith(".png") && !entry.isDirectory()) {
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
                plugin.getLogger().info("Extracted " + extracted + " default icons to /" + textureDirectory + "/ folder.");
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