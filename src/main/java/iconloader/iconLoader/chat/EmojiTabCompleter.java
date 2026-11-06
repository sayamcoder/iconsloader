package iconloader.iconLoader.chat;

import iconloader.iconLoader.IconLoader;
import iconloader.iconLoader.glyph.GlyphDefinition;
import iconloader.iconLoader.util.EmojiTokenizer;
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class EmojiTabCompleter implements Listener {
    private final IconLoader plugin;

    public EmojiTabCompleter(IconLoader plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTabComplete(AsyncTabCompleteEvent event) {
        String buffer = event.getBuffer();

        // Only trigger if they are typing in chat (not commands) and using a colon
        if (buffer.startsWith("/") || !buffer.contains(":")) return;

        String tokenFormat = EmojiTokenizer.sanitizeFormat(plugin.getConfig().getString("chat.token-format",
                EmojiTokenizer.DEFAULT_FORMAT));
        int lastSpace = buffer.lastIndexOf(' ');
        String currentWord = buffer.substring(lastSpace + 1);

        // If they haven't started with ':' or already closed it, ignore
        if (!currentWord.startsWith(":") || currentWord.endsWith(":")) return;

        String search = currentWord.substring(1).toLowerCase();
        Set<String> completions = new LinkedHashSet<>();

        for (GlyphDefinition glyph : plugin.getConfigManager().getGlyphRegistry().chatEnabledGlyphs()) {
            String displayName = glyph.getName();
            if (displayName.toLowerCase().startsWith(search)) {
                completions.add(EmojiTokenizer.renderToken(displayName, tokenFormat));
            }
            for (String alias : glyph.getAliases()) {
                if (alias.toLowerCase().startsWith(search)) {
                    completions.add(EmojiTokenizer.renderToken(alias, tokenFormat));
                }
            }
        }

        if (!completions.isEmpty()) {
            List<String> adjustedCompletions = new ArrayList<>();
            String prefix = buffer.substring(0, lastSpace + 1);
            for (String completion : completions) {
                adjustedCompletions.add(prefix + completion);
            }
            event.setCompletions(adjustedCompletions);
        }
    }
}
