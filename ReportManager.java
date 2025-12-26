package ac.grim.grimac.manager.report;

import ac.grim.grimac.utils.anticheat.LogUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ReportManager {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static volatile ReportManager instance;

    private final File file;
    private final Lock lock = new ReentrantLock();

    public ReportManager(File dataFolder) {
        this.file = new File(dataFolder, "reports.jsonl");
    }

    public static ReportManager getDefault() {
        if (instance == null) {
            synchronized (ReportManager.class) {
                if (instance == null) {
                    instance = new ReportManager(new File(System.getProperty("user.dir")));
                }
            }
        }
        return instance;
    }

    public Report append(Report report) {
        lock.lock();
        try {
            Path parent = file.toPath().getParent();
            if (parent != null) Files.createDirectories(parent);
            try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(GSON.toJson(report));
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            LogUtil.error("Failed to append report", e);
        } finally {
            lock.unlock();
        }
        return report;
    }

    public List<Report> recent(int limit) {
        if (!file.exists()) return Collections.emptyList();
        try (Stream<String> lines = Files.lines(file.toPath(), StandardCharsets.UTF_8)) {
            List<String> all = lines.collect(Collectors.toList());
            if (all.isEmpty()) return Collections.emptyList();
            int start = Math.max(0, all.size() - limit);
            List<String> sub = all.subList(start, all.size());
            List<Report> out = new ArrayList<>(sub.size());
            for (String s : sub) out.add(GSON.fromJson(s, Report.class));
            return out;
        } catch (IOException e) {
            LogUtil.error("Failed to read recent reports", e);
            return Collections.emptyList();
        }
    }

    public Optional<Report> findById(String id) {
        if (!file.exists()) return Optional.empty();
        try (Stream<String> lines = Files.lines(file.toPath(), StandardCharsets.UTF_8)) {
            return lines.map(s -> GSON.fromJson(s, Report.class)).filter(r -> r.getId().equals(id)).findFirst();
        } catch (IOException e) {
            LogUtil.error("Failed to search report by id", e);
            return Optional.empty();
        }
    }

    public File getFile() {
        return file;
    }
}
