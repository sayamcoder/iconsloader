package iconloader.iconLoader.gui;

import iconloader.iconLoader.IconLoader;
import iconloader.iconLoader.glyph.GlyphDefinition;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
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

    public GuiManager(IconLoader plugin) {
        this.plugin = plugin;
        this.glyphKey = new NamespacedKey(plugin, "glyph_name");
    }

    public void openGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, plugin.getLangManager().getMessage("gui-title"));
        int slot = 0;

        for (GlyphDefinition glyph : plugin.getConfigManager().getGlyphRegistry().getGlyphs().values()) {
            if (slot >= 54) break;

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();

            // THIS IS THE MAGIC: Apply CMD so the paper looks like the emoji
            meta.setCustomModelData(glyph.getCustomModelData());

            meta.displayName(Component.text(glyph.getName(), NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Character: " + glyph.getCharacter(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Click to send to chat!", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            meta.getPersistentDataContainer().set(glyphKey, PersistentDataType.STRING, glyph.getName());

            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(plugin.getLangManager().getMessage("gui-title"))) return;
        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        ItemMeta meta = event.getCurrentItem().getItemMeta();
        String glyphName = meta.getPersistentDataContainer().get(glyphKey, PersistentDataType.STRING);

        if (glyphName != null && event.getWhoClicked() instanceof Player player) {
            GlyphDefinition glyph = plugin.getConfigManager().getGlyphRegistry().getGlyphs().get(glyphName);
            if (glyph != null) {
                player.closeInventory();
                player.sendMessage(plugin.getLangManager().getMessage("gui-preview").append(
                    Component.text(glyph.getCharacter()).font(Key.key("iconsloader:default"))
                ));
            }
        }
    }
}