package ac.grim.grimac.utils.anticheat.update;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.HitData;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import com.github.retrooper.packetevents.protocol.world.BlockFace;
import com.github.retrooper.packetevents.util.Vector3i;
import lombok.Getter;

public class PostBlockPlace extends BlockPlace {
    @Getter
    boolean isFlying;

    boolean hasLook;
    @Getter
    float yaw;
    @Getter
    float pitch;

    public PostBlockPlace(GrimPlayer player, InteractionHand hand, Vector3i blockPosition, BlockFace face, ItemStack itemStack, HitData hitData) {
        this(player, hand, blockPosition, face, itemStack, hitData, false, false, 0f, 0f);
    }

    public PostBlockPlace(GrimPlayer player, InteractionHand hand, Vector3i blockPosition, BlockFace face, ItemStack itemStack, HitData hitData, boolean isFlying, boolean hasLook, float yaw, float pitch) {
        super(player, hand, blockPosition, face, itemStack, hitData);

        this.isFlying = isFlying;
        this.hasLook = hasLook;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public PostBlockPlace(GrimPlayer player, BlockPlace place) {
        this(player, place, false, false, 0f, 0f);
    }

    public PostBlockPlace(GrimPlayer player, BlockPlace place, boolean isFlying, boolean hasLook, float yaw, float pitch) {
        this(player, place.getHand(), place.getPlacedAgainstBlockLocation(), place.getDirection(), place.getItemStack(), place.getHitData(), isFlying, hasLook, yaw, pitch);
    }


    public boolean hasLook() {
        return this.hasLook;
    }
}
