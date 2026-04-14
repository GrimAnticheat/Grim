package ac.grim.grimac.platform.fabric.utils.convert;

import ac.grim.grimac.utils.anticheat.LogUtil;
import com.mojang.serialization.JsonOps;
import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.Component;
import net.minecraft.network.chat.ComponentSerialization;

public final class FabricTextConversion {
    private FabricTextConversion() {}

    public static net.minecraft.network.chat.Component parseAdventureToNative(Component component) {
        try {
            return ComponentSerialization.CODEC
                    .parse(JsonOps.INSTANCE, GsonComponentSerializer.gson().serializeToTree(component))
                    .getOrThrow();
        } catch (RuntimeException e) {
            LogUtil.error(
                    "Failed to parse Adventure Component to native (invalid JSON / codec): "
                            + String.valueOf(component),
                    e);
            return net.minecraft.network.chat.Component.literal("");
        }
    }
}
