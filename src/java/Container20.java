public final class Container20 extends Container {
    private static final double HEAVY_MASS_T = 24;
    private static final double SHORT_PLATFORM_M = 19;
    private static final double LONG_PLATFORM_M = 25;

    public Container20(String number, Request request, ContainerType type,
                String typeCode, Double mass, String destination) {
        super(number, request, type, typeCode, mass, destination);
    }

    @Override
    public double teuOn(PlatformModel model) {
        if (grossMass() <= HEAVY_MASS_T) return 1;
        if (model.lengthM() > LONG_PLATFORM_M) return 2;
        if (model.lengthM() >= SHORT_PLATFORM_M) return 1.5;
        return 1;
    }
}
