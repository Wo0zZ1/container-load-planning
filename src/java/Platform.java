import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record Platform(String number, PlatformModel model, String stopsLayout) {
    public String requiredStops(List<Container> containers) {
        Map<Integer, Integer> available = new HashMap<>();
        for (String slot : stopsLayout.split("\\+"))
            available.merge(Integer.parseInt(slot), 1, Integer::sum);
        boolean change = false;
        for (Container container : containers) {
            int length = container.type().lengthFt();
            int count = available.getOrDefault(length, 0);
            if (count == 0) change = true;
            else available.put(length, count - 1);
        }
        if (!change) return null;
        return containers.stream().map(c -> c.type().lengthFt())
            .sorted(java.util.Comparator.reverseOrder()).map(String::valueOf)
            .collect(java.util.stream.Collectors.joining("+"));
    }
}
