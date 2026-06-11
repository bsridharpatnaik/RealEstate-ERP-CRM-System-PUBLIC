import React, { Component } from "react";
import { connect } from "react-redux";

const COLORS = {
  primary: "#1a3557",
  accent: "#2563a8",
  border: "#c8d6e5",
  rowEven: "#f4f7fb",
  headerBg: "#1a3557",
  headerText: "#ffffff",
  text: "#1c2a3a",
  muted: "#5a7089",
  statusBg: "#e8f0fb",
  statusText: "#1a3557",
};

const S = {
  page: {
    fontFamily: '"Helvetica Neue", Arial, sans-serif',
    color: COLORS.text,
    background: "#ffffff",
    width: "100%",
    padding: "24px 28px",
    boxSizing: "border-box",
  },
  header: {
    display: "grid",
    gridTemplateColumns: "110px 1fr auto",
    alignItems: "start",
    gap: "20px",
  },
  logoImg: {
    width: "100px",
    height: "auto",
    maxHeight: "68px",
    objectFit: "contain",
    display: "block",
  },
  titlesBox: { display: "flex", flexDirection: "column", gap: "2px" },
  appName: { fontSize: "10px", fontWeight: "600", letterSpacing: "0.12em", textTransform: "uppercase", color: COLORS.muted },
  tenantName: { fontSize: "18px", fontWeight: "700", color: COLORS.primary, lineHeight: "1.2", marginTop: "4px" },
  docType: { fontSize: "10px", fontWeight: "700", letterSpacing: "0.2em", textTransform: "uppercase", color: COLORS.accent, marginTop: "8px", borderTop: `2px solid ${COLORS.accent}`, paddingTop: "4px", display: "inline-block" },
  metaBox: { display: "flex", flexDirection: "column", gap: "4px", minWidth: "210px" },
  metaRow: { display: "flex", justifyContent: "space-between", alignItems: "baseline", gap: "8px", borderBottom: `1px dotted ${COLORS.border}`, paddingBottom: "3px" },
  metaRowLast: { display: "flex", justifyContent: "space-between", alignItems: "baseline", gap: "8px" },
  metaLabel: { fontSize: "9px", fontWeight: "600", letterSpacing: "0.08em", textTransform: "uppercase", color: COLORS.muted, whiteSpace: "nowrap" },
  metaValue: { fontSize: "10px", fontWeight: "600", color: COLORS.text, textAlign: "right" },
  metaValueStatus: { fontSize: "9px", fontWeight: "700", color: COLORS.statusText, background: COLORS.statusBg, padding: "1px 7px", borderRadius: "3px", letterSpacing: "0.06em", textTransform: "uppercase", textAlign: "right" },
  divider: { height: "2px", background: `linear-gradient(to right, ${COLORS.primary}, ${COLORS.accent}, ${COLORS.border})`, margin: "14px 0 16px", borderRadius: "1px" },
  table: { width: "100%", borderCollapse: "collapse", tableLayout: "auto", fontSize: "9px", border: `1.5px solid ${COLORS.primary}` },
  th: { background: COLORS.headerBg, color: COLORS.headerText, fontSize: "8px", fontWeight: "700", letterSpacing: "0.07em", textTransform: "uppercase", padding: "7px 5px", textAlign: "center", border: "1px solid rgba(255,255,255,0.15)", whiteSpace: "nowrap", verticalAlign: "middle" },
  tdBase: { padding: "5px", border: `1px solid ${COLORS.border}`, color: COLORS.text, verticalAlign: "top", wordBreak: "break-word", lineHeight: "1.4", fontSize: "9px" },
  statusBadge: { display: "inline-block", padding: "2px 6px", borderRadius: "3px", background: COLORS.statusBg, color: COLORS.statusText, fontSize: "8px", fontWeight: "700", letterSpacing: "0.06em", textTransform: "uppercase", whiteSpace: "nowrap" },
  footer: { display: "flex", justifyContent: "space-between", alignItems: "flex-end", marginTop: "32px", paddingTop: "16px", borderTop: `1.5px solid ${COLORS.border}`, gap: "20px" },
  footerSig: { flex: "1", textAlign: "center" },
  footerLine: { width: "80%", height: "1px", background: COLORS.text, margin: "0 auto 6px" },
  footerLabel: { fontSize: "9px", fontWeight: "600", letterSpacing: "0.1em", textTransform: "uppercase", color: COLORS.muted },
  footerName: { fontSize: "9px", fontWeight: "600", color: COLORS.text, marginTop: "2px" },
};

