import java.util.List;

public final class Train {
    private final String number;
    private final Request request;
    private final String departureDate;
    private final double maxLengthM;
    private final List<TrainPlatform> composition;

    public Train(String number, Request request, String departureDate,
                 double maxLengthM, List<TrainPlatform> composition) {
        this.number = number;
        this.request = request;
        this.departureDate = departureDate;
        this.maxLengthM = maxLengthM;
        this.composition = List.copyOf(composition);
    }
    public String number() { return number; }
    public Request request() { return request; }
    public String departureDate() { return departureDate; }
    public double maxLengthM() { return maxLengthM; }
    public List<TrainPlatform> composition() { return composition; }
}
