package ac.grim.grimac.manager;

import ac.grim.grimac.api.TransferData;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.Location;
import ac.grim.grimac.utils.math.Vector3dm;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransferDataTest {
    @Test
    void transferSeedsTheProductionPlayerAndFirstRelativeTeleport() {
        GrimPlayer player = player();
        SetbackTeleportUtil teleports = player.getSetbackTeleportUtil();
        assertNull(teleports.lastKnownGoodPosition);
        TransferData data = TransferData.builder().position(100, 64, -30).velocity(0.1, 0.2, -0.3)
                .rotation(90, -20).build();
        player.setTransferData(data);

        assertEquals(100, player.x);
        assertEquals(64, player.y);
        assertEquals(-30, player.z);
        assertEquals(player.x, player.lastX);
        assertEquals(player.y, player.lastY);
        assertEquals(player.z, player.lastZ);
        assertEquals(90, player.yaw);
        assertEquals(-20, player.pitch);
        assertEquals(player.yaw, player.lastYaw);
        assertEquals(player.pitch, player.lastPitch);
        assertFalse(teleports.hasAcceptedSpawnTeleport);
        assertEquals(0.1, player.clientVelocity.getX());
        assertEquals(0.2, player.clientVelocity.getY());
        assertEquals(-0.3, player.clientVelocity.getZ());

        teleports.addSentTeleport(new Location(null, 2, 3, 4), null, 1,
                RelativeFlag.X.or(RelativeFlag.Y).or(RelativeFlag.Z), true, 42);
        assertEquals(102, teleports.getRequiredSetBack().getTeleportData().getLocation().getX());
        assertEquals(67, teleports.getRequiredSetBack().getTeleportData().getLocation().getY());
        assertEquals(-26, teleports.getRequiredSetBack().getTeleportData().getLocation().getZ());
        assertEquals(0.1, teleports.lastKnownGoodPosition.getVector().getX());
        assertEquals(0.2, teleports.lastKnownGoodPosition.getVector().getY());
        assertEquals(-0.3, teleports.lastKnownGoodPosition.getVector().getZ());
        assertEquals(1, teleports.pendingTeleports.size());
        assertFalse(teleports.hasAcceptedSpawnTeleport);

        teleports.addSentTeleport(new Location(null, 1, 2, 3), null, 2, RelativeFlag.NONE, true, 43);
        assertEquals(0, teleports.lastKnownGoodPosition.getVector().lengthSquared());
    }

    @Test
    void rejectsLateOrNullTransferBeforeChangingPlayerState() {
        GrimPlayer player = player();
        assertThrows(NullPointerException.class, () -> player.setTransferData(null));
        player.getSetbackTeleportUtil().addSentTeleport(
                new Location(null, 4, 5, 6), null, 1, RelativeFlag.NONE, true, 42);
        assertThrows(IllegalStateException.class, () -> player.setTransferData(
                TransferData.builder().position(100, 64, -30).velocity(1, 2, 3).build()));
        assertEquals(0, player.x);
        assertEquals(4, player.getSetbackTeleportUtil().lastKnownGoodPosition.getPos().getX());
    }

    @Test
    void omittedRotationPreservesTheExistingRotation() {
        GrimPlayer player = player();
        player.yaw = 45;
        player.pitch = 15;
        player.lastYaw = 40;
        player.lastPitch = 10;
        player.setTransferData(TransferData.builder().position(1, 2, 3).velocity(0, 0, 0).build());
        assertEquals(45, player.yaw);
        assertEquals(15, player.pitch);
        assertEquals(40, player.lastYaw);
        assertEquals(10, player.lastPitch);
    }

    private static GrimPlayer player() {
        GrimPlayer player = mock(GrimPlayer.class);
        player.clientVelocity = new Vector3dm();
        when(player.getClientVersion()).thenReturn(ClientVersion.V_1_21_2);
        when(player.getSetbackTeleportUtil()).thenReturn(new SetbackTeleportUtil(player));
        doCallRealMethod().when(player).setTransferData(any());
        return player;
    }
}