class Print extends Component {
  getColumnConfig() {
    const status = (this.props.data?.status || "").toLowerCase().trim();
    const showPOColumn = ["po partial","po completed","inward partial","closed"].includes(status);
    const showReceivedPendingColumns = ["inward partial","closed"].includes(status);
    return { showPOColumn, showReceivedPendingColumns };
  }

  getTenantName() {
    return this.props.data?.tenant || "";
  }

  // Called externally by details.js to trigger print
  handlePrint() {
    const data = this.props.data;
    const items = data.inventoryItems || data.inventoryList || [];
    const { showPOColumn, showReceivedPendingColumns } = this.getColumnConfig();
    const tenantName = this.getTenantName();

    const metaItems = [
      { label: "Indent ID", value: data.indentId || "—" },
      { label: "Indent Date", value: data.indentDate || "—" },
      { label: "Status", value: data.status || "—", isStatus: true },
      { label: "Created By", value: data.createdBy || "—" },
      ...(data.poNumber ? [{ label: "PO Number", value: data.poNumber }] : []),
      ...(data.poDate ? [{ label: "PO Date", value: data.poDate }] : []),
      { label: "Item Count", value: data.inventoryCount ?? 0 },
    ];

    const tableRows = items.map((item, index) => {
      const rowBg = index % 2 === 0 ? COLORS.rowEven : "#ffffff";
      const td = `padding:5px;border:1px solid ${COLORS.border};color:${COLORS.text};vertical-align:top;word-break:break-word;line-height:1.4;font-size:9px;background:${rowBg};`;
      const tdC = td + "text-align:center;";
      const tdM = td + `font-family:'Courier New',monospace;font-size:8.5px;color:${COLORS.muted};text-align:center;`;
      return `
        <tr>
          <td style="${tdC}font-weight:700;color:${COLORS.muted};width:28px;">${index + 1}</td>
          <td style="${td}">${item.product?.productName || "—"}</td>
          <td style="${tdM}">${item.product?.productCode || "—"}</td>
          <td style="${tdC}">${item.quantity || "—"}</td>
          <td style="${tdC}color:${COLORS.muted};">${item.measurementUnit || "—"}</td>
          <td style="${td}">${item.specification || "—"}</td>
          <td style="${td}">${item.remarks || item.remark || "—"}</td>
          ${showPOColumn ? `<td style="${tdM}">${item.purchaseOrderId || "—"}</td>` : ""}
          <td style="${tdM}">${item.lineItemCode || "—"}</td>
          ${showReceivedPendingColumns ? `
            <td style="${tdC}">${item.quantityReceived ?? "—"}</td>
            <td style="${tdC}">${item.quantityPending ?? "—"}</td>
          ` : ""}
          <td style="${tdC}"><span style="display:inline-block;padding:2px 6px;border-radius:3px;background:${COLORS.statusBg};color:${COLORS.statusText};font-size:8px;font-weight:700;letter-spacing:0.06em;text-transform:uppercase;white-space:nowrap;">${item.lineItemStatus || "—"}</span></td>
        </tr>`;
    }).join("");

    const th = `style="background:${COLORS.headerBg};color:${COLORS.headerText};font-size:8px;font-weight:700;letter-spacing:0.07em;text-transform:uppercase;padding:7px 5px;text-align:center;border:1px solid rgba(255,255,255,0.15);white-space:nowrap;vertical-align:middle;-webkit-print-color-adjust:exact;print-color-adjust:exact;"`;
    const thL = `style="background:${COLORS.headerBg};color:${COLORS.headerText};font-size:8px;font-weight:700;letter-spacing:0.07em;text-transform:uppercase;padding:7px 5px;text-align:left;border:1px solid rgba(255,255,255,0.15);white-space:nowrap;vertical-align:middle;-webkit-print-color-adjust:exact;print-color-adjust:exact;"`;

    const logoSrc = `${window.location.origin}${process.env.PUBLIC_URL}/${process.env.REACT_APP_LOGIN_LOGO}.png`;
    const appName = process.env.REACT_APP_NAME || "";

    const html = `<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8"/>
  <title>Indent - ${data.indentId || ""}</title>
  <style>
    @page { margin: 12mm 10mm; }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: "Helvetica Neue", Arial, sans-serif; color: ${COLORS.text}; background: #fff; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
  </style>
</head>
<body>
<div style="width:100%;padding:24px 28px;">

  <!-- Header -->
  <div style="display:grid;grid-template-columns:110px 1fr auto;align-items:start;gap:20px;">
    <div>
      <img src="${logoSrc}" alt="Logo" style="width:100px;height:auto;max-height:68px;object-fit:contain;display:block;" onerror="this.style.display='none'"/>
    </div>
    <div style="display:flex;flex-direction:column;gap:2px;">
      <div style="font-size:10px;font-weight:600;letter-spacing:0.12em;text-transform:uppercase;color:${COLORS.muted};">${appName}</div>
      <div style="font-size:18px;font-weight:700;color:${COLORS.primary};line-height:1.2;margin-top:4px;">${tenantName}</div>
      <div style="font-size:10px;font-weight:700;letter-spacing:0.2em;text-transform:uppercase;color:${COLORS.accent};margin-top:8px;border-top:2px solid ${COLORS.accent};padding-top:4px;display:inline-block;">INDENT</div>
    </div>
    <div style="display:flex;flex-direction:column;gap:4px;min-width:210px;">
      ${metaItems.map((item, i) => `
        <div style="display:flex;justify-content:space-between;align-items:baseline;gap:8px;${i < metaItems.length - 1 ? `border-bottom:1px dotted ${COLORS.border};padding-bottom:3px;` : ""}">
          <span style="font-size:9px;font-weight:600;letter-spacing:0.08em;text-transform:uppercase;color:${COLORS.muted};white-space:nowrap;">${item.label}</span>
          <span style="${item.isStatus
            ? `font-size:9px;font-weight:700;color:${COLORS.statusText};background:${COLORS.statusBg};padding:1px 7px;border-radius:3px;letter-spacing:0.06em;text-transform:uppercase;`
            : `font-size:10px;font-weight:600;color:${COLORS.text};`}text-align:right;">${item.value}</span>
        </div>`).join("")}
    </div>
  </div>

  <!-- Divider -->
  <div style="height:2px;background:linear-gradient(to right,${COLORS.primary},${COLORS.accent},${COLORS.border});margin:14px 0 16px;border-radius:1px;"></div>

  <!-- Table -->
  <table style="width:100%;border-collapse:collapse;table-layout:auto;font-size:9px;border:1.5px solid ${COLORS.primary};">
    <thead>
      <tr>
        <th ${th}>SR.</th>
        <th ${thL}>DESCRIPTION</th>
        <th ${th}>CODE</th>
        <th ${th}>QTY</th>
        <th ${th}>UOM</th>
        <th ${thL}>SPECIFICATION</th>
        <th ${thL}>REMARKS</th>
        ${showPOColumn ? `<th ${th}>PO NUMBER</th>` : ""}
        <th ${th}>LINE ITEM CODE</th>
        ${showReceivedPendingColumns ? `<th ${th}>RECEIVED QTY</th><th ${th}>PENDING QTY</th>` : ""}
        <th ${th}>STATUS</th>
      </tr>
    </thead>
    <tbody>${tableRows}</tbody>
  </table>

  <!-- Footer -->
  <div style="display:flex;justify-content:space-between;align-items:flex-end;margin-top:32px;padding-top:16px;border-top:1.5px solid ${COLORS.border};gap:20px;">
    <div style="flex:1;text-align:center;">
      <div style="width:80%;height:1px;background:${COLORS.text};margin:0 auto 6px;"></div>
      <div style="font-size:9px;font-weight:600;letter-spacing:0.1em;text-transform:uppercase;color:${COLORS.muted};">Prepared By</div>
      <div style="font-size:9px;font-weight:600;color:${COLORS.text};margin-top:2px;">${data.createdBy || ""}</div>
    </div>
    <div style="flex:1;text-align:center;">
      <div style="width:80%;height:1px;background:${COLORS.text};margin:0 auto 6px;"></div>
      <div style="font-size:9px;font-weight:600;letter-spacing:0.1em;text-transform:uppercase;color:${COLORS.muted};">Authorised Signatory</div>
    </div>
  </div>

</div>
</body>
</html>`;

    const printWindow = window.open("", "_blank", "width=1200,height=800");
    printWindow.document.write(html);
    printWindow.document.close();
    printWindow.focus();
    // Wait for images to load before printing
    printWindow.onload = () => {
      printWindow.print();
      printWindow.close();
    };
    // Fallback if onload doesn't fire (some browsers)
    setTimeout(() => {
      try { printWindow.print(); printWindow.close(); } catch(e) {}
    }, 1000);
  }

  render() {
    // This component renders nothing visible — printing is done via handlePrint()
    return null;
  }
}

const mapStateToProps = () => ({});

export default connect(mapStateToProps, null, null, {
  forwardRef: true,
})(Print);