package ru.privatenull.chat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import ru.privatenull.pnChatPlugin;

import java.util.ArrayList;
import java.util.List;

public class ChatMessageService {

    private final pnChatPlugin plugin;
    private final ChatFormatter formatter;

    public ChatMessageService(pnChatPlugin plugin, ChatFormatter formatter) {
        this.plugin = plugin;
        this.formatter = formatter;
    }

    public void dispatchLocalChat(AsyncPlayerChatEvent event, Player sender, String message) {
        if (message == null || message.isBlank()) {
            sender.sendMessage(ChatUtils.color(plugin.getMessage("messages.general.empty-message", "&cВведите текст сообщения.")));
            if (event != null) {
                event.setCancelled(true);
            }
            return;
        }

        int radius = plugin.getConfig().getInt("radius", 100);
        String formatted = formatter.format(sender, "local", message);
        List<Player> recipients = new ArrayList<>();

        for (Player player : sender.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(sender.getLocation()) <= (double) radius * radius) {
                recipients.add(player);
            }
        }

        if (recipients.isEmpty()) {
            sender.sendMessage(ChatUtils.color(plugin.getMessage("messages.general.no-nearby-players", "&eРядом никого нет. Сообщение доставлено только вам.")));
            sender.sendMessage(formatted);
            if (event != null) {
                event.setCancelled(true);
            }
            return;
        }

        for (Player player : recipients) {
            player.sendMessage(formatted);
        }
        if (event != null) {
            event.setCancelled(true);
        }
    }

    public void dispatchGlobalChat(AsyncPlayerChatEvent event, Player sender, String message) {
        if (message == null || message.isBlank()) {
            sender.sendMessage(ChatUtils.color(plugin.getMessage("messages.general.empty-message", "&cВведите текст сообщения.")));
            if (event != null) {
                event.setCancelled(true);
            }
            return;
        }

        String formatted = formatter.format(sender, "global", message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(formatted);
        }
        if (event != null) {
            event.setCancelled(true);
        }
    }

    public void dispatchAdminChat(AsyncPlayerChatEvent event, Player sender, String message) {
        if (message == null || message.isBlank()) {
            sender.sendMessage(ChatUtils.color(plugin.getMessage("messages.general.empty-message", "&cВведите текст сообщения.")));
            if (event != null) {
                event.setCancelled(true);
            }
            return;
        }

        String formatted = formatter.format(sender, "admin", message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("pnchat.admin")) {
                player.sendMessage(formatted);
            }
        }
        if (event != null) {
            event.setCancelled(true);
        }
    }

    public void dispatchLocalChat(Player sender, String message) {
        dispatchLocalChat(null, sender, message);
    }

    public void dispatchGlobalChat(Player sender, String message) {
        dispatchGlobalChat(null, sender, message);
    }

    public void dispatchAdminChat(Player sender, String message) {
        dispatchAdminChat(null, sender, message);
    }
}
