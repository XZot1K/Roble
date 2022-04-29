package dev.zotware.roble.util.channels;

import dev.zotware.roble.RoblePlugin;
import dev.zotware.roble.util.channels.events.ChannelReceiveEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;

public class ChannelHandler implements PluginMessageListener {

    private final RoblePlugin INSTANCE;
    private final ArrayList<String> channels;

    public ChannelHandler(@NotNull RoblePlugin instance) {
        INSTANCE = instance;
        channels = new ArrayList<>();

        INSTANCE.getServer().getMessenger().registerOutgoingPluginChannel(INSTANCE, "BungeeCord");
        INSTANCE.getServer().getMessenger().registerIncomingPluginChannel(INSTANCE, "BungeeCord", this);
    }

    public void registerChannel(@NotNull String identifier, @NotNull ChannelType channelType) {
        switch (channelType) {

            case INCOMING: {
                if (INSTANCE.getServer().getMessenger().isIncomingChannelRegistered(INSTANCE, identifier)) {
                    INSTANCE.getServer().getLogger().warning("The channel \"" + identifier + "\" is already register as an \""
                            + channelType.name() + "\" channel.");
                    return;
                }

                INSTANCE.getServer().getMessenger().registerIncomingPluginChannel(INSTANCE, identifier, this);
            }

            case OUTGOING: {
                if (INSTANCE.getServer().getMessenger().isOutgoingChannelRegistered(INSTANCE, identifier)) {
                    INSTANCE.getServer().getLogger().warning("The channel \"" + identifier + "\" is already register as an \""
                            + channelType.name() + "\" channel.");
                    return;
                }

                INSTANCE.getServer().getMessenger().registerOutgoingPluginChannel(INSTANCE, identifier);
            }

            case BOTH: {
                if (INSTANCE.getServer().getMessenger().isIncomingChannelRegistered(INSTANCE, identifier))
                    INSTANCE.getServer().getLogger().warning("The channel \"" + identifier + "\" is already register as an \""
                            + ChannelType.INCOMING.name() + "\" channel.");
                else INSTANCE.getServer().getMessenger().registerIncomingPluginChannel(INSTANCE, identifier, this);

                if (INSTANCE.getServer().getMessenger().isOutgoingChannelRegistered(INSTANCE, identifier))
                    INSTANCE.getServer().getLogger().warning("The channel \"" + identifier + "\" is already register as an \""
                            + ChannelType.OUTGOING.name() + "\" channel.");
                else INSTANCE.getServer().getMessenger().registerOutgoingPluginChannel(INSTANCE, identifier);


            }

        }
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            final String subChannel = in.readUTF();

            if (!channel.equals("BungeeCord")) {

                if (getChannels().contains(channel)) {
                    final ChannelReceiveEvent event = new ChannelReceiveEvent(channel, subChannel);
                    INSTANCE.getServer().getPluginManager().callEvent(event);
                }

                return;
            }

            // channel is "BungeeCord"


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void send(@NotNull String channel, @NotNull String message) {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(b)) {

            out.writeUTF(channel);
            out.writeUTF(message);

            INSTANCE.getServer().sendPluginMessage(INSTANCE, channel, b.toByteArray());
        } catch (IOException e) {e.printStackTrace();}
    }

    public void connect(@NotNull Player player, @NotNull String server) {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(b)) {

            out.writeUTF("Connect");
            out.writeUTF(server);

            player.sendPluginMessage(INSTANCE, "BungeeCord", b.toByteArray());
        } catch (IOException e) {e.printStackTrace();}
    }

    public void connectOther(@NotNull String playerName, @NotNull String server) {
        try (ByteArrayOutputStream b = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(b)) {

            out.writeUTF("ConnectOther");
            out.writeUTF(playerName);
            out.writeUTF(server);

            INSTANCE.getServer().sendPluginMessage(INSTANCE, "BungeeCord", b.toByteArray());
        } catch (IOException e) {e.printStackTrace();}
    }


    public ArrayList<String> getChannels() {return channels;}

    // TAG getters & setters

    public enum ChannelType {INCOMING, OUTGOING, BOTH}

}