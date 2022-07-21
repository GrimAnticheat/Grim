package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.manager.init.Initable;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ExemptOnlinePlayers implements Initable {
    @Override
    public void start() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
            GrimAPI.INSTANCE.getPlayerDataManager().exemptUsers.add(user);
        }
    }
}
