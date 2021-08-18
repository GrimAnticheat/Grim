package ac.grim.grimac.alert;

import ac.grim.grimac.checks.Check;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.utils.anticheat.ColorUtil;
import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;

import java.util.List;

@RequiredArgsConstructor
public class AlertManager {
    private final Check<?> check;

    private final String base = ColorUtil.format("&8[&7GrimAC&8] &a%s &7failed &a%s &8[&7VL&A%s&8]");
    private final String broadcast = ColorUtil.format("&8[&7GrimAC&8] &a%s &7was found using an unfair advantage and was removed from the network.");

    private final List<Long> alerts = Lists.newArrayList();

    private final long lastFlag = 0;

    public void fail() {
        final long now = System.currentTimeMillis();

        final GrimPlayer player = check.getPlayer();
        final Player bukkitPlayer = player.bukkitPlayer;

    }
}
