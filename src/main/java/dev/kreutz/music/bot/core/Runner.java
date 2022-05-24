package dev.kreutz.music.bot.core;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Channel;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command.Subcommand;
import net.dv8tion.jda.api.interactions.commands.Command.SubcommandGroup;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class Runner {

    static Logger logger = LoggerFactory.getLogger(Runner.class);

    public static void run(JDA jda, Class<?> clazz) {
        logger.info("Initializing Runner");

        ConfigurationBuilder config = new ConfigurationBuilder()
                .setScanners(Scanners.TypesAnnotated)
                .setUrls(ClasspathHelper.forClass(clazz));

        Reflections reflections = new Reflections(config);

        Set<Class<?>> classes = reflections.getTypesAnnotatedWith(CommandGroup.class);

        for (Class<?> c : classes) {
            try {
                logger.info("Found <" + c + "> as command group");

                Object object = c.getDeclaredConstructor().newInstance();

                Set<SlashCommand> commands = Arrays.stream(c.getMethods()).filter(method -> method.getAnnotation(Command.class) != null).map(Runner::methodToSlashCommand).collect(Collectors.toSet());

                jda.addEventListener(new Listener(object, commands));
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static SlashCommand methodToSlashCommand(Method method) {
        Command command = method.getAnnotation(Command.class);
        String name = command.name().equals("") ? method.getName() : command.name();

        logger.info("Found <" + name + "> command");

        Parameter[] parameters = method.getParameters();

        List<Parameter> temp = Arrays.stream(parameters).filter(parameter -> parameter.getAnnotation(Option.class) == null).toList();

        if (!(temp.size() == 1 && SlashCommandInteractionEvent.class.isAssignableFrom(temp.get(0).getType()))) {
            logger.error("Command <" + name + "> has wrong parameters. It needs at least one SlashCommandInteractionEvent");
            System.exit(-1);
        }

        OptionData[] options = Arrays.stream(parameters).filter(parameter -> parameter.getAnnotation(Option.class) != null).map(Runner::parameterToOptionData).toArray(OptionData[]::new);

        logger.info("Found <" + options.length + "> options for <" + name + ">");

        CommandPrivilege[] privileges = new CommandPrivilege[command.enabledUsers().length + command.disabledUsers().length + command.enabledRoles().length + command.disabledRoles().length];

        int i = 0;
        for (long user : command.enabledUsers()) {
            privileges[i++] = CommandPrivilege.enableUser(user);
        }
        for (long user : command.disabledUsers()) {
            privileges[i++] = CommandPrivilege.disableUser(user);
        }
        for (long role : command.enabledRoles()) {
            privileges[i++] = CommandPrivilege.enableRole(role);
        }
        for (long role : command.disabledRoles()) {
            privileges[i++] = CommandPrivilege.disableRole(role);
        }
        
        return new SlashCommand(method, name, command.description(), options, privileges);
    }

    private static OptionData parameterToOptionData(Parameter parameter) {
        Option option = parameter.getAnnotation(Option.class);

        String name = option.name().equals("") ? parameter.getName() : option.name();

        Class<?> clazz = parameter.getType();

        OptionType type = null;

        if (Subcommand.class.isAssignableFrom(clazz)) {
            type = OptionType.SUB_COMMAND;
        } else if (SubcommandGroup.class.isAssignableFrom(clazz)) {
            type = OptionType.SUB_COMMAND_GROUP;
        } else if (String.class.isAssignableFrom(clazz)) {
            type = OptionType.STRING;
        } else if (Integer.class.isAssignableFrom(clazz)) {
            type = OptionType.INTEGER;
        } else if (Boolean.class.isAssignableFrom(clazz)) {
            type = OptionType.BOOLEAN;
        } else if (User.class.isAssignableFrom(clazz)) {
            type = OptionType.USER;
        } else if (Channel.class.isAssignableFrom(clazz)) {
            type = OptionType.CHANNEL;
        } else if (Role.class.isAssignableFrom(clazz)) {
            type = OptionType.ROLE;
        } else if (IMentionable.class.isAssignableFrom(clazz)) {
            type = OptionType.MENTIONABLE;
        } else if (Number.class.isAssignableFrom(clazz)) {
            type = OptionType.NUMBER;
        } else if (clazz.isPrimitive()) {
            logger.error("Type of parameter <" + name + "> is primitive");
            System.exit(-1);
        } else {
            logger.error("Type of parameter <" + name + "> can't be <" + clazz + ">");
            System.exit(-1);
        }

        return new OptionData(type, name, option.description(), option.required());
    }

}
