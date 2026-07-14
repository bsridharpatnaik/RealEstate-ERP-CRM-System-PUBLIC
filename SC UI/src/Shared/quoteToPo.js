// Shared helpers for turning a Quote Comparison's finalized lines into Purchase Order line items.
// Used by QuoteComparison/details.js ("Create PO" button) and
// PurchaseOrder/add/LoadFromQuoteDialog.js ("Load from Quote" picker) so both stay in sync.

// Groups finalized-but-not-yet-PO'd matrix rows by their winning supplier quote — a PO has
// exactly one supplier, so each group becomes one PO.
export function getFinalizedPoGroups(matrix) {
  const groups = {};
  (matrix || []).forEach(row => {
    if (row.lineStatus !== "FINALIZED") return;
    const resp = (row.supplierResponses || []).find(sr => sr.supplierQuoteLineId === row.finalizedSupplierQuoteLineId);
    if (!resp) return;
    const key = resp.supplierQuoteId;
    if (!groups[key]) {
      groups[key] = {
        supplierQuoteId: resp.supplierQuoteId,
        supplierId: resp.supplierId,
        supplierName: resp.supplierName,
        revisionLabel: resp.revisionLabel,
        lines: [],
      };
    }
    groups[key].lines.push({ row, resp });
  });
  return Object.values(groups);
}

// Builds PO "Add Purchase Order" items (the shape add.js's state.items expects) from a group
// produced by getFinalizedPoGroups, plus the original QuoteComparisonLine[] (for indent refs).
export function buildPoItemsFromGroup(group, qcLines, qcId) {
  return group.lines.map(({ row, resp }) => {
    const qcLine = (qcLines || []).find(l => l.id === row.lineId);
    const rate = parseFloat(resp.quotedRate) || 0;
    const qty = parseFloat(row.requiredQty) || 0;
    const discount = parseFloat(resp.discountPercent) || 0;
    const gst = parseFloat(resp.gstPercent) || 0;
    const discountedRate = rate - (rate * discount / 100);
    const netRate = discountedRate * qty;
    const totalAmt = netRate + (netRate * gst / 100);
    return {
      productId: row.productId,
      inventoryName: row.productName,
      unit: row.unit,
      quantity: String(qty),
      rate: String(rate),
      gst: String(gst),
      discount: String(discount),
      tolerance: "0",
      brandName: "",
      grade: "",
      diameter: "",
      specification: row.specifications || "",
      netRate: netRate.toFixed(2),
      totalAmt: totalAmt.toFixed(2),
      sampleImageFileId: null,
      indentId: qcLine?.indentLineId || null,
      _linkedQcLineId: row.lineId,
      _linkedSupplierQuoteLineId: resp.supplierQuoteLineId,
      _linkedQcId: qcId,
    };
  });
}

export function buildQuotePrefill(group, qcLines, qcId, project) {
  // A QuoteComparison's project field can be a comma-joined list when its indents came from
  // multiple projects (auto-filled at creation time). That combined string isn't a real PO
  // project, so only carry it forward when it's a single, unambiguous value.
  const singleProject = project && !project.includes(",") ? project : "";
  return {
    items: buildPoItemsFromGroup(group, qcLines, qcId),
    orderTo: group.supplierId ? { id: group.supplierId, name: group.supplierName } : null,
    projectName: singleProject,
  };
}
