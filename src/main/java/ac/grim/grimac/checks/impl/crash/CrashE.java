package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.exploit.ExploitA;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings;

@CheckData(name = "CrashE")
public class CrashE extends Check implements PacketCheck {

    public CrashE(final GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CLIENT_SETTINGS) return;

        final WrapperPlayClientSettings wrapper = new WrapperPlayClientSettings(event);
        final int viewDistance = wrapper.getViewDistance();
        final boolean invalidLocale = player.checkManager.getPrePredictionCheck(ExploitA.class).checkString(wrapper.getLocale());
        if (viewDistance < 2) {
            flagAndAlert("distance=" + viewDistance);
            wrapper.setViewDistance(2);
        }
        if (invalidLocale) wrapper.setLocale("en_us");
    }

}
