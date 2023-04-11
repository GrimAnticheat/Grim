package ac.grim.grimac.events.packets;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.GameMode;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerFlying;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;

import java.util.ArrayList;
import java.util.EnumSet;
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
            if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThanOrEquals(ServerVersion.V_1_12_2))
                return;

            GrimPlayer receiver = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());

            if (receiver == null) { // Exempt
                return;
            }

            WrapperPlayServerPlayerInfo info = new WrapperPlayServerPlayerInfo(event);

            if (info.getAction() == WrapperPlayServerPlayerInfo.Action.UPDATE_GAME_MODE || info.getAction() == WrapperPlayServerPlayerInfo.Action.ADD_PLAYER) {
                List<WrapperPlayServerPlayerInfo.PlayerData> nmsPlayerInfoDataList = info.getPlayerDataList();

                int hideCount = 0;
                for (WrapperPlayServerPlayerInfo.PlayerData playerData : nmsPlayerInfoDataList) {
                    if (GrimAPI.INSTANCE.getSpectateManager().shouldHidePlayer(receiver, playerData)) {
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
        } else if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO_UPDATE) {
            GrimPlayer receiver = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getUser());
            if (receiver == null) return;
            //create wrappers
            WrapperPlayServerPlayerInfoUpdate wrapper = new WrapperPlayServerPlayerInfoUpdate(event);
            EnumSet<WrapperPlayServerPlayerInfoUpdate.Action> actions = wrapper.getActions();
            //player's game mode updated
            if (actions.contains(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_GAME_MODE)) {
                boolean onlyGameMode = actions.size() == 1; // packet is being sent to only update game modes
                int hideCount = 0;
                List<WrapperPlayServerPlayerInfoUpdate.PlayerInfo> modified = new ArrayList<>(wrapper.getEntries().size());
                //iterate through the player entries
                for (WrapperPlayServerPlayerInfoUpdate.PlayerInfo entry : wrapper.getEntries()) {
                    //check if the player should be hidden
                    WrapperPlayServerPlayerInfoUpdate.PlayerInfo modifiedPacket = null;
                    if (GrimAPI.INSTANCE.getSpectateManager().shouldHidePlayer(receiver, entry.getProfileId())) {
                        hideCount++;
                        //modify & create a new packet from pre-existing one if they are a spectator
                        if (entry.getGameMode() == GameMode.SPECTATOR) {
                            modifiedPacket = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                                    entry.getGameProfile(),
                                    entry.isListed(),
                                    entry.getLatency(),
                                    GameMode.SURVIVAL,
                                    entry.getDisplayName(),
                                    entry.getChatSession()
                            );
                            modified.add(modifiedPacket);
                        }
                    }

                    if (modifiedPacket == null) {  //if the packet wasn't modified, send original
                        modified.add(entry);
                    } else if (!onlyGameMode) { //if more than just the game mode updated, modify the packet
                        modified.add(modifiedPacket);
                    } //if only the game mode was updated and the packet was modified, don't send anything

                }
                //if no hidden players, don't modify packet
                if (hideCount <= 0) {
                    event.setLastUsedWrapper(null);
                } else if (hideCount == modified.size()) { //if the amount of hidden players & modified entries are the same
                    if (onlyGameMode) { // if only the game mode changed, cancel
                        event.setCancelled(true);
                    } else { //if more than the game mode changed, remove the action
                        wrapper.getActions().remove(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_GAME_MODE);
                    }
                } else { //modify entries
                    wrapper.setEntries(modified);
                }
            }

        } else if (event.getPacketType() != PacketType.Play.Server.PLAYER_POSITION_AND_LOOK) {
            event.setLastUsedWrapper(null);
        }
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon packetType = event.getPacketType();
        if (!WrapperPlayClientPlayerFlying.isFlying(packetType) && packetType != PacketType.Play.Client.CLIENT_SETTINGS && !event.isCancelled()) {
            event.setLastUsedWrapper(null);
        }
    }
}
