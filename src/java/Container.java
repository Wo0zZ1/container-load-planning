import java.util.List;
import java.util.Set;

public abstract class Container {
    private final String number;
    private final Request request;
    private final ContainerType type;
    private final String typeCode;
    private final Double grossMass;
    private final String destination;

    protected Container(String number, Request request, ContainerType type,
                        String typeCode, Double grossMass, String destination) {
        this.number = number;
        this.request = request;
        this.type = type;
        this.typeCode = typeCode;
        this.grossMass = grossMass;
        this.destination = destination;
    }

    public String number() { return number; }
    public Request request() { return request; }
    public ContainerType type() { return type; }
    public double grossMass() { return grossMass; }
    public String destination() { return destination; }
    public String typeCode() { return typeCode; }
    public abstract double teuOn(PlatformModel model);

    public void validate(Set<String> numbers, List<String> problems) {
        if (!numbers.add(number))
            problems.add(number + ": контейнер указан в списке повторно");
        if (type == null)
            problems.add(number + ": неизвестный типоразмер " + typeCode);
        if (destination == null || destination.isEmpty())
            problems.add(number + ": не указана станция назначения");
        if (grossMass == null || grossMass == 0)
            problems.add(number + ": не указана масса брутто");
        else if (grossMass < 0)
            problems.add(number + ": масса брутто должна быть больше нуля");
        else if (type != null && grossMass > type.maxGrossMass())
            problems.add(number + ": масса брутто " + grossMass +
                " т больше допустимой для типоразмера " + typeCode);
    }
}
