package ac.grim.grimac.utils.data.attribute;

import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.protocol.attribute.Attribute;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;

import java.util.List;
import java.util.Optional;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

import static ac.grim.grimac.utils.latency.CompensatedEntities.SPRINTING_MODIFIER_UUID;

public final class ValuedAttribute {

    private static final DoubleUnaryOperator DEFAULT_GET_REWRITE = DoubleUnaryOperator.identity();

    private final Attribute attribute;
    // Attribute limits defined by https://minecraft.wiki/w/Attribute
    // These seem to be clamped on the client, but not the server
    private final double min, max;
    private final double defaultValue;
    private WrapperPlayServerUpdateAttributes.Property lastProperty;
    private double value;

    // This allows us to rewrite the value based on client & server version
    private DoubleBinaryOperator setRewriter;
    private DoubleUnaryOperator getRewriter;

    private ValuedAttribute(Attribute attribute, double defaultValue, double min, double max) {
        if (defaultValue < min || defaultValue > max) {
            throw new IllegalArgumentException("Default value must be between min and max!");
        }

        this.attribute = attribute;
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        this.min = min;
        this.max = max;
        this.getRewriter = DEFAULT_GET_REWRITE;
    }

    public static ValuedAttribute ranged(Attribute attribute, double defaultValue, double min,
                                         double max) {
        return new ValuedAttribute(attribute, defaultValue, min, max);
    }

    public ValuedAttribute withSetRewriter(DoubleBinaryOperator rewriteFunction) {
        this.setRewriter = rewriteFunction;
        return this;
    }

    /**
     * Creates a rewriter that prevents the value from ever being modified unless the player meets the required version.
     * @param player the player
     * @param requiredVersion the required version for the attribute
     * @return this instance for chaining
     */
    public ValuedAttribute requiredVersion(GrimPlayer player, ClientVersion requiredVersion) {
        withSetRewriter((oldValue, newValue) -> {
            if (player.getClientVersion().isOlderThan(requiredVersion)) {
                return oldValue;
            }
            return newValue;
        });
        return this;
    }

    public ValuedAttribute withGetRewriter(DoubleUnaryOperator getRewriteFunction) {
        this.getRewriter = getRewriteFunction;
        return this;
    }

    public Attribute attribute() {
        return attribute;
    }

    public void reset() {
        this.value = defaultValue;
        this.lastProperty =
                null; // Remove the old property with its modifiers so we don't accidentally use it again, messing up the calculation.
    }

    public double get() {
        return getRewriter.applyAsDouble(this.value);
    }

    public void override(double value) {
        this.value = value;
    }

    @Deprecated // Avoid using this, it only exists for special cases
    public Optional<WrapperPlayServerUpdateAttributes.Property> property() {
        return Optional.ofNullable(lastProperty);
    }

    public void recalculate() {
        with(lastProperty);
    }

    public double with(WrapperPlayServerUpdateAttributes.Property property) {
        double baseValue = property.getValue();
        double additionSum = 0;
        double multiplyBaseSum = 0;
        double multiplyTotalProduct = 1.0;

        List<WrapperPlayServerUpdateAttributes.PropertyModifier> modifiers =
                property.getModifiers();
        modifiers.removeIf(modifier -> modifier.getUUID().equals(SPRINTING_MODIFIER_UUID) ||
                modifier.getName().getKey().equals("sprinting"));

        for (WrapperPlayServerUpdateAttributes.PropertyModifier modifier : modifiers) {
            switch (modifier.getOperation()) {
                case ADDITION:
                    additionSum += modifier.getAmount();
                    break;
                case MULTIPLY_BASE:
                    multiplyBaseSum += modifier.getAmount();
                    break;
                case MULTIPLY_TOTAL:
                    multiplyTotalProduct *= (1.0 + modifier.getAmount());
                    break;
            }
        }

        double newValue = GrimMath.clamp((baseValue + additionSum) * (1 + multiplyBaseSum) * multiplyTotalProduct, min, max);
        if (setRewriter != null) {
            newValue = setRewriter.applyAsDouble(this.value, newValue);
        }

        if (newValue < min || newValue > max) {
            throw new IllegalArgumentException("New value must be between min and max!");
        }

        this.lastProperty = property;
        return this.value = newValue;
    }
}
