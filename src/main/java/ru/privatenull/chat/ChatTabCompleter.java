package ru.privatenull.chat;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ChatTabCompleter implements TabCompleter {

    private static final List<String> COMPLETIONS = List.of("reload", "ac", "adminchat");

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                       @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) {
            return new ArrayList<>();
        }

        String partial = args[0].toLowerCase();
        List<String> result = new ArrayList<>();

        for (String completion : COMPLETIONS) {
            if (completion.startsWith(partial)) {
                result.add(completion);
            }
        }

        return result;
    }
}
