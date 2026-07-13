package ru.privatenull.chat;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.privatenull.pnChatPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class ChatStyleCommandExecutor implements CommandExecutor, TabCompleter {

    private static final String RESET_VALUE = "none";
    private static final int MAX_NICKNAME_LENGTH = 64;
    private static final int MAX_PREFIX_LENGTH = 128;
    private static final Pattern HEX_COLOR = Pattern.compile("#?[0-9a-fA-F]{6}");
    private static final List<String> COLOR_NAMES = List.of(
            "черный", "темно-синий", "темно-зеленый", "темно-бирюзовый",
            "темно-красный", "темно-фиолетовый", "золотой", "серый",
            "темно-серый", "синий", "зеленый", "голубой",
            "красный", "розовый", "желтый", "белый"
    );
    private static final List<String> FONT_NAMES = List.of(
            "жирный", "курсив", "подчеркнутый", "зачеркнутый", "магический"
    );
    private static final Map<String, String> COLORS = createColors();
    private static final Map<String, String> FONTS = createFonts();

    private final pnChatPlugin plugin;
    private final ChatStyleService styleService;

    public ChatStyleCommandExecutor(pnChatPlugin plugin, ChatStyleService styleService) {
        this.plugin = plugin;
        this.styleService = styleService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        if (!isStyleCommand(commandName)) {
            return false;
        }

        if (!sender.hasPermission("pnchat." + commandName)) {
            sender.sendMessage(message("messages.admin-chat.no-permission", "&cНедостаточно прав."));
            return true;
        }

        return switch (commandName) {
            case "chatcolor" -> setColor(sender, args);
            case "chatfont" -> setFonts(sender, args);
            case "nick" -> setNickname(sender, args);
            case "prefix" -> setPrefix(sender, args);
            default -> false;
        };
    }

    private boolean setColor(CommandSender sender, String[] args) {
        if (args.length != 2) {
            sendUsage(sender, "chatcolor");
            return true;
        }

        ParsedValue parsed = parseSingleValue(args);
        Target target = resolveTarget(parsed.targetName());
        if (target == null) {
            sendPlayerNotFound(sender, parsed.targetName());
            return true;
        }
        String requested = parsed.value().toLowerCase(Locale.ROOT);
        if (isReset(requested)) {
            styleService.setColor(target.playerId(), null);
            sender.sendMessage(withPlayer(message("messages.commands.chatcolor-reset",
                    "&aЦвет чата игрока {player} сброшен."), target.name()));
            return true;
        }

        String color = COLORS.get(requested);
        if (color == null && HEX_COLOR.matcher(requested).matches()) {
            if (!plugin.getConfig().getBoolean("allow-hex-colors", true)) {
                sender.sendMessage(message("messages.commands.hex-colors-disabled",
                        "&cHEX-цвета отключены в config.yml."));
                return true;
            }
            color = requested.startsWith("#") ? "&" + requested : "&#" + requested;
        }
        if (color == null) {
            sender.sendMessage(message("messages.commands.invalid-chatcolor",
                    "&cНеизвестный цвет. Используйте русское название цвета или HEX (#RRGGBB)."));
            return true;
        }

        styleService.setColor(target.playerId(), color);
        sender.sendMessage(withPlayer(message("messages.commands.chatcolor-set",
                "&aЦвет чата игрока {player} установлен."), target.name()));
        return true;
    }

    private boolean setFonts(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender, "chatfont");
            return true;
        }

        boolean legacyValueFirst = isReset(args[0]) || isFontList(args[0]);
        String targetName = legacyValueFirst ? args[args.length - 1] : args[0];
        int from = legacyValueFirst ? 0 : 1;
        int to = legacyValueFirst ? args.length - 1 : args.length;
        List<String> requestedFonts = Arrays.asList(Arrays.copyOfRange(args, from, to));
        Target target = resolveTarget(targetName);
        if (target == null) {
            sendPlayerNotFound(sender, targetName);
            return true;
        }

        if (requestedFonts.size() == 1 && isReset(requestedFonts.getFirst())) {
            styleService.setFonts(target.playerId(), List.of());
            sender.sendMessage(withPlayer(message("messages.commands.chatfont-reset",
                    "&aСтили чата игрока {player} сброшены."), target.name()));
            return true;
        }

        List<String> fonts = new ArrayList<>();
        for (String argument : requestedFonts) {
            for (String fontName : argument.split(",")) {
                String font = FONTS.get(fontName.toLowerCase(Locale.ROOT));
                if (font == null) {
                    sender.sendMessage(message("messages.commands.invalid-chatfont",
                            "&cНеизвестный стиль. Доступно: жирный, курсив, подчеркнутый, зачеркнутый, магический."));
                    return true;
                }
                if (!fonts.contains(font)) {
                    fonts.add(font);
                }
            }
        }

        styleService.setFonts(target.playerId(), fonts);
        sender.sendMessage(withPlayer(message("messages.commands.chatfont-set",
                "&aСтили чата игрока {player} установлены."), target.name()));
        return true;
    }

    private boolean setNickname(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender, "nick");
            return true;
        }

        Target target = resolveTarget(args[0]);
        if (target == null) {
            sendPlayerNotFound(sender, args[0]);
            return true;
        }
        String nickname = joinValue(args, 1);
        if (isReset(nickname)) {
            styleService.setNickname(target.playerId(), null);
            sender.sendMessage(withPlayer(message("messages.commands.nick-reset",
                    "&aНик игрока {player} сброшен."), target.name()));
            return true;
        }
        if (!isValidText(nickname, MAX_NICKNAME_LENGTH)) {
            sender.sendMessage(message("messages.commands.invalid-nick",
                    "&cНовый ник должен содержать от 1 до 64 символов без управляющих знаков."));
            return true;
        }

        styleService.setNickname(target.playerId(), nickname);
        sender.sendMessage(withValue(withPlayer(message("messages.commands.nick-set",
                "&aНик игрока {player} изменён на {value}&a."), target.name()), nickname));
        return true;
    }

    private boolean setPrefix(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender, "prefix");
            return true;
        }

        Target target = resolveTarget(args[0]);
        if (target == null) {
            sendPlayerNotFound(sender, args[0]);
            return true;
        }
        String prefix = joinValue(args, 1);
        if (isReset(prefix)) {
            styleService.setCustomPrefix(target.playerId(), null);
            sender.sendMessage(withPlayer(message("messages.commands.prefix-reset",
                    "&aПрефикс игрока {player} сброшен."), target.name()));
            return true;
        }
        if (!isValidText(prefix, MAX_PREFIX_LENGTH)) {
            sender.sendMessage(message("messages.commands.invalid-prefix",
                    "&cПрефикс должен содержать от 1 до 128 символов без управляющих знаков."));
            return true;
        }

        styleService.setCustomPrefix(target.playerId(), prefix);
        sender.sendMessage(withValue(withPlayer(message("messages.commands.prefix-set",
                "&aПрефикс игрока {player} установлен: {value}&a."), target.name()), prefix));
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        String commandName = command.getName().toLowerCase(Locale.ROOT);
        if (!isStyleCommand(commandName) || !sender.hasPermission("pnchat." + commandName)) {
            return List.of();
        }

        if (args.length == 1) {
            return matching(Bukkit.getOnlinePlayers().stream().map(player -> player.getName()).toList(), args[0]);
        }

        if (commandName.equals("chatcolor") && args.length == 2) {
            return matching(withReset(COLOR_NAMES), args[1]);
        }
        if (commandName.equals("chatfont") && args.length >= 2) {
            return completeFont(args[args.length - 1]);
        }
        if ((commandName.equals("nick") || commandName.equals("prefix")) && args.length == 2) {
            return matching(List.of(RESET_VALUE), args[1]);
        }
        return List.of();
    }

    private ParsedValue parseSingleValue(String[] args) {
        if (isColorValue(args[0])) {
            return new ParsedValue(args[1], args[0]);
        }
        return new ParsedValue(args[0], args[1]);
    }

    private boolean isColorValue(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return isReset(normalized) || COLORS.containsKey(normalized) || HEX_COLOR.matcher(normalized).matches();
    }

    private boolean isFontList(String value) {
        for (String part : value.split(",")) {
            if (!FONTS.containsKey(part.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    private Target resolveTarget(String input) {
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) {
            return new Target(online.getUniqueId(), online.getName());
        }

        OfflinePlayer player = Bukkit.getOfflinePlayer(input);
        if (!player.hasPlayedBefore()) {
            return null;
        }
        String knownName = player.getName();
        return new Target(player.getUniqueId(), knownName == null || knownName.isBlank() ? input : knownName);
    }

    private void sendPlayerNotFound(CommandSender sender, String input) {
        sender.sendMessage(message("messages.commands.player-not-found",
                "&cИгрок {player} не найден.").replace("{player}", input));
    }

    private void sendUsage(CommandSender sender, String commandName) {
        String fallback = switch (commandName) {
            case "chatcolor" -> "&cИспользование: /chatcolor <игрок> <цвет|none>";
            case "chatfont" -> "&cИспользование: /chatfont <игрок> <стиль...|none>";
            case "nick" -> "&cИспользование: /nick <игрок> <новый ник|none>";
            case "prefix" -> "&cИспользование: /prefix <игрок> <префикс|none>";
            default -> "&cНеверное использование команды.";
        };
        sender.sendMessage(message("messages.commands." + commandName + "-usage", fallback));
    }

    private String message(String path, String fallback) {
        return ChatUtils.color(plugin.getMessage(path, fallback));
    }

    private static String withPlayer(String message, String player) {
        return message.replace("{player}", player);
    }

    private static String withValue(String message, String value) {
        return message.replace("{value}", value);
    }

    private static String joinValue(String[] args, int start) {
        return String.join(" ", Arrays.copyOfRange(args, start, args.length)).trim();
    }

    private static boolean isValidText(String value, int maxLength) {
        if (value == null || value.isBlank() || value.length() > maxLength) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isISOControl(current)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isReset(String value) {
        return value != null && value.equalsIgnoreCase(RESET_VALUE);
    }

    private static boolean isStyleCommand(String commandName) {
        return commandName.equals("chatcolor") || commandName.equals("chatfont")
                || commandName.equals("nick") || commandName.equals("prefix");
    }

    private static List<String> withReset(List<String> values) {
        List<String> result = new ArrayList<>(values);
        result.add(RESET_VALUE);
        return result;
    }

    private static List<String> completeFont(String input) {
        int separator = input.lastIndexOf(',');
        if (separator < 0) {
            return matching(withReset(FONT_NAMES), input);
        }

        String prefix = input.substring(0, separator + 1);
        String partial = input.substring(separator + 1);
        return matching(FONT_NAMES, partial).stream().map(prefix::concat).toList();
    }

    private static List<String> matching(Iterable<String> values, String input) {
        List<String> result = new ArrayList<>();
        String partial = input.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(partial)) {
                result.add(value);
            }
        }
        return result;
    }

    private static Map<String, String> createColors() {
        Map<String, String> colors = new LinkedHashMap<>();
        colors.put("черный", "&0");
        colors.put("чёрный", "&0");
        colors.put("темно-синий", "&1");
        colors.put("тёмно-синий", "&1");
        colors.put("темно-зеленый", "&2");
        colors.put("тёмно-зелёный", "&2");
        colors.put("темно-бирюзовый", "&3");
        colors.put("тёмно-бирюзовый", "&3");
        colors.put("темно-красный", "&4");
        colors.put("тёмно-красный", "&4");
        colors.put("темно-фиолетовый", "&5");
        colors.put("тёмно-фиолетовый", "&5");
        colors.put("золотой", "&6");
        colors.put("серый", "&7");
        colors.put("темно-серый", "&8");
        colors.put("тёмно-серый", "&8");
        colors.put("синий", "&9");
        colors.put("зеленый", "&a");
        colors.put("зелёный", "&a");
        colors.put("голубой", "&b");
        colors.put("бирюзовый", "&b");
        colors.put("красный", "&c");
        colors.put("розовый", "&d");
        colors.put("желтый", "&e");
        colors.put("жёлтый", "&e");
        colors.put("белый", "&f");
        return Map.copyOf(colors);
    }

    private static Map<String, String> createFonts() {
        Map<String, String> fonts = new LinkedHashMap<>();
        fonts.put("жирный", "&l");
        fonts.put("курсив", "&o");
        fonts.put("подчеркнутый", "&n");
        fonts.put("подчёркнутый", "&n");
        fonts.put("зачеркнутый", "&m");
        fonts.put("зачёркнутый", "&m");
        fonts.put("магический", "&k");
        return Map.copyOf(fonts);
    }

    private record Target(UUID playerId, String name) {
    }

    private record ParsedValue(String targetName, String value) {
    }
}
