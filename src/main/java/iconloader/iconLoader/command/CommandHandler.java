package iconloader.iconLoader.command;

import iconloader.iconLoader.IconLoader;
import iconloader.iconLoader.glyph.GlyphDefinition;
import iconloader.iconLoader.pack.ResourcePackGenerator;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private final IconLoader plugin;
    private static final Key CUSTOM_FONT = Key.key("iconsloader:default");

    public CommandHandler(IconLoader plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "help": sendHelp(sender); break;
            case "gui":
                if (sender instanceof Player player) plugin.getGuiManager().openGui(player);
                else sender.sendMessage("Only players can use the GUI.");
                break;
            case "support":
                Component link = plugin.getLangManager().getMessage("prefix").append(Component.text("Click here to join the Discord Support Server").clickEvent(ClickEvent.openUrl("https://discord.gg/dgJza7ZrE")));
                sender.sendMessage(link);
                break;
            case "update": handleUpdateCommand(sender, args); break;
            case "reload":
                plugin.getConfigManager().load();
                plugin.getLangManager().load();
                sender.sendMessage(plugin.getLangManager().getMessage("reload-success"));
                break;
            case "pack": handlePackCommand(sender, args); break;
            case "glyph": handleGlyphCommand(sender, args); break;
            default: sendHelp(sender); break;
        }
        return true;
    }

    private void handleUpdateCommand(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage("§cUsage: /cl update <check|start|stop|notifications>"); return; }
        switch (args[1].toLowerCase()) {
            case "check":
                if (!plugin.getUpdateChecker().isEnabled()) { sender.sendMessage(plugin.getLangManager().getMessage("update-disabled")); return; }
                plugin.getUpdateChecker().checkForUpdates(); sender.sendMessage(plugin.getLangManager().getMessage("update-checking")); break;
            case "start": plugin.getUpdateChecker().setEnabled(true); sender.sendMessage(plugin.getLangManager().getMessage("update-enabled")); break;
            case "stop": plugin.getUpdateChecker().setEnabled(false); sender.sendMessage(plugin.getLangManager().getMessage("update-stopped")); break;
            case "notifications":
                if (args.length > 2 && args[2].equalsIgnoreCase("off")) { plugin.getUpdateChecker().setNotifyOnJoin(false); sender.sendMessage(plugin.getLangManager().getMessage("update-notify-off")); }
                else if (args.length > 2 && args[2].equalsIgnoreCase("on")) { plugin.getUpdateChecker().setNotifyOnJoin(true); sender.sendMessage(plugin.getLangManager().getMessage("update-notify-on")); }
                else { sender.sendMessage("§cUsage: /cl update notifications <on|off>"); }
                break;
        }
    }

    private void handlePackCommand(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage("§cUsage: /cl pack <generate|send>"); return; }
        if (args[1].equalsIgnoreCase("generate")) {
            new ResourcePackGenerator(plugin).generate(sender);
        } else if (args[1].equalsIgnoreCase("send")) {
            if (args.length < 3) { sender.sendMessage("§cUsage: /cl pack send <player|@a>"); return; }
            if (args[2].equalsIgnoreCase("@a")) {
                plugin.getResourcePackManager().sendPackToAll();
                sender.sendMessage("§aSent resource pack to all players.");
            } else {
                Player target = Bukkit.getPlayer(args[2]);
                if (target != null) {
                    plugin.getResourcePackManager().sendPack(target);
                    sender.sendMessage("§aSent resource pack to " + target.getName());
                } else {
                    sender.sendMessage(plugin.getLangManager().getMessage("player-not-found"));
                }
            }
        }
    }

    private void handleGlyphCommand(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage("§cUsage: /cl glyph <list|info>"); return; }

        if (args[1].equalsIgnoreCase("list")) {
            sender.sendMessage("§dRegistered Glyphs:");
            for (GlyphDefinition def : plugin.getConfigManager().getGlyphRegistry().getGlyphs().values()) {
                // Render the character using our custom font!
                Component glyphComponent = Component.text(def.getCharacter()).font(CUSTOM_FONT).decoration(TextDecoration.ITALIC, false);
                sender.sendMessage(Component.text(" §7- §f" + def.getName() + " ").append(glyphComponent));
            }

        } else if (args[1].equalsIgnoreCase("info")) {
            if (args.length < 3) { sender.sendMessage("§cUsage: /cl glyph info <name>"); return; }

            GlyphDefinition def = plugin.getConfigManager().getGlyphRegistry().getGlyphs().get(args[2].toLowerCase());
            if (def == null) { sender.sendMessage("§cGlyph not found: " + args[2]); return; }

            Component glyphComponent = Component.text(def.getCharacter()).font(CUSTOM_FONT).decoration(TextDecoration.ITALIC, false);

            sender.sendMessage("§8§m----------------------------------------");
            sender.sendMessage("§dGlyph Info: §f" + def.getName());
            sender.sendMessage("§7Character: " + glyphComponent);
            sender.sendMessage("§7Texture: §f" + def.getTexturePath());
            sender.sendMessage("§7Custom Model Data: §f" + def.getCustomModelData());
            sender.sendMessage("§7Permission: §f" + (def.getPermission().isEmpty() ? "None" : def.getPermission()));
            sender.sendMessage("§8§m----------------------------------------");
        } else {
            sender.sendMessage("§cUsage: /cl glyph <list|info>");
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§d§lIconsLoader v2.1");
        sender.sendMessage("§7/cl gui §8- §fOpen the visual emoji manager");
        sender.sendMessage("§7/cl reload §8- §fReload configs instantly");
        sender.sendMessage("§7/cl pack generate §8- §fGenerate resource pack (Async)");
        sender.sendMessage("§7/cl pack send <player|@a> §8- §fSend pack");
        sender.sendMessage("§7/cl glyph list §8- §fList all registered emojis");
        sender.sendMessage("§7/cl glyph info <name> §8- §fShow details of an emoji");
        sender.sendMessage("§8§m----------------------------------------");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) completions.addAll(Arrays.asList("help", "gui", "reload", "support", "update", "pack", "glyph"));
        else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("pack")) completions.addAll(Arrays.asList("generate", "send"));
            else if (args[0].equalsIgnoreCase("update")) completions.addAll(Arrays.asList("check", "start", "stop", "notifications"));
            else if (args[0].equalsIgnoreCase("glyph")) completions.addAll(Arrays.asList("list", "info"));
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("pack") && args[1].equalsIgnoreCase("send")) {
                completions.add("@a");
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            } else if (args[0].equalsIgnoreCase("update") && args[1].equalsIgnoreCase("notifications")) {
                completions.addAll(Arrays.asList("on", "off"));
            } else if (args[0].equalsIgnoreCase("glyph") && args[1].equalsIgnoreCase("info")) {
                // Tab complete emoji names!
                completions.addAll(plugin.getConfigManager().getGlyphRegistry().getGlyphs().keySet());
            }
        }
        return completions.stream().filter(c -> c.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
    }
}