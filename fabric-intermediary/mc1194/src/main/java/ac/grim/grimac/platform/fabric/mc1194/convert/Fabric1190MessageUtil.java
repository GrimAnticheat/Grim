package ac.grim.grimac.platform.fabric.mc1194.convert;

import ac.grim.grimac.platform.fabric.mc1161.util.convert.Fabric1161MessageUtil;
import net.minecraft.network.chat.Component;

public class Fabric1190MessageUtil extends Fabric1161MessageUtil {
    @Override
    public Component textLiteral(String message) {
        return Component.literal(message);
    }
}
