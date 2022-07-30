package ac.grim.grimac.checks.impl.crash;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.impl.exploit.ExploitA;
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
            int viewDistance = wrapper.getViewDistance();
            String locale = wrapper.getLocale();
            boolean invalidViewDistance = viewDistance < 2;
            boolean invalidLocale = locale.length() < 3 || locale.length() > 6;
            //TODO: Client locales don't follow ISO formatting for some reason, so we need to create a list of all valid locales

            if (locale.length() > 64) {
                locale = "sent " + locale.length() + " bytes as locale";
            } else if (player.checkManager.getPrePredictionCheck(ExploitA.class).checkString(wrapper.getLocale())) {
                locale = "sent log4j";
            }

            if (invalidViewDistance || invalidLocale) {
                String debug = "";

                if (invalidLocale) debug += "locale=" + locale;
                if (invalidViewDistance) debug += " viewDistance=" + viewDistance;

                debug = debug.trim();
                if (flagAndAlert(debug)) {
                    if (invalidViewDistance) wrapper.setViewDistance(2);
                    if (invalidLocale) wrapper.setLocale("en_us");
                }
            }
        }
    }

}
