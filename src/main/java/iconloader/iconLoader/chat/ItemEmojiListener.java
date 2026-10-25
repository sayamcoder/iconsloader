package iconloader.iconLoader.chat;

import iconloader.iconLoader.IconLoader;
import iconloader.iconLoader.glyph.GlyphDefinition;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
import java.util.regex.Pattern;

public class ItemEmojiListener implements Listener {
    private final IconLoader plugin;
    private static final Key CUSTOM_FONT = Key.key("iconsloader:default");
    private static final Pattern EMOJI_PATTERN = Pattern.compile(":([a-zA-Z0-9_]+):");

    public ItemEmojiListener(IconLoader plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result == null || !result.hasItemMeta() || !result.getItemMeta().hasDisplayName()) return;

        ItemMeta meta = result.getItemMeta();
        Component originalName = meta.displayName();
        Map<String, GlyphDefinition> glyphs = plugin.getConfigManager().getGlyphRegistry().getGlyphs();

        TextReplacementConfig config = TextReplacementConfig.builder()
            .match(EMOJI_PATTERN)
            .replacement((matchResult, builder) -> {
                GlyphDefinition glyph = glyphs.get(matchResult.group(1).toLowerCase());
                if (glyph != null) {
                    return Component.text(glyph.getCharacter()).font(CUSTOM_FONT);
                }
                return builder;
            })
            .build();

        Component newName = originalName.replaceText(config);
        if (!originalName.equals(newName)) {
            meta.displayName(newName);
            result.setItemMeta(meta);
            event.setResult(result);
        }
    }
}