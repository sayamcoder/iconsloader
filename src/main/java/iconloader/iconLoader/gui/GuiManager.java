package iconloader.iconLoader.gui;

import iconloader.iconLoader.IconLoader;
import iconloader.iconLoader.glyph.GlyphDefinition;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class GuiManager implements Listener {
    private final IconLoader plugin;
    private final NamespacedKey glyphKey;
    private static final Key CUSTOM_FONT = Key.key("iconsloader:default");

    public GuiManager(IconLoader plugin) {
        this.plugin = plugin;
        this.glyphKey = new NamespacedKey(plugin, "glyph_name");
    }

    public void openAdminGui(Player player) {
        openGui(player, "IconsLoader | Admin", true);
    }

    public void openPlayerGui(Player player) {
        openGui(player, "IconsLoader | Emojis", false);
    }

    private void openGui(Player player, String title, boolean isAdmin) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(title));
        int slot = 0;

        for (GlyphDefinition glyph : plugin.getConfigManager().getGlyphRegistry().getGlyphs().values()) {
            if (slot >= 54) break;

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();

            // Keep item as Paper, no custom models
            meta.displayName(Component.text(glyph.getName(), NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();

            // Render the emoji in the lore using our custom font!
            Component emojiComponent = Component.text(glyph.getCharacter()).font(CUSTOM_FONT).decoration(TextDecoration.ITALIC, false);
            lore.add(Component.text("Emoji: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false).append(emojiComponent));

            if (isAdmin) {
                lore.add(Component.text("Permission: " + (glyph.getPermission().isEmpty() ? "None" : glyph.getPermission()), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            }

            lore.add(Component.empty());
            lore.add(Component.text(isAdmin ? "Click to send to chat!" : "Click to put in chat bar!", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            meta.getPersistentDataContainer().set(glyphKey, PersistentDataType.STRING, glyph.getName());

            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.startsWith("IconsLoader")) return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        ItemMeta meta = event.getCurrentItem().getItemMeta();
        String glyphName = meta.getPersistentDataContainer().get(glyphKey, PersistentDataType.STRING);

        if (glyphName != null && event.getWhoClicked() instanceof Player player) {
            GlyphDefinition glyph = plugin.getConfigManager().getGlyphRegistry().getGlyphs().get(glyphName);
            if (glyph != null) {
                player.closeInventory();

                if (title.contains("Admin")) {
                    player.sendMessage(Component.text("Preview: ").append(
                        Component.text(glyph.getCharacter()).font(CUSTOM_FONT)
                    ));
                } else {
                    player.sendMessage(Component.text("§aClick below to insert the emoji into your chat:").appendNewline()
                        .append(Component.text("§b§l[INSERT EMOJI]").clickEvent(ClickEvent.suggestCommand(":" + glyph.getName() + ":"))));
                }
            }
        }
    }
}