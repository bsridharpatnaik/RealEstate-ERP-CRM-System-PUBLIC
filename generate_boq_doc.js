'use strict';
const fs = require('fs');
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  Header, Footer, AlignmentType, LevelFormat, BorderStyle, WidthType,
  ShadingType, PageNumber, PageBreak, HeadingLevel, TableOfContents,
} = require('docx');

// ── Constants ─────────────────────────────────────────────────────────────────
const CONTENT_W = 9026; // A4 (11906) - 2 × 1440 margins
const MARGIN    = 1440;

const C = {
  h1:     '1F3864',
  h2:     '2E75B6',
  h3:     '404040',
  hdrBg:  '2E75B6',
  hdrFg:  'FFFFFF',
  altRow: 'EBF3FB',
  tipBg:  'DEEAF1',
  warnBg: 'FFF2CC',
  codeBg: 'F2F2F2',
  shotBg: 'F0F0F0',
  border: 'CCCCCC',
  accent: '2E75B6',
};

// ── Border helpers ────────────────────────────────────────────────────────────
const thin = { style: BorderStyle.SINGLE, size: 1, color: C.border };
const cellBorders = { top: thin, bottom: thin, left: thin, right: thin };

// ── Text helpers ──────────────────────────────────────────────────────────────
function t(text, opts) {
  return new TextRun(Object.assign({ text: String(text) }, opts || {}));
}
function bold(text, color) {
  return t(text, Object.assign({ bold: true }, color ? { color } : {}));
}
function mono(text) {
  return t(text, { font: 'Courier New', size: 18 });
}
function italic(text) {
  return t(text, { italics: true });
}

// ── Paragraph helpers ─────────────────────────────────────────────────────────
function h1(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    children: [bold(text, C.h1)],
    spacing: { before: 360, after: 120 },
  });
}
function h2(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_2,
    children: [bold(text, C.h2)],
    spacing: { before: 240, after: 80 },
  });
}
function h3(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_3,
    children: [bold(text, C.h3)],
    spacing: { before: 180, after: 60 },
  });
}

function para(runs, extra) {
  const children = typeof runs === 'string' ? [t(runs)] : runs;
  return new Paragraph(Object.assign({ children, spacing: { before: 60, after: 60 } }, extra || {}));
}

function spacer() {
  return new Paragraph({ children: [t('')], spacing: { before: 60, after: 60 } });
}

function tip(text) {
  const children = typeof text === 'string'
    ? [bold('Tip  ', '1565C0'), t(text)]
    : [bold('Tip  ', '1565C0'), ...text];
  return new Paragraph({
    children,
    spacing: { before: 140, after: 140 },
    indent: { left: 120, right: 120 },
    shading: { fill: C.tipBg, type: ShadingType.CLEAR },
    border: { left: { style: BorderStyle.SINGLE, size: 14, color: C.accent } },
  });
}

function warn(text) {
  const children = typeof text === 'string'
    ? [bold('Note  ', 'B45309'), t(text)]
    : [bold('Note  ', 'B45309'), ...text];
  return new Paragraph({
    children,
    spacing: { before: 140, after: 140 },
    indent: { left: 120, right: 120 },
    shading: { fill: C.warnBg, type: ShadingType.CLEAR },
    border: { left: { style: BorderStyle.SINGLE, size: 14, color: 'F59E0B' } },
  });
}

function success(text) {
  return new Paragraph({
    children: [bold('Success  ', '2E7D32'), t(text)],
    spacing: { before: 140, after: 140 },
    indent: { left: 120, right: 120 },
    shading: { fill: 'E8F5E9', type: ShadingType.CLEAR },
    border: { left: { style: BorderStyle.SINGLE, size: 14, color: '2E7D32' } },
  });
}

function screenshot(label) {
  return new Paragraph({
    children: [italic(label)],
    alignment: AlignmentType.CENTER,
    spacing: { before: 120, after: 120 },
    indent: { left: 120, right: 120 },
    shading: { fill: C.shotBg, type: ShadingType.CLEAR },
    border: {
      top:    { style: BorderStyle.DASHED, size: 4, color: 'AAAAAA' },
      bottom: { style: BorderStyle.DASHED, size: 4, color: 'AAAAAA' },
      left:   { style: BorderStyle.DASHED, size: 4, color: 'AAAAAA' },
      right:  { style: BorderStyle.DASHED, size: 4, color: 'AAAAAA' },
    },
  });
}

