package com.example.WooBot;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

public class TrackContainer {
    private final Map<String, String> trackContainer =  Map.ofEntries(
            Map.entry("!wo", "woooo.mp3"),
            Map.entry("!gachi", "MainThemerevamped.mp3"),
            Map.entry("!gachiporsh", "Porsh.mp3"),
            Map.entry("!nanowar", "Hyusbekistan.mp3"),
            Map.entry("!gargoyle", "Gargoyle.mp3"),
            Map.entry("!zombie", "Zombieland.mp3")
    );

    private final Set<String> commandSet = trackContainer.keySet();

    public String supplyTrack(String key) {
        return trackContainer.get(key);
    }

    public boolean containsCommand(String key) {
        return trackContainer.get(key) != null;
    }

    public  void printCommands(MessageReceivedEvent event) {
        String[] commands = commandSet.toArray(new String[0]);
        String formattedString = Arrays.toString(commands)
                .replace("[", "")
                .replace("]", "")
                .trim();

        event.getChannel().sendMessage(formattedString).queue();
    }
}
