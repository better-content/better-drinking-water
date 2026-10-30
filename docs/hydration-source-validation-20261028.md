# Hydration source validation — 2026-10-28

Scope: source-only typed cooking integration and hydration rebalance. No production JAR deployment,
activation manifest, Packwiz refresh, distribution, commit or push. No native Minecraft/client launch.

## Retained attempts

1. `/home/dev/.tmp/cooking-hydration-gradle-20261028T0920.log`: FAIL at Gradle configuration before
   compilation. Missing explicit immutable compatibility-provider bindings. Retained, not overwritten.
2. `/home/dev/.tmp/cooking-hydration-gradle-20261028T0921.log`: PASS, 26 seconds, Java17.0.17,
   `compileJava test stageRuntimeJar`, fail-fast shared flock, 2 workers, Xmx2G and 2CPU affinity.
   43 JUnit tests / 10 suites, zero failures/errors. Intermediate source-local JAR hash
   `965ea6764be442997054b402e22744a78c431e9a2380e8441813ac16a28bac4c`.
   **Intermediate only**: later low-thirst regression review found a fractional quenching edge.
   Its original bytes are retained at
   `/home/dev/.tmp/cooking-hydration-artifacts-20261028/intermediate-965ea676-better-drinking-water-1.1.0.jar`.
3. Direct real-ledger javac + JUnit Assertions runner after correction:
   `/home/dev/.tmp/hydration-edge-20261028/results-low-one-full.log`: PASS8 deterministic tests,
   including zero/one thirst, persisted fractional-state restoration, same receipt retry and full20.
   This is not Gradle/native acceptance.
4. `/home/dev/.tmp/cooking-hydration-gradle-final-20261028T0931.log`: PASS, 20 seconds,
   corrected source `compileJava test stageRuntimeJar`, Java17, Xmx2G/2workers/2CPU under the shared
   fail-fast flock. **45 JUnit tests / 10 suites, zero failures/errors**. Final source-local JAR:
   `/home/dev/mod_source/better-drinking-water/build/libs/better-drinking-water-1.1.0.jar`, SHA256
   `d4de27ca3049b3a4b442eb3695bd8e9c542a4b60e9acead38c057697b72ee7d6`.
   Both bound provider inputs still match their before hashes. Build slot released; no owned jobs.

The edge: quenching residue was incorrectly cleared whenever `quenched == current thirst`, including
0/0, so two .5/.625 meat servings produced1/0. Corrected to clear subunit quenching only at actual20;
whole-point overflow is still discarded, and native `quenched <= thirst` remains enforced. Added
zero-thirst and one-thirst/reconnect/full20 regressions, and strengthened the native NBT fixture to0/0.

## Bound explicit build inputs

Current bundled provider artifacts were inputs only, not rebuilt or declared newly qualified:

- `/home/dev/better-content-modpack/mods/better-compat-fixes-0.1.9.jar`:
  `eea20e71dc25faa195e0ea0285cbe5e9eaa8f3819cd7a3c02707d0bc01fe68d8`
- `/home/dev/better-content-modpack/mods/better-gameplay-notices-1.0.0.jar`:
  `a684b3211867e98b1fcd50f34274ad2517fa08bf718476068998837038c102c2`

Their before/after SHA checks passed in attempts2 and4; hashes are retained in
`/home/dev/.tmp/cooking-hydration-providers-20261028T0921.sha256`. Gradle snapshots them content-addressed.

## Other checks and pending gates

- Generator: `PYTHONPATH=/home/dev/.tmp/hydration-tools python3 scripts/generate_plant_drinks.py --check`
  PASS125 fluid profiles / 73 coverage entries / 903 reproducible outputs; source catalogue179 drinks.
  Pillow13.0.0 was installed as a temporary Python library under `/home/dev/.tmp/hydration-tools`;
  no system tools, SDKs, or shell startup configuration changed.
- Source/owned pack config `git diff --check`: PASS.
- Three new registered native FoodHydrationGameTests including strengthened0/0 NBT fixture:
  compiled in final attempt4, **not run**. Expected full native fixture count adjusted45->48.
- Actual independently restarted client/server persistence and cooking integration completion for
  nested vessels/portable vessels/placed bites remain the coordinator's separate native gates.
- No claim of native hydration qualification, acceptance of an unchanged exact fixture, or pack acceptance.
