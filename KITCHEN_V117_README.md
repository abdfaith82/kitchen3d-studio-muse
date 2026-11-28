# Kitchen 3D Studio v1.17 — "Chinese Features, Batch 3" (panorama + lighting + growth)

Batch 3 of the Chinese-apps research (Kujiale / Sanweijia): features 4 + 7 + 13.
Batch 4 (water/electric points + simplified cut list) is v1.18, not started.

## What was checked first (the researcher's "gaps" were partly wrong again)

- **Lighting simulation (feature 7) ALREADY EXISTS in large part**: the 3D viewport
  has a lighting-simulation bar with sun azimuth/elevation/intensity sliders
  (`updateLightingSimulation`) and a VR gyro walkthrough; the render studio has
  full sun controls, mood presets (V-Ray/Corona…), env presets, LED strips and
  exposure. NOT rebuilt. The real gaps were: no time-of-day clock, no named
  day scenarios, and no false-colour lux map.
- **Gyro walkthrough ALREADY EXISTS** (`rememberVrGyroscopeEffect` + VR 360° bar
  in first-person walk). The real panorama gap was a *shareable spherical image*,
  not the live walkthrough.
- **Growth animation (feature 13) did NOT exist** — built new.
- **DXF export already does CNC cut lists** (found while surveying) — relevant
  to batch 4, to be checked again before v1.18.

## What was actually built

1. **بانوراما 360° (720°全景漫游)** — `PanoramaEngine.equirectDirection`
   (pure, tested) + `equirectangular` mode in `OffscreenRenderer4K.renderScene`
   (fixed 2048×1024, DoF disabled — a blur makes no sense on a panorama).
   The render studio gains a "بانوراما 360°" button (eye level, room centre);
   the result opens in `PanoramaViewerDialog`: drag to look around (seamless
   horizontal wrap), gyroscope toggle reusing `rememberVrGyroscopeEffect`,
   save to Pictures/Kitchen3DStudio, share to any app (cache file +
   `ShareExportHelper.shareFile`).
2. **وقت اليوم + سيناريوهات + خريطة إضاءة (灯光设计)** — `DaylightSimulation`
   (pure, tested): hour 0–24 → sun azimuth/elevation/Kelvin/lux + LED on/off
   (sun arc 05:30–19:30, azimuth 70°→290°; night = 900 lux, LED on).
   The lighting tab in the render studio gains a clock slider (00:00–24:00)
   and four scenario chips (فجر 06:00 / ظهر 12:00 / غروب 17:30 / ليل 21:00)
   that set all sun values at once. The rendered output gains a
   "خريطة الإضاءة" toggle: `FalseColorLuxMap` re-colours the render by
   luminance (blue → cyan → green → yellow → red), computed on a ≤960px
   downscale on Dispatchers.Default, with a legend line.
3. **جولة النمو (生长动画)** — `GrowthTourEngine` (pure, tested): build order
   tall → base → wall → appliances/other, left-to-right within a stage.
   Transient `growthShownCount` in `KitchenEditorUiState` (null = show all);
   `Viewport3D` filters to the first N in build order when set; `setGrowthStage`
   in the ViewModel; `GrowthTourDialog` (toolbar ▶ button) plays it at
   ~450 ms/step with play/pause, a scrub slider and stage labels. Dismissing
   restores the full kitchen — nothing is saved or undone.

## Tests (pure JVM, 12 new)

- `PanoramaEngineTest` (9): equirect compass points + poles + unit length;
  noon/dawn/night/sunset sun values; hour labels; false-colour ramp endpoints
  and bright-end behaviour.
- `GrowthTourEngineTest` (4): stage mapping; build order grouping + x order;
  every element exactly once; stage labels non-empty.

## Honest limits

- The panorama is a still equirectangular PNG (2048×1024), not a video and
  not a live VR scene — it opens in any panorama/photo viewer and can be
  shared, but it is one fixed viewpoint (room centre, eye level 1600 mm).
- The false-colour map is computed from the rendered image's luminance, not
  from a real lux simulation — it shows *relative* light distribution, not
  measured lux values.
- The growth tour is a presentation animation in the 3D viewport, not a
  video file — to share it, screen-record the tour.
- The daylight model is a simple sun arc (no location/latitude, no shadows
  from neighbouring buildings); the sun angles are architectural, not
  astronomical.

## Phone checks owed by Abd

1. Render studio → "بانوراما 360°": the viewer opens; drag looks around
   seamlessly; gyro toggle moves the view with the phone; save lands in
   Pictures/Kitchen3DStudio; share opens the share sheet.
2. Render studio → lighting tab → clock slider to 06:00 / 12:00 / 17:30 /
   21:00: the sun values change (night turns LED on); render at غروب vs ظهر
   and compare; "خريطة الإضاءة" on the rendered output shows blue→red areas.
3. Toolbar ▶ (purple) → "تشغيل": the kitchen grows tall → base → wall →
   appliances in the 3D view behind the dialog; scrubbing the slider jumps
   to that stage; closing restores the full kitchen.
