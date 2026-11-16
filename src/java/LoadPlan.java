import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LoadPlan {
    public static final int NORM_PERCENT = 92;
    private final String id;
    private final Train train;
    private final LocalDateTime createdAt;
    private final List<PlatformLoad> loads;
    private final List<Container> rejected;
    private final List<Placement> placements;

    public LoadPlan(Train train, List<PlatformLoad> loads, List<Container> rejected) {
        this.train = train;
        this.loads = List.copyOf(loads);
        this.rejected = List.copyOf(rejected);
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        id = train.number() + "-" + createdAt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        List<Placement> positions = new ArrayList<>();
        for (PlatformLoad load : loads) {
            int slot = 1;
            for (Container container : load.containers())
                positions.add(new Placement(id, container, load.platform(), slot++));
        }
        placements = List.copyOf(positions);
    }
    public String id() { return id; }
    public Train train() { return train; }
    public LocalDateTime createdAt() { return createdAt; }
    public List<PlatformLoad> loads() { return loads; }
    public List<Container> rejected() { return rejected; }
    public List<Placement> placements() { return placements; }
    public double loadedTeu() { return loads.stream().mapToDouble(PlatformLoad::teu).sum(); }
    public double capacityTeu() { return loads.stream().mapToDouble(l -> l.platform().model().maxTeu()).sum(); }
    public double fillPercent() { return loadedTeu() / capacityTeu() * 100; }
    public boolean normMet() { return fillPercent() >= NORM_PERCENT; }
    public double lackTeu() { return Math.max(0, capacityTeu() * NORM_PERCENT / 100 - loadedTeu()); }

    public Map<String, Object> toJson() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("id", id);
        metadata.put("train_number", train.number());
        metadata.put("created_at", createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Placement placement : placements) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("plan_id", id);
            row.put("container_number", placement.container().number());
            row.put("platform_number", placement.platform().number());
            row.put("slot_no", placement.slotNo());
            rows.add(row);
        }
        return Map.of("load_plan", metadata, "placements", rows);
    }
}
