package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;

import java.util.List;

public class PacketSetWrapperNull extends PacketListenerAbstract {
    // It's faster (and less buggy) to simply not re-encode the wrapper unless we changed something
    // The two packets we change are clientbound entity metadata (to fix a netcode issue)
    // and the serverbound player flying packets (to patch NoFall)
    public PacketSetWrapperNull() {
        super(PacketListenerPriority.HIGHEST);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event);
            if (wrapper.getEntityId() != event.getUser().getEntityId()) {
                event.setLastUsedWrapper(null);
            }
        } else if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO) {
            //iterate through players and fake their game mode if they are spectating via grim spectate
            User user = event.getUser();
            WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo(event);
            if (info.getAction() == WrapperPlayServerPlayerInfo.Action.UPDATE_GAME_MODE || info.getAction() == WrapperPlayServerPlayerInfo.Action.ADD_PLAYER) {
                List<WrapperPlayServerPlayerInfo.PlayerData> nmsPlayerInfoDataList = info.getPlayerDataList();
                int hideCount = 0;
                for (WrapperPlayServerPlayerInfo.PlayerData playerData : nmsPlayerInfoDataList) {
                    if (GrimAPI.INSTANCE.getSpectateManager().shouldHidePlayer(user, playerData)) {
                        hideCount++;
                        if (playerData.getGameMode() == GameMode.SPECTATOR) playerData.setGameMode(GameMode.SURVIVAL);
                    }
                }
                //if amount of hidden players is the amount of players updated & is an update game mode action just cancel it
                if (hideCount == nmsPlayerInfoDataList.size() && info.getAction() == WrapperPlayServerPlayerInfo.Action.UPDATE_GAME_MODE) {
                    event.setCancelled(true);
                } else if (hideCount <= 0) {
                    event.setLastUsedWrapper(null);
                }
            }
        } else {
            event.setLastUsedWrapper(null);
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (!WrapperPlayClientPlayerFlying.isFlying(event.getPacketType())) event.setLastUsedWrapper(null);
    }
}
