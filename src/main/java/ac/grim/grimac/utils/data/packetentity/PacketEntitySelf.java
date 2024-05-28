package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.collisions.datatypes.SimpleCollisionBox;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.potion.PotionType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

public class PacketEntitySelf extends PacketEntity {
    public WrapperPlayServerUpdateAttributes.Property playerSpeed = new WrapperPlayServerUpdateAttributes.Property("MOVEMENT_SPEED", 0.1f, new ArrayList<>());

    private final GrimPlayer player;
    @Getter
    @Setter
    int opLevel;
    @Getter
    @Setter
    float jumpStrength = 0.42f;
    @Getter
    @Setter
    double breakSpeedMultiplier = 1.0, entityInteractRange = 3, blockInteractRange = 4.5;

    public double getBlockInteractRange() {
        // Server versions older than 1.20.5 don't send the attribute, if the player is in creative then assume legacy max reach distance.
        // Or if they are on a client version older than 1.20.5.
        if (player.gamemode == GameMode.CREATIVE
                && (player.getClientVersion().isOlderThan(ClientVersion.V_1_20_5)
                    || PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_20_5))) {
            return 5.0;
        }
        return blockInteractRange;
    }

    public PacketEntitySelf(GrimPlayer player) {
        super(EntityTypes.PLAYER);
        this.player = player;
    }

    public PacketEntitySelf(GrimPlayer player, PacketEntitySelf old) {
        super(EntityTypes.PLAYER);
        this.player = player;
        this.opLevel = old.opLevel;
        this.jumpStrength = old.jumpStrength;
        this.gravityAttribute = old.gravityAttribute;
        this.entityInteractRange = old.entityInteractRange;
        this.blockInteractRange = old.blockInteractRange;
        this.scale = old.scale;
        this.stepHeight = old.stepHeight;
    }

    public boolean inVehicle() {
        return getRiding() != null;
    }

    @Override
    public void addPotionEffect(PotionType effect, int amplifier) {
        player.pointThreeEstimator.updatePlayerPotions(effect, amplifier);
        super.addPotionEffect(effect, amplifier);
    }

    @Override
    public void removePotionEffect(PotionType effect) {
        player.pointThreeEstimator.updatePlayerPotions(effect, null);
        super.removePotionEffect(effect);
    }

    @Override
    public void onFirstTransaction(boolean relative, boolean hasPos, double relX, double relY, double relZ, GrimPlayer player) {
        // Player ignores this
    }

    @Override
    public void onSecondTransaction() {
        // Player ignores this
    }

    @Override
    public SimpleCollisionBox getPossibleCollisionBoxes() {
        return player.boundingBox.copy(); // Copy to retain behavior of PacketEntity
    }
}
