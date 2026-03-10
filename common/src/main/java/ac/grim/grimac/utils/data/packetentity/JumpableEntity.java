package ac.grim.grimac.utils.data.packetentity;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.VectorData;

import java.util.Set;

public interface JumpableEntity {

    boolean isJumping();

    void setJumping(boolean jumping);

    float getJumpPower();

    void setJumpPower(float jumpPower);

    boolean canPlayerJump(GrimPlayer player);

    boolean hasSaddle();

    void executeJump(GrimPlayer player, Set<VectorData> possibleVectors);

}
