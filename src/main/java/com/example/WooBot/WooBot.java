package com.example.WooBot;

import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3AudioTrack;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;
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

                Guild guild = event.getGuild();
                Member member = event.getMember();
                assert member != null;

                if (event.getAuthor().isBot()) return;
                if (event.getMessage().getContentRaw().equals("!list")) {
                    event.getChannel().sendMessage("Command list: !wo, !gachi," +
                            " !gachiporsh, !nanowar, !gargoyle, !stopuu").queue();
                }

                if (event.getMessage().getContentRaw().equals("!wo")) {
                    openConnection(guild, member, event.getTextChannel());
                    playTrackInGuild(guild, () -> getTrack("woooo.mp3"));
                }

                if (event.getMessage().getContentRaw().equals("!gachi")) {
                    openConnection(guild, member, event.getTextChannel());
                    playTrackInGuild(guild, () -> getTrack("MainThemerevamped.mp3"));
                }

                if (event.getMessage().getContentRaw().equals("!gachiporsh")) {
                    openConnection(guild, member, event.getTextChannel());
                    playTrackInGuild(guild, () -> getTrack("Porsh.mp3"));
                }

                if (event.getMessage().getContentRaw().equals("!nanowar")) {
                    openConnection(guild, member, event.getTextChannel());
                    playTrackInGuild(guild, () -> getTrack("Hyusbekistan.mp3"));
                }

                if (event.getMessage().getContentRaw().equals("!gargoyle")) {
                    openConnection(guild, member, event.getTextChannel());
                    playTrackInGuild(guild, () -> getTrack("Gargoyle.mp3"));
                }

                if (event.getMessage().getContentRaw().equals("!zombie")) {
                    openConnection(guild, member, event.getTextChannel());
                    playTrackInGuild(guild, () -> getTrack("Zombieland.mp3"));
                }

                if (event.getMessage().getContentRaw().equals("!stopuu")) {
                    closeConnection(guild);
                }
            }

            @Override
            public void onGuildVoiceLeave(@Nonnull GuildVoiceLeaveEvent event) {
                Guild guild = event.getGuild();
                AudioManager manager = guild.getAudioManager();
                VoiceChannel connectedChannel = manager.getConnectedChannel();

                if (connectedChannel != null) {
                    Collection<Member> connectedUsers = connectedChannel.getMembers();
                    if (connectedUsers.size() <= 1) {
                        closeConnection(guild);
                    }
                }
            }

            @Override
            public void onGuildVoiceMove(@Nonnull GuildVoiceMoveEvent event) {
                Guild guild = event.getGuild();
                AudioManager manager = guild.getAudioManager();
                VoiceChannel connectedChannel = manager.getConnectedChannel();

                if (connectedChannel != null) {
                    Collection<Member> connectedUsers = connectedChannel.getMembers();
                    if (connectedUsers.size() <= 1) {
                        closeConnection(guild);
                    }
                }
            }
        };
        wooBot.addEventListener(listener);

    }

    @NotNull
    private static Mp3AudioTrack getTrack(String name) {
        File localFile = new File(Objects.requireNonNull(WooBot.class.getClassLoader().getResource(name)).getFile());
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

        if (checkIfConnected(guild)) {
            player.addListener(event -> {
                System.out.println("player listener is activated on event: " + event);
                if (event instanceof TrackEndEvent) {
                    if (nextTrack.equals(currentTrack)) {
                        System.out.println(currentTrack + " is playing again");
                        player.playTrack(trackSupplier.get());
                    }
                }
            });
        } else {
            player.addListener(event -> {
                player.setPaused(true);
                System.out.println("player listener is activated on event: " + event);
                System.out.println("what player is playing? - " + trackSupplier.get().getIdentifier());
            });
        }
    }

    private static void closeConnection(Guild guild) {
        AudioManager manager = guild.getAudioManager();
        manager.closeAudioConnection();
    }

    private static void openConnection(Guild guild, Member member, TextChannel channel) {
        VoiceChannel currentChannel = (Objects.requireNonNull((member.getVoiceState())).getChannel());
        AudioManager manager = guild.getAudioManager();
        try {
            manager.openAudioConnection(currentChannel);
            System.out.println(currentChannel);
        } catch (Throwable error) {
            channel.sendMessage("You are not connected to any channel!").queue();
        }
    }

    @NotNull
    private static AudioPlayer getPlayerByPlayerManager(AudioPlayerManager manager) {
        return manager.createPlayer();
    }

    @NotNull
    private static AudioPlayerManager getAudioPlayerManager() {
        return new DefaultAudioPlayerManager();
    }

    private static boolean checkIfConnected(Guild guild) {
        AudioManager manager = guild.getAudioManager();
        return manager.isConnected();
    }
}

