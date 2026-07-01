# Changelog

All notable changes to Create: The Factory Must Grow are documented here.

---

## [1.2.3] - 2026-07-01

### Fixed

- **Vat pressure & freeze recipes now trigger** — Compressors and freezers declared `PositionRequirement.TOP`, yet the vat only read their pressure/heat contribution from the layer directly **below** it, so a machine placed where it actually affected the vat was never counted toward the recipe's machine list. Both now use `PositionRequirement.ANY` and the vat derives pressure/heat from the position-validated machine set, so LPG, Liquid Air and Cooling Fluid finally run whether the machine is placed under or on top of the vat.
- **Freeze recipes no longer permanently blocked** — The recipe heat gate (`heatLevel < recipe.heatLevel`) rejected any vat colder than 0, which every freeze recipe is by definition. A minimum-heat check is now only enforced when a recipe actually requires heat (`recipe.heatLevel > 0`), so Cooling Fluid completes.
- **Vat machines matched by containment, not exact count** — A recipe listing one compressor no longer refuses to run because the player filled every slot under a 3×3 vat with compressors. The vat must now hold *at least* the machines a recipe requires; extras are ignored, matching the pressure/heat "threshold" design.
- **Industrial Blast Furnace consumes Hot Air for Silicon** — The Silicon recipe shipped with no `hot_air_usage`, so it defaulted to 0 and drained nothing (and needed no hatch). It now consumes 20 mB/tick of Hot Air like the steel recipes.
- **Converter now outputs FE and TFMG power both ways** — TFMG→FE: the converter only *exposed* an energy capability, so passive consumers (energy cubes in input mode) received nothing and only a puller cable worked; it now actively pushes stored FE into adjacent consumers on every non-TFMG face. FE→TFMG: incoming FE never re-triggered the electrical network, leaving the blue-side cable at 0 V; the energy tank now schedules a network update on change, publishing the converted voltage.
- **Chemical Vats place a full layer at once** — All three vats reused the steel-tank item whose bulk-placement routine only understood steel tanks and silently no-opped for a vat. It now branches on the vat's own API, so a 2×2 / 3×3 layer places in a single click like a tank.
- **Traffic Light cycle corrected** — Sequence was Red → Orange → Green → Orange → Red. It now runs Red → Orange → Green → Red (the green phase holds until the cycle resets instead of showing a second orange).
- **Metal doors sound metallic when broken** — The four metal doors (Heavy Casing, Steel, Aluminum, Heavy Plated) forced Create's glass block-set type, so they shattered like glass. They now use the vanilla copper block-set type, giving the same metallic break sound as a TFMG metal tank.
- **Blast Stove 3×3 faces are no longer invisible** — The connected-texture atlases had fully transparent "both-sides-connected" (center-of-face) tiles, so the middle block of each face of a 3×3 stove rendered as a hole. The transparent tiles are refilled from the base texture.

### Known issues

- **Coke Oven over-sizing** — Adding a layer to the front or bottom face of a full 6×6 oven can still mis-form the multiblock. A correct fix requires reworking the multiblock (persisting its size and hardening corner detection) and is deferred to avoid regressing valid ovens. Workaround: keep ovens within 6×6.
- **Aluminum / LPG require the right vat tier** — Aluminum (electrolysis) and LPG (pressure) are intentionally restricted to **Steel** and **Firebrick-lined** vats, not Cast Iron. Using a Cast Iron vat is why they appeared broken; the mechanics themselves are correct.

---

### Correctifs

