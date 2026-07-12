package ru.privatenull.chat;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import ru.privatenull.pnChatPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatStyleCommandExecutor implements CommandExecutor, TabCompleter {

    private static final Map<String, String> COLORS = createColors();
    private static final Map<String, String> FONTS = Map.of(
            "bold", "&l", "жирный", "&l",
            "italic", "&o", "курсив", "&o",
            "underline", "&n", "подчеркнутый", "&n",
            "strike", "&m", "зачеркнутый", "&m",
            "magic", "&k", "магический", "&k"
    );

    private final pnChatPlugin plugin;
    private final ChatStyleService styleService;

    public ChatStyleCommandExecutor(pnChatPlugin plugin, ChatStyleService styleService) {
        this.plugin = plugin;
        this.styleService = styleService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        boolean colorCommand = command.getName().equalsIgnoreCase("chatcolor");
        if (!sender.hasPermission(colorCommand ? "pnchat.chatcolor" : "pnchat.chatfont")) {
            sender.sendMessage(message("messages.admin-chat.no-permission", "&cНедостаточно прав."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(message(colorCommand
                    ? "messages.commands.chatcolor-usage"
                    : "messages.commands.chatfont-usage", colorCommand
                    ? "&cИспользование: /chatcolor <цвет|none> <ник>"
                    : "&cИспользование: /chatfont <шрифт...|none> <ник>"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[args.length - 1]);
        if (colorCommand) {
            setColor(sender, target, args);
        } else {
            setFonts(sender, target, args);
        }
        return true;
    }

    private void setColor(CommandSender sender, OfflinePlayer target, String[] args) {
        if (args.length != 2) {
            sender.sendMessage(message("messages.commands.chatcolor-usage", "&cИспользование: /chatcolor <цвет|none> <ник>"));
            return;
        }
        String value = args[0].toLowerCase(Locale.ROOT);
        if (value.equals("none")) {
            styleService.setColor(target.getUniqueId(), "");
            sender.sendMessage(message("messages.commands.chatcolor-reset", "&aЦвет чата игрока {player} сброшен.").replace("{player}", target.getName()));
            return;
        }
        String color = COLORS.get(value);
        if (color == null && value.matches("#?[0-9a-f]{6}")) {
            color = value.startsWith("#") ? "&" + value : "&#" + value;
        }
        if (color == null) {
            sender.sendMessage(message("messages.commands.invalid-chatcolor", "&cНеизвестный цвет."));
            return;
        }
        styleService.setColor(target.getUniqueId(), color);
        sender.sendMessage(message("messages.commands.chatcolor-set", "&aЦвет чата игрока {player} установлен.").replace("{player}", target.getName()));
    }

    private void setFonts(CommandSender sender, OfflinePlayer target, String[] args) {
        if (args.length == 2 && args[0].equalsIgnoreCase("none")) {
            styleService.setFonts(target.getUniqueId(), List.of());
            sender.sendMessage(message("messages.commands.chatfont-reset", "&aСтили чата игрока {player} сброшены.").replace("{player}", target.getName()));
            return;
        }
        List<String> fonts = new ArrayList<>();
        for (int index = 0; index < args.length - 1; index++) {
            for (String fontName : args[index].split(",")) {
                String font = FONTS.get(fontName.toLowerCase(Locale.ROOT));
                if (font == null) {
                    sender.sendMessage(message("messages.commands.invalid-chatfont", "&cНеизвестный стиль чата."));
                    return;
                }
                if (!fonts.contains(font)) {
                    fonts.add(font);
                }
            }
        }
        styleService.setFonts(target.getUniqueId(), fonts);
        sender.sendMessage(message("messages.commands.chatfont-set", "&aСтили чата игрока {player} установлены.").replace("{player}", target.getName()));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return matching(command.getName().equalsIgnoreCase("chatcolor") ? COLORS.keySet() : FONTS.keySet(), args[0]);
        }
        if (args.length >= 2) {
            return matching(Bukkit.getOnlinePlayers().stream().map(player -> player.getName()).toList(), args[args.length - 1]);
        }
        return List.of();
    }

    private String message(String path, String fallback) {
        return ChatUtils.color(plugin.getMessage(path, fallback));
    }

    private static List<String> matching(Iterable<String> values, String input) {
        List<String> result = new ArrayList<>();
        String partial = input.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value.startsWith(partial)) {
                result.add(value);
            }
        }
        return result;
    }

    private static Map<String, String> createColors() {
        Map<String, String> colors = new LinkedHashMap<>();
        String[] names = {"black", "dark_blue", "dark_green", "dark_aqua", "dark_red", "dark_purple", "gold", "gray", "dark_gray", "blue", "green", "aqua", "red", "light_purple", "yellow", "white"};
        for (int index = 0; index < names.length; index++) {
            colors.put(names[index], "&" + Integer.toHexString(index));
        }
        colors.put("черный", "&0"); colors.put("синий", "&9"); colors.put("зеленый", "&a");
        colors.put("голубой", "&b"); colors.put("красный", "&c"); colors.put("розовый", "&d");
        colors.put("желтый", "&e"); colors.put("белый", "&f");
        return colors;
    }
}
