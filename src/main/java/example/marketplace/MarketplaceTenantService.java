package example.marketplace;

import java.io.IOException;

public final class MarketplaceTenantService {
    public record LearningFlow(String sellerAsset, String buyerUpdate, String orderHandoff) {}
    public record TenantLifecycle(String ownerAction, String keyAction) {}

    private final InfraiAccountClient infrai;

    public MarketplaceTenantService(InfraiAccountClient infrai) {
        this.infrai = infrai;
    }

    public LearningFlow modelLearningFlow(String course, String buyerEmail, String orderId) {
        return new LearningFlow("seller published " + course,
                "buyer update sent to " + buyerEmail,
                "order " + orderId + " handed to seller");
    }

    public TenantLifecycle offboardingDecision() {
        return new TenantLifecycle("user-delete-before-key-revoke", "temporary-key-only");
    }

    public void onboard(String tenantId, String ownerEmail) throws IOException, InterruptedException {
        String keyId = null;
        String userId = null;
        try {
            keyId = InfraiAccountClient.responseId(
                    infrai.createScopedKey(tenantId, "tenant-create-" + tenantId), "id", "key_id");
            userId = InfraiAccountClient.responseId(
                    infrai.createOwner(ownerEmail, "owner-create-" + tenantId), "id", "user_id");
        } finally {
            if (userId != null) {
                infrai.deleteOwner(userId);
            }
            if (keyId != null) {
                infrai.revokeTemporaryKey(keyId);
            }
        }
    }
}
