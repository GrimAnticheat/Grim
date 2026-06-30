package ac.grim.grimac.utils.data;

import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import lombok.Getter;

public class IndexedVector3d extends Vector3d {

    @Getter
    private final String name;
    @Getter
    private final int index;

    public IndexedVector3d(String name, int index, double x, double y, double z) {
        super(x, y, z);
        this.name = name;
        this.index = index;
    }

    public IndexedVector3d(String name, int index, Vector3f vector) {
        super(vector);
        this.name = name;
        this.index = index;
    }

    public IndexedVector3d(String name, int index, double[] array) {
        super(array);
        this.name = name;
        this.index = index;
    }

}
