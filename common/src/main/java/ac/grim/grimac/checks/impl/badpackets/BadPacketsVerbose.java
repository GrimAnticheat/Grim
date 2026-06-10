package ac.grim.grimac.checks.impl.badpackets;

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
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientClickWindow.WindowClickType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction.Action;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BadPacketsVerbose {
    private BadPacketsVerbose() {
    }

    public static void register(@NotNull VerboseRegistry registry, @NotNull CheckRegistry checks) {
        registerStructured(registry, checks, BadPacketsA.class, BadPacketsA.V,
                formatter(BadPacketsA.V, (in, ctx, out) -> out.text("slot=").num(in.rzz())));
        registerStructured(registry, checks, BadPacketsD.class, BadPacketsD.V,
                formatter(BadPacketsD.V, (in, ctx, out) -> out.text("pitch=").num(in.rf32())));
        registerStructured(registry, checks, BadPacketsE.class, BadPacketsE.V,
                formatter(BadPacketsE.V, (in, ctx, out) -> out.text("ticks=").num(in.rvi())));
        registerStructured(registry, checks, BadPacketsF.class, BadPacketsF.V,
                formatter(BadPacketsF.V, (in, ctx, out) -> out.text("state=").bool(in.rbool())));
        registerStructured(registry, checks, BadPacketsG.class, BadPacketsG.V,
                formatter(BadPacketsG.V, (in, ctx, out) -> out.text("state=").bool(in.rbool())));
        registerStructured(registry, checks, BadPacketsH.class, BadPacketsH.V,
                formatter(BadPacketsH.V, (in, ctx, out) ->
                        out.text("expected=").num(in.rzz()).text(", id=").num(in.rzz())));
        registerStructured(registry, checks, BadPacketsL.class, BadPacketsL.V,
                formatter(BadPacketsL.V, BadPacketsVerbose::renderBadPacketsL));
        registerStructured(registry, checks, BadPacketsO.class, BadPacketsO.V,
                formatter(BadPacketsO.V, (in, ctx, out) -> out.text("id=").num(VerboseCodecs.rSignedLong(in))));
        registerStructured(registry, checks, BadPacketsP.class, BadPacketsP.V,
                formatter(BadPacketsP.V, BadPacketsVerbose::renderBadPacketsP));
        registerStructured(registry, checks, BadPacketsQ.class, BadPacketsQ.V,
                formatter(BadPacketsQ.V, (in, ctx, out) ->
                        out.text("boost=").num(in.rzz())
                                .text(", action=").text(VerboseCodecs.enumName(in.rvi(), Action.values()))
                                .text(", entity=").num(in.rzz())));
        registerStructured(registry, checks, BadPacketsR.class, BadPacketsR.V,
                formatter(BadPacketsR.V, (in, ctx, out) ->
                        out.text("time=").num(in.rvl())
                                .text("ms, lst=").num(in.rvl())
                                .text("ms, positions=").num(in.rvi())));
        registerStructured(registry, checks, BadPacketsT.class, BadPacketsT.V,
                formatter(BadPacketsT.V, (in, ctx, out) ->
                        out.text(String.format("%.5f/%.5f/%.5f", in.rf64(), in.rf64(), in.rf64()))));
        registerStructured(registry, checks, BadPacketsU.class, BadPacketsU.V,
                formatter(BadPacketsU.V, BadPacketsVerbose::renderBadPacketsU));
        registerStructured(registry, checks, BadPacketsV.class, BadPacketsV.V,
                formatter(BadPacketsV.V, (in, ctx, out) -> out.text("delta=").num(in.rf64())));
        registerStructured(registry, checks, BadPacketsY.class, BadPacketsY.V,
                formatter(BadPacketsY.V, (in, ctx, out) -> out.text("slot=").num(in.rzz())));
    }

    private static void renderBadPacketsL(
            @NotNull VerboseBuf in,
            @NotNull VerboseRenderContext ctx,
            @NotNull VerboseSink out) {
        out.text("pos=")
                .text(VerboseCodecs.rMcBlockPos(in))
                .text(", face=").num(in.rzz())
                .text(", sequence=").num(in.rzz())
                .text(", action=").text(VerboseCodecs.lowerEnumName(in.rvi(), DiggingAction.values()));
    }

    private static void renderBadPacketsP(
            @NotNull VerboseBuf in,
            @NotNull VerboseRenderContext ctx,
            @NotNull VerboseSink out) {
        String clickType = VerboseCodecs.lowerEnumName(in.rvi(), WindowClickType.values());
        int button = in.rzz();
        boolean hasContainer = in.rbool();
        int container = in.rzz();
        out.text("clickType=").text(clickType).text(", button=").num(button);
        if (hasContainer) {
            out.text(", container=").num(container);
        }
    }

    private static void renderBadPacketsU(
            @NotNull VerboseBuf in,
            @NotNull VerboseRenderContext ctx,
            @NotNull VerboseSink out) {
        String pos = VerboseCodecs.rMcBlockPos(in);
        float cursorX = in.rf32();
        float cursorY = in.rf32();
        float cursorZ = in.rf32();
        boolean item = in.rbool();
        int sequence = in.rzz();
        out.text(String.format(
                "xyz=%s, cursor=%s, %s, %s, item=%s, sequence=%s",
                pos, cursorX, cursorY, cursorZ, item, sequence));
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
