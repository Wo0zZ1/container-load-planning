import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Planner {
    private final List<LoadRule> rules;

    public Planner(List<LoadRule> rules) { this.rules = List.copyOf(rules); }

    public List<String> validate(List<Container> containers) {
        Set<String> numbers = new HashSet<>();
        List<String> problems = new ArrayList<>();
        for (Container container : containers) container.validate(numbers, problems);
        return problems;
    }

    public LoadPlan plan(Train train, List<Container> containers) {
        if (train.composition().isEmpty()) throw new IllegalArgumentException("Train has no platforms");
        if (!validate(containers).isEmpty()) throw new IllegalArgumentException("Container validation failed");
        List<Container> queue = new ArrayList<>(containers);
        queue.sort(Comparator.comparing(Container::destination)
            .thenComparing(Comparator.comparingDouble(Container::grossMass).reversed()));
        List<PlatformLoad> loads = new ArrayList<>();
        for (TrainPlatform position : train.composition()) loads.add(new PlatformLoad(position.platform()));
        List<Container> rejected = new ArrayList<>();
        for (Container container : queue) {
            boolean placed = false;
            for (PlatformLoad load : loads)
                if (load.tryAdd(container, rules)) {
                    placed = true;
                    break;
                }
            if (!placed) rejected.add(container);
        }
        return new LoadPlan(train, loads, rejected);
    }
}
