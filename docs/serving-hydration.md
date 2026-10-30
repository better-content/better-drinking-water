# Serving hydration (source-only integration)

Drinking-water remains the sole hydration state owner. No dependency on better-cooking is added.
This implementation is not production deployment, native qualification, or pack acceptance.

## Typed integration

`com.bettercontent.betterdrinkingwater.api.ServingHydration` exposes:

```java
record Profile(double hydration, double quenching) // finite, bounded, nonnegative; ZERO available
interface Resolver {
    Optional<Profile> resolve(ServerPlayer player, ItemStack consumedServing);
}
static void register(ResourceLocation providerId, Resolver resolver);
static Optional<Profile> resolve(ServerPlayer player, ItemStack consumedServing);
static boolean completeFood(ServerPlayer player, long completionSequence, Profile profile);
static boolean completeFood(ServerPlayer player, long completionSequence, ItemStack consumedServing);
```

Register a pure resolver during common setup. Resolution receives an isolated stack copy and
makes no state changes. Display and completion use this same resolver. Empty means unmanaged,
not zero hydration. A present ZERO profile owns explicitly dry food. Duplicate IDs and overlapping
providers fail rather than silently pick one. A provider must not recursively call this resolver.
No provider is installed by drinking-water itself; without a provider native food defaults stay intact.

The caller debits an actual completed edible portion first, allocates and persists a **positive,
strictly increasing per-player** completion sequence, then calls `completeFood` on the server thread.
Retries reuse the same sequence. All callers share the sequence space; a retry older than the latest
receipt is rejected too. Serialize completions in sequence order. Cooking owns this sequence allocator,
not the hydration state. Cancelled/preview/partial actions must not call completion. A nested lunchbag
or portable vessel dispatches its actual inner serving once, never again for the outer item. Placed
food dispatches one actual paid bite with its bite-specific profile; no synthetic whole-block credit.
The overload accepting a profile supports those explicit bite/yield contexts.

The persisted `PlayerPersisted[better_drinking_water:completed_food]` ledger contains sequence and
sub-unit hydration/quenching remainders. Reconnect and Forge player clone preserve it. Each receipt
is written before meter credit. Food creates no `better_drinking_water:reserve`, item, transferable
water, effect entitlement, purity reward, or later whole-point payout. Meter overflow is discarded;
rounding residue is cleared when the corresponding meter reaches20. Subunit quenching residue
survives a temporarily lower `quenched <= thirst` bound, so two .5/.625 servings at0/0 still combine
to1/1 (only whole-point overflow is discarded). Quenching obeys the
native bound `quenched <= thirst`. Corrupt fractional state fails closed rather than replaying credit.

The `ServingHydrationMixin` intercepts Thirst's shared `PlayerThirst.drink(ItemStack, Player)` entry,
preventing config/native credits for provider-owned food. It does not award credit itself. Partial or
cancelled uses therefore remain zero, while unmanaged foods keep their original native path.

## Food policy input (owned by cooking/pack, not discovered here)

Per actual serving: juicy melon/citrus or liquid soup 4/5; fruit/salad/thick stew 2/2.5;
moist vegetables/cooked grain 1/1.25; meat .5/.625; soft bread/pastry .25/.3125;
explicitly dry 0/0. Composites use ingredient moisture plus added water, retention and actual yield.
These are explicit provider policy values, not guesses from names/tags. Do not add ingredient-derived
and direct credit, and never scale water by preparation quality.

## Finite beverage rebalance

All 179 authored entries in `src/main/resources/drinks.json` declare `hydration_class` and
`serving_mb`. `DrinkHydration` is the validated per-250mB basis:

| Class | Hydration | Quenching |
|---|---:|---:|
| WATER (water / unsweetened tea / coffee / unsweetened herbal infusion) | 4 | 5 |
| JUICE_MILK (juice, milk, fruit preparations and other nonalcoholic authored mixtures) | 3 | 4 |
| FERMENTED_ALCOHOL (beer, rice wine, mead, kombucha, ales/stout) | 3 | 3 |
| SPIRIT (vodka, red rum, saccharine rum) | 1 | 0 |
| MIXED_ALCOHOL (cocktails, nog/grog, mixed brews) | 2 | 2 |

Finite milk buckets are 1000mB = 12/16. Water buckets are 1000mB = 16/20. Bottle, bowl, tankard,
plant-fluid fill/empty and native portable-water portions are 250mB. Fruit teas/milk tea remain
juice/milk mixtures rather than unsweetened tea. Plant nonalcoholic fermenting recipes are not
reclassified as ethanol merely from their recipe machine: existing explicit alcohol doses remain.

`WaterSources.nativeHydration(ItemStack)` resolves authored quantities for manual consumption;
`NativeDrinkHydrationMixin` normalizes native Thirst helper reads instead of awarding an additional
credit. Drinkable potions retain native effects and water-base 4/5; splash/lingering are not drinks.
Automatic water-source admission remains narrower: only actual water potion contents are admitted.
Source copying, debit-before-credit, reserve purchase validation, water purity veto/safety episodes,
partial-sip reserve persistence, beverage hunger/nutrients, sugar/alcohol consequences, and timed
effect cadence are unchanged. Existing paid reserves keep their historical purchased entitlement;
new purchases use the new quantities. Rain collector direct drinking matches its bottle charge4/5.

Existing drink `BeverageEvents.Profile/Sip` remains authoritative for **fractional** drink nutrition,
sugar/alcohol and consequences. Cooking must not register those already-managed paid drink sips as
completed food or award full-serving nutrients on a partial sip. There is no quality-water multiplier.

The resource generator `scripts/generate_plant_drinks.py` authors the explicit category matrix and
preserves native actions/effects/nutrition and fill/empty recipe volumes. `--check` verifies reproducibility
(Pillow required). Pack keyword discovery stays disabled; stale fallback config values cannot add or
replace managed catalogue/food credits. Unmanaged item configuration is not globally disabled.

## Verification boundaries

`FoodHydrationLedgerTest` deterministically covers all six fractional food defaults, reconnect-state
roundtrip/retry, old receipt rejection, full meter loss, zero-thirst fraction accumulation,
native quenching bound, malformed state and
250/1000mB five-class conservation through the existing partial-sip calculator. Resource tests verify
every explicit basis/volume and representative tea/milk/beer/spirit/cocktail values. Existing paid
reserve/effect tests remain; native water test expectations are updated from6/8 to4/5.

`FoodHydrationGameTests` adds native capability/NBT/provider suppression, unmanaged default and
full-meter fixtures. Registration uses an isolated marker stack, not real cooking provider types.
These tests require an explicit separately authorized native run. A simulated NBT rehydration fixture
is not evidence of an independently restarted connected client. Actual cooking nested/placed item
completion and full process reconnect require the coordinator's source-qualified native fixtures.
