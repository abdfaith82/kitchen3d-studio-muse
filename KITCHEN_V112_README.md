# Kitchen 3D Studio — v1.12 "Deep Review" (2026-11-27)

Deep review of **all screens, commands, errors, realism, motion and control**
(طلب عبد: «مراجعة جميع الشاشات والأوامر والأخطاء والواقعية والحركة والتحكم
بعمق وجعلها أكثر قوة وهندسة»). 25 findings from the audit, 21 fixed in this
version, 4 deliberately deferred (listed below).

## P0 — fixed

- **P0-3 — renders were near-black.** The CPU ray tracer multiplied its
  display-referred light terms (sun ≈ lux/10000, ambient ≈ 1) by Filament's
  photometric `exposureMultiplier` (≈0.0009 at f/4, 1/60, ISO 100), so a
  sunlit white wall landed at ~1/255 after ACES, with only harsh specular
  glints surviving. `CameraExposure` now has `displayScale` — 1.0 at the
  defaults, doubling per stop (ISO/aperture/EV) — and the tracer scales by
  it instead. Regression test: an FHD render's mean luminance must be
  0.15–0.95.
- **P0-1 — AI design with zero units wiped the kitchen, and Undo couldn't
  bring it back.** An empty `units` array now returns an Arabic Error
  instead of Success; the Apply button is disabled when there are no units;
  `applyImportedProject` no longer clears undo history and pushes two undo
  entries (room + N units) instead.
- **P0-2 — rotating the phone wiped the whole sketch.** Strokes, the
  calibration marker and the background photo now live in the ViewModel
  (StateFlow) and survive rotation; they are cleared only when the dialog
  is dismissed or converted.

## P1 — fixed

- **P1-1 — no render cancel.** Render Studio keeps the render `Job`: an
  «إلغاء الرندر» button, the ✕ button and the back gesture all cancel it,
  and `isRendering` always resets in `finally`.
- **P1-2 — "8K" silently rendered 4K.** The header chip, progress text,
  render button and gallery filename now show the **actual** size, with a
  note when the device heap forces the 4K fallback; the dead header chip
  now cycles resolutions.
- **P1-3 (partial) — "GI" was a constant.** Live viewport: window sun
  patches and cast shadows are now driven by the real sun azimuth/elevation
  (patch skipped when the sun can't shine through that wall). The Render
  Studio subtitle was relabelled honestly: «إضاءة محيطة وظلال شمس ناعمة».
  True AO/bounce rays in the tracer were **not** done (deferred).
- **P1-4 — Cinematic Pro lens/grain/exposure did nothing to the export.**
  Lens FOV, EV compensation and film grain are now wired into the 4K
  export; the dialog text now says honestly that aperture/DoF blur is
  stored but not rendered yet (no depth buffer in the tracer).
- **P1-5 — flat-box cabinets and props in the export.** The tracer now
  adds door panels (inset, slightly darker) + brushed-steel handle bars to
  axis-aligned cabinets, and the island kettle/plant are multi-box clusters
  instead of single slabs.
- **P1-6 — style/preset recolour never undid.** Finish changes are now one
  undoable batch command (before/after element lists), so Undo reverts
  cabinets together with walls/floor.
- **P1-7 — "Save As" wiped undo history.** `commandHistory.clear()` removed
  from Save-As (kept in load/new/restore, where it belongs).
- **P1-8 — JSON export failed silently.** `exportAndShareProjectJson` now
  takes an `onFailure` callback; Projects Manager shows the Arabic error.
- **P1-9 — exports lived only in cache.** Every share (file / social /
  email, incl. the ViewModel JSON export) now also writes a copy to
  **Downloads/Kitchen3DStudio** via MediaStore and shows the path.
- **P1-10 — raw English errors.** Magic Hour / Pollinations HTTP failures
  are mapped to Arabic (401/403 → key, 402 → out of credit, 429 → quota,
  5xx → service down); the raw body goes to Logcat only. Video statuses
  (`processing`/`queued`/`error`…) are translated. All Export Studio and
  ShareExportHelper catches show fixed Arabic messages.
- **P1-11 — silent photo failures.** Sketch, Photo Cabinet Designer (front
  + side) and Export Studio logo pickers now show «تعذّر قراءة الصورة
  المختارة — جرّب صورة أخرى» on exception and on null decode.
- **P1-12 — Render Studio bottom bar overflow.** Resolution pills + AA
  toggle are in a scrolling row; the render button is full-width on its own
  row (48 dp).
- **P1-13 — 2D pan twice as fast with nothing selected.** The transform
  detector is pinch-zoom only now; single-finger pan lives in the drag
  detector alone.
- **P1-14 — 2D tap-to-select had zero tolerance.** Tap now uses the same
  20–120 mm tolerance as drag; empty-space tap deselects; resize handles
  and ruler pins are 24 dp hit targets (was 30/42 raw px).
- **P1-15 — sketch tap-to-edit only near a stroke's midpoint.** Hit-test is
  now point-to-segment distance along the whole stroke.
- **P1-17 — 26–30 dp nudge/scale buttons.** All 25 small Transform-bar
  buttons now have a 48 dp minimum touch target.
- **P1-19 — preset glide teleported halfway (orbit↔walk).** The tween keeps
  the start mode until progress 1.0; the eye is lerped as one position and
  `orbitEyeFor` reproduces Viewport3D's orbit eye formula, so hand-back is
  seamless.
- **P1-20 — two-finger pan unclamped.** 3D pan clamped to ±½ the room's
  largest dimension; 2D pan clamped to the viewport size.
- **P1-21 — auto-rotate spun invisibly in walk mode.** `stopCameraAutoRotate()`
  is now called from `setCameraMode`, `animateCameraToPreset`,
  `updateCameraPan` and `updateWalkLook`.

## P2 — fixed

- **P2-1 — Magic Hour key "saved ✓" even when saving failed.**
  `saveApiKey` returns Boolean (like `AiApiKeyStore`); the dialog only
  shows the saved state on `true`, else «تعذّر حفظ المفتاح على هذا الجهاز».

## Below the cut — fixed

- Material Picker sheet opens fully expanded (`skipPartiallyExpanded = true`).
- Export Studio / Projects Manager headers: title Column has `weight(1f)`,
  so the close button stays on-screen on narrow phones.

## Deliberately deferred (NOT in v1.12)

- **P1-16 / P1-18 — Viewport3D gesture rework** (drag-vs-orbit from the
  down point; two-finger pan without pinch). Riskiest gesture code; needs
  a phone test pass before touching.
- **P1-3 — true AO / diffuse bounce rays** in the ray tracer (costly on a
  phone CPU); the preset is relabelled honestly instead.
- **DoF blur** in the export (no depth buffer in the tracer); aperture is
  stored and the dialog says so.
- **Nudge-undo coalescing** (each 1 mm tap is still its own undo entry)
  and **frame-synced tweens** (`withFrameNanos` instead of wall-clock steps).

## Build

- Version **1.12 / code 14**. Same stable keystore as v1.7–v1.11
  (cert `9353f138…`), installs over v1.11.
- New tests: `CameraTweenAndExposureTest` (displayScale stops, tween mode
  flip, orbit-eye sanity) + the FHD mean-luminance test in
  `OffscreenRenderTest`.
