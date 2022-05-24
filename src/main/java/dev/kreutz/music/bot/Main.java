package dev.kreutz.music.bot;

import dev.kreutz.music.bot.core.Runner;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;

public class Main {

    public static void main(String[] args) throws LoginException {
        JDA jda = JDABuilder.create("token", GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS)).build();

        Runner.run(jda, Main.class);
    }
}