function codeBlock(lines) {
  return lines.map((line, i) =>
    new Paragraph({
      children: [mono(line)],
      spacing: { before: i === 0 ? 100 : 0, after: 0 },
      indent: { left: 180, right: 180 },
      shading: { fill: C.codeBg, type: ShadingType.CLEAR },
      border: i === 0
        ? { top: thin, left: thin, right: thin }
        : (i === lines.length - 1 ? { bottom: thin, left: thin, right: thin } : { left: thin, right: thin }),
    })
  );
}

function formulaBox(text) {
  return new Paragraph({
    children: [mono(text)],
    alignment: AlignmentType.CENTER,
    spacing: { before: 160, after: 160 },
    indent: { left: 360, right: 360 },
    shading: { fill: 'EBF3FB', type: ShadingType.CLEAR },
    border: {
      top:    { style: BorderStyle.SINGLE, size: 6, color: C.accent },
      bottom: { style: BorderStyle.SINGLE, size: 6, color: C.accent },
      left:   { style: BorderStyle.SINGLE, size: 6, color: C.accent },
      right:  { style: BorderStyle.SINGLE, size: 6, color: C.accent },
    },
  });
}

function bullet(runs) {
  const children = typeof runs === 'string' ? [t(runs)] : runs;
  return new Paragraph({
    numbering: { reference: 'bullets', level: 0 },
    children,
    spacing: { before: 40, after: 40 },
  });
}

function num(ref, runs) {
  const children = typeof runs === 'string' ? [t(runs)] : runs;
  return new Paragraph({
    numbering: { reference: ref, level: 0 },
    children,
    spacing: { before: 40, after: 40 },
  });
}

// ── Table helpers ─────────────────────────────────────────────────────────────
function hdrCell(text, w) {
  return new TableCell({
    width: { size: w, type: WidthType.DXA },
    borders: cellBorders,
    shading: { fill: C.hdrBg, type: ShadingType.CLEAR },
    margins: { top: 80, bottom: 80, left: 120, right: 120 },
    children: [new Paragraph({
      children: [bold(text, C.hdrFg)],
      spacing: { before: 0, after: 0 },
    })],
  });
}

function dataCell(runs, w, bg) {
  const children = typeof runs === 'string' ? [t(runs, { size: 20 })] : runs;
  const cfg = {
    width: { size: w, type: WidthType.DXA },
    borders: cellBorders,
    margins: { top: 80, bottom: 80, left: 120, right: 120 },
    children: [new Paragraph({ children, spacing: { before: 0, after: 0 } })],
  };
  if (bg) cfg.shading = { fill: bg, type: ShadingType.CLEAR };
  return new TableCell(cfg);
}

function mkTable(headers, widths, rows) {
  const sum = widths.reduce((a, b) => a + b, 0);
  const hdrRow = new TableRow({
    tableHeader: true,
    children: headers.map((h, i) => hdrCell(h, widths[i])),
  });
  const dataRows = rows.map((row, ri) => {
    const bg = ri % 2 === 1 ? C.altRow : null;
    return new TableRow({
      children: row.map((cell, ci) => dataCell(cell, widths[ci], bg)),
    });
  });
  return new Table({
    width: { size: sum, type: WidthType.DXA },
    columnWidths: widths,
    rows: [hdrRow, ...dataRows],
  });
}

// ── Section builders ──────────────────────────────────────────────────────────

