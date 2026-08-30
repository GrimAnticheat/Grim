package ac.grim.grimac.checks;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.api.event.events.FlagEvent;
import ac.grim.grimac.api.storage.verbose.Verbose;
import ac.grim.grimac.api.storage.verbose.VerboseBuf;
import ac.grim.grimac.api.storage.verbose.VerboseRenderContext;
import ac.grim.grimac.internal.storage.verbose.VerboseRegistry;
import ac.grim.grimac.player.GrimPlayer;
import lombok.Getter;
import lombok.Setter;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

// Class from https://github.com/Tecnio/AntiCheatBase/blob/master/src/main/java/me/tecnio/anticheat/check/Check.java
@Getter
public class Check extends GrimProcessor implements AbstractCheck {
    private static final FlagEvent.Channel FLAG_CHANNEL = GrimAPI.INSTANCE.getEventBus().get(FlagEvent.class);
    private static final ThreadLocal<VerboseBuf> VERBOSE = ThreadLocal.withInitial(VerboseBuf::new);

    // violations
    public double violations;
    private long lastViolationTime;
    private boolean lastFlagStoredBinaryVerbose;
    private final VerboseBuf verbose = VERBOSE.get();

    // check data
    private final @Nullable String checkName;
    private final @Nullable String configName;
    private final @Nullable String alternativeName;
    private final @NotNull String stableKey;
    private final boolean experimental;
    private final @NotNull String defaultDescription;
    private final double defaultDecay;
    private final double defaultSetbackVL;

    // configurable
    private @MonotonicNonNull String displayName;
    private @MonotonicNonNull String description;
    private double decay;
    private double setbackVL;
    @Setter private boolean isEnabled;

    // permissions
    private boolean exemptPermission;
    private boolean noSetbackPermission;
    private boolean noModifyPacketPermission;

    public Check(final @NotNull GrimPlayer player) {
        super(player, false);

        final CheckData checkData = this.getClass().getAnnotation(CheckData.class);
        if (checkData != null) {
            this.checkName = checkData.name();
            this.configName = checkData.configName().equals("DEFAULT")
                    ? this.checkName
                    : checkData.configName();
            this.defaultDecay = checkData.decay();
            this.defaultSetbackVL = checkData.setback();
            this.alternativeName = checkData.alternativeName();
            this.experimental = checkData.experimental();
            this.defaultDescription = checkData.description();
            this.stableKey = checkData.stableKey();
            this.displayName = this.checkName;
        } else {
            this.defaultDescription = CheckData.DEFAULT_DESCRIPTION;
            this.defaultDecay = CheckData.DEFAULT_DECAY;
            this.defaultSetbackVL = CheckData.DEFAULT_SETBACK;
            this.stableKey = "";
            this.alternativeName = null;
            this.checkName = null;
            this.configName = null;
            this.experimental = false;
        }

        reload();
    }

    public boolean shouldModifyPackets() {
        return isEnabled
                && !player.disableGrim
                && !player.noModifyPacketPermission
                && !noModifyPacketPermission
                && !exemptPermission;
    }

    /**
     * Evaluated once when CheckManager builds the dispatch arrays.
     * Implementations must only depend on immutable connection properties.
     */
    public boolean isApplicable() {
        return true;
    }

    public final void updatePermissions() {
        if (getConfigName() == null) return;
        final String id = getConfigName().toLowerCase();
        exemptPermission = player.hasPermission("grim.exempt." + id);
        noSetbackPermission = player.hasPermission("grim.nosetback." + id);
        noModifyPacketPermission = player.hasPermission("grim.nomodifypacket." + id);
    }

    public final boolean flag() {
        return flag("");
    }

    public final boolean flag(String verbose) {
        Supplier<String> alertText = constant(verbose);
        if (recordFlag(alertText)) {
            alert(alertText);
            return true;
        }
        return false;
    }

    public final boolean flag(@NotNull Verbose.Writer verbose) {
        BinaryVerbose binary = lazyVerbose(verbose);
        if (recordFlag(binary)) {
            alert(binary.rendered());
            return true;
        }
        return false;
    }

    public final boolean flag(@NotNull Verbose.Writer verbose, @NotNull Supplier<String> alertText) {
        BinaryVerbose binary = lazyVerbose(verbose);
        if (recordFlag(binary)) {
            alert(memoize(Objects.requireNonNull(alertText, "alertText")));
            return true;
        }
        return false;
    }

    private boolean recordFlag(@NotNull Supplier<String> verbose) {
        if (player.disableGrim || (experimental && !player.isExperimentalChecks()) || exemptPermission)
            return false; // Avoid calling event if disabled

        if (FLAG_CHANNEL.fire(player, this, verbose)) return false;

        lastFlagStoredBinaryVerbose = false;
        player.punishmentManager.handleViolation(this);
        lastViolationTime = System.currentTimeMillis();
        violations++;
        return true;
    }

