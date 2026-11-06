package iconloader.iconLoader.chat;

import iconloader.iconLoader.IconLoader;
import iconloader.iconLoader.glyph.GlyphDefinition;
import iconloader.iconLoader.util.EmojiTokenizer;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemEmojiListener implements Listener {
    private static final Key CUSTOM_FONT = Key.key("iconsloader:default");

    private final IconLoader plugin;

    public ItemEmojiListener(IconLoader plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAnvil(PrepareAnvilEvent event) {
        ItemStack result = event.getResult();
        if (result == null || !result.hasItemMeta() || !result.getItemMeta().hasDisplayName()) return;

        ItemMeta meta = result.getItemMeta();
        Component originalName = meta.displayName();
        boolean caseInsensitive = plugin.getConfig().getBoolean("chat.case-insensitive", true);

        Component newName = originalName.replaceText(TextReplacementConfig.builder()
                .match(EmojiTokenizer.pattern(plugin.getConfig()))
                .replacement((matchResult, builder) -> {
                    // chat-enabled only gates chat rendering; item names are always allowed.
                    GlyphDefinition glyph = plugin.getConfigManager().getGlyphRegistry()
                            .findByToken(matchResult.group(1), caseInsensitive);
                    if (glyph == null) return builder;
                    return Component.text(glyph.getCharacter())
                            .font(CUSTOM_FONT)
                            .decoration(TextDecoration.ITALIC, false);
                })
                .build());

        if (!originalName.equals(newName)) {
            meta.displayName(newName);
            result.setItemMeta(meta);
            event.setResult(result);
        }
    }
}
