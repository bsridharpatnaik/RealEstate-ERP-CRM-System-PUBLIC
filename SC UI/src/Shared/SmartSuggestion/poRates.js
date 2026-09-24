// Create PO step 2: compares each line with earlier purchase rates (no AI). Returns markdown, or null when
// nothing is known yet (rates still loading, or the user's role can't read previous rates).

const inr = (v) => Number(v).toLocaleString("en-IN", { maximumFractionDigits: 2 });
const capitalize = (s) => s.charAt(0).toUpperCase() + s.slice(1);
const parseDmy = (s) => {
  const [d, m, y] = String(s || "").split("-").map(Number);
  return y ? new Date(y, m - 1, d) : null;
};
const monthsBetween = (from, to) => (to.getFullYear() - from.getFullYear()) * 12 + (to.getMonth() - from.getMonth());

// "**6.5% above the last price**" / "the same as the last price" (within 0.5%)
const compare = (rate, ref, label) => {
  const diff = ((rate - ref) / ref) * 100;
  if (Math.abs(diff) < 0.5) return `the same as ${label}`;
  return `**${inr(Math.abs(diff).toFixed(1))}% ${diff > 0 ? "above" : "below"} ${label}**`;
};

const qtyUnit = (q, unit) => `${inr(q)}${unit ? " " + unit : ""}`;
const openPoRef = (o) => `${qtyUnit(o.pending, o.unit)} on ${o.po} for **${o.project}** (${o.supplier ? o.supplier + ", " : ""}raised ${o.date}, ${o.received > 0 ? "partly received" : "nothing received yet"})`;

// Open POs for the same item at any project (newest first): the total still to arrive, and the newest 3.
const alsoOnOrder = (pos) => {
  if (pos.length === 1) return `Also on order: ${openPoRef(pos[0])}.`;
  const total = qtyUnit(pos.reduce((sum, o) => sum + Number(o.pending), 0), pos[0].unit);
  const shown = pos.slice(0, 3).map(openPoRef);
  const list = shown.length === 2 ? shown.join(" and ") : shown.slice(0, -1).join(", ") + " and " + shown[shown.length - 1];
  return pos.length <= 3
    ? `Also on order: ${total} across ${pos.length} open POs — ${list}.`
    : `Also on order: ${total} across ${pos.length} open POs. Newest: ${list}.`;
};

// ratesCache: productId -> previous rates, newest first (null = could not load). poDate is dd-MM-yyyy.
// openPos: open POs for these items at any project, newest first (null = not loaded / no access).
export const poRateSuggestion = (items, ratesCache, supplierId, openPos = null, today = new Date()) => {
  const known = items.filter((it) => Array.isArray(ratesCache[it.productId]));
  if (known.length === 0) return null;
  const supplier = supplierId == null ? null : Number(supplierId);
  const cutoff = new Date(today.getFullYear(), today.getMonth() - 6, today.getDate());
  let compared = 0;
  let above = 0;

  const bullets = known.map((it) => {
    const history = ratesCache[it.productId].filter((r) => r.rate != null);
    const name = `**${it.inventoryName}**`;
    const onOrder = (openPos || []).filter((o) => String(o.productId) === String(it.productId));
    const orderNote = onOrder.length ? " " + alsoOnOrder(onOrder) : "";
    if (history.length === 0) return `- ${name} — First purchase; no earlier rate to compare.${orderNote}`;

    const last = history[0];
    const parts = [`last bought at ₹${inr(last.rate)} from ${last.supplierName} on ${last.poDate} (${last.purchaseOrderId}).`];
    const lastDate = parseDmy(last.poDate);
    const age = lastDate ? monthsBetween(lastDate, today) : 0;
    if (age >= 6) parts.push(`That was ${age} months ago, so the price may be out of date.`);

    const recent = history.filter((r) => { const d = parseDmy(r.poDate); return d && d >= cutoff; });
    const low = recent.reduce((min, r) => (!min || r.rate < min.rate ? r : min), null);
    const high = recent.reduce((max, r) => Math.max(max, r.rate), 0);
    if (recent.length >= 2) {
      parts.push(low.rate === high
        ? `Last 6 months: ${recent.length} POs, all at ₹${inr(high)}.`
        : `Last 6 months: ${recent.length} POs, ₹${inr(low.rate)} – ₹${inr(high)}` +
          (low.rate < last.rate ? `; **lowest ₹${inr(low.rate)} from ${low.supplierName}** on ${low.poDate}.` : "."));
    }

    const rate = parseFloat(it.rate);
    if (rate > 0) {
      compared++;
      const best = low && low.rate < last.rate ? low.rate : last.rate;   // lowest recent price we know of
      if (((rate - best) / best) * 100 >= 0.5) above++;
      parts.push(`Your rate ₹${inr(rate)} is ${compare(rate, last.rate, "the last price")}` +
        (best < last.rate ? ` and ${compare(rate, best, "the 6-month low")}` : "") + ".");
    }

    const own = supplier != null && history.find((r) => Number(r.supplierId) === supplier);
    if (own && own !== last) parts.push(`This supplier last charged ₹${inr(own.rate)} on ${own.poDate}.`);
    return `- ${name} — ${capitalize(parts.join(" "))}${orderNote}`;
  });

  let headline;
  if (compared === 0) headline = "Previous prices for the items on this PO.";
  else if (above === 0) headline = compared === known.length
    ? "All rates are at or below the lowest recent price."
    : "Rates entered so far are at or below the lowest recent price.";
  else if (compared === 1) headline = "The rate entered is above the lowest recent price.";
  else headline = `${above} of ${compared} rates ${above === 1 ? "is" : "are"} above the lowest recent price.`;

  return `**${headline}**\n\n${bullets.join("\n")}`;
};
