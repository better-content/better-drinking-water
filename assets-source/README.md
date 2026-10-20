# Plant drink donor and reproduction

`apple_cider.png` and `melon_juice.png` are unmodified 16×16 Farmer's Delight glass-drink
assets by vectorwing, from `FarmersDelight-1.20.1-1.3.1.jar`, paths
`assets/farmersdelight/textures/item/{apple_cider,melon_juice}.png`.

- Apple cider SHA-256: `b5de2d077829026f5df10ee11341c7d4e0088658eb3daca297bb8b81c3acc6b7`
- Melon juice SHA-256: `ca1329709233c6c671893f4d638e045556f05e10d3e053477a67b951dbf9bdcb`
- Pinned JAR SHA-256: `7d13dbeb8ed1e7cd22daf35ddf3df4c411392a65a603b11145f84d6bf2443e9d`
- Upstream: <https://github.com/vectorwing/FarmersDelight/tree/1.20>
- License: MIT, copyright (c) 2020 vectorwing; see `LICENSE-FarmersDelight.txt`.
  The pinned JAR declares MIT in `META-INF/mods.toml`; the upstream branch license was inspected.

The tint generator preserves each donor's alpha, glass shape, outlines, handles, garnish and straw.
It replaces hue/saturation only inside a geometric warm-chromatic liquid mask; palette values are in the
script's authored plant matrix. This is ordinary texture colour, not emissive rendering.

Reproduce with Python 3 and Pillow 12.3.0 (already available in `/home/dev/.venv`):

```sh
/home/dev/.venv/bin/python scripts/generate_plant_drinks.py
/home/dev/.venv/bin/python scripts/generate_plant_drinks.py --check
```

The generator also authors registration data, native workstation recipes and catalogue additions.
Its inputs are authored source and these pinned donors, not retained audit output. The license and
attribution are packaged in `META-INF/plant-drinks-donor.txt` with every derivative texture.
