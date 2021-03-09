package com.example.WooBot;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class MyListenerAdapter extends ListenerAdapter {
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) throws PermissionException {
        System.out.println("We received a message from " +
                event.getAuthor().getName() + ": " +
                event.getMessage().getContentDisplay()
        );

        if(event.getAuthor().isBot()) return;
        if(event.getMessage().getContentRaw().equals("!list")) {
            event.getChannel().sendMessage("Command list: !wo, !stopuu").queue();
        }
    }

}

