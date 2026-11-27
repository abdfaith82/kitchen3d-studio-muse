# Kitchen 3D Studio — v1.13 "Finish the Deferred" (2026-11-27)

Abd: «اكمل وابدع» — continue and be creative. This version finishes the
four items v1.12 deliberately deferred, and makes the Cinematic Pro aperture
real in the export for the first time.

## Gestures — the riskiest code, done carefully (P1-16 / P1-18)

- **P1-16 — drag-vs-orbit is decided ONCE per gesture, from the finger-down
  point.** `Viewport3D` captures the down point in `detectTapGestures.onPress`
  (`gestureDownPoint`) and classifies the gesture a single time
  (`gestureClassified3D`): gizmo hit, element hit (MOVE modes / AUTO_SMART)
  — from the down point, not the drifted centroid. The old code re-tested on
  the moved centroid on every pan event, so a drag starting on a cabinet edge
  often became an orbit mid-stroke. The per-event drag reset before the orbit
  branch is gone; drag state is cleared on release (onPress) / tap, so a
  gesture can no longer flip between drag and orbit mid-stroke.
- **P1-16 (MOVE modes) — no more `?: selectedElem?.id` fallback.** Dragging
  empty space in a Move mode used to grab the selected cabinet, making it
  impossible to orbit there. Empty space now orbits.
- **P1-18 — two-finger parallel move (no pinch) is a pan.** A passive
  pointer-count tracker (`awaitPointerEventScope`, consumes nothing) sets
  `twoFingerPanActive`; the transform detector routes two-finger pan to
  `onPan` (same damped pan + fling as the pinch branch) instead of falling
  through to orbit. Walk mode keeps the old look-around behaviour (it has no
  pan target).
- **Needs a phone pass:** gesture code cannot be unit-tested on CI — please
  try on the phone: one-finger drag on a cabinet moves it, one-finger drag on
  empty space orbits, two-finger parallel move pans, pinch still zooms.

## Realism — the ray tracer grows up (P1-3 / P1-4)

- **P1-3 — real contact ambient occlusion.** `traceRay` now fires 4 short
  (500 mm) rays in a 45° cone around the surface normal, primary rays only,
  and multiplies the ambient (SH) term by 1.0 → 0.45. Surfaces under cabinets
  and in corners get darker; cabinets stop looking like they float. The old
  height-based `floorAo` hack stays as a secondary term. Cost: ~4 extra
  intersection tests per primary pixel (renders get slower — a 4K render was
  already minutes-long; the cancel button from v1.12 is the safety valve).
- **P1-4 — depth of field is REAL in the export.** `traceRay` reports the
  primary hit distance through a `depthOut` array (zero extra intersections),
  the render loop builds a per-pixel depth buffer (AA-averaged), and a new
  `applyDepthOfField` pass does a separable box blur at the max radius and
  blends it per-pixel by `|depth − focal|` (smoothstepped), so the camera
  target stays sharp and the background/foreground melts away. Focus follows
  the camera target (the Cinematic Pro dialog exposes aperture only — the
  `focusDistanceMm` default is not used for the export). Radius comes from
  `DepthOfFieldConfig.exportBlurRadiusPx(width)`: f/2.8 → ~9.5 px at 4K,
  f/5.6 → ~4.4 px, f/8+ → exactly 0 (off). Film grain moved AFTER the blur
  (grain lives on the film plane, not the scene). DoF is null when no
  cinematic settings were applied, so plain renders are byte-identical to
  v1.12 and pay no extra memory.
- **Wiring:** `CinematicRenderSettings.toPostProcessConfig()` now passes
  `depthOfField` through (v1.12 dropped it), `RenderStudioDialog` passes
  `cinematicPost?.depthOfField` into the render, and the Cinematic Pro
  dialog text no longer says DoF is "under development".

## Polish — undo and motion

- **Nudge-undo coalescing.** `CommandHistory` gained `peekTop()` /
  `replaceTop()`. `commitTransformCommand` merges a burst of nudge taps on
  the same element within 1.5 s into ONE undo entry (keeps the ORIGINAL old
  pose, updates only the new pose) — 10 taps now undo in one step. The `===`
  check on the top entry proves the entry was not undone or replaced by
  another action in between. A nudge followed by a drag within 1.5 s merges
  too (one continuous adjustment = one undo).
- **Frame-synced tweens.** `animateCameraDelta` and `animateCameraToPreset`
  no longer step with `delay(16/25 ms)`; they drive off real vsync via a
  `Choreographer` `awaitNextFrame()` helper (`runPoseTween`), one state
  update per frame with progress from real frame timestamps — smooth on
  60/90/120 Hz screens. Cancellation removes the pending frame callback.

## Tests

- New `NudgeCoalescingAndDofTest`: `replaceTop` swaps the top entry (undo
  reverts the replacement), `replaceTop` on an empty stack returns false,
  `peekTop` follows undo, `exportBlurRadiusPx` values at 4K/1080p
  (f/1.4 → 12 px, f/2.8 → 9.45 px, f/5.6 → 4.36 px, f/8+ → 0),
  `blurAmount` formula, `isShallow` flag.

## Still honest limits (not fixed here)

- **True GI bounce rays** are still not traced: AO is contact-only
  (500 mm), and the warm bounce term is still the hand-written constant
  from v1.12. A full diffuse bounce would multiply render time several-fold
  on a phone CPU.
- **DoF is a single-radius blur blended by depth**, not a true variable-
  radius bokeh — no per-pixel circle-of-confusion size, no hexagonal bokeh
  shape.
- **AO costs render time** (~4 extra intersections per primary pixel).
  If a 4K render feels too slow, the 1080p preset renders ~4x faster.