    private boolean recordFlag(@NotNull BinaryVerbose verbose) {
        Supplier<String> rendered = verbose.rendered();
        byte[] verboseData = verbose.data();

        if (player.disableGrim || (experimental && !player.isExperimentalChecks()) || exemptPermission)
            return false; // Avoid calling event if disabled

        if (FLAG_CHANNEL.fire(player, this, rendered)) return false;

        lastFlagStoredBinaryVerbose = true;
        player.punishmentManager.handleViolation(this);
        lastViolationTime = System.currentTimeMillis();
        violations++;
        GrimAPI.INSTANCE.getDataStoreLifecycle().liveWriteHooks()
                .recordFlagDataFromCheck(player, this, violations, verboseData);
        return true;
    }

    private @NotNull BinaryVerbose lazyVerbose(@NotNull Verbose.Writer writer) {
        Objects.requireNonNull(writer, "writer");
        byte[] verboseData = writer.end().toByteArray();
        Verbose template = writer.verbose();
        Supplier<String> rendered = memoize(() -> template.render(verboseData, new VerboseRenderContext(
                player.getClientVersion().getProtocolVersion(),
                GrimAPI.INSTANCE.getPlatformServer().getPlatformImplementationString())));
        return new BinaryVerbose(verboseData, rendered);
    }

    public final void registerVerboseTemplates(@Nullable VerboseRegistry registry) {
        if (registry == null || stableKey.isEmpty()) return;
        String pluginVersion = GrimAPI.INSTANCE.getExternalAPI().getGrimVersion();
        for (Verbose template : Verbose.declaredBy(getClass(), Check.class)) {
            registry.registerTemplate(stableKey, checkName, description, pluginVersion, template);
        }
    }

    protected final @NotNull VerboseBuf verbose() {
        return verbose;
    }

    public final boolean flagWithSetback() {
        return flagWithSetback("");
    }

    public final boolean flagWithSetback(String verbose) {
        if (flag(verbose)) {
            setbackIfAboveSetbackVL();
            return true;
        }
        return false;
    }

    public final boolean flagWithSetback(@NotNull Verbose.Writer verbose) {
        if (flag(verbose)) {
            setbackIfAboveSetbackVL();
            return true;
        }
        return false;
    }

    public final boolean flagWithSetback(@NotNull Verbose.Writer verbose, @NotNull Supplier<String> alertText) {
        if (flag(verbose, alertText)) {
            setbackIfAboveSetbackVL();
            return true;
        }
        return false;
    }

    public final void reward() {
        violations = Math.max(0, violations - decay);
    }

    @Override
    public final void reload(@NotNull ConfigManager configuration) {
        if (getConfigName() != null) {
            decay = configuration.getDoubleElse(getConfigName() + ".decay", defaultDecay);
            setbackVL = configuration.getDoubleElse(getConfigName() + ".setbackvl", defaultSetbackVL);
            displayName = configuration.getStringElse(getConfigName() + ".displayname", checkName);
            description = configuration.getStringElse(getConfigName() + ".description", defaultDescription);

            if (setbackVL == -1) setbackVL = Double.MAX_VALUE;
        }
        super.reload(configuration);
    }

    public boolean alert(String verbose) {
        return alert(constant(verbose));
    }

    public boolean alert(@NotNull Supplier<String> verbose) {
        return player.punishmentManager.handleAlert(player, memoize(Objects.requireNonNull(verbose, "verbose")), this);
    }

    public boolean setbackIfAboveSetbackVL() {
        if (shouldSetback()) {
            return player.getSetbackTeleportUtil().executeViolationSetback();
        }
        return false;
    }

    public boolean shouldSetback() {
        return !noSetbackPermission && violations > setbackVL;
    }

    public boolean executeViolationSetback() {
        return !noSetbackPermission && player.getSetbackTeleportUtil().executeViolationSetback();
    }

    public String formatOffset(double offset) {
        return offset > 0.001 ? String.format("%.5f", offset) : String.format("%.2E", offset);
    }

    private static @NotNull Supplier<String> constant(String verbose) {
        String value = verbose == null ? "" : verbose;
        return () -> value;
    }

    private static @NotNull Supplier<String> memoize(@NotNull Supplier<String> supplier) {
        return new Supplier<>() {
            private String value;
            private boolean computed;

            @Override
            public synchronized String get() {
                if (!computed) {
                    try {
                        value = supplier.get();
                        if (value == null) value = "";
                    } catch (RuntimeException ignored) {
                        value = "";
                    }
                    computed = true;
                }
                return value;
            }
        };
    }

    private record BinaryVerbose(byte @NotNull [] data, @NotNull Supplier<String> rendered) {}
}
