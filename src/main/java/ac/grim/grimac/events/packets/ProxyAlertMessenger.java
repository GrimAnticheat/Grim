package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.utils.anticheat.LogUtil;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPluginMessage;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.*;

public class ProxyAlertMessenger extends PacketListenerAbstract {
    private static boolean usingProxy;

    public ProxyAlertMessenger() {
        usingProxy = ProxyAlertMessenger.getBooleanFromFile("spigot.yml", "settings.bungeecord")
                || ProxyAlertMessenger.getBooleanFromFile("paper.yml", "settings.velocity-support.enabled")
                || (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_19) && ProxyAlertMessenger.getBooleanFromFile("config/paper-global.yml", "proxies.velocity.enabled"));

        if (usingProxy) {
            LogUtil.info("Registering an outgoing plugin channel...");
            GrimAPI.INSTANCE.getPlugin().getServer().getMessenger().registerOutgoingPluginChannel(GrimAPI.INSTANCE.getPlugin(), "BungeeCord");
        }
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.PLUGIN_MESSAGE || !ProxyAlertMessenger.canReceiveAlerts())
            return;

        WrapperPlayClientPluginMessage wrapper = new WrapperPlayClientPluginMessage(event);

        if (!wrapper.getChannelName().equals("BungeeCord") && !wrapper.getChannelName().equals("bungeecord:main"))
            return;

        ByteArrayDataInput in = ByteStreams.newDataInput(wrapper.getData());

        if (!in.readUTF().equals("GRIMAC")) return;

        final String alert;
        byte[] messageBytes = new byte[in.readShort()];
        in.readFully(messageBytes);

        try {
            alert = new DataInputStream(new ByteArrayInputStream(messageBytes)).readUTF();
        } catch (IOException exception) {
            LogUtil.error("Something went wrong whilst reading an alert forwarded from another server!");
            exception.printStackTrace();
            return;
        }

        for (Player bukkitPlayer : GrimAPI.INSTANCE.getAlertManager().getEnabledAlerts())
            bukkitPlayer.sendMessage(alert);
    }

    public static void sendPluginMessage(String message) {
        if (!canSendAlerts())
            return;

        ByteArrayOutputStream messageBytes = new ByteArrayOutputStream();
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ONLINE");
        out.writeUTF("GRIMAC");

        try {
            new DataOutputStream(messageBytes).writeUTF(message);
        } catch (IOException exception) {
            LogUtil.error("Something went wrong whilst forwarding an alert to other servers!");
            exception.printStackTrace();
            return;
        }

        out.writeShort(messageBytes.toByteArray().length);
        out.write(messageBytes.toByteArray());

        Iterables.getFirst(Bukkit.getOnlinePlayers(), null).sendPluginMessage(GrimAPI.INSTANCE.getPlugin(), "BungeeCord", out.toByteArray());
    }

    public static boolean canSendAlerts() {
        return usingProxy && GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("alerts.proxy.send", false) && Bukkit.getOnlinePlayers().size() > 0;
    }

    public static boolean canReceiveAlerts() {
        return usingProxy && GrimAPI.INSTANCE.getConfigManager().getConfig().getBooleanElse("alerts.proxy.receive", false) && GrimAPI.INSTANCE.getAlertManager().getEnabledAlerts().size() > 0;
    }

    private static boolean getBooleanFromFile(String pathToFile, String pathToValue) {
        File file = new File(pathToFile);
        if (!file.exists()) return false;
        return YamlConfiguration.loadConfiguration(file).getBoolean(pathToValue);
    }
}