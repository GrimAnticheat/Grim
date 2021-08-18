package ac.grim.grimac.utils.data;

import io.github.retrooper.packetevents.utils.vector.Vector3d;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.World;
import org.bukkit.util.Vector;

@Getter
@Setter
@AllArgsConstructor
public class SetBackData {
    World world;
    Vector3d position;
    float xRot, yRot;
    Vector velocity;
    Integer vehicle;
    int trans;
}
