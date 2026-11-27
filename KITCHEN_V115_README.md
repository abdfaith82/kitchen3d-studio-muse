# Kitchen 3D Studio — v1.15 "Chinese Features, Batch 1" (2026-11-28)

Built from the Chinese-apps deep research (酷家乐 Kujiale / 三维家 Sanweijia,
report `~/workspace/research_notes/chinese-kitchen-design-features-20261127-2052/report.md`).
Before writing code, the researcher's "gaps" were checked against the real
codebase — two of the top features already exist, so they were NOT rebuilt:

- **一键导出施工图 (construction drawing export) — already exists**: the PDF
  blueprint exporter (dimensioned plan + BOM + KPI + cost + cut list +
  hardware summary) + DXF export, wired into the export dialog.
- **拆单 (cut list) — already exists**: same exporter, cut-list page.
- **一键换风格 (one-click style) — already exists in Cinematic Pro**:
  `GlobalKitchenStyle` + `applyGlobalKitchenStyle` recolours wall + floor +
  ALL cabinets + countertops, undoable (v1.12 P1-6).

What v1.15 actually adds — the real gaps:

## 1. Harmony presets now recolour the cabinets too (一键换风格)

The wall/floor harmony presets in the 3D viewport (`Viewport3D`, "ثنائيات
التناسق المقترحة") changed wall + floor only and left the cabinets untouched,
even though each preset carries a `recommendedCabinetFinish`. One tap now
applies wall + floor + every cabinet's finish (new
`ViewModel.applyCabinetFinishToAllElements`, one undoable
`BatchFloorPlanLayoutCommand`, countertops untouched). The section header now
says "(حائط + أرضية + خزائن)". Wired at both `Viewport3D` call sites in
`KitchenStudioScreen`. (One tap = 3 undo entries, same as the global style —
wall, floor, cabinets — matching existing behaviour.)

## 2. Door-swing ARC + drawer pull-out interference (干涉检测)

The existing corridor check only measures straight-line clearance in front
of a door and ignores an obstacle whose lateral overlap with the front
corridor is ≤ 80 mm. A hinged leaf and a sliding drawer still run into such
an obstacle. New in `ErgonomicValidationEngine` (runs live on every project
change, issues render on the 2D plan via `Viewport2D` highlights):

- **`DOOR_SWING_ARC_COLLISION` (ERROR)**: each hinged leaf sweeps a
  quarter-disc of radius = leaf width around its hinge (leaf = width /
  leaves; 2 leaves when width ≥ 750 mm or `customDivisionsCount` set).
  A footprint intersecting the disc — corner inside, edge crossing the arc,
  or arc endpoint inside the rectangle — is a hit. A 25 mm penetration
  rule (`ARC_PENETRATION_MM`) means a neighbour whose corner merely grazes
  the arc (a normal L-junction) is NOT flagged. Excluded: drop-down doors
  (dishwasher/oven/washer — no plan sweep), corner units (diagonal leaf),
  drawer units (they slide; see below).
- **`DRAWER_PULL_OUT_BLOCKED` (ERROR)**: the drawer's straight pull-out
  rectangle (cabinet width × travel = min(depth, 500 mm)) vs other
  footprints, same 25 mm penetration rule and the same vertical-band rule
  (> 120 mm overlap) as the corridor check.

Honest scope, verified by geometry before building: drawer travel and wall
hits need no separate checks — a drawer travels straight forward inside the
corridor, and the disc never reaches a side wall (it stays inside the
cabinet's own width span); the opposing-wall distance in the corridor check
already covers walls for rectangular rooms.

## Tests

New `DoorSwingArcTest` (5 pure-JVM tests, run in CI): arc hits a side
sliver the corridor check ignores (and the corridor stays silent);
L-junction grazing is NOT flagged; a wide 1200 mm leaf is caught 800 mm
out front (beyond the 650 mm corridor requirement); a drawer is blocked in
the sliver (and no arc issue fires for it); a drawer is free beyond its
500 mm travel.

## What to check on the phone

1. In the 3D view, open the wall/floor material sheet → tap a harmony
   preset → the cabinets change colour with the wall and floor; Undo
   reverts all three.
2. Put a narrow pull-out (or any cabinet) just beside a door's hinge line,
   mostly outside the door's front corridor → a red "باب … يصطدم بـ…"
   error appears; move it away and the error clears.
3. A normal L-shaped kitchen must show NO arc errors (grazing rule).

## Next: batch 2 (not in this version)

AI智能布局 (smart layout generator) + 参数化柜体定制 (parametric cabinets)
+ 智能铺砖 (smart tiling) — the bigger build, v1.16.