function buildSection1() {
  return [
    h1('1. What Is BOQ and How Does It Work?'),

    para('BOQ (Bill of Quantities) is a pre-defined list of materials and their planned quantities allocated to a specific building unit. It acts as a budget for material consumption — allowing project managers to track how much of each material has been used compared to what was planned.'),

    spacer(),
    h2('Key Concepts'),
    mkTable(
      ['Term', 'Meaning'],
      [2700, 6326],
      [
        ['Building Type', 'The category of building — e.g., Row House, Tower Block, Villa'],
        ['Building Unit', 'A specific unit within a building type — e.g., Block A, Block B, Unit 101'],
        ['Final Location', 'The specific area within a building unit where material is used — e.g., Ground Floor, Roof, Basement'],
        ['BOQ Quantity', 'The planned/budgeted quantity of a material for a building unit + location'],
        ['Outward Quantity', 'The actual quantity of material that has been issued/consumed so far'],
        ['Consumed %', '(Outward Quantity ÷ BOQ Quantity) × 100 — how much of the budget has been used'],
      ]
    ),

    spacer(),
    h2('How BOQ Enforcement Works'),

    bullet('When material is issued (outward entry), the system checks whether a BOQ entry exists for that product + building unit combination.'),
    bullet([bold('If no BOQ entry exists'), t(' — no check is performed. The outward entry is saved normally.')]),
    bullet([bold('If a BOQ entry exists'), t(' — the system calculates what the consumed % would be after the new quantity is added.')]),
    bullet('Depending on the system configuration, the outward entry is either warned or blocked when 100% consumption is reached.'),

    spacer(),
    tip('BOQ is tracked per building unit, not per project overall. The same product can have different BOQ quantities in different building units.'),
    spacer(),
    screenshot('[SCREENSHOT: BOQ Status page overview showing summary cards, progress bars, and status table]'),
  ];
}

function buildSection2() {
  return [
    h1('2. How to Add BOQ for a Single Building Unit (Excel Import)'),
    h2('Step-by-Step'),

    h3('Step 1 — Navigate to BOQ Upload'),
    para([t('Go to the side menu and click '), bold('BOQ Upload'), t('.')]),
    spacer(),
    screenshot('[SCREENSHOT: Side menu with BOQ Upload option highlighted]'),
    spacer(),

    h3('Step 2 — Select Building'),
    para([t('Under '), bold('Step 1: Select Building'), t(', choose:')]),
    bullet([bold('Building Type'), t(' — Select the type from the dropdown (e.g., Row House, Tower)')]),
    bullet([bold('Building Unit'), t(' — Select one or more units from the dropdown')]),
    warn('Both fields are required before you can download a template or upload a file.'),
    spacer(),
    screenshot('[SCREENSHOT: Step 1 — Select Building dropdowns with a building type and unit selected]'),
    spacer(),

    h3('Step 3 — Download the Template'),
    para([t('Under '), bold('Step 2: Get Template'), t(', click '), bold('Download New Template'), t('.')]),
    para('Open the downloaded file in Excel or Google Sheets. It contains the following columns:'),
    mkTable(
      ['Column', 'Description', 'How to Fill'],
      [2000, 3526, 3500],
      [
        ['Inventory', 'Name of the material/product', 'Select from the dropdown in the cell. Only products already created in the system will appear.'],
        ['Quantity', 'Planned quantity for this material', 'Enter a number (e.g., 50, 120.5)'],
        ['FinalLocation', 'The specific area within the building unit', 'Select from the dropdown in the cell. Only locations already created in the system will appear.'],
        ['Changes', 'The type of operation', 'Select from dropdown: addition, update, or deletion'],
      ]
    ),
    spacer(),
    tip('Do not type product names or location names manually. Always use the dropdown inside the Excel cell to ensure exact spelling matches what is in the system.'),
    tip([bold('Changes column:'), t('  addition'), t(' — adds a new BOQ entry (fails if it already exists).  '), t('update'), t(' — changes the quantity of an existing entry.  '), t('deletion'), t(' — removes the entry.')]),
    spacer(),
    screenshot('[SCREENSHOT: Downloaded Excel template open in Excel showing dropdown in Inventory column]'),
    spacer(),

    h3('Step 4 — Fill in the Data and Upload'),
    num('num-s2-4', 'Fill in all rows in the Excel file.'),
    num('num-s2-4', 'Save the file.'),
    num('num-s2-4', 'Return to the BOQ Upload screen.'),
    num('num-s2-4', [t('Under '), bold('Step 3: Upload & Preview'), t(', either:')]),
    bullet('Click the upload zone and select your file, or'),
    bullet('Drag and drop the file into the upload zone.'),
    num('num-s2-4', 'The file name will appear once selected.'),
    num('num-s2-4', [t('Click '), bold('Preview'), t(' to review the data before submitting.')]),
    spacer(),
    screenshot('[SCREENSHOT: Upload zone with a file selected showing the file name]'),
    spacer(),

    h3('Step 5 — Preview and Submit'),
    bullet('A preview table appears showing all rows from your file.'),
    bullet([t('The '), bold('mode badge'), t(' shows '), bold('New BOQ'), t(' (since you used the new template).')]),
    bullet('Review the data. You can click on any cell in the table to edit it directly.'),
    bullet([t('To remove a row, click the '), bold('delete icon'), t(' on the right.')]),
    bullet([t('Click '), bold('Submit BOQ'), t(' to save.')]),
    spacer(),
    success('A success message confirms the upload. Rows with errors are highlighted in red — correct and re-submit.'),
    spacer(),
    screenshot('[SCREENSHOT: Preview table showing BOQ rows with the Submit BOQ button]'),
  ];
}

