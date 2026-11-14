package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.tags.SyncedTags;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;

import java.util.UUID;

public class PacketEntityArrow extends PacketEntity {

    // only accurate in 1.21.2+, always false otherwise
    public boolean inGround;

    public PacketEntityArrow(GrimPlayer player, UUID uuid, EntityType type, double x, double y, double z) {
        super(player, uuid, type, x, y, z);
    }

    @Override
    public boolean canHit() {
        return this.hasTag(SyncedTags.REDIRECTABLE_PROJECTILE) && super.canHit();
    }
}
