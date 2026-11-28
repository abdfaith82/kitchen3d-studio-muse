# Kitchen 3D Studio v1.18 — "Chinese Features, Batch 4" (water/electric + cut list)

Batch 4 (last) of the Chinese-apps research (Kujiale / Sanweijia): features 9 + 10.
All four batches are now done: v1.15 (style + interference), v1.16 (smart
layout + tiling), v1.17 (panorama + lighting + growth), v1.18 (this one).

## What was checked first (the researcher's "gaps" were partly wrong again)

- **Utility points (feature 9) PARTLY EXIST**: the point model (4 types with
  elevations + the 60 cm backsplash-zone rule), the manual add/remove editor
  in RoomSettingsDialog, default points, and the MEP layer + services table
  in the PDF blueprint all exist. NOT rebuilt. The real gaps were: no
  automatic socket placement, no pipe routing at all, and no CSV list.
- **Cut list (feature 10) PARTLY EXISTS — in depth**: `ParametricCabinetEngine`
  already decomposes a cabinet into `CutPart`s (dimensions, edge banding,
  hardware), `NestingEngine` packs sheets, and the export studio already has
  cut-list toggles (cabinet cut list, CNC nested sheets, aluminium bars,
  router grooves). NOT rebuilt. The real gap was: no readable per-cabinet
  list on the phone, and no CSV export (the app had PDF/DXF only).

## What was actually built

1. **توزيع المقابس تلقائياً (智能插座自动布置)** — `MepEngine.suggestPoints`
   (pure, tested): one socket per appliance (sink / cooktop / oven / fridge /
   dishwasher, 300 mm to the side of its centre) + worktop sockets every
   1200 mm along each base-cabinet run, all at 1100 mm in the 60 cm zone;
   a gas outlet near the cooktop and a drain near the sink when missing.
   Existing points of the same type within 200 mm are never duplicated.
   Applied through the existing undoable `addWallUtilityPoint` path.
2. **مسارات الأنابيب (水暖电)** — `MepEngine.planPipes` (pure, tested):
   cold + hot supply from the riser (the existing drain point) to the sink
   and dishwasher, drain lines back to the riser, as Manhattan polylines
   120 mm off the wall face, routed through shared corners (never diagonal).
   Empty with an Arabic note when there is no sink or no drain point.
3. **حوار الماء والكهرباء** — `WaterElectricDialog` (toolbar 💧 button):
   auto-place card with the suggestion list + "تطبيق الكل", a schematic
   2D plan canvas (room outline, fixture rects, coloured point dots, pipe
   polylines — blue cold, dashed red hot, grey drain), the current points
   list with delete, and CSV save/share.
4. **قائمة القطع (拆单 مبسّط)** — `CutListDialog` (toolbar 📋 button):
   from `getParametricCabinetsFromScene()`, each cabinet →
   `generateCarcassCutParts()` rows (part, W×L×thickness, qty, edge-banding
   metres) + `calculateHardwareRequirements()` lines, totals (panel count,
   area m², edge-banding metres, estimated 2440×1220 sheets), and CSV
   save/share.
5. **تصدير CSV** — `CsvExportHelper` (pure `buildCsv`, tested): quoted-field
   CSV with a BOM so Excel/Sheets open Arabic correctly; save to
   Downloads/Kitchen3DStudio (MediaStore) + share via the existing
   `ShareExportHelper.shareFile`.

## Tests (pure JVM, 10 new)

- `MepEngineTest` (7): worktop socket spacing; appliance sockets + gas +
  drain; dedup against existing points; pipe runs riser→sink→riser;
  dishwasher branch adds 2 runs; empty plan with a note (no sink / no drain);
  corner routing (same wall = 2 points, adjacent walls = 3 with the corner).
- `CsvExportHelperTest` (3): simple rows, quoting (comma / quote), empty input.

## Honest limits

- Pipe routing is a 2D plan schematic (Manhattan lines 120 mm off the wall),
  not a 3D pipe model and not a hydraulic calculation — no diameters, no
  pressure, no slope. The plumber still sizes the pipes.
- Auto sockets follow fixed rules (1200 mm spacing, 1100 mm elevation);
  local electrical codes may demand other spacings — the manual editor in
  RoomSettingsDialog can move any point afterwards.
- The sheet count in the cut list is an area estimate before nesting; the
  real sheet count comes from the existing nesting optimizer / DXF export.
- CSV is the only new export format; the full CNC package stays in the
  existing PDF/DXF export.

## Phone checks owed by Abd

1. Toolbar 💧 → "تطبيق الكل": sockets appear on the walls near the sink,
   cooktop and fridge + along the worktop; the schematic shows blue/red/grey
   pipe lines from the riser to the sink; save the CSV and open it.
2. Delete a suggested point from the dialog's list, then re-run auto-place —
   it should suggest it again (no phantom dedup).
3. Toolbar 📋 → open the cut list: each cabinet shows its panels with
   dimensions and edge-banding metres + hardware lines; totals at the top;
   save the CSV and open it in a spreadsheet.
