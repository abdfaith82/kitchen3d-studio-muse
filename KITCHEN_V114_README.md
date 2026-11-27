# Kitchen 3D Studio — v1.14 "No More Silent Resets" (2026-11-27)

Abd: «النماذج الجاهزة اعادة ضبطها ولوحة السكتش بعدها صغيرة» — applying a
ready-made model reset his kitchen, and the sketch board afterwards was small.
Two bugs, one version.

## 1. Templates no longer reset the kitchen silently

**The bug.** `KitchenEditorViewModel.loadTemplate()` called
`repository.loadTemplate()` and then `commandHistory.clear()`: tapping a
ready-made model replaced the whole project (room + all cabinets) with no
warning and no way back — Undo was deliberately wiped at the same moment.

**The fix (same pattern as the v1.12 P0-1 import fix).**
- `KitchenStudioScreen` now asks first: tapping a template opens a
  confirmation dialog — «سيستبدل نموذج … مطبخك الحالي بالكامل (الغرفة
  والخزائن). يمكنك التراجع عنه فوراً من زر التراجع ↩️.» — with
  [إلغاء] / [تطبيق النموذج]. Nothing is replaced until he confirms.
- `loadTemplate()` is now undoable instead of wiping history: it snapshots
  the old project, sanitises the template's elements against overlaps
  (`CollisionDetector.resolveAllSceneOverlaps`, like the import path),
  writes it via `repository.updateProject()` (which persists to Room, and
  the existing two-way project sync carries it into the UI), then pushes the
  same two undoable commands the import uses — `ModifyRoomLayoutCommand`
  for the room (only when it differs) and `BatchFloorPlanLayoutCommand`
  for the elements («تطبيق نموذج جاهز (N وحدة)»). One tap on Undo brings
  his old kitchen back, room and all. `commandHistory.clear()` is gone.
- The repository's own `loadTemplate()` is now unused by the ViewModel
  (kept — it is part of the repository interface).

## 2. The sketch board is small because the preview steals its space

**The bug.** In `SketchCanvasDialog`, the live-3D preview pane was ALWAYS
placed in the layout — a fixed 180dp in portrait, weight 0.95f in
landscape — even when its toggle («Live 3D Split Preview») was OFF. The
pane itself renders nothing when off (`if (showLive3DSplitPreview)` wraps
the whole Surface), but the layout still reserved its space, so the canvas
was squeezed to ~200–300dp in portrait no matter what the toggle said.

**The fix.** The preview pane is now only placed when the toggle is on, in
both orientations (portrait: canvas takes the whole main area; landscape:
canvas takes the full width). Turn the preview off and the board grows to
fill the dialog. Turn it on and the split returns exactly as before.

## Tests

- No new unit tests: both fixes are UI/ViewModel behaviour that needs a
  phone pass (confirm dialog flow, Undo-after-template, board size with the
  preview toggle off/on). The existing suite (incl. the v1.13
  `NudgeCoalescingAndDofTest`) still runs in CI.

## What to check on the phone

1. Tap a ready model → a confirmation appears → Cancel keeps your kitchen,
   Apply replaces it → Undo (↩️) brings your old kitchen back.
2. Open the sketch with the 3D preview toggle OFF → the board is now big,
   filling the screen. Turn the preview ON → the split view returns.
