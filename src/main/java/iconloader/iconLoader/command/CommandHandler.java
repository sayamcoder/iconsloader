package iconloader.iconLoader.command;

import iconloader.iconLoader.IconLoader;
import iconloader.iconLoader.dialog.DialogManager;
import iconloader.iconLoader.glyph.GlyphDefinition;
import iconloader.iconLoader.pack.ResourcePackGenerator;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
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

    public CommandHandler(IconLoader plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("emojis")) {
            if (sender instanceof Player player) plugin.getGuiManager().openPlayerGui(player);
            else sender.sendMessage("Only players can use the GUI.");
            return true;
        }
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "help": sendHelp(sender); break;
            case "gui":
                if (sender instanceof Player player) plugin.getGuiManager().openAdminGui(player);
                else sender.sendMessage("Only players can use the GUI.");
                break;
            case "reload":
                // Native Dialog Menu for /cl reload
                if (sender instanceof Player player) {
                    DialogManager.openConfirmDialog(player, "Confirm Reload",
                        "Are you sure you want to reload configs and restart the HTTP server?",
                        () -> {
                            plugin.getHttpServerManager().stop();
                            plugin.getConfigManager().load();
                            plugin.getLangManager().load();
                            plugin.getHttpServerManager().start(); // Restart HTTP Server
                            player.sendMessage(plugin.getLangManager().getMessage("reload-success"));
                        });
                } else {
                    plugin.getHttpServerManager().stop();
                    plugin.getConfigManager().load();
                    plugin.getLangManager().load();
                    plugin.getHttpServerManager().start();
                    sender.sendMessage(plugin.getLangManager().getMessage("reload-success"));
                }
                break;
            case "pack": handlePackCommand(sender, args); break;
            case "glyph": handleGlyphCommand(sender, args); break;
            default: sendHelp(sender); break;
        }
        return true;
    }

    private void handlePackCommand(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage("§cUsage: /cl pack <generate|send>"); return; }
        if (args[1].equalsIgnoreCase("generate")) {
            new ResourcePackGenerator(plugin).generate(sender);
        } else if (args[1].equalsIgnoreCase("send")) {
            if (args.length < 3) { sender.sendMessage("§cUsage: /cl pack send <player|@a>"); return; }
            if (args[2].equalsIgnoreCase("@a")) {
                plugin.getResourcePackManager().sendPackToAll();
                sender.sendMessage("§aSent pack to all players.");
            } else {
                Player target = Bukkit.getPlayer(args[2]);
                if (target != null) {
                    plugin.getResourcePackManager().sendPack(target);
                    sender.sendMessage("§aSent pack to " + target.getName());
                } else sender.sendMessage("§cPlayer not found.");
            }
        }
    }

    private void handleGlyphCommand(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage("§cUsage: /cl glyph <list|info>"); return; }
        if (args[1].equalsIgnoreCase("list")) {
            sender.sendMessage("§dRegistered Glyphs:");
            for (GlyphDefinition def : plugin.getConfigManager().getGlyphRegistry().getGlyphs().values()) {
                Component glyphComponent = Component.text(def.getCharacter()).font(CUSTOM_FONT).decoration(TextDecoration.ITALIC, false);
                sender.sendMessage(Component.text(" §7- §f" + def.getName() + " ").append(glyphComponent));
            }
        } else if (args[1].equalsIgnoreCase("info")) {
            if (args.length < 3) { sender.sendMessage("§cUsage: /cl glyph info <name>"); return; }
            GlyphDefinition def = plugin.getConfigManager().getGlyphRegistry().getGlyphs().get(args[2].toLowerCase());
            if (def == null) { sender.sendMessage("§cGlyph not found."); return; }
            sender.sendMessage("§dGlyph: §f" + def.getName() + " | §dTexture: §f" + def.getTexturePath());
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§d§lIconsLoader v4.0");
        sender.sendMessage("§7/cl gui §8- §fAdmin Manager");
        sender.sendMessage("§7/emojis §8- §fPlayer Picker");
        sender.sendMessage("§7/cl pack generate §8- §fBuild pack");
        sender.sendMessage("§7/cl pack send <p|@a> §8- §fSend pack");
        sender.sendMessage("§8§m----------------------------------------");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("emojis")) return completions;

        if (args.length == 1) completions.addAll(Arrays.asList("help", "gui", "reload", "pack", "glyph"));
        else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("pack")) completions.addAll(Arrays.asList("generate", "send"));
            if (args[0].equalsIgnoreCase("glyph")) completions.addAll(Arrays.asList("list", "info"));
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("pack") && args[1].equalsIgnoreCase("send")) {
                completions.add("@a");
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            } else if (args[0].equalsIgnoreCase("glyph") && args[1].equalsIgnoreCase("info")) {
                completions.addAll(plugin.getConfigManager().getGlyphRegistry().getGlyphs().keySet());
            }
        }
        return completions.stream().filter(c -> c.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
    }
}