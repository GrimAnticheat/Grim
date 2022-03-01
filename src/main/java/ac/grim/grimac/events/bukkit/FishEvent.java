package ac.grim.grimac.events.bukkit;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.util.Vector;

import java.util.ArrayList;

// Replace stupid client-sided fishing mechanic with explosion packet
// Client-sided mechanic uses interpolated position which is
// impossible to compute on 1.9+ because of the lack of the idle packet.
// Why the hell did mojang decide to do this? The explosion packet exists for a reason.
public class FishEvent implements Listener {
    @EventHandler(priority = EventPriority.MONITOR)
    public void onFishEvent(PlayerFishEvent event) {
        if (event.getPlayer().hasMetadata("NPC")) return;
        if (event.getCaught() instanceof Player && event.getState() == PlayerFishEvent.State.CAUGHT_ENTITY) {
            GrimPlayer player = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer((Player) event.getCaught());
            if (player == null) return;

            // Hide the explosion noise
            // going too far will cause a memory leak in the client
            // So 256 blocks is good enough and far past the minimum 16 blocks away we need to be for no sound
            Vector3f pos = new Vector3f((float) player.x, (float) (player.y - 256), (float) player.z);

            // Exact calculation
            Vector diff = event.getPlayer().getLocation().subtract(event.getCaught().getLocation()).toVector().multiply(0.1);
            Vector3f diffF = new Vector3f((float) diff.getX(), (float) diff.getY(), (float) diff.getZ());

            WrapperPlayServerExplosion explosion = new WrapperPlayServerExplosion(pos, 0, new ArrayList<>(), diffF);
            // There we go, this is how you implement this packet correctly, Mojang.
            // Please stop being so stupid.
            PacketEvents.getAPI().getPlayerManager().sendPacket(event.getCaught(), explosion);
        }
    }
}
