package ru.privatenull.chat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.privatenull.pnChatPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AdminChatService {

    private final pnChatPlugin plugin;
    private final Set<UUID> adminChatEnabled = new HashSet<>();
    private String permission;

    public AdminChatService(pnChatPlugin plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        permission = plugin.getConfig().getString("admin-chat-permission", "pnchat.admin");
    }

    public boolean isAdminChatEnabled(Player player) {
        return player != null && adminChatEnabled.contains(player.getUniqueId());
    }

    public void toggleAdminChat(Player player) {
        if (adminChatEnabled.contains(player.getUniqueId())) {
            adminChatEnabled.remove(player.getUniqueId());
            player.sendMessage(ChatUtils.color(plugin.getMessage("messages.admin-chat.disabled", "&7Админ-чат отключён.")));
            return;
        }

        if (!player.hasPermission(permission)) {
            player.sendMessage(ChatUtils.color(plugin.getMessage("messages.admin-chat.no-permission", "&cНедостаточно прав.")));
            return;
        }

        adminChatEnabled.add(player.getUniqueId());
        player.sendMessage(ChatUtils.color(plugin.getMessage("messages.admin-chat.enabled", "&aАдмин-чат включён.")));
    }

    public void sendAdminMessage(Player sender, String message) {
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(ChatUtils.color(plugin.getMessage("messages.admin-chat.no-permission", "&cНедостаточно прав.")));
            return;
        }

        String format = plugin.getConfig().getString("admin-format", "&8[&cADMIN&8] &f{player}&7: &f{message}");
        String formatted = ChatUtils.color(format.replace("{player}", sender.getName()).replace("{message}", message));
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(formatted);
            }
        }
    }
}
