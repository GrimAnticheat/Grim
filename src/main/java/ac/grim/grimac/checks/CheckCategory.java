package ac.grim.grimac.checks;

import ac.grim.grimac.AbstractCheck;
import ac.grim.grimac.checks.type.*;
import ac.grim.grimac.player.GrimPlayer;
import lombok.Getter;
import java.util.Map;
import java.util.function.Function;

public class CheckCategory<T extends AbstractCheck> {
    private Class <T> type;
    @Getter
    private Function<GrimPlayer, Map<Class<? extends AbstractCheck>, T>> function;

    public static final CheckCategory<PacketCheck> PACKET =
            new CheckCategory<>(PacketCheck.class, player -> player.getCheckManager().getPacketChecks());
    public static final CheckCategory<PositionCheck> POSITION =
            new CheckCategory<>(PositionCheck.class, player -> player.getCheckManager().getPositionCheck());
    public static final CheckCategory<BlockPlaceCheck> BLOCK =
            new CheckCategory<>(BlockPlaceCheck.class, player -> player.getCheckManager().getBlockPlaceCheck());
    public static final CheckCategory<PacketCheck> PRE_PREDICTION =
            new CheckCategory<>(PacketCheck.class, player -> player.getCheckManager().getPrePredictionChecks());
    public static final CheckCategory<PostPredictionCheck> POST_PREDICTION =
            new CheckCategory<>(PostPredictionCheck.class, player -> player.getCheckManager().getPostPredictionCheck());
    public static final CheckCategory<RotationCheck> ROTATION =
            new CheckCategory<>(RotationCheck.class, player -> player.getCheckManager().getRotationCheck());
    public static final CheckCategory<VehicleCheck> VEHICLE =
            new CheckCategory<>(VehicleCheck.class, player -> player.getCheckManager().getVehicleCheck());

    public CheckCategory(Class<T> type, Function<GrimPlayer, Map<Class<? extends AbstractCheck>, T>> function) {
        this.type = type;
        this.function = function;
    }
}
