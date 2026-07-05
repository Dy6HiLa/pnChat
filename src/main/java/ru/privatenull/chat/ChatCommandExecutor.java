package ru.privatenull.chat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.privatenull.pnChatPlugin;

public class ChatCommandExecutor implements CommandExecutor {

    private final pnChatPlugin plugin;
    private final ChatMessageService messageService;
    private final AdminChatService adminChatService;

    public ChatCommandExecutor(pnChatPlugin plugin, ChatMessageService messageService, AdminChatService adminChatService) {
        this.plugin = plugin;
        this.messageService = messageService;
        this.adminChatService = adminChatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatUtils.color(plugin.getMessage("messages.general.only-player", "&cТолько игроки могут использовать чат.")));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatUtils.color(plugin.getMessage("messages.commands.usage", "&cИспользование: /pnchat reload | /pnchat ac")));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.reloadPlugin();
                sender.sendMessage(ChatUtils.color(plugin.getMessage("messages.general.reload-success", "&aКонфигурация pnChat перезагружена.")));
                return true;
            }
            case "ac", "adminchat" -> {
                adminChatService.toggleAdminChat(player);
                return true;
            }
            default -> {
                sender.sendMessage(ChatUtils.color(plugin.getMessage("messages.commands.usage", "&cИспользование: /pnchat reload | /pnchat ac")));
                return true;
            }
        }
    }
}
