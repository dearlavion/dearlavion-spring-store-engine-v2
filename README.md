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

Same collections as v1 (minus `kit_builder_settings` and `kit_builder_axis_order`, dropped as
unused): `products`, `product_items`, `categories`, `carts`, `saved_surveys`, `orders`, `shipping_details`, `popular_kits`,
`store_settings` (two singleton docs: `free_shipping_minimum` and `exchange_rate`), `user_profiles`,
`newsletter_subscribers`, `saved_kits`. `Product` and `SavedKit` use a slugified-name string `_id`
(not an auto-generated ObjectId), matching v1.

The product-item public listing (`GET /product-items`) runs the same `$lookup`/`$facet` discount
computation as v1, ported as literal BSON aggregation stages rather than re-derived with Spring
Data's fluent DSL, specifically to avoid behavioral drift.

## How the kit engine scores (KitEngine)

Deterministic and hand-checkable: no randomness, no catalog-dependent weights. The same answers
against the same catalog always produce the same kit, and you can work out any product's score with
the table below.

### Step 1 — hard filters (in or out)

Destination, season and party are tri-state. A product tagged for *other* values is **excluded
outright**, not merely down-ranked — a swimsuit has no place in a winter mountain kit.

| Product's tags on that axis | Result |
| --- | --- |
| empty, contains `All`, **or lists every value the catalog uses** | +0.5 (`AXIS_ALL`) — eligible but dampened |
| contains the shopper's answer | +2 (`AXIS_MATCH`) |
| tagged, but not with this answer | **excluded** |

The "lists every value" case matters: `[Beach, Mountain, City]` claims exactly what `All` claims,
but scored naively it would earn 2 where the honest `All` earns 0.5 — paying four times more for
the same non-claim. Exhaustive tagging doesn't outrank honesty.

### Step 2 — soft boosts (ranking only, never exclude)

An untagged product suits everything, so tagging can only ever lift a product.

| Signal | Weight |
| --- | --- |
| Activities | **+3 per overlapping activity, capped at +6** — capped for the same reason kit category is: a product tagged into many activities shouldn't win on breadth of tagging alone |
| Kit categories | **+5** first match, **+1.5** each further match, **capped at +8** (so 1 = 5, 2 = 6.5, 3+ = 8) |
| Transportation, trip length, gender | **+1.5 each** (`TRIP_SIGNAL_WEIGHT`) — attributes of the trip, not something the shopper asked to prioritise. Trip length matches on Duration's stable `code`, not its label |
| Popular | +0.8 |
| Field-tested | +0.5 |

There are no hidden nudges. Earlier versions matched words in product *names* — `laundry|packing|cube`
for long trips, `group|shared|family` for groups — which was invisible in the admin UI and broke on
a rename. Those axes are now real tags (`durations`, `parties`), so a product that belongs on a long
trip should say so with `durations: [medium, long]` and be scored like anything else.

**`Essentials Kit` never earns a place on its own.** It's the broadest bucket in the catalog, so a
product has to match one of the shopper's *other* picked kits first — this then adds to that. A
laundry bag tagged `[Essentials Kit, Laundry Kit]` is packed when Laundry was asked for, not merely
because Essentials was ticked alongside six other things.

| Product's kits | Shopper picks | Boost |
| --- | --- | --- |
| `[Essentials Kit, Laundry Kit]` | `[Essentials Kit, Weather Kit]` | **0** — only the baseline matched |
| `[Essentials Kit, Laundry Kit]` | `[Essentials Kit, Laundry Kit]` | **6.5** — Laundry, plus the baseline |
| `[Laundry Kit]` | `[Laundry Kit]` | **5** — one match |
| `[Essentials Kit]` | anything | **0** — the baseline is never enough alone |

### Step 3 — selection

**A product that earned nothing in step 2 is not recommended at all.** Neutral on all three axis
tags and no activity, kit-category, transport, trip-length or gender hit means it fits this trip no
better than any other — and being untagged, or tagged for something the shopper didn't ask for, is
not a reason to pack it. The kit comes back shorter rather than padded.

