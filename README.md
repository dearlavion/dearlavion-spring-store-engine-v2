# dearlavion-spring-store-engine-v2

Spring Boot (Java 21) port of `dearlavion-store-engine` (NestJS + Mongoose). Same API contract,
same MongoDB collections/field names, same auth-delegation model — a mechanical, like-for-like
port, not a redesign. Backend for the **Travel Kit** storefront: the free packing-list survey, the
product catalog, cart/checkout, orders, and the admin dashboard.

Runs **alongside** the NestJS v1 service on a **new port (4010)**. Both point at the same MongoDB
deployment once cut over; during development this service uses its own local/dev database so
iterative admin-write testing never touches shared data. Nothing is wired into the live frontend or
`payment-service` yet — the port is complete and verified, but the actual cutover (flipping
`environment.dev.ts`'s `apiUrl` and payment-service's `STORE_ENGINE_URL`) is a separate, explicit
step.

## Auth (delegates to auth-service, doesn't decode JWTs locally)

Bearer tokens are verified against auth-service's `POST /auth/verify`
(`AuthClientService` + `AuthenticationFilter`), which returns
`{ valid, username, email, userId, activeProfile, customer }`.

- **Admin** is role-based: `activeProfile` ∈ `{ADMIN, STAFF}` grants `/admin/**`. The
  `ADMIN_USERNAMES` allowlist is kept only as a bootstrap escape hatch.
- **Tenant isolation**: if `EXPECTED_CUSTOMER` is set, tokens whose `customer` claim differs are
  rejected.
- No token on a route that needs one → **403**; a present-but-invalid token → **401** — this
  distinction is easy to get backwards in a fresh Spring Security filter chain and is covered by
  integration tests.

## Endpoints

70 routes total, 1:1 with the NestJS source (verified by an automated route-inventory diff during
the port — see "Verification" below).

| Module | Public | Authenticated (user) | Admin |
|---|---|---|---|
| Products | `GET /products`, `/products/{id}` | — | `/admin/products/**` |
| Product Items | `GET /product-items` | — | `/admin/product-items/**` |
| Categories | `GET /categories` | — | `/admin/categories/**` |
| Taxonomy (Kit Settings) | `GET /taxonomies`, `/taxonomies/axis-order` | — | `/admin/taxonomies/**` |
| Survey | `POST /survey/recommendations` | `/surveys` (save/list/delete) | — |
| Cart | — | `/cart/**` | — |
| Orders | — | `/orders/**` (place, list, cancel, payment-pending) | `/admin/orders/**` (fulfillment) |
| Shipping details | — | `/shipping-details` | — |
| Popular Kits | `GET /popular-kits/**` | — | `/admin/popular-kits/**` |
| Store settings / exchange rates | `GET /store-settings`, `/exchange-rates` | — | `/admin/store-settings`, `/admin/exchange-rates` |
| Profile | — | `/profile` | — |
| Stats | `GET /stats/top-selling` | — | `/admin/stats/product-items` |
| Newsletter | `POST /newsletter/subscribe` | — | — |
| Collection (saved kits) | — | `/kits/**` | — |

## Data model

Same collections as v1: `products`, `product_items`, `categories`, `kit_builder_settings`,
`kit_builder_axis_order`, `carts`, `saved_surveys`, `orders`, `shipping_details`, `popular_kits`,
`store_settings` (two singleton docs: `free_shipping_minimum` and `exchange_rate`), `user_profiles`,
`newsletter_subscribers`, `saved_kits`. `Product` and `SavedKit` use a slugified-name string `_id`
(not an auto-generated ObjectId), matching v1.

The product-item public listing (`GET /product-items`) runs the same `$lookup`/`$facet` discount
computation as v1, ported as literal BSON aggregation stages rather than re-derived with Spring
Data's fluent DSL, specifically to avoid behavioral drift.

## Known, intentional deviation from v1

`StatsService.getProductItemPerformance()` has a pre-existing bug in the NestJS source: the sales
aggregation groups by the order line's generic Product slug, but the join then looks up by the
ProductItem's own id — two key spaces that never intersect, so `unitsSold`/`orderCount`/`revenue`
read `0` even for real orders. This was ported **as-is** (not fixed) so this service matches v1
exactly for the Phase 4 parity diff; see the comment on that method if you're the one who eventually
fixes it upstream.

## Commands

```bash
mvn compile
mvn test                              # Testcontainers integration tests (needs Docker)
mvn spring-boot:run                   # dev server on :4010
mvn spring-boot:run -Dspring-boot.run.profiles=seed   # one-shot: seed categories/taxonomy/products, then exits like a normal run (Ctrl+C after "Seeded ..." logs)
```

Tests use Testcontainers (`mongo:7`) and mock `AuthClientService` — no real auth-service needed to
run them.

## Running locally

```bash
export MONGODB_URI=mongodb://localhost:27017/dearlavion-spring-store-engine-v2-dev
export AUTH_SERVER_URL=http://localhost:9081   # or :9082 for auth-service-v3
mvn spring-boot:run
```

Seed the dev database once (idempotent, upserts by slug):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=seed
```

Swagger UI at `/swagger-ui/index.html`.

## Configuration (env vars)

| Var | Default |
|---|---|
| `PORT` | 4010 |
| `MONGODB_URI` | `mongodb://localhost:27017/dearlavion-spring-store-engine-v2-dev` |
| `AUTH_SERVER_URL` | `http://localhost:8081` |
| `EXPECTED_CUSTOMER` | *(empty)* |
| `ADMIN_USERNAMES` | `admin` |
| `FRONTEND_ORIGIN` | `http://localhost:4200` (CORS) |

Health at `/health`.

## Verification performed

- All 4 phases (foundation/reads, purchase flow, admin/secondary modules, structural parity diff)
  built and tested incrementally — 17 Testcontainers integration tests covering catalog CRUD, the
  aggregation pipeline, cart, the full order lifecycle (place → payment-pending → admin approve →
  ship-with-inventory-decrement → deliver → archive), and every admin/secondary module.
- A route-inventory diff against the NestJS source: **70/70 routes match exactly** (verb + path).
- A collection-name diff: **14/14 MongoDB collections match exactly**.
- Manual curl sweeps against a seeded local dev database for every module.

No live-data parity diff against the shared Atlas cluster has been done (would need a read-only DB
credential — the write-capable service was correctly blocked from connecting to production data
during development).
