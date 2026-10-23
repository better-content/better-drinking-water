# Better Drinking Water

## Scope and authority

This repository owns rain collection, campfire snow melting, water quality and native Curios
fractional water/beverage consumption for Forge 1.20.1. Read [local instructions](AGENTS.md) and the
[shared documentation/policy index](../../better-content-modpack/docs/README.md).

## SOURCE verification fixture

`./gradlew verifyFull stageRuntimeJar` requires the actual qualified production Compat Fixes
and Gameplay Notices providers. Bind `-PcompatFixesRuntimeJar=/immutable/provider.jar` and
`-PgameplayNoticesRuntimeJar=/immutable/notices.jar`, or use canonical
`BC_CUSTOM_MOD_JAR_DIR`. No mutable sibling `build/libs` fallback is used for these inputs.
Content-addressed flatDir snapshots outside `build/` retain exact input bytes across clean;
ForgeGradle remaps the real providers. Heat and HUD are not mandatory for this dedicated fixture.
Compat already owns Thirst's native codec registration and nested-loot recursion mixin; Water
adds no replacement codec, registration, dispatcher, mixin or loot data.

The required native count is now **44** (the existing43 plus one nonshipping compatibility
contract): exact native codec identity, all15 original GLM JSON decodes and native loot sources,
public Mixin applied-target metadata, unchanged queried-table context through actual raw nested
loot generation, and real native chest generation. A fresh UUID `run-gametest/attempts/` RunConfig
never recycles an existing world or adopts ordinary developer instances. Both latest and DEBUG
logs are required; real loot-modifier WARN decode failures are rejected alongside ERROR/FATAL
and mixin failures. Pure regression includes the exact historical native WARN, not an exemption.
These source changes still need a newly admitted full gate with the qualified current provider
closure; prior43-test receipts with failed native loot operations are not accepted inputs.

## Beverage catalogue and fractional effects

The native **Drinks** slot uses the same admission predicate as the concise **Drinks slot
compatible** item tooltip. Its internal `water` slot and predicate identities remain stable.
The authored [`drinks.json`](src/main/resources/drinks.json) lists finite supported beverages,
whole-serving hydration/quenching, exact remainders, timed effects and special actions; its
exclusions classify discovered consumables that are not ordinary beverages or need bespoke
integration. Native drink animation alone does not admit soups, spreads, ingredients or magic
flasks. Item/effect registrations and implementations were inspected from the pack's pinned
upstream JARs. Loaded supported profiles must resolve their actual item, remainder and effects.

A beverage is paid upfront like water. Its immutable, bounded player-owned snapshot releases
hydration, quenching, hunger, saturation and timed effects in proportion to the fraction consumed.
Timed effects retain their pack-normalized amplifier; cumulative integer accounting preserves
unawarded ticks. Equal-strength durations add up to the profile's full-serving duration cap;
longer or stronger pre-existing effects are never downgraded or extended with weaker credit.
Suppressed or capped credit is spent, not banked for a later surprise award. Duration-only
extensions preserve the active native instance and do not replay its attribute/on-apply actions.
Regeneration, Poison, Wither, Comfort and Sweet Heart use an independent active-tick cadence so
replenishing their remaining duration cannot reset or accelerate healing/damage. Absorption also
releases proportional shield points, rather than granting a whole shield on every short slice.

The optional typed `BeverageEvents` integration lets Better Survival Physiology snapshot Diet's
ordinary nutrient gains and its Nectar/Booze loads once and award each actual consumed fraction.
It does not synthesize whole-item use events or replay `finishUsingItem`. Blackout can veto both
purchase and release of paid beverage credit. Meal recovery and Diet's eaten-item history occur at
serving completion, not every sip. Fruits Delight effect normalization is captured before splitting:
suppressed Health Boost/Cycling/Digesting stay suppressed, and newly supplied vanilla benefits stay
level I with at most 60 seconds per full serving. Custom utility effects requiring instantaneous
on-add actions remain explicitly excluded instead of being flattened into generic buffs.

