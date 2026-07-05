package ru.privatenull.chat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.privatenull.pnChatPlugin;

public class ChatListener implements Listener {

    private final ChatMessageService messageService;
    private final ChatModerationService moderationService;
    private final AdminChatService adminChatService;
    private final pnChatPlugin plugin;

    public ChatListener(ChatMessageService messageService, ChatModerationService moderationService,
                         AdminChatService adminChatService, pnChatPlugin plugin) {
        this.messageService = messageService;
        this.moderationService = moderationService;
        this.adminChatService = adminChatService;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        if (event.isCancelled()) {
            return;
        }

        if (message == null || message.isBlank()) {
            return;
        }

        if (message.startsWith("/")) {
            return;
        }

        event.setCancelled(true);

        if (moderationService.isMutedByExternalPlugin(player)) {
            player.sendMessage(ChatUtils.color("&cВы сейчас не можете писать в чат."));
            return;
        }

        String cleaned = moderationService.sanitizeAndCheck(player, message);
        if (cleaned == null) {
            return;
        }

        if (cleaned.startsWith("!")) {
            messageService.dispatchGlobalChat(event, player, cleaned.substring(1).trim());
            return;
        }

        if (adminChatService.isAdminChatEnabled(player)) {
            messageService.dispatchAdminChat(event, player, cleaned);
            return;
        }

        messageService.dispatchLocalChat(event, player, cleaned);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (plugin.getUpdateChecker() != null) {
            plugin.getUpdateChecker().notifyAdminOnJoin(event.getPlayer());
        }
    }
}
