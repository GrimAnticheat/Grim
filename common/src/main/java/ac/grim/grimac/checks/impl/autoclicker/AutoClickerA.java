package ac.grim.grimac.checks.impl.autoclicker;

import ac.grim.grimac.api.config.ConfigManager;
import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import org.jetbrains.annotations.NotNull;

@CheckData(name = "AutoClickerA", experimental = false, description = "Detects high CPS")
public class AutoClickerA extends Check implements PacketCheck {

    private double limitCps = 20.0;

    public AutoClickerA(@NotNull GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            if (player.packetOrderProcessor.isDigging()) return;

            double cps = player.clickData.getCps();

            if (cps > limitCps && player.clickData.getSamples().isCollected()) {
                flagAndAlert("cps=" + cps);
            }
        }
    }

    @Override
    public void onReload(ConfigManager config) {
        limitCps = config.getDoubleElse(getConfigName() + ".max-cps", 20.0);
    }
}
