package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings;

@CheckData(name = "CrashE", experimental = true)
public class CrashE extends PacketCheck {

    public CrashE(GrimPlayer playerData) {
        super(playerData);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.CLIENT_SETTINGS) {
            WrapperPlayClientSettings wrapper = new WrapperPlayClientSettings(event);
            boolean invalidViewDistance = wrapper.getViewDistance() < 2;
            boolean invalidLocale = wrapper.getLocale().length() < 4 || wrapper.getLocale().length() > 6;
            if (invalidViewDistance || invalidLocale) {
                if (flagAndAlert("invalidLocale=" + invalidLocale + " invalidViewDistance=" + invalidViewDistance)) {
                    if (invalidViewDistance) wrapper.setViewDistance(2);
                    if (invalidLocale) wrapper.setLocale("en_us");
                }
            }
        }
    }

}
