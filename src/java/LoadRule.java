public interface LoadRule {
    boolean allows(PlatformLoad load, Container container);
}
