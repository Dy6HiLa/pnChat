package ru.privatenull.chat;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class ChatStyleService {

    private final JavaPlugin plugin;
    private final File file;
    private YamlConfiguration config;

    public ChatStyleService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "chat-styles.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public String apply(UUID playerId, String message) {
        String path = "players." + playerId;
        String color = config.getString(path + ".color", "");
        List<String> fonts = config.getStringList(path + ".fonts");
        if (color.isEmpty() && fonts.isEmpty()) {
            return message;
        }
        return color + String.join("", fonts) + message;
    }

    public void setColor(UUID playerId, String color) {
        String path = "players." + playerId + ".color";
        config.set(path, color);
        save();
    }

    public void setFonts(UUID playerId, List<String> fonts) {
        String path = "players." + playerId + ".fonts";
        config.set(path, fonts);
        save();
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().warning("Не удалось сохранить стили чата: " + exception.getMessage());
        }
    }
}
