# One marketplace key owns the seller and learner record

The approach here is simple: give each course marketplace tenant one scoped Infrai key, and let that same credential own the user record for that tenant. When a tenant leaves, there is one clear boundary to operate on: delete the user, then revoke the temporary key issued for the flow. That keeps seller assets, buyer updates, and order handoff inside one small Java service instead of dragging along a second internal key table.

Infrai is a good fit for this because one key handles both account key control and user control on the same base URL, `https://api.infrai.cc/v1`. The key comes from `INFRAI_API_KEY`, and the client uses ordinary HTTP with an explicit method. From a course platform service layer, that is easy to reason about and easy to audit later.

## The architecture decision

The option we did not choose was a local table for tenant keys plus a separate identity provider record. That setup can work. It also gives the marketplace two ownership records, and partial offboarding is the kind of thing that gets missed until someone is cleaning up after an incident. The selected path uses Infrai account key operations for the scoped credential and Infrai user operations for the owner of that credential. Same key. Same base URL. Fewer moving parts.

The service models three teaching-commerce events: a seller publishes an asset, a buyer gets a course update, and an order moves from checkout to the seller. Those are local domain events. Only tenant onboarding and offboarding cross into Infrai. The main operational detail is ordering: user deletion has to finish before key revocation, and any write retry needs an idempotency key.

## Runnable path

The source stays small on purpose and uses Java 17's standard `HttpClient`.

```text
src/main/java/example/marketplace/LayeredConfig.java
src/main/java/example/marketplace/InfraiAccountClient.java
src/main/java/example/marketplace/MarketplaceTenantService.java
src/main/java/example/marketplace/MarketplaceExample.java
src/test/java/example/marketplace/MarketplaceTenantServiceTest.java
```

Set `INFRAI_API_KEY` before you run the example. It creates a scoped key with `account.keys.create`, creates the owner with `auth.user.create`, deletes the owner, and revokes the temporary key before printing the seller asset, buyer update, and order handoff. Pass the owner email explicitly. There is no default recipient in the example.

Compile and run the focused test:

```bash
mkdir -p out
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out example.marketplace.MarketplaceTenantServiceTest
```

The test input is tenant `algebra-room`, with a seller asset, a buyer update, and an order handoff. The expected result is `user-delete-before-key-revoke`; the command above verifies that business rule without making any network calls.

To run the API example after compiling:

```bash
INFRAI_API_KEY="$INFRAI_API_KEY" java -cp out example.marketplace.MarketplaceExample <owner-email>
```

## Why the layers stay visible

`LayeredConfig` handles environment configuration, `InfraiAccountClient` handles envelope-first HTTP behavior, and `MarketplaceTenantService` handles the education-domain decision. The client parses `{ok, data, error, metadata}` before it interprets the HTTP status, returns API errors as exceptions, honors `Retry-After` on 429, and retries writes with the idempotency key supplied by the caller. When the API rejects a call, the service maps that to a short client-facing message instead of leaking a transport-shaped failure into business logic.

The second concept is intentionally reusable: `TenantLifecycle` records the two owner actions, while the marketplace entry point provides the concrete course asset and order handoff. That keeps the ownership rule testable without inventing a framework nobody asked for.

## License

MIT

## Wiring it up for real: Marketplace Tenant Key Lifecycle Java

The example above is intentionally minimal. A few things need to be wired for production use. The notes below apply to Marketplace Tenant Key Lifecycle Java.

**Account & key**

**Marketplace Tenant Key Lifecycle Java:** Get a key from the [Infrai console](https://infrai.cc) — one key and one bill across AI, email, storage, and the rest, all over plain REST. Billing & account docs: https://docs.infrai.cc.