package iconloader.iconLoader.integration;

import iconloader.iconLoader.IconLoader;
import iconloader.iconLoader.glyph.GlyphDefinition;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PlaceholderApiIntegration extends PlaceholderExpansion {
    private final IconLoader plugin;

    public PlaceholderApiIntegration(IconLoader plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "iconsloader"; }

    @Override
    public @NotNull String getAuthor() { return "Sayam"; }

    @Override
    public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // Usage: %iconsloader_heart%
        if (params.startsWith("replace_")) {
            String textToReplace = params.substring("replace_".length());
            return replaceGlyphs(textToReplace.replace("_", " "));
        }

        GlyphDefinition def = plugin.getConfigManager().getGlyphRegistry().getGlyphs().get(params.toLowerCase());
        if (def != null) {
            // Return the MiniMessage format so TAB/Holograms using MiniMessage will render it correctly
            return "<font:iconsloader:default>" + def.getCharacter() + "</font>";
        }

        return null;
    }

    private String replaceGlyphs(String input) {
        String result = input;
        for (GlyphDefinition def : plugin.getConfigManager().getGlyphRegistry().getGlyphs().values()) {
            result = result.replace(":" + def.getName() + ":", "<font:iconsloader:default>" + def.getCharacter() + "</font>");
        }
        return result;
    }
}