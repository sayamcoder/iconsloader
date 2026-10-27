package iconloader.iconLoader;

import iconloader.iconLoader.chat.ChatEmojiListener;
import iconloader.iconLoader.chat.ItemEmojiListener;
import iconloader.iconLoader.command.CommandHandler;
import iconloader.iconLoader.config.ConfigManager;
import iconloader.iconLoader.gui.GuiManager;
import iconloader.iconLoader.integration.PlaceholderApiIntegration;
import iconloader.iconLoader.resourcepack.HttpServerManager;
import iconloader.iconLoader.resourcepack.ResourcePackListener;
import iconloader.iconLoader.resourcepack.ResourcePackManager;
import iconloader.iconLoader.util.FileUtils;
import iconloader.iconLoader.util.LangManager;
import iconloader.iconLoader.util.UpdateChecker;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class IconLoader extends JavaPlugin {
    private ConfigManager configManager;
    private UpdateChecker updateChecker;
    private ResourcePackManager resourcePackManager;
    private GuiManager guiManager;
    private LangManager langManager;
    private HttpServerManager httpServerManager;

    @Override
    public void onEnable() {
        printBanner();
        FileUtils.extractDefaultTextures(this);

        this.langManager = new LangManager(this);
        this.configManager = new ConfigManager(this);
        this.configManager.load();
        this.langManager.load();

        this.resourcePackManager = new ResourcePackManager(this);
        this.guiManager = new GuiManager(this);
        this.httpServerManager = new HttpServerManager(this);

        // Start Internal HTTP Server
        this.httpServerManager.start();

        CommandHandler commandHandler = new CommandHandler(this);
        PluginCommand clCommand = getCommand("cl");
        if (clCommand != null) {
            clCommand.setExecutor(commandHandler);
            clCommand.setTabCompleter(commandHandler);
        }

        // Register /emojis command dynamically
        PluginCommand emojisCommand = getCommand("emojis");
        if (emojisCommand != null) {
            emojisCommand.setExecutor(commandHandler);
        }

        getServer().getPluginManager().registerEvents(new ChatEmojiListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemEmojiListener(this), this);
        getServer().getPluginManager().registerEvents(new ResourcePackListener(this), this);
        getServer().getPluginManager().registerEvents(guiManager, this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderApiIntegration(this).register();
        }

        this.updateChecker = new UpdateChecker(this);
        this.updateChecker.checkForUpdates();

        getLogger().info("IconsLoader v3.0 enabled successfully.");
    }

    @Override
    public void onDisable() {
        if (httpServerManager != null) httpServerManager.stop();
        getLogger().info("IconsLoader disabled.");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public UpdateChecker getUpdateChecker() { return updateChecker; }
    public ResourcePackManager getResourcePackManager() { return resourcePackManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public LangManager getLangManager() { return langManager; }

    private void printBanner() {
        String[] banner = {
            "§d   ___                     _                    ",
            "§d  |_ _|___ ___  _ __  ___ | |    ___   __ _  __| | ___ _ __ ",
            "§d   | |/ __/ _ \\| '_ \\/ __|| |   / _ \\ / _` |/ _` |/ _ \\ '__|",
            "§d   | | (_| (_) | | | \\__ \\| |__| (_) | (_| | (_| |  __/ |   ",
            "§d  |___\\___\\___/|_| |_|___/|_____\\___/ \\__,_|\\__,_|\\___|_|   ",
            "§8--------------------------------------------------",
            "§7  Plugin: §fIconsLoader v3.0 §7| Author: §fSayam",
            "§8--------------------------------------------------"
        };
        for (String line : banner) { Bukkit.getConsoleSender().sendMessage(line); }
    }
}