Everything that survives is then split into two tiers, because a single additive score let an
incidental tag outbid an explicit request — a swimsuit matching the selected activity outscored the
toiletry kit the shopper actually asked for. No weight tuning fixes that reliably; separating demand
from relevance does.

| Tier | Definition |
| --- | --- |
| **Requested** | Matches at least one *non-baseline* kit category the shopper picked — i.e. exactly when the kit-category boost is non-zero |
| **Related** | Earned something (an activity, destination or season match) but isn't in a kit they asked for |

Kit size comes from trip length (`day` 10, `short` 14, `medium` 20, `long` 26), widened for larger
parties, clamped to 10–30 — a ceiling, not a quota. Slots fill in three passes:

1. **Breadth** — the best still-available *requested* item for each kit the shopper picked, so the
   kit spans what they asked for. Keyed on kit category rather than the shop's product category: a
   shopper asking for a Toiletry Kit shouldn't be guaranteed one of every department, which used to
   hand a slot to the only Beauty & Grooming product however weakly it fitted.
2. **Depth** — the rest of the requested tier, highest score first.
3. **Round it out** — related items, held to a minority of the finished kit.

**The related tier is capped as a share of the kit as built, not of the target size.** Target comes
from trip length, so on a long trip it's 26 whether the shopper asked for one kit or six; measured
against that, the related tier had a budget it could never exhaust and a one-kit survey still came
back two-thirds full of things nobody asked for. Measured against what was actually requested, the
kit scales with demand:

| Requested items found | Related allowed | Finished kit |
| --- | --- | --- |
| 12 | 8 | 20 (60% requested) |
| 5 | 3 | 8 |
| 1 | 2 (`MIN_RELATED` floor) | 3 |
| 0 — nothing picked, or nothing matched | fills the whole target | up to 26 |

The floor exists because a strict share starves the narrow cases: one requested item earns a budget
of zero, so asking for a thinly-stocked kit returned a kit of one. Related items must not
*dominate*; they aren't forbidden. That last row matters too — kit category is Optional in Kit
Settings, and with nothing requested there's nothing for the related tier to be a minority of.

**No product category may exceed `max(3, target / 5)` slots** (10 → 3, 26 → 5), so a kit can't come
back as five near-identical travel accessories. It scales with kit size rather than binding hard on
the long trips that legitimately want more of everything.

Output order is requested items first, then related, each block by score — a flat score sort buried
what the shopper asked for among the extras, which reads as not having listened.

### Worked example

**Rain Jacket** — category *Clothing*; kit categories *Weather Kit, Activity Gear Kit*; activities
*Hiking, Sightseeing*; seasons *Rainy*; destinations *Mountain, City*.

Shopper answers: destination *Mountain*, season *Rainy*, party *Solo*, activities *[Hiking]*,
transport *Car*, kits *[Weather Kit, Essentials Kit]*.

```
destination  Mountain is tagged                     +2.0
season       Rainy is tagged                        +2.0
party        untagged -> neutral                    +0.5
activities   Hiking overlaps (1 x 3)                +3.0
kit category Weather Kit matches; the jacket
             isn't tagged Essentials -> 1 match     +5.0
transport    Car not listed                          0.0
popular/tested (say both)                           +1.3
                                                   ------
                                                    13.8
```

Add *Activity Gear Kit* to the shopper's picks and the kit-category term becomes 6.5 (two matches),
not 10 — that's the diminishing return. Essentials Kit is in this shopper's picks and does count
here (it isn't their only choice), but the Rain Jacket isn't tagged with it, so it earns nothing
from it. Had the shopper picked *only* Essentials Kit, every product would score 0 on this axis and
the kit would be decided by destination, season and activities alone.

> `kit-recommendation.ts` in the frontend mirrors this exactly for mock mode. The two must be
> changed together, or mock and real mode will rank differently.

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
