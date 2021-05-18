package ac.grim.grimac.utils.blockstate;

import ac.grim.grimac.utils.nmsImplementations.XMaterial;
import io.github.retrooper.packetevents.utils.reflection.Reflection;
import org.bukkit.Material;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MagicBlockState implements BaseBlockState {
    private static final Method getMaterialMethod;
    private static final Material air = XMaterial.AIR.parseMaterial();

    static {
        // This breaks on 1.13+, but magic block values were thankfully removed in 1.13
        getMaterialMethod = Reflection.getMethod(Material.class, "getMaterial", Material.class, Integer.class);
    }

    private final int id;
    private final int data;

    public MagicBlockState(int combinedID) {
        this.id = combinedID & 0xFF;
        this.data = combinedID >> 12;
    }

    public MagicBlockState(int id, int data) {
        this.id = id;
        this.data = data;
    }

    @Override
    public Material getMaterial() {
        try {
            return (Material) getMaterialMethod.invoke(id);
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return air;
    }

    public int getBlockData() {
        return data;
    }

    public int getCombinedId() {
        return id + (data << 12);
    }

    public int getId() {
        return this.id;
    }

    public int getData() {
        return this.data;
    }
}