Melon-specialty healing is proportional. Honey poison removal, milk cleansing, and cocoa/milk-bottle
random cleansing occur once at completion. Cleansing honors native curability, Forge vetoes and
Brewin' and Chewin' low-priority tags; strong cocoa retains its native up-to-two draws. Gambler's
Tea resolves one correlated outcome at purchase, then stores it; reconnects and later sips cannot
reroll it. Brewin' and Chewin' Tipsy/Bad Omen potency resolves once per paid serving against existing
native effects (native caps 9/4); later sips cannot repeatedly escalate potency. Their durations
use the same bounded fractional accounting, while Booze remains a separate proportional load.

Changing equipment cannot change an existing paid serving. Reconnects/restarts preserve its
remaining entitlements, with no active buff until a sip releases duration. Death discards the
entire unfinished beverage serving without refund; it does not retain hydration while deleting
its costs. Existing plain-water reserves keep their established death persistence. Full thirst
never buys or releases any serving, even for food or expired buffs. No inventory beverage search
or automatic substitution is introduced.

## Plant drink production

[`plant_drinks.json`](src/main/resources/plant_drinks.json) is the finite plant/portion/plain/brew
matrix; [`generate_plant_drinks.py`](scripts/generate_plant_drinks.py) owns its reproducible source.
The manifest includes exact native item mappings, required mods, colours, role, nutrition and
recipe process. Duplicate names, invalid quantities/nutrients, unknown roles and unresolved base
references fail closed. Registration is deterministic and conditional on actual provider mods;
common setup checks every active item, ingredient, fluid and drink profile.

Every available fruit has a separate plain juice and specialty. Whole fruit uses one edible
portion; small berries and slices use two. Onion, garlic, potato and kelp are explicitly juiceable.
Coffee cherries and rose hips have juice; tea leaves, coffee beans, cocoa and herbs keep their
appropriate existing preparations. Native Long/Strong tea/cider families, milkshakes, cocktails,
processed-starch vodka, kombucha, Gambler's Tea, Purulent Tea and cursed sap remain distinct.
Torchberries and enchanted Aether berries have explicit additional preparations. A broader pinned
inventory audit also admits actual Nature's Spirit coconut halves, olives and desert turnip,
Aether white apples, Goety Firethorn berries and Fallout corn. Coconut has separate plain juice,
fat-bearing milk and a water-breathing specialty; olives carry fats, not invented Nectar. White
apple specialty retains the native 300-tick Aether Remedy (against Aether Inebriation, not a new
Poison cure or Physiology Booze cure). The source witnesses and exclusions are recorded in
[plant-stock.md](docs/plant-stock.md). Farmer's Respite
Wither Roots are recorded as **unavailable inventory stock**, not silently omitted: pinned
`FRItems` registers no roots item and `WitherRootsBlock` is an environmental hazard with no item
harvest. It is not substituted for Purulent Tea's actual nether wart/fermented-spider-eye input.

Ordinary hand juicing takes only the portion and a glass bottle. Create basin compacting uses
the identical portion for 250 mB: automation does not multiply yield. Filling, emptying, keg pouring
and kettle pouring conserve 250 mB and the actual vessel (native alcoholic drinks use tankards).
Grain/seed/coconut/ube milks and fresh herb/flower infusions use 250 mB water, the specified portion and
60 seconds of heated native Respite kettle brewing. Primary fruit brewing conserves 1,000 mB
juice, uses one safe starter flower and takes eight minutes in B&C temperature band 3. Secondary
Summerheart/Blackwake preparations conserve 1,000 mB and add four minutes with nether wart/crimson
fungus respectively. Wither rose and chorus flower are explicit specialty agents, never members
of the safe starter tag. Native recipes for mixtures and developed teas remain separate.

