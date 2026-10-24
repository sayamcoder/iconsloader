package iconloader.iconLoader.chat;

import iconloader.iconLoader.IconLoader;
import iconloader.iconLoader.glyph.GlyphDefinition;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.TextReplacementConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatEmojiListener implements Listener {
    private final IconLoader plugin;
    private static final Key CUSTOM_FONT = Key.key("iconsloader:default");

    public ChatEmojiListener(IconLoader plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getConfig().getBoolean("chat.enabled", true)) return;

        Player player = event.getPlayer();
        Component originalMessage = event.message();
        Component newMessage = originalMessage;

        for (GlyphDefinition glyph : plugin.getConfigManager().getGlyphRegistry().getGlyphs().values()) {
            if (!glyph.getPermission().isEmpty() && !player.hasPermission(glyph.getPermission())) {
                continue;
            }

            String token = ":" + glyph.getName() + ":";

            // Apply custom font to the emoji component
            Component emojiComponent = Component.text(glyph.getCharacter())
                .font(CUSTOM_FONT)
                .decoration(TextDecoration.ITALIC, false);

            TextReplacementConfig config = TextReplacementConfig.builder()
                .matchLiteral(token)
                .replacement(emojiComponent)
                .build();

            newMessage = newMessage.replaceText(config);
        }

        if (!originalMessage.equals(newMessage)) {
            event.message(newMessage);
        }
    }
}