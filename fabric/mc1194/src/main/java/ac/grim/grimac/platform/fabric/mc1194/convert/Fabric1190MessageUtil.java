package ac.grim.grimac.platform.fabric.mc1194.convert;

import ac.grim.grimac.platform.fabric.mc1161.util.convert.Fabric1161MessageUtil;
import net.minecraft.text.Text;

public class Fabric1190MessageUtil extends Fabric1161MessageUtil {
    @Override
    public Text textLiteral(String message) {
        return Text.literal(message);
    }
}
