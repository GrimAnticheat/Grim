package ac.grim.grimac.manager.init.load;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.utils.anticheat.LogUtil;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.protocol.chat.ChatTypes;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.enchantment.type.EnchantmentTypes;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes;
import com.github.retrooper.packetevents.util.PEVersion;

import java.util.concurrent.Executors;

public class PacketEventsInit implements LoadableInitable {

    PacketEventsAPI<?> packetEventsAPI;
    PEVersion MINIMUM_REQUIRED_PE_VERSION = new PEVersion(2, 8, 0, true);

    public PacketEventsInit(PacketEventsAPI<?> packetEventsAPI) {
        this.packetEventsAPI = packetEventsAPI;
    }

    @Override
    public void load() {
        LogUtil.info("Loading PacketEvents...");
        PacketEvents.setAPI(packetEventsAPI);

        if (!checkPacketEventsVersion()) {
            LogUtil.error("\n" +
                    "******************************************************\n" +
                    "GrimAC requires PacketEvents >= " + MINIMUM_REQUIRED_PE_VERSION +
                    (MINIMUM_REQUIRED_PE_VERSION.snapshot() ? "-SNAPSHOT" : "") + "\n" +
                    "Current version: " + PacketEvents.getAPI().getVersion() + "\n" +
                    "Please update PacketEvents to a compatible version.\n" +
                    "*****************************************************");
        }

        PacketEvents.getAPI().getSettings()
                .fullStackTrace(true)
                .kickOnPacketException(true)
                .checkForUpdates(false)
                .reEncodeByDefault(false)
                .debug(false);
        PacketEvents.getAPI().load();
        // This may seem useless, but it causes java to start loading stuff async before we need it
        Executors.defaultThreadFactory().newThread(() -> {
            StateTypes.AIR.getName();
            ItemTypes.AIR.getName();
            EntityTypes.PLAYER.getParent();
            EntityDataTypes.BOOLEAN.getName();
            ChatTypes.CHAT.getName();
            EnchantmentTypes.ALL_DAMAGE_PROTECTION.getName();
            ParticleTypes.DUST.getName();
        }).start();
    }

    private boolean checkPacketEventsVersion() {
        PEVersion current = PacketEvents.getAPI().getVersion();
        PEVersion required = MINIMUM_REQUIRED_PE_VERSION;

        // If current version is newer, always accept
        if (current.isNewerThan(required)) {
            return true;
        }

        // If current version is exactly equal to required (including snapshot status), accept
        if (current.major() == required.major()
                && current.minor() == required.minor()
                && current.patch() == required.patch()
                && current.snapshot() == required.snapshot()) {
            return true;
        }

        // If required is a snapshot, accept matching release or snapshot
        if (required.snapshot()
                && current.major() == required.major()
                && current.minor() == required.minor()
                && current.patch() == required.patch()) {
            return true;
        }

        // Otherwise, reject
        return false;
    }
}
