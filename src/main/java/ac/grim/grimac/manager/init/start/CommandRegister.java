package ac.grim.grimac.manager.init.start;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.commands.*;
import ac.grim.grimac.manager.init.Initable;
import co.aikar.commands.PaperCommandManager;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;

public class CommandRegister implements Initable {
    @Override
    public void start() {
        // This does not make Grim require paper
        // It only enables new features such as asynchronous tab completion on paper
        PaperCommandManager commandManager = new PaperCommandManager(GrimAPI.INSTANCE.getPlugin());

        // brigadier is currently broken on 1.19.1+ with acf
        if (PacketEvents.getAPI().getServerManager().getVersion().isOlderThan(ServerVersion.V_1_19_1)) {
            commandManager.enableUnstableAPI("brigadier");
        }

        commandManager.registerCommand(new GrimPerf());
        commandManager.registerCommand(new GrimDebug());
        commandManager.registerCommand(new GrimAlerts());
        commandManager.registerCommand(new GrimProfile());
        commandManager.registerCommand(new GrimSendAlert());
        commandManager.registerCommand(new GrimHelp());
        commandManager.registerCommand(new GrimReload());
        commandManager.registerCommand(new GrimSpectate());
        commandManager.registerCommand(new GrimStopSpectating());
        commandManager.registerCommand(new GrimLog());
        commandManager.registerCommand(new GrimLogShortcut());
        commandManager.registerCommand(new GrimVerbose());
    }
}
