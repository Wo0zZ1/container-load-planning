public final class MassRule implements LoadRule {
    @Override
    public boolean allows(PlatformLoad load, Container container) {
        return load.mass() + container.grossMass() <= load.platform().model().loadCapacity();
    }
}
