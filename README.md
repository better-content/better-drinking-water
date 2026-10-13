# Better Drinking Water

## Scope and authority

This repository owns its mod-specific behavior and authoring inputs. Read [local instructions](AGENTS.md)
and the [shared documentation/policy index](../../better-content-modpack/docs/README.md).


Forge 1.20.1 mod owning rain collection, campfire snow melting, and dedicated water/empty-bottle Curios slots.

Plain drinkable water potions stack to 64. Curios drinking prepays a bottle on its first sip: one water bottle is consumed and one glass bottle immediately returns to the empty-bottle Curio slot (up to 64). Overflow goes to an equipped backpack, then the hotbar, then drops at the player. The remaining water, hydration values, cumulative sip progress and purity are stored only in versioned persistent player data. This reserve keeps hydrating even with an empty water slot and survives reconnects, restarts, dimension changes and death/respawn. Bottle swaps, splits and transfers cannot reset or transfer paid credit. Full thirst never charges a bottle. Purity effects run once when charging; later sips retain that bottle's purity and cumulative quenched rounding.

Historical player-wide fractions are not imported because they do not prove payment. An existing `BetterContentSippedFraction` bottle tag is honored only when its first bottle is consumed upfront; the remaining stack loses the obsolete tag. New sips never write progress to items. Malformed or unsupported player reserves grant no water. Hand drinking and other water sources are unchanged.

The optional Better Content Threads bridge begins `water_made_safe` on the
first observed thirst decrease. It persists that exact episode across reloads
and completes only after the player actually drinks purity-3 water, whether
from a consumed bottle, the water-bottle Curio, or a safe Rain Collector.
Servers without Threads continue to run without a hard dependency.
