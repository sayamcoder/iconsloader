package iconloader.iconLoader.command;

import iconloader.iconLoader.IconLoader;
import iconloader.iconLoader.dialog.DialogManager;
import iconloader.iconLoader.glyph.GlyphDefinition;
import iconloader.iconLoader.pack.ResourcePackGenerator;
import iconloader.iconLoader.util.EmojiTokenizer;
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
import java.util.Locale;
import java.util.stream.Collectors;

public class CommandHandler implements CommandExecutor, TabCompleter {
    private static final Key CUSTOM_FONT = Key.key("iconsloader:default");
    private static final String ADMIN_PERMISSION = "iconsloader.admin";

    private final IconLoader plugin;

    public CommandHandler(IconLoader plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("emojis")) {
            if (sender instanceof Player player) plugin.getGuiManager().openPlayerGui(player);
            else sender.sendMessage("Only players can use the GUI.");
            return true;
        }
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> sendHelp(sender);
            case "gui" -> {
                if (!requireAdmin(sender)) return true;
                if (sender instanceof Player player) plugin.getGuiManager().openAdminGui(player);
                else sender.sendMessage("Only players can use the GUI.");
            }
            case "reload" -> handleReload(sender);
            case "pack" -> {
                if (!requireAdmin(sender)) return true;
                handlePackCommand(sender, args);
            }
            case "glyph" -> {
                if (!requireAdmin(sender)) return true;
                handleGlyphCommand(sender, args);
            }
            case "update" -> {
                if (!requireAdmin(sender)) return true;
                if (args.length > 1 && args[1].equalsIgnoreCase("check")) {
                    plugin.getUpdateChecker().checkNow(sender);
                } else {
                    sender.sendMessage("§cUsage: /cl update check");
                }
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!requireAdmin(sender)) return;

        Runnable reload = () -> {
            plugin.getHttpServerManager().stop();
            plugin.getConfigManager().load();
            plugin.getLangManager().load();
            plugin.getHttpServerManager().start();
            plugin.getResourcePackManager().warnIfUrlMissing();
            sender.sendMessage(plugin.getLangManager().getMessage("reload-success"));
        };

        if (sender instanceof Player player) {
            DialogManager.openConfirmDialog(player, "Confirm Reload",
                    "Are you sure you want to reload configs and restart the HTTP server?", reload);
        } else {
            reload.run();
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
                sender.sendMessage("§aSent pack to all players.");
            } else {
                Player target = Bukkit.getPlayerExact(args[2]);
                if (target == null) {
                    sender.sendMessage(plugin.getLangManager().getMessage("player-not-found"));
                    return;
                }
                plugin.getResourcePackManager().sendPack(target);
                sender.sendMessage("§aSent pack to " + target.getName());
            }
        } else {
            sender.sendMessage("§cUsage: /cl pack <generate|send>");
        }
    }

    private void handleGlyphCommand(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage("§cUsage: /cl glyph <list|info>"); return; }

        if (args[1].equalsIgnoreCase("list")) {
            List<GlyphDefinition> glyphs = new ArrayList<>(plugin.getConfigManager().getGlyphRegistry().getGlyphs().values());
            glyphs.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            sender.sendMessage("§dRegistered Glyphs (§f" + glyphs.size() + "§d):");
            for (GlyphDefinition def : glyphs) {
                Component glyphComponent = Component.text(def.getCharacter()).font(CUSTOM_FONT)
                        .decoration(TextDecoration.ITALIC, false);
                sender.sendMessage(Component.text(" §7- §f" + def.getName() + " ").append(glyphComponent)
                        .append(Component.text(def.isChatEnabled() ? "" : " §8(chat off)")));
            }
        } else if (args[1].equalsIgnoreCase("info")) {
            if (args.length < 3) { sender.sendMessage("§cUsage: /cl glyph info <name>"); return; }
            GlyphDefinition def = plugin.getConfigManager().getGlyphRegistry().getByToken(args[2]);
            if (def == null) { sender.sendMessage("§cGlyph not found."); return; }

            String tokenFormat = EmojiTokenizer.sanitizeFormat(
                    plugin.getConfig().getString("chat.token-format", EmojiTokenizer.DEFAULT_FORMAT));
            sender.sendMessage("§dGlyph: §f" + def.getName()
                    + " §7| §dToken: §f" + EmojiTokenizer.renderToken(def.getName(), tokenFormat)
                    + " §7| §dTexture: §f" + def.getTexturePath()
                    + " §7| §dCharacter: §f" + String.format("\\u%04X", (int) def.getCharacter().charAt(0))
                    + " §7| §dCMD: §f" + def.getCustomModelData()
                    + " §7| §dChat: §f" + def.isChatEnabled());
            if (!def.getAliases().isEmpty()) {
                sender.sendMessage("§dAliases: §f" + String.join(", ", def.getAliases()));
            }
        } else {
            sender.sendMessage("§cUsage: /cl glyph <list|info>");
        }
    }

    private boolean requireAdmin(CommandSender sender) {
        if (sender.hasPermission(ADMIN_PERMISSION)) return true;
        sender.sendMessage(plugin.getLangManager().getMessage("no-permission"));
        return false;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m----------------------------------------");
        sender.sendMessage("§d§lIconsLoader v" + plugin.getVersion());
        sender.sendMessage("§7/cl gui §8- §fAdmin Manager");
        sender.sendMessage("§7/emojis §8- §fPlayer Picker");
        sender.sendMessage("§7/cl pack generate §8- §fBuild pack");
        sender.sendMessage("§7/cl pack send <p|@a> §8- §fSend pack");
        sender.sendMessage("§7/cl glyph <list|info> §8- §fInspect glyphs");
        sender.sendMessage("§7/cl update check §8- §fCheck for a new version");
        sender.sendMessage("§7/cl reload §8- §fReload config, glyphs and HTTP server");
        sender.sendMessage("§8§m----------------------------------------");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (command.getName().equalsIgnoreCase("emojis")) return completions;

        if (args.length == 1) {
            completions.addAll(Arrays.asList("help", "gui", "reload", "pack", "glyph", "update"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "pack" -> completions.addAll(Arrays.asList("generate", "send"));
                case "glyph" -> completions.addAll(Arrays.asList("list", "info"));
                case "update" -> completions.add("check");
                default -> { }
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("pack") && args[1].equalsIgnoreCase("send")) {
                completions.add("@a");
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            } else if (args[0].equalsIgnoreCase("glyph") && args[1].equalsIgnoreCase("info")) {
                completions.addAll(plugin.getConfigManager().getGlyphRegistry().getGlyphs().keySet());
            }
        }

        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        return completions.stream().filter(c -> c.toLowerCase(Locale.ROOT).startsWith(last)).collect(Collectors.toList());
    }
}