function buildSection3() {
  return [
    h1('3. How to Add BOQ for Multiple Building Units Together'),

    para('You can apply the same BOQ data to multiple building units in a single upload. This is useful when several units of the same type share identical material budgets.'),

    h2('Step-by-Step'),

    num('num-s3', [t('In '), bold('Step 1: Select Building'), t(', select the '), bold('Building Type'), t('.')]),
    num('num-s3', [t('In the '), bold('Building Unit'), t(' field, select '), bold('multiple units'), t(' using the multi-select dropdown.')]),
    bullet([t('Use '), bold('Select All'), t(' to select every unit under the building type at once.')]),
    bullet([t('Use '), bold('Clear'), t(' to deselect all.')]),
    spacer(),
    screenshot('[SCREENSHOT: Building Unit dropdown with multiple units selected and Select All / Clear links visible]'),
    num('num-s3', 'Download the template (same as single unit — the template format does not change).'),
    num('num-s3', 'Fill in the BOQ data once.'),
    num('num-s3', 'Upload the file.'),
    spacer(),
    tip('The system will apply the same BOQ rows to each selected building unit automatically. For example, if you selected 5 building units and your file has 10 material rows, the system will create 50 BOQ entries (10 per unit).'),
    spacer(),
    screenshot('[SCREENSHOT: Preview table showing rows duplicated across multiple building units]'),
  ];
}

function buildSection4() {
  return [
    h1('4. How to Download Existing BOQ, Update Quantities, and Re-upload'),

    para('Use this when BOQ entries already exist and you need to revise quantities — for example, when the project scope changes mid-way.'),
    warn('You can only download the existing BOQ for one building unit at a time.'),

    h2('Step-by-Step'),

    h3('Step 1 — Select a Single Building Unit'),
    para([t('In Step 1, select the Building Type and then select '), bold('exactly one'), t(' Building Unit.')]),
    spacer(),
    screenshot('[SCREENSHOT: Building unit dropdown with exactly one unit selected]'),
    spacer(),

    h3('Step 2 — Download Existing BOQ'),
    para([t('Under '), bold('Step 2: Get Template'), t(', click '), bold('Download Existing BOQ'), t('.')]),
    para('The downloaded file contains all existing BOQ entries for that unit with their current quantities. The Changes column will already be populated.'),
    mkTable(
      ['Column', 'Description'],
      [2700, 6326],
      [
        ['Inventory', 'Product name (pre-filled)'],
        ['Quantity', 'Current BOQ quantity — edit this column to update'],
        ['FinalLocation', 'Final location (pre-filled)'],
        ['Changes', 'Pre-filled — do not modify'],
      ]
    ),
    spacer(),
    screenshot('[SCREENSHOT: Downloaded existing BOQ file open in Excel showing pre-filled data]'),
    spacer(),

    h3('Step 3 — Modify and Re-upload'),
    num('num-s4-3', [t('Change the '), bold('Quantity'), t(' values for the materials you want to update.')]),
    num('num-s4-3', 'Do not modify the Inventory or FinalLocation columns (they are used to match the existing record).'),
    num('num-s4-3', 'Save the file.'),
    num('num-s4-3', 'Return to BOQ Upload and upload the modified file.'),
    num('num-s4-3', 'Preview and Submit.'),
    spacer(),
    tip('The system uses upsert logic: if the entry already exists it is updated; if a new row is found it is inserted. No need to set the Changes column manually.'),
    tip('You can upload this modified file to multiple building units by selecting more than one unit in Step 1 — even though you downloaded from just one unit. All selected units will receive the same updated quantities.'),
    spacer(),
    screenshot('[SCREENSHOT: Preview table in Modify Existing mode showing the mode badge]'),
  ];
}

