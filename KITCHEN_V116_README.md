# Kitchen 3D Studio — v1.16 "Chinese Features, Batch 2" (2026-11-28)

Batch 2 of the Chinese-apps research (Kujiale / Sanweijia, report
`~/workspace/research_notes/chinese-kitchen-design-features-20261127-2052/report.md`):
**AI智能布局 smart layout (feature 2) + 参数化柜体 parametric cabinets (feature 3)
+ 智能铺砖 smart tiling (feature 8).**

## What was checked first (honest scope)

- **Parametric cabinets already exist in full** — `ParametricCabinetDialog`
  (657 lines: width/height/depth, shelves, drawers, door style, handle,
  facade material, countertop, LED, presets, 3D preview) + `ViewModel.applyParametricCabinet`,
  reachable from the top toolbar (Tune icon). NOT rebuilt.
- **The cut list already exists** (PDF + DXF export) and the filler tool
  (`AutomaticFillerEngine` + `CabinetFillerToolDialog`) already fills wall gaps.
- **Real gaps built: smart layout + smart tiling.**

## 1. Smart layout generator (NEW) — `SmartLayoutEngine` + `SmartLayoutDialog`

- Shape I / L / U, main wall (N/S/E/W), dishwasher + tall-pantry toggles.
- Two-phase, deterministic (no ML): phase 1 picks the anchor slots —
  sink (snapped under the window when its centre falls in the middle of the
  run, else 40%; a 100 mm-step search picks the position closest to the
  preference that actually leaves room for the stove), stove 1500 mm from
  the sink (1200 mm fallback), fridge at the far end of the main run or,
  when the main run is too short (the U case), right after the corner on a
  side run; phase 2 fills the gaps with 600 mm units + one custom-width
  unit / filler strip, and the wall row above (skipping the 900 mm hood
  span, tall-unit spans and window spans whose top is above 1450 mm).
- Work triangle is AIMED at the optimal band (sink–stove 1200–2700 mm,
  perimeter 3400–7900 mm — the same limits the validator checks); the
  dialog shows both numbers live plus warnings.
- Door openings are reserved (span + 200 mm swing margin) — no element is
  ever placed across a door. Base cabinets may sit under windows
  (sill ≥ 900 mm); wall cabinets skip window spans.
- Applying replaces the whole kitchen (elements only — the room never
  changes), with a confirmation dialog and one undoable batch command,
  mirroring templates (v1.14). Entry: top toolbar (✨ icon).
- Tests (`SmartLayoutEngineTest`, 6): L/I/U on the default 4200×3600 room
  — anchors present, all floor units in bounds and pairwise disjoint,
  triangle in band, corner counts 1/0/2; U falls back to L on a narrow
  room; tiny main wall returns empty + warning; south main wall never
  blocks the door.

## 2. Smart tiling (NEW) — `SmartTilingEngine` + `SmartTilingDialog`

- Floor or backsplash (600 mm strip above the counter), tile sizes
  30×30 / 30×60 / 60×60 / 80×80 / 60×120, patterns straight / brick
  (half-offset) / diagonal 45°, grout 2/3/5 mm, optional price per tile.
- Counts are computed cell-by-cell exactly for straight and brick:
  full tiles, cut tiles, tiles saved by skipping the area under
  floor-standing cabinets (≥ 90% covered cells), total to buy with a 10%
  spare, offcut waste % (= offcut area / bought tile area).
- Diagonal 45° is an ESTIMATE: the straight grid + the industry-standard
  15% extra allowance — labelled as such in the dialog.
- Backsplash length = summed wall-adjacent length of base cabinets; with
  no base cabinets the result carries a note instead of zeros.
- "Apply" (floor only) sets the room's floor finish to the chosen tile
  material — undoable, like any floor-material change. Entry: top toolbar
  (⊞ icon).
- Tests (`SmartTilingEngineTest`, 6): hand-computed counts for the default
  room (straight 30 full / 12 cut / 47 to buy; brick 30 / 15 / 50);
  diagonal buys more than straight; one cabinet saves exactly one tile;
  backsplash 1200 mm run with 30×60 tiles → 3 full / 1 cut / 5 to buy;
  empty backsplash carries a note.

## Honest limits

- The generator aims at the optimal triangle band but does not guarantee
  it: a tiny room or a door cutting the main wall can force a shorter leg
  or drop the fridge/dishwasher — the result carries a warning and the
  app's own ergonomic validator still runs afterwards.
- Corner units are 900×900 squares in plan, so their rotation is cosmetic
  only (set to the main wall's rotation).
- Diagonal tiling is an estimate (+15%), not exact diamond geometry.
- Parametric cabinets were NOT rebuilt — they already existed (see above).

## Phone checks (owed by Abd)

1. ✨ التخطيط الذكي → حرف L → تطبيق → تأكيد: مطبخ كامل يظهر (حوض تحت
   الشباك، موقد وشفاط، ثلاجة، غسالة صحون)، و«تراجع» يعيده كما كان.
2. جرّب U على الغرفة الافتراضية: 3 جدران ممتلئة ومثلث عمل بلا تحذيرات.
3. ⊞ التبليط الذكي → أرضية 60×60 مستقيم: 30 كاملة + 12 مقصوصة + 47
   للشراء؛ بدّل إلى «طوب» فتتغير الأرقام؛ طبّق خامة رخامية فتتغير الأرضية.
4. ضع باباً في الجدار الرئيسي (أو غيّر الجدار الرئيسي إلى الجنوبي) ثم
   ولّد تخطيطاً: لا وحدة تعبر فتحة الباب.

## Build

- Version 1.16 / versionCode 18. Push → GitHub Actions → verify APK
  (cert 9353f138… = stable key, versionName 1.16, markers in dex:
  SmartLayoutEngine / SmartTilingEngine / التخطيط الذكي / التبليط الذكي).
