package com.example.WooBot;

import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3AudioTrack;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;
import com.sedmelluq.discord.lavaplayer.player.event.TrackStartEvent;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.local.LocalSeekableInputStream;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;
import java.io.File;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.Supplier;

public class WooBot extends ListenerAdapter {
    public static void main(String[] args) throws LoginException {

        String token = args[0];
        JDABuilder jdaBuilder = JDABuilder.create(EnumSet.allOf(GatewayIntent.class));
        jdaBuilder.setToken(token);

        JDA wooBot = jdaBuilder.build();
        ListenerAdapter listener = new ListenerAdapter() {

            @Override
            public void onMessageReceived(@NotNull MessageReceivedEvent event) throws PermissionException {

                System.out.println("We received a message from " +
                        event.getAuthor().getName() + ": " +
                        event.getMessage().getContentDisplay()
                );
                if (event.getAuthor().isBot()) return;

                Guild guild = event.getGuild();
                String command = event.getMessage().getContentRaw();
                TrackContainer container = new TrackContainer();
                if (container.containsCommand(command)) {
                    openConnection(event);
                    playTrackInGuild(guild, () -> getTrack(container.supplyTrack(command)));
                }

                if (command.equals("!list")) {
                    container.printCommands(event);
                }

                if (command.equals("!stopuu")) {
                    closeConnection(guild);
                }
            }

            @Override
            public void onGuildVoiceLeave(@Nonnull GuildVoiceLeaveEvent event) {
                Guild guild = event.getGuild();
                if (checkChannelIfConnected(guild)) {
                    closeConnection(guild);
                }
            }

            @Override
            public void onGuildVoiceMove(@Nonnull GuildVoiceMoveEvent event) {
                Guild guild = event.getGuild();
                if (checkChannelIfConnected(guild)) {
                    closeConnection(guild);
                }
            }
        };

        wooBot.addEventListener(listener);
    }

    @NotNull
    private static Mp3AudioTrack getTrack(String name) {
        File localFile = new File(Objects.requireNonNull(WooBotTest.class.getClassLoader().getResource(name)).getFile());
        return new Mp3AudioTrack(new AudioTrackInfo("1", "2", 3, name, true,
                localFile.toURI().toString()), new LocalSeekableInputStream(localFile));
    }

    private static void playTrackInGuild(Guild guild, Supplier<AudioTrack> trackSupplier) {
        AudioManager manager = guild.getAudioManager();
        AudioPlayerManager audioManager = getAudioPlayerManager();
        AudioSourceManagers.registerLocalSource(audioManager);
        AudioPlayer player = getPlayerByPlayerManager(audioManager);
        manager.setSendingHandler(new MyAudioSendHandler(player));
        String nextTrack = trackSupplier.get().getIdentifier();

        player.playTrack(trackSupplier.get());
        System.out.println(nextTrack + " is playing");
        String currentTrack = player.getPlayingTrack().getIdentifier();
        System.out.println("is bot connected? - " + manager.getConnectionStatus());
        System.out.println("audio manager is manager of guild: " + manager.getGuild());

        player.addListener(event -> {
            System.out.println("player listener is activated on event: " + event);
            if (event instanceof TrackStartEvent) {
                System.out.println("is bot connected? - " + manager.getConnectionStatus());
            }
            if (event instanceof TrackEndEvent) {
                System.out.println(currentTrack + " is playing again");
                player.playTrack(trackSupplier.get());
            } else {
                player.setPaused(!checkManagerIfConnected(manager));
                System.out.println("is player paused? " + player.isPaused());
                System.out.println("what player was playing? - " + currentTrack);
                if (!checkManagerIfConnected(manager)) {
                    audioManager.shutdown();
                    System.out.println("player was destroyed");
                }
            }
        });
    }

    private static void closeConnection(Guild guild) {
        AudioManager manager = guild.getAudioManager();
        manager.closeAudioConnection();
    }

    private static void openConnection(MessageReceivedEvent event) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        String mentionAuthor = event.getAuthor().getAsMention();
        TextChannel channel = event.getTextChannel();
        assert member != null;
        VoiceChannel currentChannel = (((Objects.requireNonNull(member.getVoiceState()))).getChannel());
        AudioManager manager = guild.getAudioManager();
        try {
            manager.openAudioConnection(currentChannel);
            System.out.println(currentChannel);
        } catch (Throwable error) {
            channel.sendMessage(mentionAuthor + " You are not connected to any channel!").queue();
        }
    }

    private static boolean checkChannelIfConnected(Guild guild) {
        AudioManager manager = guild.getAudioManager();
        VoiceChannel connectedChannel = manager.getConnectedChannel();

        if (connectedChannel != null) {
            Collection<Member> connectedUsers = connectedChannel.getMembers();
            return connectedUsers.size() <= 1;
        }

        return false;
    }

    @NotNull
    private static AudioPlayer getPlayerByPlayerManager(AudioPlayerManager manager) {
        return manager.createPlayer();
    }

    @NotNull
    private static AudioPlayerManager getAudioPlayerManager() {
        return new DefaultAudioPlayerManager();
    }


    /**
     * @param manager - this is an instance of AudioManager, which connections we want to check.
     * @return true - if number of connected channels are not null, false - if they are null.
     */
    private static boolean checkManagerIfConnected(AudioManager manager) {
        return manager.getConnectedChannel() != null;
    }
}

