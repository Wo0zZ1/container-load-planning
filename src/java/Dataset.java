import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Dataset {
    private final List<Train> trains;
    private final List<Container> containers;

    private Dataset(List<Train> trains, List<Container> containers) {
        this.trains = List.copyOf(trains);
        this.containers = List.copyOf(containers);
    }
    public Train train(String number) {
        return trains.stream().filter(t -> t.number().equals(number)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Train not found: " + number));
    }
    public List<Container> containersFor(Train train) {
        return containers.stream().filter(c -> c.request().id() == train.request().id()).toList();
    }
    private static List<Map<?, ?>> rows(Map<?, ?> root, String key) {
        if (!(root.get(key) instanceof List<?> values)) throw new IllegalArgumentException("Missing list: " + key);
        List<Map<?, ?>> rows = new ArrayList<>();
        for (Object value : values) {
            if (!(value instanceof Map<?, ?> row)) throw new IllegalArgumentException("Invalid row: " + key);
            rows.add(row);
        }
        return rows;
    }
    private static String str(Map<?, ?> row, String key) {
        Object value = row.get(key);
        return value == null ? null : value.toString();
    }
    private static double num(Map<?, ?> row, String key) {
        if (!(row.get(key) instanceof Number value)) throw new IllegalArgumentException("Missing number: " + key);
        return value.doubleValue();
    }
    private static int integer(Map<?, ?> row, String key) {
        if (!(row.get(key) instanceof BigDecimal value)) throw new IllegalArgumentException("Missing number: " + key);
        try {
            return value.intValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Expected integer within int range: " + key);
        }
    }
    private static <T> T reference(Map<?, T> map, Object key) {
        T value = map.get(key);
        if (value == null) throw new IllegalArgumentException("Unknown reference: " + key);
        return value;
    }
    public static Dataset read(Path file) throws IOException {
        Object value = Json.parse(Files.readString(file, StandardCharsets.UTF_8));
        if (!(value instanceof Map<?, ?> data)) throw new IllegalArgumentException("Expected JSON object");
        Map<Integer, Organizer> organizers = new LinkedHashMap<>();
        for (var row : rows(data, "organizers")) {
            int id = integer(row, "id");
            organizers.put(id, new Organizer(id, str(row, "inn"), str(row, "name")));
        }
        Map<Integer, Request> requests = new LinkedHashMap<>();
        for (var row : rows(data, "requests")) {
            int id = integer(row, "id");
            requests.put(id, new Request(id, reference(organizers, integer(row, "organizer_id")),
                str(row, "number"), str(row, "approval_date")));
        }
        Map<String, ContainerType> types = new LinkedHashMap<>();
        for (var row : rows(data, "container_types")) {
            String code = str(row, "code");
            types.put(code, new ContainerType(code, integer(row, "length_ft"), num(row, "max_gross_mass")));
        }
        Map<String, PlatformModel> models = new LinkedHashMap<>();
        for (var row : rows(data, "platform_models")) {
            String code = str(row, "code");
            models.put(code, new PlatformModel(code, num(row, "length_m"), integer(row, "length_ft"),
                num(row, "load_capacity"), num(row, "max_teu")));
        }
        Map<String, Platform> platforms = new LinkedHashMap<>();
        for (var row : rows(data, "platforms")) {
            String number = str(row, "number");
            platforms.put(number, new Platform(number, reference(models, str(row, "model_code")), str(row, "stops_layout")));
        }
        List<Train> trains = new ArrayList<>();
        for (var row : rows(data, "trains")) {
            String number = str(row, "number");
            List<TrainPlatform> composition = new ArrayList<>();
            for (var position : rows(data, "train_platforms"))
                if (number.equals(str(position, "train_number")))
                    composition.add(new TrainPlatform(number, reference(platforms, str(position, "platform_number")),
                        integer(position, "order_no")));
            composition.sort(Comparator.comparingInt(TrainPlatform::orderNo));
            trains.add(new Train(number, reference(requests, integer(row, "request_id")),
                str(row, "departure_date"), num(row, "max_length_m"), composition));
        }
        List<Container> containers = new ArrayList<>();
        for (var row : rows(data, "containers")) {
            String code = str(row, "type_code");
            ContainerType type = types.get(code);
            Request request = reference(requests, integer(row, "request_id"));
            Double mass = row.get("gross_mass") == null ? null : num(row, "gross_mass");
            String number = str(row, "number"), destination = str(row, "dest_station");
            containers.add(type != null && type.lengthFt() == 40
                ? new Container40(number, request, type, code, mass, destination)
                : new Container20(number, request, type, code, mass, destination));
        }
        return new Dataset(trains, containers);
    }
}
