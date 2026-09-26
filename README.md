# Water Survival

Forge 1.20.1 mod owning rain collection, campfire snow melting, and dedicated water/empty-bottle Curios slots.

Plain drinkable water potions stack to 64, so the water Curios slot can hold a real bottle stack. Partial Curios sips are stored on the equipped stack. Removing or transferring it carries the first bottle's consumed fraction and purity; a different stack begins at zero and gets its own purity check. Consumed bottles return glass bottles to the separate empty-bottle slot. Legacy player-wide fraction data is ignored because it cannot be safely attributed to a physical bottle.

The optional Better Content Threads bridge begins `water_made_safe` on the
first observed thirst decrease. It persists that exact episode across reloads
and completes only after the player actually drinks purity-3 water, whether
from a consumed bottle, the water-bottle Curio, or a safe Rain Collector.
Servers without Threads continue to run without a hard dependency.
