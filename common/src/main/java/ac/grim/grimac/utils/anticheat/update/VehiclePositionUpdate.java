package ac.grim.grimac.utils.anticheat.update;

import com.github.retrooper.packetevents.util.Vector3d;

public record VehiclePositionUpdate(Vector3d from, Vector3d to, float xRot, float yRot, boolean onGround, boolean isTeleport) {}
