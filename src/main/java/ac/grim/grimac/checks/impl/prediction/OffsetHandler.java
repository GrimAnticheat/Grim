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
    List<OffsetData> offsets;

    public OffsetHandler(GrimPlayer player) {
        super(player);
    }

    public void onPredictionComplete(final PredictionComplete predictionComplete) {
        double offset = predictionComplete.getOffset();

        for (OffsetData offsetHandler : offsets) {
            if (offset > offsetHandler.getThreshold()) {
                offsetHandler.flag();
                double violations = offsetHandler.getViolations();

                if (violations > offsetHandler.getThreshold()) {
                    setback();
                }


                if (violations > offsetHandler.getAlertMin()) {
                    int diff = GrimMath.floor(violations) - GrimMath.floor(offsetHandler.getAlertMin());
                    if (diff % offsetHandler.getAlertInterval() == 0) {
                        alert("offset: " + offset, getCheckName() + "-" + offsetHandler.getName(), offsetHandler.getViolations());
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

        try {
            ConfigurationSection section = getConfig().getConfigurationSection("Prediction");

            for (String key : section.getKeys(false)) {
                double threshold = getConfig().getDouble("Prediction." + key + ".threshold");
                double setbackVL = getConfig().getDouble("Prediction." + key + ".setbackvl");
                double reward = getConfig().getDouble("Prediction." + key + ".decay");
                double alertMin = getConfig().getDouble("Prediction." + key + ".dont-alert-until");
                double alertInterval = getConfig().getDouble("Prediction." + key + ".alert-interval");

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

        this.offsets = offsets;
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