function buildSection5() {
  return [
    h1('5. Handling Different BOQ Sizes Within the Same Building Type'),

    para([bold('Scenario:'), t(' You have Row Houses. All middle units share the same BOQ. Corner units need additional materials (e.g., extra bricks, extra paint) because they have an extra wall.')]),

    h2('Recommended Approach'),

    h3('Phase 1 — Upload the common BOQ for all units'),
    num('num-s5-p1', 'Select the Building Type (Row House).'),
    num('num-s5-p1', [t('Select '), bold('all units'), t(' using Select All in the Building Unit dropdown.')]),
    num('num-s5-p1', 'Upload the base BOQ file that applies to every unit.'),
    spacer(),
    screenshot('[SCREENSHOT: All building units selected in the dropdown]'),
    spacer(),

    h3('Phase 2 — Upload additional materials only for corner units'),
    num('num-s5-p2', 'Deselect all units, then select only the corner units.'),
    num('num-s5-p2', 'Either:'),
    bullet([t('Upload a new file with just the extra materials using '), mono('addition'), t(' as the Changes value.')]),
    bullet('Or download the existing BOQ for a corner unit, add the extra rows, and re-upload.'),
    spacer(),
    tip('Because the system uses upsert logic when re-uploading existing BOQ, existing rows are not affected — only new rows are added, and modified quantity rows are updated.'),

    h3('Example'),
    mkTable(
      ['Unit', 'Material', 'Qty', 'Method'],
      [2200, 2700, 900, 3226],
      [
        ['All units', 'Cement OPC 53', '200 bags', 'Phase 1 upload (all units selected)'],
        ['All units', 'Sand', '50 cubic ft', 'Phase 1 upload (all units selected)'],
        ['Corner units only', 'Bricks (extra)', '500 nos', 'Phase 2 upload (corner units only)'],
        ['Corner units only', 'Paint (extra)', '20 litres', 'Phase 2 upload (corner units only)'],
      ]
    ),
    spacer(),
    screenshot('[SCREENSHOT: Phase 2 upload with only corner units selected in the Building Unit dropdown]'),
  ];
}

function buildSection6() {
  return [
    h1('6. How to Add or Edit a Single BOQ Entry from the Status Screen'),

    para('Instead of downloading and uploading an Excel file just to fix one value, you can add or edit a single BOQ entry directly from the BOQ Status screen.'),
    warn('This feature is available only to users with the Admin, Purchase Manager, or Project Manager role.'),

    h2('Add a New BOQ Entry'),
    num('num-s6-add', [t('Go to '), bold('BOQ Status'), t(' from the side menu.')]),
    num('num-s6-add', [t('Click the '), bold('+ Add BOQ Entry'), t(' button in the top-right area.')]),
    spacer(),
    screenshot('[SCREENSHOT: BOQ Status screen with the Add BOQ Entry button highlighted]'),
    num('num-s6-add', 'A popup form appears with the following fields:'),
    mkTable(
      ['Field', 'Description'],
      [2700, 6326],
      [
        ['Building Type', 'Select the building type from the dropdown'],
        ['Building Unit', 'Select a single building unit (loads after Building Type is selected)'],
        ['Product / Inventory', 'Select the material from the dropdown'],
        ['Final Location', 'Select the specific area/location'],
        ['BOQ Quantity', 'Enter the planned quantity'],
      ]
    ),
    num('num-s6-add', [t('Click '), bold('Save'), t('. The entry is added and the status list refreshes automatically.')]),
    spacer(),
    screenshot('[SCREENSHOT: Add BOQ Entry popup with all fields filled in]'),
    spacer(),

    h2('Edit an Existing BOQ Entry'),
    num('num-s6-edit', 'On the BOQ Status screen, find the row you want to edit.'),
    num('num-s6-edit', [t('Click the '), bold('edit icon'), t(' on the right side of the row.')]),
    spacer(),
    screenshot('[SCREENSHOT: BOQ Status table row with the edit icon highlighted]'),
    num('num-s6-edit', 'The same popup form opens, pre-filled with:'),
    bullet('The building type and unit for that row'),
    bullet('The product name'),
    bullet('If the row has only one final location, that location and its current BOQ quantity are also pre-filled'),
    num('num-s6-edit', [t('Make your changes and click '), bold('Save'), t('.')]),
    spacer(),
    tip('If a row has multiple final locations, the Final Location dropdown will be empty — select the specific location you want to update from the dropdown, enter the new quantity, and save.'),
    spacer(),
    screenshot('[SCREENSHOT: Edit BOQ popup pre-filled with existing data]'),
  ];
}

