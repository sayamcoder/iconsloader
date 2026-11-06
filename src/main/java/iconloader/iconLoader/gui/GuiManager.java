package iconloader.iconLoader.gui;

import iconloader.iconLoader.IconLoader;
import iconloader.iconLoader.glyph.GlyphDefinition;
import iconloader.iconLoader.util.EmojiTokenizer;
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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuiManager implements Listener {
    private static final Key CUSTOM_FONT = Key.key("iconsloader:default");
    private static final int INVENTORY_SIZE = 54;

    private enum Mode {
        ADMIN,
        PICKER
    }

    private final IconLoader plugin;
    private final NamespacedKey glyphKey;
    // Which GUI a player currently has open, so clicks are matched by inventory instead of by title text.
    private final Map<UUID, Mode> openModes = new ConcurrentHashMap<>();

    public GuiManager(IconLoader plugin) {
        this.plugin = plugin;
        this.glyphKey = new NamespacedKey(plugin, "glyph_name");
    }

    public void openAdminGui(Player player) {
        openGui(player, Mode.ADMIN);
    }

    public void openPlayerGui(Player player) {
        openGui(player, Mode.PICKER);
    }

    private void openGui(Player player, Mode mode) {
        List<GlyphDefinition> glyphs = new ArrayList<>(plugin.getConfigManager().getGlyphRegistry().getGlyphs().values());
        if (mode == Mode.PICKER) {
            glyphs.removeIf(glyph -> !glyph.isChatEnabled());
        }
        // ConcurrentHashMap is unordered, so the picker would reshuffle itself on every open.
        glyphs.sort(Comparator.comparing(GlyphDefinition::getName, String.CASE_INSENSITIVE_ORDER));

        if (glyphs.isEmpty()) {
            player.sendMessage(plugin.getLangManager().getMessage("gui-empty"));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, INVENTORY_SIZE,
                Component.text(plugin.getLangManager().getString(mode == Mode.ADMIN ? "gui-title-admin" : "gui-title")));
        String tokenFormat = tokenFormat();

        int slot = 0;
        for (GlyphDefinition glyph : glyphs) {
            if (slot >= INVENTORY_SIZE) break;

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            meta.displayName(Component.text(glyph.getName(), NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Emoji: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(glyph.getCharacter()).font(CUSTOM_FONT)
                            .decoration(TextDecoration.ITALIC, false)));

            if (mode == Mode.ADMIN) {
                lore.add(Component.text("Permission: "
                        + (glyph.getPermission().isEmpty() ? "None" : glyph.getPermission()), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                lore.add(Component.text("Custom model data: " + glyph.getCustomModelData(), NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
            }

            lore.add(Component.text("Token: " + EmojiTokenizer.renderToken(glyph.getName(), tokenFormat),
                    NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
            lore.add(Component.text(mode == Mode.ADMIN ? "Click to preview in chat!" : "Click to put in chat bar!",
                    NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            meta.getPersistentDataContainer().set(glyphKey, PersistentDataType.STRING, glyph.getName());
            item.setItemMeta(meta);
            inv.setItem(slot++, item);
        }

        // A menu is at most 54 slots, so past that the surplus has to be acknowledged rather than dropped quietly.
        if (glyphs.size() > slot) {
            player.sendMessage(plugin.getLangManager().getMessage("gui-truncated",
                    "<shown>", String.valueOf(slot), "<total>", String.valueOf(glyphs.size())));
        }

        openModes.put(player.getUniqueId(), mode);
        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Mode mode = openModes.get(player.getUniqueId());
        if (mode == null) return;
        event.setCancelled(true);

        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) return;

        ItemMeta meta = current.getItemMeta();
        if (meta == null) return;

        String glyphName = meta.getPersistentDataContainer().get(glyphKey, PersistentDataType.STRING);
        if (glyphName == null) return;

        GlyphDefinition glyph = plugin.getConfigManager().getGlyphRegistry().getByToken(glyphName);
        if (glyph == null) return;

        player.closeInventory();

        if (mode == Mode.ADMIN) {
            player.sendMessage(plugin.getLangManager().getMessage("gui-preview")
                    .append(Component.text(glyph.getCharacter()).font(CUSTOM_FONT)));
        } else {
            player.sendMessage(plugin.getLangManager().getMessage("gui-insert-hint").appendNewline()
                    .append(Component.text("§b§l[INSERT EMOJI]")
                            .clickEvent(ClickEvent.suggestCommand(
                                    EmojiTokenizer.renderToken(glyph.getName(), tokenFormat())))));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && openModes.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        openModes.remove(event.getPlayer().getUniqueId());
    }

    private String tokenFormat() {
        return EmojiTokenizer.sanitizeFormat(plugin.getConfig().getString("chat.token-format",
                EmojiTokenizer.DEFAULT_FORMAT));
    }
}