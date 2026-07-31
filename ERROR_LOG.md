# Error Log

Session-discovered errors, root causes, and prevention rules for TFMG (Arcadia fork).

---

## [2026-07-01 12:00] — Stale datagen output silently overriding 1.2.3 fixes

**Context:** Session start audit of the working tree (3355 modified files).
**Error:** `src/generated/resources` contained an uncommitted `runData` output dated 2026-05-23 (pre-1.2.3 code). It deleted the hand-authored compressor/freezer crafting recipes, the zh_cn lang file and hundreds of `mineable/pickaxe` tag entries, and left stale *untracked* copies of the four vat recipes (`compressed_lpg`, `cooling_fluid`, `liquid_air`, `liquid_asphalt`) that the 1.2.3 commit had moved to `src/main/resources`. Since both resource roots are merged into the jar, the stale copies could override the fixed recipes at runtime.
**Root cause:** Hand-authored files were placed inside `src/generated/resources`, which is owned by datagen and fully rewritten by every `runData` run; plus a datagen run from an old code state was left half-applied in the working tree.
**Fix:** Restored `src/generated` to HEAD, deleted stale untracked datagen leftovers, moved hand-authored files (compressor/freezer recipes + advancements) to `src/main/resources`, merged the duplicated zh_cn.json (1172-key legacy + 436-key port → single 1298-key file in `src/main/resources`).
**Prevention:** NEVER hand-edit or hand-add files under `src/generated/resources` — put manual resources in `src/main/resources`. After changing datagen-relevant code, run `gradlew runData` and commit its full output in the same commit. Before committing datagen output, `git diff --stat src/generated` and investigate any *deletion*.

---

## [2026-07-01 12:05] — Build artifacts (`bin/`) tracked in git

**Context:** Same audit.
**Error:** 6033 compiled `.class`/resource files under `bin/` (Eclipse/VSCode compiler output) were tracked in git despite `/bin/` being in `.gitignore` (ignored only applies to untracked files).
**Root cause:** `bin/` was committed before the `.gitignore` entry was added; git keeps tracking already-tracked files.
**Fix:** `git rm -r --cached bin` (commit d3611d09).
**Prevention:** After adding a path to `.gitignore`, always check `git ls-files <path>` and untrack leftovers.

---

## [2026-07-02 05:30] — Session-limit interruption of the verification workflow