function buildSection7() {
  return [
    h1('7. How the BOQ Status Screen Works'),

    para('The BOQ Status screen gives a real-time view of material consumption vs. budget across all building units.'),

    h2('Accessing the Screen'),
    para([t('Go to the side menu and click '), bold('BOQ Status'), t('.')]),
    tip('The screen loads all data automatically. No building type or unit selection is required to see data.'),
    spacer(),
    screenshot('[SCREENSHOT: Full BOQ Status screen with summary cards at top and table below]'),
    spacer(),

    h2('Summary Cards'),
    para('At the top of the screen, four summary cards show:'),
    mkTable(
      ['Card', 'Meaning'],
      [2700, 6326],
      [
        ['Total', 'Total number of product-unit combinations tracked'],
        ['On Track', 'Items where less than 80% of BOQ is consumed (green)'],
        ['At Risk', 'Items where 80% to 100% of BOQ is consumed (amber)'],
        ['Exceeded', 'Items where more than 100% of BOQ is consumed (red)'],
      ]
    ),
    spacer(),
    para([t('Click any card to '), bold('quick-filter'), t(' the table to show only that group. Click again to remove the filter.')]),
    spacer(),
    screenshot('[SCREENSHOT: Summary cards with the At Risk card clicked and table filtered]'),
    spacer(),

    h2('Filters'),
    para([t('Use the filter panel (click the '), bold('Filter'), t(' button) to narrow down by:')]),
    bullet('Building Type'),
    bullet('Building Unit'),
    bullet('Product / Inventory'),
    bullet('Category'),
    bullet('Consumed Percentage range'),
    spacer(),
    screenshot('[SCREENSHOT: Filter panel open with a building type filter applied]'),
    spacer(),

    h2('Status Table'),
    para([t('Each row in the table represents a unique combination of '), bold('Building Unit + Product'), t('.')]),
    mkTable(
      ['Column', 'Description'],
      [2700, 6326],
      [
        ['Building Unit', 'The building unit this entry belongs to'],
        ['Category', 'Product category'],
        ['Product', 'Material name'],
        ['BOQ Qty', 'Total planned quantity across all final locations'],
        ['Outward Qty', 'Total quantity issued so far'],
        ['Status', 'Progress bar showing consumed %. Green = on track, Amber = at risk, Red = exceeded'],
      ]
    ),
    spacer(),
    para('The colour of each row also reflects its status:'),
    bullet('Light green background — On Track (less than 80% consumed)'),
    bullet('Light amber background — At Risk (80 to 100% consumed)'),
    bullet('Light red background — Exceeded (more than 100% consumed)'),
    spacer(),
    screenshot('[SCREENSHOT: Status table showing rows in different colours with progress bars]'),
    spacer(),

    h2('Downloading the Status Report'),
    para([t('Click the '), bold('Download Excel'), t(' button to export the currently filtered data. The downloaded file includes all visible rows with their BOQ quantities, outward quantities, status %, and final location breakdowns.')]),
    spacer(),
    screenshot('[SCREENSHOT: Download Excel button in the top-right of the BOQ Status screen]'),
  ];
}

