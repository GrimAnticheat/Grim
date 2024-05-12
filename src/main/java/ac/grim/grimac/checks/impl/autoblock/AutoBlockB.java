package ac.grim.grimac.checks.impl.autoblock;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.DiggingAction;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerDigging;

@CheckData(name = "AutoBlockB (MultiActions)", configName = "AutoBlockB", setback = 1, experimental = true)
public class AutoBlockB extends Check implements PacketCheck {
    private long lastUseItem = -1L;
    private long useItem = -1L;
    private double buffer;
    public AutoBlockB(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType().equals(PacketType.Play.Client.INTERACT_ENTITY)) {
            WrapperPlayClientInteractEntity wrapper = new WrapperPlayClientInteractEntity(event);
            if (wrapper.getAction().equals(WrapperPlayClientInteractEntity.InteractAction.ATTACK)) {
                if (this.player.packetStateData.slowedByUsingItem) {
                    if (this.lastUseItem == -1L && this.useItem == -1L) {
                        this.useItem = this.player.lastBlockPlaceUseItem;
                        this.lastUseItem = this.useItem;
                        return;
                    }

                    //vanilla is unable to do blocking when sending more than two interact_entity packets
                    if (this.useItem == this.lastUseItem) {
                        if (flagAndAlert("Duplicate tick")) {
                            setbackIfAboveSetbackVL();
                            if (shouldModifyPackets()) {
                                event.setCancelled(true);
                                this.player.onPacketCancel();
                            }
                        }
                    } else {
                        reward();
                        buffer = Math.max(0,buffer - getDecay());
                    }

                    this.lastUseItem = this.useItem;
                    this.useItem = this.player.lastBlockPlaceUseItem;
                }
            }
        }
    }
}
