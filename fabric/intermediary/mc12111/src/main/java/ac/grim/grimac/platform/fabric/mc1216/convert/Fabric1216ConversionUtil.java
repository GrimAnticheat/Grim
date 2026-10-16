package ac.grim.grimac.platform.fabric.mc1216.convert;

import ac.grim.grimac.platform.fabric.utils.convert.FabricComponentSerializer;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.text.Component;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.ComponentSerialization;

public class Fabric1216ConversionUtil extends ac.grim.grimac.platform.fabric.mc1205.convert.Fabric1205ConversionUtil {

    @Override
    public Object toNativeText(Component component) {
        return ComponentSerialization.CODEC.decode(
                RegistryAccess.EMPTY.createSerializationContext(JsonOps.INSTANCE),
                FabricComponentSerializer.serializeToTree(component)
        ).getOrThrow(IllegalArgumentException::new).getFirst();
    }
}