function buildSection8() {
  return [
    h1('8. How Outward Inventory Entry Gets Blocked When BOQ Limit Is Reached'),

    h2('Overview'),
    para('When a material issue (outward entry) is being saved, the system automatically checks whether a BOQ limit exists for the product being issued. The check is:'),
    spacer(),
    formulaBox('(Current Outward Quantity + New Quantity Being Issued) / Total BOQ Quantity x 100'),
    para([t('If this calculated value exceeds '), bold('100%'), t(', the system either warns or blocks the save depending on the system configuration.')]),
    spacer(),

    h2('When Is the Check Triggered?'),
    para('The check runs only when all of the following are true:'),
    num('num-s8-cond', [t('A BOQ entry exists for the '), bold('same product'), t(' in the '), bold('same building unit'), t(' (usage location).')]),
    num('num-s8-cond', 'The outward entry is being created for that building unit.'),
    num('num-s8-cond', [t('The system configuration flag '), mono('boq.enforcement.block'), t(' is set to '), mono('true'), t('.')]),
    spacer(),
    warn('If no BOQ entry exists for a product + building unit combination, the check is skipped entirely. There is no restriction on how much of that product can be issued.'),
    spacer(),

    h2('What Happens When the Limit Is Exceeded?'),

    h3('If boq.enforcement.block = true (Block mode)'),
    para([bold('No line items are saved'), t(' — even if only one product in the entry exceeds its BOQ. An error message lists every product that has exceeded its limit with details:')]),
    spacer(),
    ...codeBlock([
      'BOQ limit exceeded for the following items -- save blocked:',
      '  Cement OPC 53: BOQ limit is 200.00, already consumed 190.00,',
      '  remaining 10.00 but requested 20.00',
      '  Sand: BOQ limit is 50.00, already consumed 48.00,',
      '  remaining 2.00 but requested 5.00',
    ]),
    spacer(),
    screenshot('[SCREENSHOT: Outward entry form showing the BOQ blocked error message in red]'),
    spacer(),

    h3('If boq.enforcement.block = false (Warn mode)'),
    para([t('A warning notification appears on screen when a quantity is entered that would exceed the BOQ. The save is '), bold('not blocked'), t(' — the user can proceed. This mode is useful during early project stages when BOQ figures are still being refined.')]),
    spacer(),
    screenshot('[SCREENSHOT: Outward entry form showing the yellow BOQ warning notification]'),
    spacer(),

    h2('Changing Between Block and Warn Mode'),
    para('This is a system-level configuration. Contact your system administrator to change the mode. The setting is in the server configuration file:'),
    spacer(),
    ...codeBlock([
      '# Set to true to BLOCK saves when BOQ is 100% consumed',
      '# Set to false to only warn (save proceeds)',
      'boq.enforcement.block=true',
    ]),
    spacer(),
    warn('Changing this setting requires a server restart. Plan the change outside of active working hours.'),
    spacer(),

    h2('Summary Table'),
    mkTable(
      ['Scenario', 'BOQ Entry Exists?', 'Enforcement Mode', 'Result'],
      [2800, 1800, 1900, 2526],
      [
        ['Product has no BOQ entry', 'No', 'Any', 'Save proceeds, no check'],
        ['Product has BOQ, quantity within limit', 'Yes', 'Any', 'Save proceeds'],
        ['Product has BOQ, quantity exceeds limit', 'Yes', 'Warn (false)', 'Warning shown, save proceeds'],
        ['Product has BOQ, quantity exceeds limit', 'Yes', 'Block (true)', 'Save blocked, error shown'],
        ['Multiple products — one exceeds limit', 'Yes (for that product)', 'Block (true)', 'Entire entry blocked'],
      ]
    ),
    spacer(),
    screenshot('[SCREENSHOT: Outward entry form with multiple products where one is highlighted in warning colour]'),
  ];
}

// ── Numbering config ──────────────────────────────────────────────────────────
const NUM_REFS = ['num-s2-4', 'num-s3', 'num-s4-3', 'num-s5-p1', 'num-s5-p2', 'num-s6-add', 'num-s6-edit', 'num-s8-cond'];

const numbering = {
  config: [
    {
      reference: 'bullets',
      levels: [{
        level: 0,
        format: LevelFormat.BULLET,
        text: '\u2022',
        alignment: AlignmentType.LEFT,
        style: { paragraph: { indent: { left: 720, hanging: 360 } } },
      }],
    },
    ...NUM_REFS.map(ref => ({
      reference: ref,
      levels: [{
        level: 0,
        format: LevelFormat.DECIMAL,
        text: '%1.',
        alignment: AlignmentType.LEFT,
        style: { paragraph: { indent: { left: 720, hanging: 360 } } },
      }],
    })),
  ],
};

// ── Styles ────────────────────────────────────────────────────────────────────
const styles = {
  default: {
    document: { run: { font: 'Arial', size: 22 } },
  },
  paragraphStyles: [
    {
      id: 'Heading1', name: 'Heading 1', basedOn: 'Normal', next: 'Normal', quickFormat: true,
      run: { size: 32, bold: true, font: 'Arial', color: C.h1 },
      paragraph: { spacing: { before: 360, after: 120 }, outlineLevel: 0 },
    },
    {
      id: 'Heading2', name: 'Heading 2', basedOn: 'Normal', next: 'Normal', quickFormat: true,
      run: { size: 26, bold: true, font: 'Arial', color: C.h2 },
      paragraph: { spacing: { before: 240, after: 80 }, outlineLevel: 1 },
    },
    {
      id: 'Heading3', name: 'Heading 3', basedOn: 'Normal', next: 'Normal', quickFormat: true,
      run: { size: 22, bold: true, font: 'Arial', color: C.h3 },
      paragraph: { spacing: { before: 180, after: 60 }, outlineLevel: 2 },
    },
  ],
};

