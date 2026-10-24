package iconloader.iconLoader.command;

import iconloader.iconLoader.IconLoader;
import iconloader.iconLoader.dialog.DialogManager;
import iconloader.iconLoader.pack.ResourcePackGenerator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
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

    public CommandHandler(IconLoader plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "help": sendHelp(sender); break;
            case "support":
                Component link = Component.text("§bClick here to join the Discord Support Server")
                    .clickEvent(ClickEvent.openUrl("https://discord.gg/dgJza7ZrE"));
                sender.sendMessage(Component.text("§8[IconsLoader] §7Need help? Visit:").append(link));
                break;
            case "update": handleUpdateCommand(sender, args); break;

            case "reload":
                // INSTANT RELOAD (No more dialog that can fail)
                plugin.getConfigManager().load();
                sender.sendMessage("§aConfiguration and Glyphs reloaded successfully!");
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
            case "check": plugin.getUpdateChecker().checkForUpdates(); sender.sendMessage("§aChecking for updates..."); break;
            case "start": plugin.getUpdateChecker().setEnabled(true); sender.sendMessage("§aUpdate checker enabled."); break;
            case "stop": plugin.getUpdateChecker().setEnabled(false); sender.sendMessage("§cUpdate checker disabled."); break;
            case "notifications":
                if (args.length > 2 && args[2].equalsIgnoreCase("off")) { plugin.getUpdateChecker().setNotifyOnJoin(false); sender.sendMessage("§cJoin notifications disabled."); }
                else if (args.length > 2 && args[2].equalsIgnoreCase("on")) { plugin.getUpdateChecker().setNotifyOnJoin(true); sender.sendMessage("§aJoin notifications enabled."); }
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
                    sender.sendMessage("§cPlayer not found.");
                }
            }
        }
    }

    private void handleGlyphCommand(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage("§cUsage: /cl glyph <list|info>"); return; }
        if (args[1].equalsIgnoreCase("list")) {
            sender.sendMessage("§dRegistered Glyphs:");
            for (var def : plugin.getConfigManager().getGlyphRegistry().getGlyphs().values()) {
                sender.sendMessage(" §7- §f" + def.getName() + " " + def.getCharacter());
            }
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§d§lIconsLoader Commands");
        sender.sendMessage("§7/cl reload §8- §fReload configs instantly");
        sender.sendMessage("§7/cl support §8- §fGet Discord link");
        sender.sendMessage("§7/cl pack generate §8- §fGenerate resource pack");
        sender.sendMessage("§7/cl pack send <player|@a> §8- §fSend pack");
        sender.sendMessage("§8§m----------------------------------------");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) completions.addAll(Arrays.asList("help", "reload", "support", "update", "pack", "glyph"));
        else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("pack")) completions.addAll(Arrays.asList("generate", "send"));
            else if (args[0].equalsIgnoreCase("update")) completions.addAll(Arrays.asList("check", "start", "stop", "notifications"));
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("pack") && args[1].equalsIgnoreCase("send")) {
                completions.add("@a");
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            } else if (args[0].equalsIgnoreCase("update") && args[1].equalsIgnoreCase("notifications")) {
                completions.addAll(Arrays.asList("on", "off"));
            }
        }
        return completions.stream().filter(c -> c.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
    }
}