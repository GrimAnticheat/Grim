package ac.grim.grimac.manager.report;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReportManagerTest {

    @Test
    public void appendAndReadRecent() throws Exception {
        Path tmp = Files.createTempDirectory("grim-reports-test");
        ReportManager manager = new ReportManager(tmp.toFile());

        Report r = Report.create("tester", "0000-0000", "evt-1", "target", "reasonable reason");
        manager.append(r);

        List<Report> recent = manager.recent(10);
        assertEquals(1, recent.size());
        assertEquals(r.getId(), recent.get(0).getId());
    }
}
