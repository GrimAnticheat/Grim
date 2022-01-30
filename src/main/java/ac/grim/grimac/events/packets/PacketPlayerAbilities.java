package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.impl.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.impl.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerAbilities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerAbilities;

public class PacketPlayerAbilities extends PacketListenerAbstract {

    public PacketPlayerAbilities() {
        super(PacketListenerPriority.MONITOR);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.PLAYER_ABILITIES) {
            WrapperPlayClientPlayerAbilities abilities = new WrapperPlayClientPlayerAbilities(event);
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (player == null) return;

            // In one tick you can do the following
            // - Start flying, send server abilities that you are flying
            // - Make flying movement
            // - Stop flying, send server abilities that you are no longer flying, in the same tick.
            // 1.8 through 1.17, and likely 1.7 too.
            //
            // To do this, you need to:
            // - Gain a good amount of downwards momentum
            // - Tap jump once just before the ground
            // - The tick before you you hit the ground, tap space again
            // - This causes you to start flying
            //-  Downwards momentum causes you to stop flying after you hit the ground
            // - This causes you to stop flying in the same tick
            //
            // I mean, it's logical, but packet order is wrong.  At least it is easy to fix:
            if (player.compensatedFlying.lastToggleTransaction == player.lastTransactionReceived.get())
                player.compensatedFlying.lagCompensatedIsFlyingMap.put(player.lastTransactionReceived.get() + 1, abilities.isFlying());
            else
                player.compensatedFlying.lagCompensatedIsFlyingMap.put(player.lastTransactionReceived.get(), abilities.isFlying());

            player.compensatedFlying.lastToggleTransaction = player.lastTransactionReceived.get();
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_ABILITIES) {
            WrapperPlayServerPlayerAbilities abilities = new WrapperPlayServerPlayerAbilities(event);
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());

            // Occurs on login - we set if the player can fly on PlayerJoinEvent
            if (player == null) return;

            player.compensatedFlying.setCanPlayerFly(abilities.isFlightAllowed());
            player.compensatedFlying.lagCompensatedIsFlyingMap.put(player.lastTransactionSent.get() + 1, abilities.isFlying());

            event.getPostTasks().add(player::sendTransaction);
        }
    }
}
