package iconloader.iconLoader.chat;

import iconloader.iconLoader.IconLoader;
import iconloader.iconLoader.glyph.GlyphDefinition;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import com.destroystokyo.paper.event.server.AsyncTabCompleteEvent;
import java.util.ArrayList;
import java.util.List;

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

        int lastSpace = buffer.lastIndexOf(' ');
        String currentWord = buffer.substring(lastSpace + 1);

        // If they haven't started with ':' or already closed it, ignore
        if (!currentWord.startsWith(":") || currentWord.endsWith(":")) return;

        String search = currentWord.substring(1).toLowerCase();
        List<String> completions = new ArrayList<>();

        for (GlyphDefinition glyph : plugin.getConfigManager().getGlyphRegistry().getGlyphs().values()) {
            // Check main name
            if (glyph.getName().toLowerCase().startsWith(search)) {
                completions.add(":" + glyph.getName() + ":");
            }
            // Check aliases
            for (String alias : glyph.getAliases()) {
                if (alias.toLowerCase().startsWith(search)) {
                    completions.add(":" + alias + ":");
                }
            }
        }

        if (!completions.isEmpty()) {
            // Adjust the completions to replace the current word in the buffer
            List<String> adjustedCompletions = new ArrayList<>();
            String prefix = buffer.substring(0, lastSpace + 1);
            for (String comp : completions) {
                adjustedCompletions.add(prefix + comp);
            }
            event.setCompletions(adjustedCompletions);
        }
    }
}