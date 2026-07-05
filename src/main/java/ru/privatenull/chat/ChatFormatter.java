package ru.privatenull.chat;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import ru.privatenull.pnChatPlugin;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFormatter {

    private static final Pattern MENTION_PATTERN = Pattern.compile("(?<!\\w)@([A-Za-z0-9_]{1,16})");

    private final pnChatPlugin plugin;
    private final Map<UUID, Long> mentionSoundCooldown = new HashMap<>();

    // cached config values
    private String localFormat;
    private String globalFormat;
    private String adminFormat;
    private String mentionFormat;
    private String mentionSound;
    private float mentionSoundVolume;
    private float mentionSoundPitch;
    private long mentionSoundCooldownMs;
    private boolean mentionSoundEnabled;
    private boolean allowHexColors;
    private boolean colorPermRequired;
    private String colorPermission;

    public ChatFormatter(pnChatPlugin plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    public void reloadConfig() {
        var cfg = plugin.getConfig();
        localFormat = cfg.getString("local-format", "&8[&aL&8] &7{player} &8» &f{message}");
        globalFormat = cfg.getString("global-format", "&8[&dG&8] &f{player} &8» &f{message}");
        adminFormat = cfg.getString("admin-format", "&8[&cADMIN&8] &f{player}&7: &f{message}");
        mentionFormat = cfg.getString("mention-format", "&c@{player}");
        mentionSound = cfg.getString("mention-sound", "entity.experience_orb.pickup");
        mentionSoundVolume = (float) cfg.getDouble("mention-sound-volume", 1.0);
        mentionSoundPitch = (float) cfg.getDouble("mention-sound-pitch", 1.0);
        mentionSoundCooldownMs = cfg.getLong("mention-sound-cooldown-ms", 4000L);
        mentionSoundEnabled = cfg.getBoolean("mention-sound-enabled", true);
        allowHexColors = cfg.getBoolean("allow-hex-colors", true);
        colorPermRequired = cfg.getBoolean("color-permission-required", false);
        colorPermission = cfg.getString("color-permission", "pnchat.color");
    }

    public String format(Player player, String channel, String message) {
        String format;
        if (channel.equalsIgnoreCase("global")) {
            format = globalFormat;
        } else if (channel.equalsIgnoreCase("admin")) {
            format = adminFormat;
        } else {
            format = localFormat;
        }

        // Sanitize and apply formatting
        String processed = sanitizeMessage(player, message);

        // Apply PlaceholderAPI
        processed = applyPlaceholdersSoft(player, processed);

        String luckPermsPrefix = getLuckPermsPrefixSoft(player);

        String built = format
                .replace("{player}", player.getName())
                .replace("{message}", processed);

        String result = ChatUtils.color(built);

        // Prepend LuckPerms prefix if present
        if (luckPermsPrefix != null && !luckPermsPrefix.isEmpty()) {
            result = ChatUtils.color(luckPermsPrefix) + " " + result;
        }

        return result;
    }

    public String formatMention(Player sender, Player target) {
        return ChatUtils.color(mentionFormat.replace("{player}", target.getName()));
    }

    private String applyPlaceholdersSoft(Player player, String input) {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return input;
        }
        try {
            Class<?> apiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Method method = apiClass.getMethod("setPlaceholders", org.bukkit.OfflinePlayer.class, String.class);
            Object result = method.invoke(null, player, input);
            return result == null ? input : String.valueOf(result);
        } catch (Throwable ignored) {
            return input;
        }
    }

    private String getLuckPermsPrefixSoft(Player player) {
        if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            return "";
        }
        try {
            Class<?> lpClass = Class.forName("net.luckperms.api.LuckPerms");
            Object lp = Bukkit.getServicesManager().load(lpClass);
            if (lp == null) {
                return "";
            }
            Object userManager = lpClass.getMethod("getUserManager").invoke(lp);
            Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, player.getUniqueId());
            if (user == null) {
                return "";
            }
            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            Object metaData = null;
            for (Method m : cachedData.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getName().equals("getMetaData")) {
                    metaData = m.invoke(cachedData);
                    break;
                }
            }
            if (metaData == null) {
                return "";
            }
            Object prefix = metaData.getClass().getMethod("getPrefix").invoke(metaData);
            return prefix == null ? "" : String.valueOf(prefix);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private String sanitizeMessage(Player player, String input) {
        if (input == null) {
            return "";
        }

        String escaped = input.replace("\\", "\\\\");
        escaped = escaped.replace("%", "%%");
        return applyFormatting(player, escaped);
    }

    private String applyFormatting(Player player, String input) {
        if (input == null) {
            return "";
        }

        // Check color permission
        if (colorPermRequired && !player.hasPermission(colorPermission)
                && !player.hasPermission("pnchat.format")
                && !player.hasPermission("pnchat.chatcolor")) {
            return ChatUtils.stripColorCodes(input);
        }

        String result = ChatUtils.color(input);
        if (allowHexColors) {
            result = ChatUtils.translateHex(result);
        }

        // Handle mentions
        result = formatMentions(player, result);

        return result;
    }

    private String formatMentions(Player sender, String input) {
        Matcher matcher = MENTION_PATTERN.matcher(input);
        StringBuilder buffer = new StringBuilder();

        while (matcher.find()) {
            String nick = matcher.group(1);
            Player target = Bukkit.getPlayerExact(nick);
            if (target != null && !target.getUniqueId().equals(sender.getUniqueId())) {
                String replacement = formatMention(sender, target);
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
                playMentionSound(target);
            } else {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(0)));
            }
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private void playMentionSound(Player target) {
        if (!mentionSoundEnabled) {
            return;
        }

        long now = System.currentTimeMillis();
        Long last = mentionSoundCooldown.get(target.getUniqueId());
        if (last != null && now - last < mentionSoundCooldownMs) {
            return;
        }

        mentionSoundCooldown.put(target.getUniqueId(), now);

        Location location = target.getLocation();
        target.playSound(location, mentionSound, SoundCategory.MASTER, mentionSoundVolume, mentionSoundPitch);
    }
}
