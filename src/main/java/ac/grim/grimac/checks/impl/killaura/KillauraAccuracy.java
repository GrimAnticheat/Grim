package ac.grim.grimac.checks.impl.killaura;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PacketCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.data.packetentity.PacketEntity;
import ac.grim.grimac.utils.lists.EvictingQueue;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;

import java.util.List;


@CheckData(name = "KillauraAccuracy")
public class KillauraAccuracy extends Check implements PacketCheck {
    private int maxAccuracy;
    private int sampleSize;
    private double minDistance;
    private boolean hit = false;
    private EvictingQueue<Boolean> hitList;

    public KillauraAccuracy(GrimPlayer player) {
        super(player);
    }

    @Override
    public void onPacketReceive(final PacketReceiveEvent event) {
        if (!player.disableGrim && event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            WrapperPlayClientInteractEntity action = new WrapperPlayClientInteractEntity(event);
            if (action.getAction() != WrapperPlayClientInteractEntity.InteractAction.ATTACK) {
                return;
            }
            PacketEntity entity = player.compensatedEntities.entityMap.get(action.getEntityId());
            if (entity.type != EntityTypes.PLAYER) {
                return;
            }
            if(entity.getPossibleCollisionBoxes().distance(player.boundingBox) > minDistance) {
                hit = true;
            }

            if (hitList.size() == sampleSize) {
                double accuracy = calculateTruePercentage(hitList);
                if (accuracy >= maxAccuracy) {
                    flagAndAlert("accuracy=" + accuracy);
                }
            }

        } else if (!player.disableGrim && event.getPacketType() == PacketType.Play.Client.ANIMATION) {
            hitList.add(hit);
            hit = false; //set the current hit to false
        }


    }

    public static double calculateTruePercentage(List<Boolean> booleanList) {
        int trueCount = 0;
        for (Boolean bool : booleanList) {
            if (bool) {
                trueCount++;
            }
        }
        return (double) trueCount / booleanList.size() * 100;
    }


    @Override
    public void reload() {
        super.reload();
        this.maxAccuracy = Math.min(100, getConfig().getIntElse(getConfigName() + ".maxAccuracy", 95));
        this.sampleSize = getConfig().getIntElse(getConfigName() + ".sampleSize", 25);
        this.minDistance = getConfig().getDoubleElse(getConfigName() + ".minDistance", 0.7);
        hitList = new EvictingQueue<>(sampleSize);
    }
}
