package dev.kreutz.music.bot.music;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.kreutz.music.bot.core.Command;
import dev.kreutz.music.bot.core.CommandGroup;
import dev.kreutz.music.bot.core.Option;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
@CommandGroup
public class Music {

    private final Map<Guild, GuildMusicManager> musicManagers;
    private final AudioPlayerManager playerManager;

    public Music() {
        this.musicManagers = new HashMap<>();

        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
    }

    @Command(name = "play", description = "Plays a song")
    public void play(SlashCommandInteractionEvent event, @Option(name = "query", description = "query", required = true) String query) {
        InteractionHook hook = event.getHook();

        Member member = event.getMember();

        if (member == null)
            return;

        if (!connect(member)) {
            event.deferReply(true).queue();
            hook.sendMessage("You need to be in a voice channel for this command").queue();
        } else {
            event.deferReply().queue();

            GuildMusicManager musicManager = getGuildAudioPlayer(event.getGuild());

            playerManager.loadItemOrdered(musicManager, query, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack track) {
                    hook.sendMessage("Successfully enqueued " + track.getInfo().title).queue();

                    musicManager.queue(track);
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    AudioTrack track = playlist.getSelectedTrack();

                    if (track == null) {
                        track = playlist.getTracks().get(0);
                    }

                    hook.sendMessage("Successfully enqueued " + track.getInfo().title).queue();

                    musicManager.queue(track);
                }

                @Override
                public void noMatches() {
                    hook.sendMessage("No matched found").queue();
                }

                @Override
                public void loadFailed(FriendlyException e) {
                    hook.sendMessage("Could not play: " + e.getMessage()).queue();
                }
            });
        }
    }

    @Command(description = "Skips the song")
    public void skip(SlashCommandInteractionEvent event) {
        InteractionHook hook = event.getHook();
        event.deferReply().queue();

        GuildMusicManager musicManager = getGuildAudioPlayer(event.getGuild());

        musicManager.skip();

    }

    private boolean connect(Member member) {
        GuildVoiceState voiceState = member.getVoiceState();

        if (voiceState == null)
            return false;

        AudioChannel channel = voiceState.getChannel();

        if (channel == null)
            return false;

        Guild guild = member.getGuild();
        AudioManager audioManager = guild.getAudioManager();
        audioManager.openAudioConnection(channel);

        return true;
    }

    private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild) {
        GuildMusicManager musicManager = musicManagers.get(guild);

        if (musicManager == null) {
            musicManager = new GuildMusicManager(playerManager);
            musicManagers.put(guild, musicManager);
        }

        guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

        return musicManager;
    }
}