- **Les recettes de pression et de gel de la cuve se lancent enfin** — Les compresseurs et congélateurs déclaraient `PositionRequirement.TOP`, alors que la cuve ne lisait leur pression/chaleur que dans la couche **en dessous** d'elle : une machine placée là où elle agissait réellement n'était jamais comptée dans la liste de machines de la recette. Les deux utilisent désormais `PositionRequirement.ANY` et la cuve calcule pression/chaleur depuis l'ensemble de machines validé par position, donc le LPG, l'Air Liquide et le Fluide de Refroidissement fonctionnent que la machine soit sous ou sur la cuve.
- **Les recettes de gel ne sont plus bloquées en permanence** — Le contrôle de chaleur (`heatLevel < recipe.heatLevel`) rejetait toute cuve plus froide que 0, ce que toute recette de gel est par définition. Un minimum de chaleur n'est désormais exigé que si la recette requiert vraiment de la chaleur (`recipe.heatLevel > 0`), donc le Fluide de Refroidissement se termine.
- **Machines de cuve comparées par inclusion, pas par nombre exact** — Une recette listant un compresseur ne refuse plus de tourner parce que le joueur a rempli de compresseurs tous les emplacements sous une cuve 3×3. La cuve doit désormais contenir *au moins* les machines requises ; les surplus sont ignorés, conformément à la logique de « seuil » de pression/chaleur.
- **Le Haut Fourneau Industriel consomme de l'Air Chaud pour le Silicium** — La recette de Silicium était livrée sans `hot_air_usage`, donc par défaut 0 et ne drainait rien (ni ne nécessitait de trappe). Elle consomme désormais 20 mB/tick d'Air Chaud comme les recettes d'acier.
- **Le Convertisseur restitue l'énergie FE et TFMG dans les deux sens** — TFMG→FE : le convertisseur ne faisait qu'*exposer* une capacité d'énergie, donc les consommateurs passifs (energy cubes en mode entrée) ne recevaient rien et seul un câble en aspiration fonctionnait ; il pousse désormais activement le FE stocké vers les consommateurs adjacents sur chaque face non-TFMG. FE→TFMG : le FE entrant ne relançait jamais le réseau électrique, laissant le câble côté bleu à 0 V ; le réservoir programme désormais une mise à jour du réseau à chaque changement, publiant la tension convertie.
- **Les Cuves Chimiques se posent par couche entière** — Les trois cuves réutilisaient l'objet du réservoir en acier, dont la pose groupée ne comprenait que les réservoirs en acier et ne faisait rien pour une cuve. Elle s'appuie désormais sur l'API propre de la cuve, donc une couche 2×2 / 3×3 se pose en un clic comme un réservoir.
- **Cycle du Feu de Circulation corrigé** — La séquence était Rouge → Orange → Vert → Orange → Rouge. Elle est désormais Rouge → Orange → Vert → Rouge (le vert est maintenu jusqu'au reset du cycle au lieu d'un second orange).
- **Les portes métalliques sonnent métallique quand on les casse** — Les quatre portes métalliques (Blindage Lourd, Acier, Aluminium, Plaqué Lourd) forçaient le type de bloc « verre » de Create et se brisaient donc comme du verre. Elles utilisent désormais le type de bloc « cuivre » vanilla, donnant le même son métallique qu'un réservoir métallique TFMG.
- **Les faces du Blast Stove 3×3 ne sont plus invisibles** — Les atlas de texture connectée avaient des tuiles « connecté des deux côtés » (centre de face) totalement transparentes, donc le bloc central de chaque face d'un four 3×3 apparaissait comme un trou. Les tuiles transparentes sont recomblées depuis la texture de base.

### Problèmes connus

- **Sur-dimensionnement du Coke Oven** — Ajouter une couche sur la face avant ou dessous d'un four 6×6 complet peut encore mal former le multibloc. Une correction propre nécessite de retravailler le multibloc (persistance de la taille et durcissement de la détection du coin) et est reportée pour éviter de casser les fours valides. Contournement : garder les fours en 6×6.
- **Aluminium / LPG nécessitent le bon palier de cuve** — L'Aluminium (électrolyse) et le LPG (pression) sont volontairement réservés aux cuves en **Acier** et **Briques Réfractaires**, pas en Fonte. Utiliser une cuve en Fonte explique pourquoi ils semblaient cassés ; les mécaniques elles-mêmes sont correctes.

---
