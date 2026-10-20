# Plant inventory source witnesses

The authored input is `scripts/generate_plant_drinks.py`; generated `plant_drinks.json`
records exact native ingredient IDs, portions, required providers, outputs and retained families.
This is a finite, reviewed drink catalogue, **not** automatic admission of all edible items.
The design's listed fruit/vegetable/herb rows and the additional stock below have explicit
coverage tests. Resource checks do not prove full-pack effective recipes or rendering.

## Additional pinned inventory audit

These witnesses were inspected in the actual Forge 1.20.1 pack inputs, using archive resources
and `javap -c -p` on the named classes, not names inferred from translations alone:

| Pinned input | SHA-256 | Source witness and decision |
| --- | --- | --- |
| `natures_spirit-2.2.5-1.20.1.jar` | `07818dd985b100ae71b98c755961c135b07c3a08faf5d95afc87fab71a3b9e86` | `net.hibiscus.naturespirit.registration.NSBlocks` registers `coconut_half`, `olives`, `desert_turnip`. Their constructors attach `COCONUT_COMPONENT`, `OLIVE_COMPONENT`, `DESERT_TURNIP_FOOD_COMPONENT`. Actual edible stock: two halves/olives or one turnip per 250 mB. Coconut has separate plain juice, milk and specialty; olive preparations carry fats without invented sugar. No new coconut plant/harvest is invented. |
| `aether-1.20.1-1.5.2-neoforge.jar` | `5cb64ab1f27e78e8338fe67f9c766578904aad9bc1f8bcc90f306ea52aea3fc5` | `com.aetherteam.aether.item.food.WhiteAppleItem.finishUsingItem` applies native `AetherEffects.REMEDY` for 300 ticks. `RemedyEffect` removes Aether Inebriation; it is not a generic Poison cleanse and does not erase the separate Physiology Booze load. Plain white-apple juice is effect-free; Cloudwhite Remedy preserves this timed native benefit. |
| `goety-2.5.53.1.jar` | `2d1a9c2621980af2425ebbb1fde62b6a12ce90f4733120d953a2af2b5cbf28a9` | `com.Polarice3.Goety.common.blocks.ModBlocks` registers `firethorn_berries` in `ModItems.ITEMS`, backed by a food-bearing `ItemNameBlockItem` for `FIRETHORN`. `FirethornBushBlock` returns this actual stock. Two berries produce plain juice; the separate Firethorn Guard's Fire Resistance is authored, not an assertion that raw berries grant it. |
| `Fallout Wasteland 1.20.1 Beta 3.8.1.jar` | `ced3c8a2fb902f60a98c3c9914e5f9d4ad070499644ef90a39f25420d594ace5` | `net.mcreator.falloutwastelands.item.CornItem` attaches FoodProperties (nutrition 3, saturation .5). The actual registry namespace is `fallout_wastelands_`. Corn gets water-based grain milk and a separately brewed Sungrain Kvass, not invented oat/soy stock. |
| `farmersrespite-1.20.1-2.1.2.jar` | `21c15ef71f66684cbd94080e04649741bdf2b2ea6a6d542783e8961365d090bb` | `vectorwing.farmersrespite.common.registry.FRItems` has no Wither Roots item. Environmental `WitherRootsBlock` has no native inventory harvest/loot; breaking releases a harmful cloud. Coverage records **inspected unavailable inventory stock** with null outputs. No phantom ingredient, substitute nether wart or new harvest mechanic. Purulent Tea retains its actual wart/fermented-spider-eye recipe. |

The broader archive inventory search also inspected registered item names and stock classes
for additional fruit/grain/nut terms. Finished/honeyed/caramel/jeweled/darkened apples,
glistering melon, dried/baked fruit, desserts and dynamic-tree planting seeds are processed
variants/propagation stock, not new plant species to turn into a second plain-fruit family.
Native processed drinks/mixtures remain independently covered; these exclusions do not delete
items, saves or upstream effects. No almond/soy/oat/peanut source item is invented to populate
plant-milk categories. Existing pumpkin seeds, wheat, rice, corn, coconut and ube are the
finite implemented milk stock. This audit is a source inventory witness, not a claim that every
future optional mod or newly added edible has been examined.

## Existing specialist source semantics

Pinned Fruits Delight lemon grants Haste plus native Refreshing (Mining Fatigue removal),
not the earlier proposed Resistance. Orange grants Regeneration plus Recovering (Poison/Wither
removal). Their renamed brews retain these actual semantics; the cleansing applications are
completion-only. Plain juices have none of them. Farmer's Delight melon healing remains in
Summerheart Cordial, and the secondary Reserve has its separate four-HP entitlement.
The existing drink catalogue remains the source for retained specialty quantities, correlated
outcomes, nutrition/potency and native vessels; native Long/Strong tea/cider, vodka, milk tea,
cocktails and developed mixtures are not replaced by generic fruit juice.

## Acceptance boundary

The owning native gate resolves active registrations/recipes and, for every active custom
plant item, compares manual completion plus the real Thirst Finish event with one direct native
hydration/quenching award from that item's fractional profile. Custom plant items must not be
added to Thirst's item config: their explicit profile-driven manual path would then double-award.
The Physiology fixture independently tests actual Start/Finish events, native Diet and explicit
nutrition/Nectar once. Optional-provider interactions, full-pack recipe removal, paired native
fluid/inventory models and color captures require same-candidate pack runtime acceptance by
the release owner. No such acceptance is inferred from the source inventory or resource gates.