// ── Shared header/footer builders ─────────────────────────────────────────────
function makeHeader() {
  return new Header({
    children: [
      new Paragraph({
        children: [t('BOQ Help Guide  |  Real Estate ERP/CRM', { color: '777777', size: 18 })],
        border: { bottom: { style: BorderStyle.SINGLE, size: 4, color: C.accent, space: 1 } },
      }),
    ],
  });
}

function makeFooter() {
  return new Footer({
    children: [
      new Paragraph({
        children: [
          t('Version 1.0  |  April 2026    ', { color: '999999', size: 18 }),
          new TextRun({ children: [PageNumber.CURRENT], color: '999999', size: 18 }),
          t('  /  ', { color: '999999', size: 18 }),
          new TextRun({ children: [PageNumber.TOTAL_PAGES], color: '999999', size: 18 }),
        ],
        alignment: AlignmentType.RIGHT,
      }),
    ],
  });
}

// ── Document sections ─────────────────────────────────────────────────────────

// Title page — no header/footer
const titleSection = {
  properties: {
    page: {
      size: { width: 11906, height: 16838 },
      margin: { top: MARGIN, right: MARGIN, bottom: MARGIN, left: MARGIN },
    },
  },
  children: [
    new Paragraph({ children: [t('')], spacing: { before: 3600 } }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { before: 0, after: 200 },
      children: [t('BOQ (Bill of Quantities)', { font: 'Arial', size: 60, bold: true, color: C.h1 })],
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { before: 0, after: 400 },
      children: [t('Help Guide', { font: 'Arial', size: 48, bold: true, color: C.h2 })],
    }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      spacing: { before: 0, after: 600 },
      border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: C.accent, space: 6 } },
      children: [t('Real Estate ERP/CRM System', { font: 'Arial', size: 28, color: '555555' })],
    }),
    new Paragraph({ children: [t('')], spacing: { before: 800 } }),
    new Paragraph({
      alignment: AlignmentType.CENTER,
      children: [t('Version 1.0  |  April 2026', { font: 'Arial', size: 20, color: '999999' })],
    }),
  ],
};

// TOC page
const tocSection = {
  properties: {
    page: {
      size: { width: 11906, height: 16838 },
      margin: { top: MARGIN, right: MARGIN, bottom: MARGIN, left: MARGIN },
    },
  },
  headers: { default: makeHeader() },
  footers: { default: makeFooter() },
  children: [
    new TableOfContents('Table of Contents', { hyperlink: true, headingStyleRange: '1-3' }),
    new Paragraph({ children: [new PageBreak()] }),
  ],
};

// Main content section
const contentSection = {
  properties: {
    page: {
      size: { width: 11906, height: 16838 },
      margin: { top: MARGIN, right: MARGIN, bottom: MARGIN, left: MARGIN },
    },
  },
  headers: { default: makeHeader() },
  footers: { default: makeFooter() },
  children: [
    ...buildSection1(),
    new Paragraph({ children: [new PageBreak()] }),
    ...buildSection2(),
    new Paragraph({ children: [new PageBreak()] }),
    ...buildSection3(),
    new Paragraph({ children: [new PageBreak()] }),
    ...buildSection4(),
    new Paragraph({ children: [new PageBreak()] }),
    ...buildSection5(),
    new Paragraph({ children: [new PageBreak()] }),
    ...buildSection6(),
    new Paragraph({ children: [new PageBreak()] }),
    ...buildSection7(),
    new Paragraph({ children: [new PageBreak()] }),
    ...buildSection8(),
  ],
};

// ── Generate ──────────────────────────────────────────────────────────────────
const doc = new Document({
  numbering,
  styles,
  sections: [titleSection, tocSection, contentSection],
});

const OUT = '/Users/bsridharpatnaik/GitHub/RealEstate-ERP-CRM-System/BOQ_Help_Guide.docx';

Packer.toBuffer(doc)
  .then(buf => {
    fs.writeFileSync(OUT, buf);
    console.log('Generated: ' + OUT);
  })
  .catch(err => {
    console.error('Error:', err.message || err);
    process.exit(1);
  });
