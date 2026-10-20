# Better Drinking Water

## Scope and authority

This repository owns rain collection, campfire snow melting, water quality and native Curios
fractional water/beverage consumption for Forge 1.20.1. Read [local instructions](AGENTS.md) and the
[shared documentation/policy index](../../better-content-modpack/docs/README.md).

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

Melon juice healing is proportional. Honey poison removal, milk cleansing, and cocoa/milk-bottle
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
