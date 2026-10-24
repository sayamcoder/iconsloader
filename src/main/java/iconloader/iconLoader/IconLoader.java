package iconloader.iconLoader;

import iconloader.iconLoader.chat.ChatEmojiListener;
import iconloader.iconLoader.command.CommandHandler;
import iconloader.iconLoader.config.ConfigManager;
import iconloader.iconLoader.dialog.DialogManager;
import iconloader.iconLoader.integration.PlaceholderApiIntegration;
import iconloader.iconLoader.resourcepack.ResourcePackListener;
import iconloader.iconLoader.resourcepack.ResourcePackManager;
import iconloader.iconLoader.util.FileUtils;
import iconloader.iconLoader.util.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class IconLoader extends JavaPlugin {
    private ConfigManager configManager;
    private UpdateChecker updateChecker;
    private ResourcePackManager resourcePackManager;

    @Override
    public void onEnable() {
        printBanner();
        FileUtils.extractDefaultTextures(this);

        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.resourcePackManager = new ResourcePackManager(this);

        CommandHandler commandHandler = new CommandHandler(this);
        PluginCommand clCommand = getCommand("cl");
        if (clCommand != null) {
            clCommand.setExecutor(commandHandler);
            clCommand.setTabCompleter(commandHandler);
        }

        getServer().getPluginManager().registerEvents(new ChatEmojiListener(this), this);
        getServer().getPluginManager().registerEvents(new ResourcePackListener(this), this);

        // Register PlaceholderAPI if it exists
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderApiIntegration(this).register();
            getLogger().info("Successfully hooked into PlaceholderAPI!");
        }

        this.updateChecker = new UpdateChecker(this);
        this.updateChecker.checkForUpdates();

        getLogger().info("IconsLoader enabled successfully.");
    }

    @Override
    public void onDisable() {
        getLogger().info("IconsLoader disabled.");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public UpdateChecker getUpdateChecker() { return updateChecker; }
    public ResourcePackManager getResourcePackManager() { return resourcePackManager; }

    private void printBanner() {
        String[] banner = {
            "§d   ___                     _                    ",
            "§d  |_ _|___ ___  _ __  ___ | |    ___   __ _  __| | ___ _ __ ",
            "§d   | |/ __/ _ \\| '_ \\/ __|| |   / _ \\ / _` |/ _` |/ _ \\ '__|",
            "§d   | | (_| (_) | | | \\__ \\| |__| (_) | (_| | (_| |  __/ |   ",
            "§d  |___\\___\\___/|_| |_|___/|_____\\___/ \\__,_|\\__,_|\\___|_|   ",
            "§8--------------------------------------------------",
            "§7  Plugin: §fIconsLoader §7| Author: §fSayam §7| Ver: §f" + getPluginMeta().getVersion(),
            "§8--------------------------------------------------"
        };
        for (String line : banner) { Bukkit.getConsoleSender().sendMessage(line); }
    }
}