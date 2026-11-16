import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PlatformLoad {
    private final Platform platform;
    private final List<Container> containers = new ArrayList<>();
    private int usedFt;
    private double mass;

    public PlatformLoad(Platform platform) { this.platform = platform; }
    public Platform platform() { return platform; }
    public int usedFt() { return usedFt; }
    public double mass() { return mass; }
    public List<Container> containers() { return Collections.unmodifiableList(containers); }

    public boolean tryAdd(Container container, List<LoadRule> rules) {
        for (LoadRule rule : rules)
            if (!rule.allows(this, container)) return false;
        containers.add(container);
        usedFt += container.type().lengthFt();
        mass += container.grossMass();
        return true;
    }

    public double teu() {
        double sum = 0;
        for (Container container : containers) sum += container.teuOn(platform.model());
        return Math.min(sum, platform.model().maxTeu());
    }
    public String requiredStops() { return platform.requiredStops(containers); }
}
