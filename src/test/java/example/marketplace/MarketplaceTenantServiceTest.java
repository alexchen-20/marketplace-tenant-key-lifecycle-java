package example.marketplace;

public final class MarketplaceTenantServiceTest {
    public static void main(String[] args) {
        MarketplaceTenantService service = new MarketplaceTenantService(null);
        MarketplaceTenantService.LearningFlow flow = service.modelLearningFlow(
                "fractions-101", "learner@example.com", "order-204");
        if (!flow.sellerAsset().equals("seller published fractions-101")
                || !flow.buyerUpdate().equals("buyer update sent to learner@example.com")
                || !flow.orderHandoff().equals("order order-204 handed to seller")) {
            throw new AssertionError("learning flow did not preserve the marketplace events");
        }
        if (!service.offboardingDecision().ownerAction().equals("user-delete-before-key-revoke")) {
            throw new AssertionError("offboarding must remove the owner before revoking the temporary key");
        }
        System.out.println("PASS: user-delete-before-key-revoke");
    }
}
