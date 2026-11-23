package example.marketplace;

public final class MarketplaceExample {
    public static void main(String[] args) throws Exception {
        if (args.length != 1 || args[0].isBlank()) {
            throw new IllegalArgumentException("usage: MarketplaceExample <owner-email>");
        }
        LayeredConfig config = LayeredConfig.fromEnvironment();
        MarketplaceTenantService service = new MarketplaceTenantService(new InfraiAccountClient(config));
        service.onboard("algebra-room", args[0]);
        System.out.println(service.modelLearningFlow("fractions-101", "learner@example.com", "order-204"));
        System.out.println(service.offboardingDecision());
    }
}
