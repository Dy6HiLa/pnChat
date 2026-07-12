package ru.privatenull;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.privatenull.chat.AdminChatService;
import ru.privatenull.chat.ChatCommandExecutor;
import ru.privatenull.chat.ChatFormatter;
import ru.privatenull.chat.ChatListener;
import ru.privatenull.chat.ChatMessageService;
import ru.privatenull.chat.ChatModerationService;
import ru.privatenull.chat.ChatStyleCommandExecutor;
import ru.privatenull.chat.ChatStyleService;
import ru.privatenull.chat.ChatTabCompleter;
import ru.privatenull.update.UpdateChecker;

import java.io.File;

public final class pnChatPlugin extends JavaPlugin {

    public static final String SUPPORT_DISCORD = "https://discord.gg/rRbzq6cnc6";

    private ChatMessageService messageService;
    private ChatFormatter formatter;
    private ChatStyleService styleService;
    private ChatModerationService moderationService;
    private AdminChatService adminChatService;
    private UpdateChecker updateChecker;
    private FileConfiguration messagesConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("allowed-links.txt", false);

        File messagesFile = new File(getDataFolder(), "messages.yml");
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        styleService = new ChatStyleService(this);
        formatter = new ChatFormatter(this, styleService);
        moderationService = new ChatModerationService(this);
        messageService = new ChatMessageService(this, formatter);
        adminChatService = new AdminChatService(this);

        getCommand("pnchat").setExecutor(new ChatCommandExecutor(this, messageService, adminChatService));
        getCommand("pnchat").setTabCompleter(new ChatTabCompleter());
        ChatStyleCommandExecutor styleCommands = new ChatStyleCommandExecutor(this, styleService);
        getCommand("chatcolor").setExecutor(styleCommands);
        getCommand("chatcolor").setTabCompleter(styleCommands);
        getCommand("chatfont").setExecutor(styleCommands);
        getCommand("chatfont").setTabCompleter(styleCommands);

        getServer().getPluginManager().registerEvents(
                new ChatListener(messageService, moderationService, adminChatService, this), this);

        setupUpdateChecker();
        logStartupMessage();
    }

    @Override
    public void onDisable() {
        if (updateChecker != null) {
            updateChecker.cancel();
        }
        logShutdownMessage();
    }

    public void reloadPlugin() {
        reloadConfig();
        File messagesFile = new File(getDataFolder(), "messages.yml");
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        formatter.reloadConfig();
        moderationService.reloadConfig();
        moderationService.loadLists();
        adminChatService.reloadConfig();
        if (updateChecker != null) {
            updateChecker.reload();
        }
        getLogger().info("pnChat: конфигурация перезагружена.");
    }

    public String getMessage(String path, String defaultValue) {
        return messagesConfig != null ? messagesConfig.getString(path, defaultValue) : defaultValue;
    }

    public ChatFormatter getFormatter() {
        return formatter;
    }

    public ChatModerationService getModerationService() {
        return moderationService;
    }

    public ChatMessageService getMessageService() {
        return messageService;
    }

    public AdminChatService getAdminChatService() {
        return adminChatService;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    private void setupUpdateChecker() {
        updateChecker = new UpdateChecker(this);
        updateChecker.reload();
    }

    private void logStartupMessage() {
        logBanner();
        getLogger().info("");
        getLogger().info("pnChat v" + getDescription().getVersion() + " успешно включён!");
        getLogger().info("Локальный чат: радиус " + getConfig().getInt("radius", 100) + " блоков.");
        getLogger().info("Глобальный чат: префикс ! перед сообщением.");
        getLogger().info("Упоминания: @ник и автоупоминания по нику онлайн-игрока.");
        getLogger().info("Поддержка pnChat: " + SUPPORT_DISCORD);
    }

    private void logShutdownMessage() {
        logBanner();
        getLogger().info("");
        getLogger().info("pnChat отключён");
        getLogger().info("Поддержка pnChat: " + SUPPORT_DISCORD);
    }

    private void logBanner() {
        getLogger().info("██████╗░██████╗░██╗██╗░░░██╗░█████╗░████████╗███████╗███╗░░██╗██╗░░░██╗██╗░░░░░██╗░░░░░");
        getLogger().info("██╔══██╗██╔══██╗██║██║░░░██║██╔══██╗╚══██╔══╝██╔════╝████╗░██║██║░░░██║██║░░░░░██║░░░░░");
        getLogger().info("██████╔╝██████╔╝██║╚██╗░██╔╝███████║░░░██║░░░█████╗░░██╔██╗██║██║░░░██║██║░░░░░██║░░░░░");
        getLogger().info("██╔═══╝░██╔══██╗██║░╚████╔╝░██╔══██║░░░██║░░░██╔══╝░░██║╚████║██║░░░██║██║░░░░░██║░░░░░");
        getLogger().info("██║░░░░░██║░░██║██║░░╚██╔╝░░██║░░██║░░░██║░░░███████╗██║░╚███║╚██████╔╝███████╗███████╗");
        getLogger().info("╚═╝░░░░░╚═╝░░╚═╝╚═╝░░░╚═╝░░░╚═╝░░╚═╝░░░╚═╝░░░╚══════╝╚═╝░░╚══╝░╚═════╝░╚══════╝╚══════╝");
    }
}
