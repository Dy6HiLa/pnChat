package ru.privatenull.chat;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatUtils {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_PATTERN_LEGACY = Pattern.compile("#([A-Fa-f0-9]{6})");
    private static final Pattern STRIP_AMPERSAND = Pattern.compile("(?i)&[0-9a-fk-orx]");
    private static final Pattern STRIP_SECTION = Pattern.compile("§[0-9a-fk-orx]");
    private static final Pattern STRIP_HEX_AMPERSAND = Pattern.compile("(?i)&#[0-9a-f]{6}");
    private static final Pattern STRIP_HEX_LEGACY = Pattern.compile("(?i)#[0-9a-f]{6}");

    private ChatUtils() {
    }

    public static String color(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static String translateHex(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // First handle &#RRGGBB format
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (int i = 0; i < hex.length(); i++) {
                replacement.append('§').append(hex.charAt(i));
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(buffer);
        // Then handle legacy #RRGGBB format (without &)
        String intermediate = buffer.toString();
        Matcher legacyMatcher = HEX_PATTERN_LEGACY.matcher(intermediate);
        StringBuilder buffer2 = new StringBuilder();
        while (legacyMatcher.find()) {
            String hex = legacyMatcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (int i = 0; i < hex.length(); i++) {
                replacement.append('§').append(hex.charAt(i));
            }
            legacyMatcher.appendReplacement(buffer2, Matcher.quoteReplacement(replacement.toString()));
        }
        legacyMatcher.appendTail(buffer2);
        return buffer2.toString();
    }

    public static String stripColorCodes(String input) {
        if (input == null) {
            return "";
        }
        String result = input;
        result = result.replace("&&", "\\&");
        result = STRIP_HEX_AMPERSAND.matcher(result).replaceAll("");
        result = STRIP_HEX_LEGACY.matcher(result).replaceAll("");
        result = STRIP_AMPERSAND.matcher(result).replaceAll("");
        result = STRIP_SECTION.matcher(result).replaceAll("");
        result = result.replace("\\&", "&");
        return result;
    }

    public static String stripFormattingCodes(String input) {
        return stripColorCodes(input);
    }
}
