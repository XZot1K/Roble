package dev.zotware.roble.util.channels.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ChannelReceiveEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final String channel, dataLine;

    public ChannelReceiveEvent(@NotNull String channel, @NotNull String dataLine) {
        this.channel = channel;
        this.dataLine = dataLine;
    }

    // TAG getters & setters

    public static HandlerList getHandlerList() {return handlers;}

    public @NotNull HandlerList getHandlers() {return handlers;}

    public String getChannel() {return channel;}

    public String getDataLine() {return dataLine;}

}