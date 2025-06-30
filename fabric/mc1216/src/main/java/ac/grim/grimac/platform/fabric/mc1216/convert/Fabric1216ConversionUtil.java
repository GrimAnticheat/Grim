package ac.grim.grimac.platform.fabric.mc1216.convert;

import com.mojang.serialization.JsonOps;
import io.github.retrooper.packetevents.adventure.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.Component;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;

public class Fabric1216ConversionUtil extends ac.grim.grimac.platform.fabric.mc1205.convert.Fabric1205ConversionUtil {

    @Override
    public Text toNativeText(Component component) {
        return TextCodecs.CODEC.decode(
                DynamicRegistryManager.EMPTY.getOps(JsonOps.INSTANCE),
                GsonComponentSerializer.gson().serializeToTree(component)
        ).getOrThrow(IllegalArgumentException::new).getFirst();
    }
}
