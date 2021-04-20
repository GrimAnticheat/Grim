package ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.server.v1_16_R3.EnumDirection;

import java.util.ArrayList;
import java.util.List;

public final class AABBVoxelShape extends VoxelShape {

    public final AxisAlignedBB aabb;
    private DoubleList cachedListX;
    private DoubleList cachedListY;
    private DoubleList cachedListZ;

    public AABBVoxelShape(AxisAlignedBB aabb) {
        super(VoxelShapes.getFullUnoptimisedCube().getShape());
        this.aabb = aabb;
    }

    @Override
    public boolean isEmpty() {
        return this.aabb.isEmpty();
    }

    @Override
    public double b(EnumDirection.EnumAxis enumdirection_enumaxis) { // getMin
        switch (enumdirection_enumaxis.ordinal()) {
            case 0:
                return this.aabb.minX;
            case 1:
                return this.aabb.minY;
            case 2:
                return this.aabb.minZ;
            default:
                throw new IllegalStateException("Unknown axis requested");
        }
    }

    @Override
    public double c(EnumDirection.EnumAxis enumdirection_enumaxis) { //getMax
        switch (enumdirection_enumaxis.ordinal()) {
            case 0:
                return this.aabb.maxX;
            case 1:
                return this.aabb.maxY;
            case 2:
                return this.aabb.maxZ;
            default:
                throw new IllegalStateException("Unknown axis requested");
        }
    }

    @Override
    public AxisAlignedBB getBoundingBox() { // rets bounding box enclosing this entire shape
        return this.aabb;
    }

    // enum direction axis is from 0 -> 2, so we keep the lower bits for direction axis.
    @Override
    protected double a(EnumDirection.EnumAxis enumdirection_enumaxis, int i) { // getPointFromIndex
        switch (enumdirection_enumaxis.ordinal() | (i << 2)) {
            case (0 | (0 << 2)):
                return this.aabb.minX;
            case (1 | (0 << 2)):
                return this.aabb.minY;
            case (2 | (0 << 2)):
                return this.aabb.minZ;
            case (0 | (1 << 2)):
                return this.aabb.maxX;
            case (1 | (1 << 2)):
                return this.aabb.maxY;
            case (2 | (1 << 2)):
                return this.aabb.maxZ;
            default:
                throw new IllegalStateException("Unknown axis requested");
        }
    }

    @Override
    protected DoubleList a(EnumDirection.EnumAxis enumdirection_enumaxis) { // getPoints
        switch (enumdirection_enumaxis.ordinal()) {
            case 0:
                return this.cachedListX == null ? this.cachedListX = DoubleArrayList.wrap(new double[]{this.aabb.minX, this.aabb.maxX}) : this.cachedListX;
            case 1:
                return this.cachedListY == null ? this.cachedListY = DoubleArrayList.wrap(new double[]{this.aabb.minY, this.aabb.maxY}) : this.cachedListY;
            case 2:
                return this.cachedListZ == null ? this.cachedListZ = DoubleArrayList.wrap(new double[]{this.aabb.minZ, this.aabb.maxZ}) : this.cachedListZ;
            default:
                throw new IllegalStateException("Unknown axis requested");
        }
    }

    @Override
    public VoxelShape a(double d0, double d1, double d2) { // createOffset
        return new AABBVoxelShape(this.aabb.offset(d0, d1, d2));
    }

    @Override
    public VoxelShape c() { // simplify
        return this;
    }

    @Override
    public void b(VoxelShapes.a voxelshapes_a) { // forEachAABB
        voxelshapes_a.consume(this.aabb.minX, this.aabb.minY, this.aabb.minZ, this.aabb.maxX, this.aabb.maxY, this.aabb.maxZ);
    }

    @Override
    public List<AxisAlignedBB> d() { // getAABBs
        List<AxisAlignedBB> ret = new ArrayList<>(1);
        ret.add(this.aabb);
        return ret;
    }

    @Override
    protected int a(EnumDirection.EnumAxis enumdirection_enumaxis, double d0) { // findPointIndexAfterOffset
        switch (enumdirection_enumaxis.ordinal()) {
            case 0:
                return d0 < this.aabb.maxX ? (d0 < this.aabb.minX ? -1 : 0) : 1;
            case 1:
                return d0 < this.aabb.maxY ? (d0 < this.aabb.minY ? -1 : 0) : 1;
            case 2:
                return d0 < this.aabb.maxZ ? (d0 < this.aabb.minZ ? -1 : 0) : 1;
            default:
                throw new IllegalStateException("Unknown axis requested");
        }
    }

    @Override
    protected boolean b(double d0, double d1, double d2) { // containsPoint
        return this.aabb.contains(d0, d1, d2);
    }

    @Override
    public VoxelShape a(EnumDirection enumdirection) { // unknown
        return super.a(enumdirection);
    }

    @Override
    public double a(EnumDirection.EnumAxis enumdirection_enumaxis, AxisAlignedBB axisalignedbb, double d0) { // collide
        if (this.aabb.isEmpty() || axisalignedbb.isEmpty()) {
            return d0;
        }
        switch (enumdirection_enumaxis.ordinal()) {
            case 0:
                return AxisAlignedBB.collideX(this.aabb, axisalignedbb, d0);
            case 1:
                return AxisAlignedBB.collideY(this.aabb, axisalignedbb, d0);
            case 2:
                return AxisAlignedBB.collideZ(this.aabb, axisalignedbb, d0);
            default:
                throw new IllegalStateException("Unknown axis requested");
        }
    }

    @Override
    public boolean intersects(AxisAlignedBB axisalingedbb) {
        return this.aabb.voxelShapeIntersect(axisalingedbb);
    }
}

