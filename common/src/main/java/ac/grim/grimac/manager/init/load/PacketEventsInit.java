package ac.grim.grimac.manager.init.load;

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

import java.util.concurrent.Executors;

public class PacketEventsInit implements LoadableInitable {

    private final PacketEventsAPI<?> packetEventsAPI;

    public PacketEventsInit(PacketEventsAPI<?> packetEventsAPI) {
        this.packetEventsAPI = packetEventsAPI;
    }

    @Override
    public void load() {
        LogUtil.info("Loading PacketEvents...");
        PacketEvents.setAPI(packetEventsAPI);
        PacketEvents.getAPI().getSettings()
                .fullStackTrace(true)
                .kickOnPacketException(true)
                .preViaInjection(true)
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
}