The pack's plant-only recipe script removes old effect-bearing juice acquisition and conflicting
primary routes while preserving these new namespaced recipes. This is a fresh-world catalogue
break, not a save deletion, remapper or retroactive rewrite of existing items/paid reserves.
Plain juices never inherit healing, cleansing, teleportation or specialist effects. Belladonna
juice is explicitly **unsafe** and carries Poison. Hydration+ describes nutrition/calories/sugar;
Buff describes a functional specialist effect, not the vessel or workstation. Most new servings
have 6 hydration/8 quenching, one food and 0.025 of their ordinary nutrient; fruit Nectar is 0.0125,
subject to Physiology's existing half-smallest-gain cap. Infusions have no invented food/nutrients.
Native specialist profiles retain their existing quantities/effects rather than being flattened.
These bounded doses are source-informed tuning, not a claim of measured gameplay balance.

New native plant items route manual completion through the same entitlement release as automatic
sipping, never `super.finishUsingItem`; full thirst and blackout deny purchase. Native melon
healing becomes Summerheart Cordial (2 HP), with Reserve (4 HP). Orange's Suncrest Tonic retains
Regeneration and completion-only native Recovering (Poison/Wither cleansing). Lemon's Bittergold
Tonic preserves the **actual pinned Haste + Refreshing/Mining Fatigue cleanse**, not an inferred
Resistance effect. Completion actions run after paid reserve progress has been committed.
Cranberry/allium/garlic remove Poison, ginger removes Nausea, and onion/yellow dock cleanse one
harmful milk-curable effect. Voidstep attempts up to sixteen native safe, Forge-vetoable random
blink destinations only on completion. Spellbloom restores twenty Ars mana per full serving,
proportionally capped to missing native mana and synced through Ars's typed API. None adds
implicit alcohol; native alcoholic specialist outputs retain their own existing profile.

New hover text labels 250 mB, actual role/effects, unsafe Poison and completion-gated actions;
compatibility still derives from real admission. Retints preserve two native Farmer's Delight
juice/cider glass silhouettes, glass, handles, garnish and pixel shading with a geometric liquid
mask. The original MIT donor bytes, exact hashes, attribution and regeneration instructions are
in [assets-source](assets-source/README.md); attribution/license also ships in the JAR.

The source gate includes plant native registration/recipe resolution, manual vessel/hydration/
food conservation, every active custom item's catalogue-driven thirst/quenching against the real
native Finish listener (with no duplicate Thirst config awards), no plain-melon healing, specialty
2/4 HP healing and completion-safe native
reserve cleansing. Resource tests check full coverage, safe agents, exceptional agents, volumes,
vessels, process times, bounded nutrition and detailed licensed art. Optional-provider mana,
orange/lemon actions, full-pack effective recipe removal and paired native inventory/fluid-model
captures still require exact-candidate runtime acceptance; source passes do not establish them.

## Fractional water drinking

The **Drinks** Curios slot also accepts plain water bottles, Thirst terracotta water bowls,
vanilla water buckets and Aether Skyroot water buckets. An explicit native-fluid-container
allowlist also covers Tinkers' copper cans and seared/scorched tanks, gauges and lanterns,
Create Diesel Generators' canister, and PneumaticCraft's four tank sizes. These entries require
the item's real Forge fluid-handler capability; only plain water or empty contents are admitted.
Empty refillable fluid vessels can remain equipped. Non-water fluids and creature buckets are
not admitted. Bottle hip rendering remains bottle-specific; other vessels retain their native
inventory models without a new worn renderer.

Automatic drinking prepays a portion when thirst is missing. The actual filled item or native
fluid contents are debited **before** hydration credit is created. Bottles and bowls return their
own empty vessel immediately. The **Empty Vessels** Curio accepts glass bottles, terracotta bowls,
vanilla/Skyroot buckets and empty copper cans. It remains one native slot, not mixed-item storage.
Remainders that do not fit go to the equipped backpack, then hotbar, then drop at the player.
Refillable single fluid vessels stay in the water slot after draining; a stacked fluid vessel is
split and its updated single vessel is returned through the same remainder route.

