# Better Drinking Water

## Scope and authority

This repository owns rain collection, campfire snow melting, water quality and native Curios
water storage for Forge 1.20.1. Read [local instructions](AGENTS.md) and the
[shared documentation/policy index](../../better-content-modpack/docs/README.md).

## Fractional drinking

The **Drinking Water** Curios slot accepts plain water bottles, Thirst terracotta water bowls,
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
configured default (pack: dirty), including its risks; it no longer bypasses effects.

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
five exact authored Farmer's Respite data repairs from the pack. Prior native worlds/logs/mods
are retained, and ERROR/FATAL diagnostics fail the gate.

These source checks do not establish deployed-pack or paired native-inventory visual acceptance;
those remain separately authorized under the shared testing and inventory policies.
