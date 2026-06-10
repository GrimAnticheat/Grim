package ac.grim.grimac.checks.impl.packetorder;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.storage.verbose.VerboseBuf;
import ac.grim.grimac.api.storage.verbose.VerboseFormatter;
import ac.grim.grimac.api.storage.verbose.VerboseRenderContext;
import ac.grim.grimac.api.storage.verbose.VerboseSchema;
import ac.grim.grimac.api.storage.verbose.VerboseSink;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.verbose.VerboseCodecs;
import ac.grim.grimac.internal.storage.checks.CheckRegistry;
import ac.grim.grimac.internal.storage.verbose.VerboseRegistry;
import com.github.retrooper.packetevents.protocol.player.InteractionHand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PacketOrderVerbose {
    private PacketOrderVerbose() {
    }

    public static void register(@NotNull VerboseRegistry registry, @NotNull CheckRegistry checks) {
        registerStructured(registry, checks, PacketOrderB.class, PacketOrderB.V,
                formatter(PacketOrderB.V, (in, ctx, out) -> out.text(PacketOrderB.verbose(in.rvi()))));
        registerStructured(registry, checks, PacketOrderC.class, PacketOrderC.V,
                formatter(PacketOrderC.V, PacketOrderVerbose::renderPacketOrderC));
        registerStructured(registry, checks, PacketOrderD.class, PacketOrderD.V,
                formatter(PacketOrderD.V, PacketOrderVerbose::renderPacketOrderD));
        registerStructured(registry, checks, PacketOrderE.class, PacketOrderE.V,
                formatter(PacketOrderE.V, (in, ctx, out) -> out.text(PacketOrderE.verbose(in.rvi()))));
        registerStructured(registry, checks, PacketOrderF.class, PacketOrderF.V,
                formatter(PacketOrderF.V, PacketOrderVerbose::renderPacketOrderF));
        registerStructured(registry, checks, PacketOrderG.class, PacketOrderG.V,
                formatter(PacketOrderG.V, PacketOrderVerbose::renderPacketOrderG));
        registerStructured(registry, checks, PacketOrderI.class, PacketOrderI.V,
                formatter(PacketOrderI.V, PacketOrderVerbose::renderPacketOrderI));
        registerStructured(registry, checks, PacketOrderK.class, PacketOrderK.V,
                formatter(PacketOrderK.V, PacketOrderVerbose::renderPacketOrderK));
        registerStructured(registry, checks, PacketOrderL.class, PacketOrderL.V,
                formatter(PacketOrderL.V, (in, ctx, out) -> out.text(PacketOrderL.verbose(in.rvi()))));
        registerStructured(registry, checks, PacketOrderO.class, PacketOrderO.V,
                formatter(PacketOrderO.V, (in, ctx, out) -> out.text("type=").text(VerboseCodecs.packetName(ctx, in.rzz()))));
        registerStructured(registry, checks, PacketOrderP.class, PacketOrderP.V,
                formatter(PacketOrderP.V, (in, ctx, out) -> out.text(PacketOrderP.verbose(in.rvi(), VerboseCodecs.packetName(ctx, in.rzz())))));
    }

    private static void renderPacketOrderC(
            @NotNull VerboseBuf in,
            @NotNull VerboseRenderContext ctx,
            @NotNull VerboseSink out) {
        int kind = in.rvi();
        int requiredEntity = in.rzz();
        int entity = in.rzz();
        int requiredHand = in.rvi();
        int hand = in.rvi();
        boolean requiredSneaking = in.rbool();
        boolean sneaking = in.rbool();

        if (kind == PacketOrderC.KIND_MISMATCH) {
            out.text("requiredEntity=").num(requiredEntity)
                    .text(", entity=").num(entity)
                    .text(", requiredHand=").text(VerboseCodecs.enumName(requiredHand, InteractionHand.values()))
                    .text(", hand=").text(VerboseCodecs.enumName(hand, InteractionHand.values()))
                    .text(", requiredSneaking=").bool(requiredSneaking)
                    .text(", sneaking=").bool(sneaking);
        } else {
            out.text(PacketOrderC.literal(kind));
        }
    }

    private static void renderPacketOrderD(
            @NotNull VerboseBuf in,
            @NotNull VerboseRenderContext ctx,
            @NotNull VerboseSink out) {
        int kind = in.rvi();
        int requiredEntity = in.rzz();
        int entity = in.rzz();
        boolean requiredSneaking = in.rbool();
        boolean sneaking = in.rbool();

        if (kind == PacketOrderD.KIND_MISMATCH) {
            out.text("requiredEntity=").num(requiredEntity)
                    .text(", entity=").num(entity)
                    .text(", requiredSneaking=").bool(requiredSneaking)
                    .text(", sneaking=").bool(sneaking);
        } else {
            out.text(PacketOrderD.literal(kind));
        }
    }

    private static void renderPacketOrderF(
            @NotNull VerboseBuf in,
            @NotNull VerboseRenderContext ctx,
            @NotNull VerboseSink out) {
        out.text(PacketOrderF.verbose(in.rvi(), in.rbool(), in.rbool()));
    }

    private static void renderPacketOrderG(
            @NotNull VerboseBuf in,
            @NotNull VerboseRenderContext ctx,
            @NotNull VerboseSink out) {
        out.text(PacketOrderG.verbose(
                in.rvi(),
                in.rbool(),
                in.rbool(),
                in.rbool(),
                in.rbool(),
                in.rbool()));
    }

    private static void renderPacketOrderI(
            @NotNull VerboseBuf in,
            @NotNull VerboseRenderContext ctx,
            @NotNull VerboseSink out) {
        out.text(PacketOrderI.verbose(
                in.rvi(),
                in.rbool(),
                in.rbool(),
                in.rbool(),
                in.rbool(),
                in.rbool()));
    }

    private static void renderPacketOrderK(
            @NotNull VerboseBuf in,
            @NotNull VerboseRenderContext ctx,
            @NotNull VerboseSink out) {
        out.text(PacketOrderK.verbose(in.rvi(), in.rbool(), in.rbool()));
    }

    private static void registerStructured(
            @NotNull VerboseRegistry registry,
            @NotNull CheckRegistry checks,
            @NotNull Class<? extends Check> checkClass,
            @NotNull VerboseSchema schema,
            @NotNull VerboseFormatter formatter) {
        registerSchema(registry, checks, checkClass, schema);
        registerFormatter(registry, checkClass, formatter);
    }

    private static void registerSchema(
            @NotNull VerboseRegistry registry,
            @NotNull CheckRegistry checks,
            @NotNull Class<? extends Check> checkClass,
            @NotNull VerboseSchema schema) {
        CheckData data = checkData(checkClass);
        if (data.verboseVersion() < 1) {
            throw new IllegalStateException(checkClass.getName() + " is missing verboseVersion");
        }
        if (schema.version() != data.verboseVersion()) {
            throw new IllegalStateException(checkClass.getName() + " verbose schema v"
                    + schema.version() + " does not match @CheckData verboseVersion="
                    + data.verboseVersion());
        }

        checks.intern(data.stableKey(), data.name(), data.description(), safePluginVersion());
        registry.register(data.stableKey(), schema);
    }

    private static void registerFormatter(
            @NotNull VerboseRegistry registry,
            @NotNull Class<? extends Check> checkClass,
            @NotNull VerboseFormatter formatter) {
        CheckData data = checkData(checkClass);
        if (formatter.version() != data.verboseVersion()) {
            throw new IllegalStateException(checkClass.getName() + " verbose formatter v"
                    + formatter.version() + " does not match @CheckData verboseVersion="
                    + data.verboseVersion());
        }

        registry.registerFormatter(data.stableKey(), formatter);
    }

    private static @NotNull CheckData checkData(@NotNull Class<? extends Check> checkClass) {
        CheckData data = checkClass.getAnnotation(CheckData.class);
        if (data == null) {
            throw new IllegalStateException(checkClass.getName() + " is missing @CheckData");
        }
        if (data.stableKey().isBlank()) {
            throw new IllegalStateException(checkClass.getName() + " is missing a stableKey");
        }
        return data;
    }

    private static @Nullable String safePluginVersion() {
        try {
            return GrimAPI.INSTANCE.getExternalAPI().getGrimVersion();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static @NotNull VerboseFormatter formatter(
            @NotNull VerboseSchema schema,
            @NotNull Renderer renderer) {
        return new VerboseFormatter() {
            @Override
            public int version() {
                return schema.version();
            }

            @Override
            public void render(
                    @NotNull VerboseBuf in,
                    @NotNull VerboseRenderContext ctx,
                    @NotNull VerboseSink out) {
                renderer.render(in, ctx, out);
            }
        };
    }

    private interface Renderer {
        void render(
                @NotNull VerboseBuf in,
                @NotNull VerboseRenderContext ctx,
                @NotNull VerboseSink out);
    }
}