Plain drinkable water potions stack to 64 and retain Thirst's configured hydration/quenching
(pack: 6/8); bowls retain their native values (pack: 4/5). A water bucket prepays 16/20.
Native fluid sources debit at most 250 mB per purchase, worth 4 hydration/5 quenching. Smaller
extractions round both values down: a 90 mB copper can supplies 1/1, and less than 63 mB cannot
supply a sip. No sub-point volume accounting or upward rounding is introduced. This conservative
rule can waste small amounts, but cannot turn tiny fluid purchases into a whole bottle's credit.

The remaining hydration, cumulative sip progress and purity are stored only in versioned persistent
player data. The bounded reserve keeps hydrating even with an empty water slot and survives
reconnects, restarts, dimension changes and death/respawn. Container swaps, splits and transfers
cannot reset or transfer paid credit. Full thirst never charges a portion, even if quenching is
low. Purity effects run once when charging; later sips retain the paid portion's purity and
cumulative quenching rounding. A new source cannot upgrade an already-paid dirty reserve.

Historical player-wide fractions are not imported because they do not prove payment. An existing
`BetterContentSippedFraction` bottle tag is honored only when its first bottle is consumed upfront;
the remaining stack loses the obsolete tag. New sips never write progress to items. Malformed or
unsupported player reserves grant no water.

## Drinking from blocks

**Sneak-right-click with both hands empty** to drink from a vanilla/copper water cauldron or an
approved ordinary tank: Create fluid tank, Diesel Generators canister/oil barrel, PneumaticCraft
fluid tanks, AE2 Sky Stone Tank, or the seared/scorched Tinkers' tanks, gauges and lanterns.
The server respects interaction permission, prior event cancellation/denial, and the clicked
side's actual extraction capability. It does not fall back to unrestricted internal storage.
The gesture claims the interaction instead of opening a second GUI action, with a ten-tick
cooldown. Hidden machine buffers and arbitrary fluid-capable blocks are not drinking sources.

A cauldron purchase consumes one level for 4/5 hydration/quenching. Native tanks supply a measured
portion using the same conservative volume rule as items. Water quality comes from the stored
fluid or cauldron's native purity property, never the tank's surrounding biome. A cauldron
without tracked purity is dirty. The ordinary Rain Collector still supports empty-hand drinking
and bottle filling; direct drinking now also uses prepaid fractional credit (one charge: 3/2).
Existing paid credit is spent before any new source is charged.

## Quality and purification

Water has exactly two gameplay quality levels: **dirty** (native value 0) and **purified**
(native value 3). Legacy intermediate tiers count as dirty, including stored Curio reserves.
Native item/fluid reads, quality writes, drinking effects, tooltips and block-quality reads use
those endpoints. The native -1 no-block-quality sentinel is preserved. Untagged water uses the
configured default (pack: dirty), including its penalties; it no longer bypasses effects.

Dirty water always applies **Hunger VIII for 60 seconds** and, when Industrial Heat's
effect is registered, **Malnourishment X for 25 seconds**. The pack includes that provider;
without it, standalone Drinking Water applies only Hunger. These replace native poison/nausea
chance rolls and apply independently of the old Thirst purity-effect settings. Dirty water
still hydrates even when the old debuff-quenching switch is false. Purified water applies no
purity penalty. Automatic reserves apply penalties once per purchased portion, not per sip.
Normal effect refresh/upgrade rules apply; doses do not create independently stacking effects.

Hunger remains vanilla: this dose adds 48 exhaustion, equivalent to 12 saturation/food points
(saturation first). Industrial Heat's Malnourishment X drains ten percentage points per second
from every Diet nutrient group, wiping full groups in ten seconds and continuing to drain any
replenishment during the remaining fifteen seconds. These are effect-only costs, excluding
ordinary activity or other drains.

Campfire and furnace water recipes purify bottles, bowls and buckets in one step, including
untagged and legacy water. The pack config gives kettles and sand filters three purification
levels. Other supported fluid vessels use **transfer-based purification**: extract water for
treatment and return the treated water through native fluid handling. Equipping, warming,
placing or refilling a vessel does not itself purify water. Finite purified supplies remain
portable; no portable purified-water generator or new in-place vessel-boiling recipe is added.

