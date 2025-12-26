package ac.grim.grimac.command.commands;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.command.BuildableCommand;
import ac.grim.grimac.manager.report.Report;
import ac.grim.grimac.manager.report.ReportManager;
import ac.grim.grimac.platform.api.sender.Sender;
import ac.grim.grimac.utils.anticheat.MessageUtil;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.description.Description;
import org.jetbrains.annotations.NotNull;

public class GrimReportFp implements BuildableCommand {

    @Override
    public void register(CommandManager<Sender> commandManager) {
        commandManager.command(
                commandManager.commandBuilder("grim", "grimac")
                        .literal("reportfp", Description.of("Report a false positive"))
                        .permission("grim.reportfp")
                        .required("eventId", StringParser.stringParser())
                        .required("reason", StringParser.stringParser())
                        .handler(this::handleReport)
        );
    }

    private void handleReport(@NotNull CommandContext<Sender> context) {
        Sender sender = context.sender();
        String eventId = context.get("eventId");
        String reason = context.get("reason");
        String reporterName = sender.getName();
        String reporterUuid = sender.getUniqueId() == null ? null : sender.getUniqueId().toString();

        Report report = Report.create(reporterName, reporterUuid, eventId, null, reason);
        ReportManager manager = new ReportManager(GrimAPI.INSTANCE.getGrimPlugin().getDataFolder());
        manager.append(report);

        String template = GrimAPI.INSTANCE.getConfigManager().getConfig().getStringElse("reportfp.success", "%prefix% &fReport saved: %id%");
        Component msg = MessageUtil.miniMessage(template.replace("%id%", report.getId()));
        sender.sendMessage(msg);
    }
}
