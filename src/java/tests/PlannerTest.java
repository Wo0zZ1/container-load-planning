import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public final class PlannerTest {
    private static int checks;
    private static void check(boolean result, String message) {
        checks++;
        if (!result) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        Request request = new Request(1, new Organizer(1, "1", "Test"), "1", "2026-11-16");
        ContainerType shortType = new ContainerType("1CC", 20, 30.48);
        ContainerType longType = new ContainerType("1AA", 40, 30.48);
        Container light = new Container20("A", request, shortType, "1CC", 24.0, "Station");
        Container heavy = new Container20("B", request, shortType, "1CC", 24.01, "Station");
        for (double length : List.of(18.99, 19.0, 25.0, 25.01)) {
            PlatformModel model = new PlatformModel("M", length, 80, 100, 4);
            check(light.teuOn(model) == 1, "24 т: один ДФЭ при любой длине платформы");
            double expected = length < 19 ? 1 : length <= 25 ? 1.5 : 2;
            check(heavy.teuOn(model) == expected, "Тяжелый контейнер: границы 19 и 25 м");
            check(new Container40("C", request, longType, "1AA", 20.0, "Station").teuOn(model) == 2,
                "40 футов: два ДФЭ");
        }
        Platform platform = new Platform("P", new PlatformModel("M", 19.6, 40, 48, 2), "40");
        PlatformLoad load = new PlatformLoad(platform);
        List<LoadRule> rules = List.of(new LengthRule(), new MassRule());
        check(load.tryAdd(light, rules), "Первый контейнер помещается");
        check(load.tryAdd(light, rules), "Точная граница массы и длины разрешена");
        check(!load.tryAdd(light, rules), "Превышение длины и массы отклонено");
        check(load.containers().size() == 2 && load.mass() == 48 && load.usedFt() == 40,
            "Отказ не меняет содержимое платформы");
        check(load.teu() == 2, "Вместимость ограничивает ДФЭ платформы");
        check("20+20".equals(load.requiredStops()), "Упоры меняются при нехватке двух позиций 20 футов");
        check(new Platform("P", platform.model(), "20+20").requiredStops(List.of(light, light)) == null,
            "Совпадающие упоры не меняются");
        check(new Platform("P", platform.model(), "40").requiredStops(List.of()) == null,
            "Пустая платформа не требует перестановки");
        Planner planner = new Planner(rules);
        check(planner.validate(List.of(light, light)).size() == 1, "Повтор номера обнаружен");
        Container invalid = new Container20("D", request, null, "missing", null, "");
        check(planner.validate(List.of(invalid)).size() == 3, "Неизвестный тип, масса и станция проверяются вместе");
        for (double mass : List.of(-28.4, 0.0)) {
            Container badMass = new Container20("Z", request, shortType, "1CC", mass, "S");
            check(planner.validate(List.of(badMass)).stream().anyMatch(problem -> problem.contains("масса брутто")),
                "Неположительная масса отклонена: " + mass);
        }
        check(planner.validate(List.of(new Container20("E", request, shortType, "1CC", 31.0, "S"))).size() == 1,
            "Превышение массы типоразмера обнаружено");
        Train train = new Train("T", request, "2026-11-16", 100, List.of(new TrainPlatform("T", platform, 1)));
        LoadPlan plan = planner.plan(train, List.of(light, heavy));
        check(plan.placements().size() == 1 && plan.rejected().size() == 1, "Ограничение массы формирует список невошедших");
        check(plan.loadedTeu() == 1.5 && plan.fillPercent() == 75 && !plan.normMet(), "Невыполнение нормы рассчитывается");
        check(Math.abs(plan.lackTeu() - 0.34) < 1e-9, "Дефицит ДФЭ равен 0,34");
        PlatformLoad draft = new PlatformLoad(platform);
        draft.tryAdd(light, rules);
        LoadPlan snapshot = new LoadPlan(train, List.of(draft), List.of());
        check(!snapshot.loads().get(0).tryAdd(light, rules), "Загрузка готового плана не принимает новые контейнеры");
        check(draft.tryAdd(light, rules) && snapshot.loadedTeu() == 1 && snapshot.placements().size() == 1,
            "Изменение черновика не меняет ДФЭ и размещения готового плана");
        Object parsed = Json.parse("{\"a\":[true,null,-1.25e2,\"\\u0410\\n\\\"\"]}");
        check(parsed instanceof Map && Json.parse(Json.stringify(parsed)).equals(parsed), "JSON с Unicode и escapes проходит обратное преобразование");
        for (String json : List.of("{", "[1,]", "01", "true false", "\"line\nend\"")) {
            boolean failed = false;
            try { Json.parse(json); } catch (IllegalArgumentException exception) { failed = true; }
            check(failed, "Некорректный JSON отклонен: " + json);
        }
        Path input = Path.of(args[0]);
        String sample = Files.readString(input);
        Path invalidInput = Files.createTempFile("load-planning-", ".json");
        try {
            for (String id : List.of("1.5", "2147483648", "1.0000000000000001")) {
                Files.writeString(invalidInput, sample.replaceFirst("\"request_id\": 1,", "\"request_id\": " + id + ","));
                String error = null;
                try { Dataset.read(invalidInput); } catch (IllegalArgumentException exception) { error = exception.getMessage(); }
                check(error != null && error.contains("request_id"), "Некорректный request_id отклонен: " + id);
            }
        } finally {
            Files.deleteIfExists(invalidInput);
        }
        Dataset data = Dataset.read(input);
        LoadPlan example = planner.plan(data.train("2041"), data.containersFor(data.train("2041")));
        check(example.placements().size() == 17 && example.rejected().get(0).number().equals("SBRU2000064"),
            "Контрольный пример: 17 размещений, один невошедший");
        check(example.loadedTeu() == 24 && example.capacityTeu() == 25 && example.fillPercent() == 96,
            "Контрольный пример: 24/25 ДФЭ, 96 процентов");
        check(example.loads().stream().filter(l -> l.requiredStops() != null).count() == 3,
            "Контрольный пример: три перестановки упоров");
        System.out.println("PASS: " + checks + " checks");
    }
}
