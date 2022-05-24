package dev.kreutz.music.bot.core;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Set;
import java.util.concurrent.ExecutionException;

public class Listener extends ListenerAdapter {

    private final Object object;
    private final Set<SlashCommand> commands;

    Listener(Object object, Set<SlashCommand> commands) {
        this.object = object;
        this.commands = commands;
    }

    @Override
    public void onGuildReady(@NotNull GuildReadyEvent event) {
        Guild guild = event.getGuild();

        try {
            guild.retrieveCommands().submit().get().forEach(command -> command.delete().submit());

            for (SlashCommand command : commands) {
                guild.upsertCommand(command.name(), command.description()).queue(c -> {
                    c.editCommand().clearOptions().submit();
                    c.editCommand().addOptions(command.options()).submit();
                    c.updatePrivileges(guild, command.privileges()).submit();
                });
            }
        } catch (InterruptedException | ExecutionException ignored) {
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        SlashCommand command = commands.stream().filter(c -> c.name().equals(event.getName())).findFirst().orElse(null);

        if (command == null)
            return;

        Method method = command.method();
        Parameter[] parameters = method.getParameters();
        Object[] objects = new Object[method.getParameterCount()];
        OptionData[] options = command.options();

        int optionCounter = 0;
        for (int i = 0; i < objects.length; i++) {
            if (SlashCommandInteractionEvent.class.isAssignableFrom(parameters[i].getType())) {
                objects[i] = event;
            } else {
                OptionData option = options[optionCounter++];

                OptionMapping mapping = event.getOption(option.getName());

                if (mapping != null) {
                    switch (option.getType()) {
                        case STRING -> {
                            objects[i] = mapping.getAsString();
                        }
                        case INTEGER -> {
                            objects[i] = mapping.getAsInt();
                        }
                        case BOOLEAN -> {
                            objects[i] = mapping.getAsBoolean();
                        }
                        case USER -> {
                            objects[i] = mapping.getAsUser();
                        }
                        case CHANNEL -> {
                            //TODO: Channel
                        }
                        case ROLE -> {
                            objects[i] = mapping.getAsRole();
                        }
                        case MENTIONABLE -> {
                            objects[i] = mapping.getAsMentionable();
                        }
                        case NUMBER -> {
                            //TODO: Number
                        }
                        case ATTACHMENT -> {
                            objects[i] = mapping.getAsAttachment();
                        }
                    }
                }
            }
        }

        try {
            method.invoke(object, objects);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
