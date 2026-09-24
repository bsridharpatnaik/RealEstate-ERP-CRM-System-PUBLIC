import { poRateSuggestion } from "./poRates";

const TODAY = new Date(2026, 8, 24); // 24-Sep-2026
const R = (po, date, sup, id, rate) => ({ purchaseOrderId: po, poDate: date, supplierName: sup, supplierId: id, rate });
const rates = {
  1: [
    R("PO-120", "12-09-2026", "Shree Traders", 7, 385),
    R("PO-98", "02-08-2026", "ABC Cement", 8, 370),
    R("PO-71", "01-07-2026", "Mehta & Sons", 9, 380),
    R("PO-20", "10-01-2026", "Old Supplier", 10, 300), // older than 6 months: ignored for the range
  ],
  2: [],
  3: null, // could not load
  4: [R("PO-5", "15-07-2025", "Shree Traders", 7, 50)],
};

test("last price, 6-month range and lowest, entered rate against both", () => {
  const text = poRateSuggestion(
    [
      { productId: 1, inventoryName: "Cement", rate: "410" },
      { productId: 2, inventoryName: "Sand", rate: "" },
      { productId: 3, inventoryName: "Bricks", rate: "8" },
    ],
    rates, 9, null, TODAY
  );
  expect(text).toContain("**The rate entered is above the lowest recent price.**");
  expect(text).toContain("- **Cement** — Last bought at ₹385 from Shree Traders on 12-09-2026 (PO-120).");
  expect(text).toContain("Last 6 months: 3 POs, ₹370 – ₹385; **lowest ₹370 from ABC Cement** on 02-08-2026.");
  expect(text).toContain("Your rate ₹410 is **6.5% above the last price** and **10.8% above the 6-month low**.");
  expect(text).toContain("This supplier last charged ₹380 on 01-07-2026.");
  expect(text).toContain("- **Sand** — First purchase; no earlier rate to compare.");
  expect(text).not.toContain("Bricks");
  expect(text).not.toContain("₹300");
});

test("old last price is called out", () => {
  const text = poRateSuggestion([{ productId: 4, inventoryName: "Nails", rate: "50" }], rates, 7, null, TODAY);
  expect(text).toContain("That was 14 months ago, so the price may be out of date.");
  expect(text).toContain("Your rate ₹50 is the same as the last price.");
  expect(text).toContain("**All rates are at or below the lowest recent price.**");
});

test("hidden until something is known", () => {
  expect(poRateSuggestion([{ productId: 3, inventoryName: "Bricks" }], rates, null, null, TODAY)).toBeNull();
  expect(poRateSuggestion([{ productId: 4, inventoryName: "Steel" }], {}, null, null, TODAY)).toBeNull();
});

test("open POs at other projects, project from the PO's indent", () => {
  const O = (po, project, pending, received, date) => ({ productId: 1, po, project, supplier: "Sai Bricks", date, unit: "Bags", pending, received });
  const one = poRateSuggestion([{ productId: 1, inventoryName: "Cement", rate: "" }], rates, 7, [O("PO-201", "BHAAV-BHUMI", 100, 0, "20-09-2026")], TODAY);
  expect(one).toContain("Also on order: 100 Bags on PO-201 for **BHAAV-BHUMI** (Sai Bricks, raised 20-09-2026, nothing received yet).");
  const many = poRateSuggestion([{ productId: 1, inventoryName: "Cement", rate: "" }], rates, 7,
    [O("PO-205", "A", 10, 5, "22-09-2026"), O("PO-204", "B", 20, 0, "21-09-2026"), O("PO-203", "C", 30, 0, "20-09-2026"), O("PO-202", "D", 40, 0, "19-09-2026")], TODAY);
  expect(many).toContain("Also on order: 100 Bags across 4 open POs. Newest: 10 Bags on PO-205 for **A**");
  expect(many).toContain("and 30 Bags on PO-203 for **C**");
  expect(many).not.toContain("PO-202");
});
