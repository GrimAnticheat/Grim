package ac.grim.grimac.utils.collisions.types;

import ac.grim.grimac.utils.blockstate.BaseBlockState;
import ac.grim.grimac.utils.collisions.CollisionBox;
import ac.grim.grimac.utils.data.ProtocolVersion;

import java.util.List;

public class DynamicCollisionBox implements CollisionBox {

    private final CollisionFactory box;
    private BaseBlockState block;
    private ProtocolVersion version;
    private int x, y, z;

    public DynamicCollisionBox(CollisionFactory box, BaseBlockState block, ProtocolVersion version) {
        this.box = box;
        this.block = block;
        this.version = version;
    }

    @Override
    public boolean isCollided(CollisionBox other) {
        return box.fetch(version, block, x, y, z).offset(x, y, z).isCollided(other);
    }

    @Override
    public boolean isIntersected(CollisionBox other) {
        return box.fetch(version, block, x, y, z).offset(x, y, z).isIntersected(other);
    }

    @Override
    public CollisionBox copy() {
        return new DynamicCollisionBox(box, block, version).offset(x, y, z);
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
        box.fetch(version, block, x, y, z).offset(x, y, z).downCast(list);
    }

    @Override
    public boolean isNull() {
        return box.fetch(version, block, x, y, z).isNull();
    }

    @Override
    public boolean isFullBlock() {
        return false;
    }

    public void setBlock(BaseBlockState block) {
        this.block = block;
    }

    public void setVersion(ProtocolVersion version) {
        this.version = version;
    }
}