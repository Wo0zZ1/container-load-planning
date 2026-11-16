public final class Container40 extends Container {

    public Container40(String number, Request request, ContainerType type,
                String typeCode, Double mass, String destination) {
        super(number, request, type, typeCode, mass, destination);
    }

    @Override
    public double teuOn(PlatformModel model) { return 2; }
}
