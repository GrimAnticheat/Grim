package ac.grim.grimac.utils.data.attribute;

import ac.grim.grimac.utils.math.GrimMath;
import com.github.retrooper.packetevents.protocol.attribute.Attribute;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAttributes;

import java.util.List;
import java.util.Optional;

import static ac.grim.grimac.utils.latency.CompensatedEntities.SPRINTING_MODIFIER_UUID;

public final class ValuedAttribute {

    private final Attribute attribute;
    // Attribute limits defined by https://minecraft.wiki/w/Attribute
    // These seem to be clamped on the client, but not the server
    private final double min, max;

    private WrapperPlayServerUpdateAttributes.Property lastProperty;
    private final double defaultValue;
    private double value;

    private ValuedAttribute(Attribute attribute, double defaultValue, double min, double max) {
        if (defaultValue < min || defaultValue > max) {
            throw new IllegalArgumentException("Default value must be between min and max!");
        }

        this.attribute = attribute;
        this.defaultValue = value;
        this.value = defaultValue;
        this.min = min;
        this.max = max;
    }

    public static ValuedAttribute ranged(Attribute attribute, double defaultValue, double min, double max) {
        return new ValuedAttribute(attribute, defaultValue, min, max);
    }

    public Attribute attribute() {
        return attribute;
    }

    public void reset() {
        this.value = defaultValue;
    }

    public double get() {
        return value;
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
        double d0 = property.getValue();

        List<WrapperPlayServerUpdateAttributes.PropertyModifier> modifiers = property.getModifiers();
        modifiers.removeIf(modifier -> modifier.getUUID().equals(SPRINTING_MODIFIER_UUID) || modifier.getName().getKey().equals("sprinting"));

        for (WrapperPlayServerUpdateAttributes.PropertyModifier attributemodifier : modifiers) {
            if (attributemodifier.getOperation() == WrapperPlayServerUpdateAttributes.PropertyModifier.Operation.ADDITION)
                d0 += attributemodifier.getAmount();
        }

        double d1 = d0;

        for (WrapperPlayServerUpdateAttributes.PropertyModifier attributemodifier : modifiers) {
            if (attributemodifier.getOperation() == WrapperPlayServerUpdateAttributes.PropertyModifier.Operation.MULTIPLY_BASE)
                d1 += d0 * attributemodifier.getAmount();
        }

        for (WrapperPlayServerUpdateAttributes.PropertyModifier attributemodifier : modifiers) {
            if (attributemodifier.getOperation() == WrapperPlayServerUpdateAttributes.PropertyModifier.Operation.MULTIPLY_TOTAL)
                d1 *= 1.0D + attributemodifier.getAmount();
        }

        this.lastProperty = property;
        return this.value = GrimMath.clampFloat((float) d1, (float) min, (float) max);
    }
}
