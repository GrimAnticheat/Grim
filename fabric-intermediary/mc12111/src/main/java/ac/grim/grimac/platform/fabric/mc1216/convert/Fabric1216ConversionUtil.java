package ac.grim.grimac.platform.fabric.mc1216.convert;

import com.mojang.serialization.JsonOps;
import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.Component;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.ComponentSerialization;

public class Fabric1216ConversionUtil extends ac.grim.grimac.platform.fabric.mc1205.convert.Fabric1205ConversionUtil {

    @Override
    public net.minecraft.network.chat.Component toNativeText(Component component) {
        return ComponentSerialization.CODEC.decode(
                RegistryAccess.EMPTY.createSerializationContext(JsonOps.INSTANCE),
                GsonComponentSerializer.gson().serializeToTree(component)
        ).getOrThrow(IllegalArgumentException::new).getFirst();
    }
}