Forge bucket-to-fluid and fluid-to-bucket conversions preserve purity. Refilled buckets take the
incoming water's quality, not their former contents'. Native NBT-sensitive tanks keep dirty and
purified fluid stacks separate; no replacement mixing/storage engine is introduced. Compatibility
does not guarantee every third-party pump or conversion preserves NBT. Open-world water placement
has no new per-position purity tracking and must not be used as a clean-water storage route.

## Deliberately deferred

Cold Sweat waterskins, Moonlight jars/goblets, AE2 fluid cells, backpack tank extraction,
creature-bucket sipping, recursive inventory searches, waterlogged construction, hidden process
tanks and per-position flowing-water purity are outside this bounded integration. Native fluid
handlers must expose extractable water; listing a vessel does not invent a missing capability.
Cold Sweat waterskins retain the pack's temperature-only behavior.

## Bounded playtest observations

The loader's SLF4J provider owns `bc.playtest.better_drinking_water.*`. Normal standalone
logging emits one startup status (actual mod version, source UNKNOWN when unavailable).
An explicitly enabled DEBUG profile records sip rejection/release, returned paid-portion
admission and a separate observed native-thirst before/after result. Admission is not called
durable publication. `purchase_attempts` counts callback/limit attempts, not successful debits.
The process UUID plus transient `operation` sequence correlates one sip without player names,
inventory, beverage payloads, persistent IDs or save/wire changes.

All categories share 120 detail records/minute, burst 20; bounded 256-key accounting coalesces
at most every 30 monotonic seconds and on server stop. Records are single-line and at most
2048 characters. Server stop clears bounded keys without rearming the bucket or TRACE.
TRACE fraction detail additionally requires both JVM properties
`bc.playtest.trace.better_drinking_water.dimension=<dimension-id>` and exactly one focused
`.subject=<UUID>` or `.pos=x,y,z`. This player-owned path matches only subject focus; it reads
UUID only behind the TRACE guard. Only focused TRACE includes that causal subject identifier.
The first matching detail arms the once-per-process 60-second/1000-record limit; ordinary rate
limits still apply. No extra RNG, inventory search, NBT traversal, game clocks or mutation are
introduced for observation. Native failures retain their existing severity independently.

## Discovery and verification

The optional Threads bridge begins `water_made_safe` on the first observed thirst decrease.
It persists that episode across reloads and completes only after actual purity-3 hydration
through the automatic reserve, direct block drinking, or a consumed water bottle. Servers without
Threads do not acquire a hard dependency.

The local full gate exercises native bowl/bucket admission and remainders, real Tinkers' can/tank
storage, real Create tank extraction, bucket purity round trips, cauldron debits, conservative
volume limits, reserve persistence and old bottle regressions. Its nonshipping fixture mirrors
five exact authored Farmer's Respite data repairs from the pack and registers an explicit
nonshipping Malnourishment stand-in when Industrial Heat is absent. It proves the cross-mod
registry lookup, effect strengths/durations and once-per-charge behavior, not the provider's
nutrient drain implementation (covered in Industrial Heat's source checks). Prior native worlds/logs/mods
are retained, and ERROR/FATAL diagnostics fail the gate.

Beverage checks exercise real registered Farmer's Delight, Farmer's Respite and Brewin' and
Chewin' profiles, native Curios admission and concise tooltips, 22-quenching portions, upfront
debits/remainders, effect duration/strength merging, real periodic ticks, fractional healing and
absorption, completion-only cleansing, correlated random outcomes, blackout vetoes, malformed
snapshots and clone/death semantics. The owning Physiology gate separately exercises the real
provider with native Diet/metabolic capabilities. The remaining catalogue registrations/behaviors
were statically inspected from pinned upstream implementations; this is not full-pack live-registry
or per-drink client acceptance.

These source checks do not establish deployed-pack or paired native-inventory visual acceptance;
those remain separately authorized under the shared testing and inventory policies.
