package ac.grim.grimac.utils.blockstate;

import ac.grim.grimac.utils.nmsutil.XMaterial;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import org.bukkit.Material;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MagicBlockState implements BaseBlockState {
    private static final Material air = XMaterial.AIR.parseMaterial();
    private static final Method getMaterialMethod;

    static {
        getMaterialMethod = Reflection.getMethod(Material.class, "getMaterial", Material.class, int.class);
    }

    private final int id;
    private final int data;

    public MagicBlockState(int combinedID) {
        this.id = combinedID & 0xFF;
        this.data = combinedID >> 12;
    }

    public MagicBlockState(Material material) {
        this.id = material.getId();
        this.data = 0;
    }

    public MagicBlockState(int id, int data) {
        this.id = id;
        this.data = data;
    }

    @Override
    public Material getMaterial() {
        try {
            return (Material) getMaterialMethod.invoke(null, id);
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return air;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MagicBlockState)) return false;

        MagicBlockState that = (MagicBlockState) o;
        return this.id == that.getId() &&
                this.data == that.getBlockData();
    }

    public int getCombinedId() {
        return id + (data << 12);
    }

    public int getId() {
        return this.id;
    }

    public int getBlockData() {
        return data;
    }
}
