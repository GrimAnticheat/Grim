package ac.grim.grimac.checks.impl.prediction;

import ac.grim.grimac.checks.CheckData;
import ac.grim.grimac.checks.type.PostPredictionCheck;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.update.PredictionComplete;
import ac.grim.grimac.utils.math.GrimMath;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@CheckData(name = "Prediction", buffer = 0)
public class OffsetHandler extends PostPredictionCheck {
    List<OffsetData> regularOffsets;
    List<OffsetData> vehicleOffsets;

    public OffsetHandler(GrimPlayer player) {
        super(player);
    }

    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        double offset = predictionComplete.getOffset();

        boolean vehicle = predictionComplete.getData().inVehicle;

        for (OffsetData offsetHandler : (vehicle ? vehicleOffsets : regularOffsets)) {
            if (offset > offsetHandler.getThreshold()) {
                offsetHandler.flag();
                double violations = offsetHandler.getViolations();

                if (violations > offsetHandler.getSetbackVL()) {
                    // Patch LiquidBounce Spartan NoFall
                    player.bukkitPlayer.setFallDistance((float) player.fallDistance);
                    player.getSetbackTeleportUtil().executeSetback(true);
                }

                if (violations > offsetHandler.getAlertMin()) {
                    int diff = GrimMath.floor(violations) - GrimMath.floor(offsetHandler.getAlertMin());
                    if (diff % offsetHandler.getAlertInterval() == 0) {
                        String formatOffset = formatOffset(offset);

                        alert("o: " + formatOffset, (vehicle ? "Prediction" : "Vehicle Prediction") + "-" + offsetHandler.getName(), GrimMath.floor(violations) + "");
                    }
                }

                // Don't flag lower offset checks
                break;
            } else {
                offsetHandler.reward();
            }
        }
    }

    @Override
    public void reload() {
        List<OffsetData> offsets = new ArrayList<>();
        loadOffsets(offsets, "Prediction");
        this.regularOffsets = offsets;

        List<OffsetData> vehicleOffsets = new ArrayList<>();
        loadOffsets(vehicleOffsets, "Vehicle");
        this.vehicleOffsets = vehicleOffsets;

        this.alertVL = -1;
        this.alertInterval = 1;
    }

    public void loadOffsets(List<OffsetData> offsets, String configName) {
        try {
            ConfigurationSection section = getConfig().getConfigurationSection(configName);

            for (String key : section.getKeys(false)) {
                double threshold = getConfig().getDouble(configName + "." + key + ".threshold");
                double setbackVL = getConfig().getDouble(configName + "." + key + ".setbackvl");
                double reward = getConfig().getDouble(configName + "." + key + ".decay");
                double alertMin = getConfig().getDouble(configName + "." + key + ".dont-alert-until");
                double alertInterval = getConfig().getDouble(configName + "." + key + ".alert-interval");

                offsets.add(new OffsetData(key, threshold, setbackVL, reward, alertMin, alertInterval));
            }
        } catch (Exception e) {
            e.printStackTrace();
            offsets.add(new OffsetData("small", 0.0001, 40, 0.125, 20, 10));
            offsets.add(new OffsetData("medium", 0.01, 15, 0.05, 10, 10));
            offsets.add(new OffsetData("large", 0.6, 1, 0.001, 3, 1));
        }

        // Order based on highest offset to the lowest offset
        offsets.sort(Collections.reverseOrder(Comparator.comparingDouble(offset -> offset.threshold)));
    }
}

@Getter
@Setter
class OffsetData {
    String name;
    double threshold;
    double setbackVL;
    double reward;

    double alertMin;
    double alertInterval;

    double violations = 0;

    public OffsetData(String name, double threshold, double setbackVL, double reward, double alertMin, double alertInterval) {
        this.name = name;
        this.threshold = threshold;
        this.setbackVL = setbackVL;
        this.reward = reward;
        this.alertMin = alertMin;
        this.alertInterval = alertInterval;
    }

    public void flag() {
        violations++;
    }

    public void reward() {
        violations = Math.max(violations - reward, 0);
    }
}