import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        try {
            if (args.length != 2 && args.length != 3)
                throw new IllegalArgumentException("usage: java Main <data.json> <train_number> [plan.json]");
            Dataset data = Dataset.read(Path.of(args[0]));
            Train train = data.train(args[1]);
            Planner planner = new Planner(List.of(new LengthRule(), new MassRule()));
            List<Container> containers = data.containersFor(train);
            List<String> problems = planner.validate(containers);
            if (!problems.isEmpty()) {
                System.out.println("В данных контейнеров есть ошибки, план не сформирован:");
                for (String problem : problems) System.out.println("  " + problem);
                System.exit(1);
            }
            LoadPlan plan = planner.plan(train, containers);
            print(plan);
            if (args.length == 3) {
                Files.writeString(Path.of(args[2]), Json.stringify(plan.toJson()) + "\n", StandardCharsets.UTF_8);
                System.out.println("План сохранен в " + args[2]);
            }
        } catch (IOException | IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
            System.exit(1);
        }
    }

    private static String number(double value) {
        return new BigDecimal(String.format(Locale.ROOT, "%.6g", value)).stripTrailingZeros().toPlainString().replace('.', ',');
    }
    private static String describe(Container container) {
        return container.number() + " " + container.typeCode() + ", " + number(container.grossMass()) +
            " т, " + container.destination();
    }
    private static void print(LoadPlan plan) {
        System.out.println("План погрузки поезда " + plan.train().number() + ", отправление " + plan.train().departureDate());
        System.out.println();
        int order = 1;
        for (PlatformLoad load : plan.loads()) {
            System.out.println(order++ + ". Платформа " + load.platform().number() + " (" +
                load.platform().model().code() + "): масса " + number(load.mass()) + " т, " + number(load.teu()) + " ДФЭ");
            int slot = 1;
            for (Container container : load.containers()) System.out.println("   " + slot++ + ") " + describe(container));
        }
        System.out.println();
        System.out.println("Загружено " + number(plan.loadedTeu()) + " ДФЭ из " + number(plan.capacityTeu()) +
            ", заполнение " + String.format(Locale.ROOT, "%.1f", plan.fillPercent()).replace('.', ',') + "%");
        System.out.println("Норма " + LoadPlan.NORM_PERCENT + "% " + (plan.normMet() ? "выполнена" :
            "не выполнена, не хватает " + number(Math.rint(plan.lackTeu() * 10) / 10) + " ДФЭ"));
        System.out.println();
        boolean changes = plan.loads().stream().anyMatch(l -> l.requiredStops() != null);
        System.out.println(changes ? "Перестановка упоров:" : "Перестановка упоров не нужна");
        for (PlatformLoad load : plan.loads())
            if (load.requiredStops() != null)
                System.out.println("  платформа " + load.platform().number() + ": " + load.platform().stopsLayout() +
                    " -> " + load.requiredStops());
        System.out.println(plan.rejected().isEmpty() ? "Все контейнеры вошли в поезд" : "Не вошли в поезд:");
        for (Container container : plan.rejected()) System.out.println("  " + describe(container));
    }
}