**Context:** 17-agent adversarial verification workflow over the 121 mapped findings.
**Error:** All agents failed twice with "You've hit your session limit"; failed agents have no checkpoint and restart from scratch on resume (only COMPLETED agents replay from cache).
**Root cause:** Subagent fan-out burned the API quota; a killed agent's partial work is lost except for files it already wrote to disk.
**Fix:** Reused the 10 patch-plan files the finished agents had written to the scratchpad; verified and fixed the remaining 7 groups inline (single-context reads) instead of re-running agents.
**Prevention:** Have verification agents WRITE their outputs to disk incrementally (patch files survive the agent's death). When quota is tight, prefer inline verification over agent re-runs — resuming a workflow only saves tokens for agents that fully completed.

## [2026-07-02 06:30] — Accumulator energy nearly truncated by a "safety" clamp

**Context:** Adding a capacity clamp to `TFMGForgeEnergyStorage.setEnergy` (verified finding AC2).
**Error:** The clamp alone would have deleted energy: `AccumulatorBlockEntity.read` restores `ForgeEnergy` into the storage built by the field initializer (capacity = length 1) BEFORE the chain rebuild resizes it — a full 5-block chain would have been clamped to 1 block's capacity on every load.
**Root cause:** `EnergyStorage.capacity` is fixed at construction; `getMaxCapacity()` is dynamic (config × length). The NBT read order (length before storage rebuild) was invisible in the finding itself.
**Fix:** `read()` now rebuilds the storage at the persisted length before `setEnergy`, then the clamp is safe.
**Prevention:** Before clamping any restore path, trace WHEN the receiving container gets its final size — clamps applied to half-initialized state destroy data.

---

## [2026-07-02 15:15] — Upstream datagen drift: block tags regress on every runData

**Context:** Re-running `gradlew runData` for the new fluid tags (hydrogen/butane/propane).
**Error:** A fresh datagen from CURRENT code deletes entries that are committed and needed at runtime: `c:ores` (lead/nickel/lithium), `c:storage_blocks/*`, `minecraft:doors`/`climbable`/`beacon_base_blocks`/`needs_iron_tool`, `create:casing`/`fan_transparent`, plus heavy churn in `mineable/pickaxe`. Identical deletions appeared in the stale 2026-05-23 run — the committed tag data comes from an older code state (pre-Create-6/1.21-port) that the current builder transforms no longer reproduce.
**Root cause:** Tagging code was lost upstream during the 1.21/Create 6 migration; the shipped jar only stayed correct because nobody re-ran datagen and committed blindly.
**Fix:** Selective accept: staged only the additive outputs (new c: fluid tags, firebox_fuel, vanilla tool/enchantable item tags, extinguisher filling default-component change, regenerated compressed_lpg) and `git checkout`-restored every drifted file.
**Prevention:** NEVER commit a full runData output on this repo without reviewing `git diff --stat src/generated` — deletions in tag files are regressions until the lost tagging transforms are restored in code (tracked as future work). Also: `runData` requires NeoForge ≥ 21.1.219 since Create 6.0.10.

## [2026-07-05 18:10] — 'git add -A' silently committed a working-tree deletion of CHANGELOG.md

**Context:** Committing the regular-engine renderer hardening (66c63730).
**Error:** CHANGELOG.md had been deleted from the working tree (outside this session); `git add -A` staged the deletion and the commit recorded it. Discovered only when a later edit failed with "File does not exist".
**Root cause:** Committed without reviewing `git status --porcelain` first; `add -A` stages deletions too.
**Fix:** Restored the file from the parent commit (`git checkout 58067f75 -- CHANGELOG.md`) and re-applied the new entries (f4ead0c1).
**Prevention:** Always review `git status --porcelain` before `git add -A`; treat unexpected ` D` entries as red flags to investigate, never to commit.

## [2026-08-01 00:55] — Blast Stove capability refresh silently dead across a package boundary

**Context:** Ticket #227, the heat recuperator (Blast Stove) losing its piping after every relog until a block was broken and replaced.
**Error:** After a chunk reload, every non-controller stove block served the empty tanks built by its own constructor instead of delegating to the controller, so pumping heated air out of the top silently did nothing.
**Root cause:** `BlastStoveBlockEntity` declares `public void refreshCapability()` believing it overrides `FluidTankBlockEntity#refreshCapability`. That parent method is **package-private** in `com.simibubi.create.content.fluids.tank`, and a package-private method cannot be overridden from another package: the child method is a new, unrelated one. The parent's own `updateCapability` hook (set in `read()`, consumed in `tick()`) therefore only ever rebuilt the parent's `fluidCapability`, never the stove's `primaryCapability`/`secondaryCapability`. The stove's constructor had built them while `controller` was still null, i.e. pointing at its own tanks.
**Fix:** Added a dedicated `refreshStoveCapability` flag set at the end of `read()` and consumed right after `super.tick()`. Deliberately NOT named `updateCapability`, which would shadow the inherited field and reintroduce the shadowing bug already documented in that class.
**Prevention:** Before relying on an override of a Create method, check the parent's access modifier: no modifier means package-private and no cross-package override, silently. TFMG already handles this correctly elsewhere via `@Invoker("refreshCapability")` in `FluidTankBlockEntityAccessor` — the existence of an `@Invoker` for a method is itself the signal that it is not directly reachable. Defer any post-`read()` capability refresh to the next tick: at read time neighbouring block entities may not be loaded, and `handlerForCapability()` silently falls back to local tanks when `getControllerBE()` returns null.
