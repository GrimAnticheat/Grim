package ac.grim.grimac.utils.collisions.datatypes;

import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;

import java.util.List;

public class DynamicCollisionBox implements CollisionBox {

    private final GrimPlayer player;
    private final CollisionFactory box;
    private ClientVersion version;
    private WrappedBlockState block;
    private int x, y, z;

    public DynamicCollisionBox(GrimPlayer player, ClientVersion version, CollisionFactory box, WrappedBlockState block) {
        this.player = player;
        this.version = version;
        this.box = box;
        this.block = block;
    }

    @Override
    public boolean isCollided(SimpleCollisionBox other) {
        return box.fetch(player, version, block, x, y, z).offset(x, y, z).isCollided(other);
    }

    @Override
    public boolean isIntersected(SimpleCollisionBox other) {
        return box.fetch(player, version, block, x, y, z).offset(x, y, z).isIntersected(other);
    }

    @Override
    public CollisionBox copy() {
        return new DynamicCollisionBox(player, version, box, block).offset(x, y, z);
    }

    @Override
    public CollisionBox offset(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    @Override
    public void downCast(List<SimpleCollisionBox> list) {
        box.fetch(player, version, block, x, y, z).offset(x, y, z).downCast(list);
    }

    @Override
    public boolean isNull() {
        return box.fetch(player, version, block, x, y, z).isNull();
    }

    @Override
    public boolean isFullBlock() {
        return box.fetch(player, version, block, x, y, z).offset(x, y, z).isFullBlock();
    }

    public void setBlock(WrappedBlockState block) {
        this.block = block;
    }

    public void setVersion(ClientVersion version) {
        this.version = version;
    }
}