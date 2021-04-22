package ac.grim.grimac.utils.nmsImplementations.tuinityVoxelShapes;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.server.v1_16_R3.EnumDirection;
import net.minecraft.server.v1_16_R3.SystemUtils;

import java.util.Arrays;

public final class VoxelShapeArray extends VoxelShape {

    // Tuinity start - optimise multi-aabb shapes
    static final AxisAlignedBB[] EMPTY = new AxisAlignedBB[0];
    final AxisAlignedBB[] boundingBoxesRepresentation;
    final double offsetX;
    final double offsetY;
    final double offsetZ;
    public DoubleList b;
    public DoubleList c;
    public DoubleList d;
    // Tuinity end - optimise multi-aabb shapes

    public VoxelShapeArray(VoxelShapeDiscrete voxelshapediscrete, double[] adouble, double[] adouble1, double[] adouble2) {
        this(voxelshapediscrete, DoubleArrayList.wrap(Arrays.copyOf(adouble, voxelshapediscrete.b() + 1)), DoubleArrayList.wrap(Arrays.copyOf(adouble1, voxelshapediscrete.c() + 1)), DoubleArrayList.wrap(Arrays.copyOf(adouble2, voxelshapediscrete.d() + 1)));
    }

    public VoxelShapeArray(VoxelShapeDiscrete voxelshapediscrete, DoubleList doublelist, DoubleList doublelist1, DoubleList doublelist2) {
        // Tuinity start - optimise multi-aabb shapes
        this(voxelshapediscrete, doublelist, doublelist1, doublelist2, null, null, 0.0, 0.0, 0.0);
    }

    VoxelShapeArray(VoxelShapeDiscrete voxelshapediscrete, DoubleList doublelist, DoubleList doublelist1, DoubleList doublelist2, VoxelShapeArray original, AxisAlignedBB[] boundingBoxesRepresentation, double offsetX, double offsetY, double offsetZ) {
        // Tuinity end - optimise multi-aabb shapes
        super(voxelshapediscrete);
        int i = voxelshapediscrete.b() + 1;
        int j = voxelshapediscrete.c() + 1;
        int k = voxelshapediscrete.d() + 1;

        if (i == doublelist.size() && j == doublelist1.size() && k == doublelist2.size()) {
            this.b = doublelist;
            this.c = doublelist1;
            this.d = doublelist2;
        } else {
            throw (IllegalArgumentException) SystemUtils.c((Throwable) (new IllegalArgumentException("Lengths of point arrays must be consistent with the size of the VoxelShape.")));
        }
        // Tuinity start - optimise multi-aabb shapes
        this.boundingBoxesRepresentation = boundingBoxesRepresentation == null ? this.getBoundingBoxesRepresentation().toArray(EMPTY) : boundingBoxesRepresentation; // Tuinity - optimise multi-aabb shapes
        if (original == null) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
        } else {
            this.offsetX = offsetX + original.offsetX;
            this.offsetY = offsetY + original.offsetY;
            this.offsetZ = offsetZ + original.offsetZ;
        }
        // Tuinity end - optimise multi-aabb shapes
    }

    @Override
    protected DoubleList a(EnumDirection.EnumAxis enumdirection_enumaxis) {
        switch (enumdirection_enumaxis) {
            case X:
                return this.b;
            case Y:
                return this.c;
            case Z:
                return this.d;
            default:
                throw new IllegalArgumentException();
        }
    }

    // Tuinity start - optimise multi-aabb shapes
    @Override
    public VoxelShape a(double d0, double d1, double d2) {
        if (this == VoxelShapes.getEmptyShape() || this.boundingBoxesRepresentation.length == 0) {
            return this;
        }
        return new VoxelShapeArray(this.a, new DoubleListOffset(this.a(EnumDirection.EnumAxis.X), d0), new DoubleListOffset(this.a(EnumDirection.EnumAxis.Y), d1), new DoubleListOffset(this.a(EnumDirection.EnumAxis.Z), d2), this, this.boundingBoxesRepresentation, d0, d1, d2);
    }

    @Override
    public java.util.List<AxisAlignedBB> d() { // getBoundingBoxesRepresentation
        if (this.boundingBoxesRepresentation == null) {
            return super.d();
        }
        java.util.List<AxisAlignedBB> ret = new java.util.ArrayList<>(this.boundingBoxesRepresentation.length);

        double offX = this.offsetX;
        double offY = this.offsetY;
        double offZ = this.offsetZ;
        for (AxisAlignedBB boundingBox : this.boundingBoxesRepresentation) {
            ret.add(boundingBox.offset(offX, offY, offZ));
        }

        return ret;
    }

    public final AxisAlignedBB[] getBoundingBoxesRepresentationRaw() {
        return this.boundingBoxesRepresentation;
    }

    public final double getOffsetX() {
        return this.offsetX;
    }

    public final double getOffsetY() {
        return this.offsetY;
    }

    public final double getOffsetZ() {
        return this.offsetZ;
    }

    public final boolean intersects(AxisAlignedBB axisalingedbb) {
        // this can be optimised by checking an "overall shape" first, but not needed
        double offX = this.offsetX;
        double offY = this.offsetY;
        double offZ = this.offsetZ;

        for (AxisAlignedBB boundingBox : this.boundingBoxesRepresentation) {
            if (axisalingedbb.voxelShapeIntersect(boundingBox.minX + offX, boundingBox.minY + offY, boundingBox.minZ + offZ,
                    boundingBox.maxX + offX, boundingBox.maxY + offY, boundingBox.maxZ + offZ)) {
                return true;
            }
        }

        return false;
    }
    // Tuinity end - optimise multi-aabb shapes
}