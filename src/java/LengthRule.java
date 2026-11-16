public final class LengthRule implements LoadRule {
    @Override
    public boolean allows(PlatformLoad load, Container container) {
        return load.usedFt() + container.type().lengthFt() <= load.platform().model().lengthFt();
    }
}
