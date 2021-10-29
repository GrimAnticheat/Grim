package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import io.github.retrooper.packetevents.event.PacketListenerAbstract;
import io.github.retrooper.packetevents.event.PacketListenerPriority;
import io.github.retrooper.packetevents.event.impl.PacketPlayReceiveEvent;
import io.github.retrooper.packetevents.event.impl.PacketPlaySendEvent;
import io.github.retrooper.packetevents.packettype.PacketType;
import io.github.retrooper.packetevents.packetwrappers.play.in.abilities.WrappedPacketInAbilities;
import io.github.retrooper.packetevents.packetwrappers.play.out.abilities.WrappedPacketOutAbilities;

public class PacketPlayerAbilities extends PacketListenerAbstract {

    public PacketPlayerAbilities() {
        super(PacketListenerPriority.MONITOR);
    }

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        if (event.getPacketId() == PacketType.Play.Client.ABILITIES) {
            WrappedPacketInAbilities abilities = new WrappedPacketInAbilities(event.getNMSPacket());
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());
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
    public void onPacketPlaySend(PacketPlaySendEvent event) {
        if (event.getPacketId() == PacketType.Play.Server.ABILITIES) {
            WrappedPacketOutAbilities abilities = new WrappedPacketOutAbilities(event.getNMSPacket());
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());

            // Occurs on login - we set if the player can fly on PlayerJoinEvent
            if (player == null) return;

            player.compensatedFlying.setCanPlayerFly(abilities.isFlightAllowed());
            player.compensatedFlying.lagCompensatedIsFlyingMap.put(player.lastTransactionSent.get() + 1, abilities.isFlying());

            event.setPostTask(player::sendTransaction);
        }
    }
}
