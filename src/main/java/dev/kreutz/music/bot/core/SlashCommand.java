package dev.kreutz.music.bot.core;

import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;

import java.lang.reflect.Method;

public record SlashCommand(Method method, String name, String description, OptionData[] options, CommandPrivilege[] privileges) {
}
