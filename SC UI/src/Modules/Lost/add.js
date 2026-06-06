//react
import React from "react";
import { connect } from "react-redux";

//third party
import { withSnackbar } from "notistack";
import AddForm from "./../../Shared/AddForm";
import { API } from "./../../axios";
import FormControl from "@material-ui/core/FormControl";
import RadioGroup from "@material-ui/core/RadioGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import Radio from "@material-ui/core/Radio";

//misc
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";
import { fetchUnit } from "./../../actions/measurementUnit";
import moment from "moment";

// ── date helpers (expiryDate arrives as "dd-MM-yyyy") ──────────────────────
function parseDDMMYYYY(s) {
  if (!s) return null;
  const [d, m, y] = s.split("-");
  return new Date(Number(y), Number(m) - 1, Number(d));
}
function isExpiredDate(s) {
  const d = parseDDMMYYYY(s);
  return d ? d < new Date() : false;
}
function isNearExpiryDate(s) {
  const d = parseDDMMYYYY(s);
  if (!d) return false;
  const now = new Date();
  const limit = new Date();
  limit.setDate(now.getDate() + 30);
  return d >= now && d <= limit;
}

class Add extends AddForm {
  title = messages.common.lost;
  addurl = apiEndpoints.createLost;
  state = {
    closing: null,
    currentStockValue: null,   // stock BEFORE this entry (for display/validation)
    entryType: "LOST_DAMAGED",
    availableBatches: [],
    existingBatchMode: false,
    selectedProductBatchMode: null, // 'BATCH_ONLY' | 'BATCH_WITH_EXPIRY' | null
    // multi-batch qty maps: batchId (number) → qty string
    batchQtyMap: {},    // LOST_DAMAGED allocation
    excessQtyMap: {},   // EXCESS_FOUND add-to-existing allocation
  };

  constructor(props) {
    super(props);
    this.formData = this.formData || {};
    this.formData.entryType = "LOST_DAMAGED";
  }

  componentDidMount() {
    const { dispatch } = this.props;
    dispatch(fetchUnit());
  }

  async getCurrentStock() {
    const warehouseId = this.formData.warehouseId;
    const productId = this.formData.productId;
    if (!productId || !warehouseId) return;
    const response = await API.GET(
      apiEndpoints.getCurrentStock +
        "productId=" + productId +
        "&warehouseId=" + warehouseId
    );
    if (response.success) {
      const qty = Number(this.formData.quantity) || 0;
      const currentStock = Number(response.data);
      const closing =
        this.formData.entryType === "EXCESS_FOUND"
          ? currentStock + qty
          : currentStock - qty;
      this.setState({ closing, currentStockValue: currentStock });
    }
  }

  async loadBatches() {
    const { productId, warehouseId } = this.formData;
    if (!productId || !warehouseId) {
      this.setState({
        availableBatches: [],
        batchQtyMap: {},
        excessQtyMap: {},
      });
      return;
    }
    try {
      const response = await API.GET(
        apiEndpoints.getBatchesForProduct(productId, warehouseId)
      );
      if (response.success && Array.isArray(response.data)) {
        const batches = response.data.filter((b) => b.qtyRemaining > 0);
        this.setState({
          availableBatches: batches,
          batchQtyMap: {},
          excessQtyMap: {},
        });
        this.formData.batchId = null;
        this.formData.batchEntries = null;
        this.formData.excessBatchEntries = null;
      } else {
        this.setState({ availableBatches: [], batchQtyMap: {}, excessQtyMap: {} });
      }
    } catch (e) {
      this.setState({ availableBatches: [], batchQtyMap: {}, excessQtyMap: {} });
    }
  }

  add(event) {
    event.preventDefault();
    if (
      !this.formData.fileInformations ||
      this.formData.fileInformations.length === 0
    ) {
      this.props.enqueueSnackbar("Add atleast one file", { variant: "error" });
      return;
    }

    const { entryType, availableBatches, batchQtyMap, excessQtyMap, existingBatchMode } = this.state;
    const totalQty = Number(this.formData.quantity) || 0;

    if (availableBatches.length > 0) {
      if (entryType === "LOST_DAMAGED") {
        const allocated = Object.values(batchQtyMap).reduce(
          (s, v) => s + (Number(v) || 0), 0
        );
        if (Math.abs(allocated - totalQty) > 0.001) {
          this.props.enqueueSnackbar(
            `Batch quantities must sum to ${totalQty} (currently ${Math.round(allocated * 1000) / 1000})`,
            { variant: "error" }
          );
          return;
        }
        const entries = availableBatches
          .filter((b) => (Number(batchQtyMap[b.batchId]) || 0) > 0)
          .map((b) => ({ batchId: b.batchId, qty: Number(batchQtyMap[b.batchId]) }));
        this.formData.batchEntries = entries.length > 0 ? entries : null;
        this.formData.batchId = null;
      } else if (entryType === "EXCESS_FOUND" && existingBatchMode) {
        const allocated = Object.values(excessQtyMap).reduce(
          (s, v) => s + (Number(v) || 0), 0
        );
        if (Math.abs(allocated - totalQty) > 0.001) {
          this.props.enqueueSnackbar(
            `Batch quantities must sum to ${totalQty} (currently ${Math.round(allocated * 1000) / 1000})`,
            { variant: "error" }
          );
          return;
        }
        const entries = availableBatches
          .filter((b) => (Number(excessQtyMap[b.batchId]) || 0) > 0)
          .map((b) => ({ batchId: b.batchId, qty: Number(excessQtyMap[b.batchId]) }));
        this.formData.excessBatchEntries = entries.length > 0 ? entries : null;
        this.formData.existingBatchId = null;
      }
    }

    super.add(event);
  }

  // ── Batch allocation cards — each batch has a qty input ───────────────────
  renderBatchAllocCards({ batches, qtyMap, onQtyChange, totalQty, label }) {
    const allocated = Object.values(qtyMap).reduce(
      (s, v) => s + (Number(v) || 0), 0
    );
    const remaining = Math.round((totalQty - allocated) * 1000) / 1000;
    const overAllocated = remaining < -0.001;
    const fullyAllocated = Math.abs(remaining) <= 0.001;

    return (
      <div>
        {/* allocation summary strip */}
        <div style={{
          display: "flex", justifyContent: "flex-end", marginBottom: 6,
          fontSize: 12, fontWeight: 500,
          color: overAllocated ? "#c62828" : fullyAllocated ? "#2e7d32" : "#555",
        }}>
          {overAllocated
            ? `⚠ Over-allocated by ${Math.abs(remaining)}`
            : fullyAllocated
            ? "✓ Fully allocated"
            : `Remaining to allocate: ${remaining}`}
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
          {batches.map((b) => {
            const expired = isExpiredDate(b.expiryDate);
            const nearExpiry = !expired && isNearExpiryDate(b.expiryDate);
            const expiryColor = expired ? "#c62828" : nearExpiry ? "#e65100" : "#2e7d32";
            const qty = qtyMap[b.batchId] !== undefined ? qtyMap[b.batchId] : "";

            return (
              <div
                key={b.batchId}
                style={{
                  display: "flex", alignItems: "center", gap: 12,
                  padding: "10px 14px", border: "1px solid #ddd",
                  borderRadius: 6, background: "#fafafa",
                }}
              >
                {/* batch meta */}
                <div style={{ flex: 1, display: "flex", gap: 12, flexWrap: "wrap", alignItems: "center" }}>
                  <span style={{
                    fontSize: 11, fontWeight: 600, color: "#5c6bc0",
                    background: "#e8eaf6", borderRadius: 4, padding: "1px 7px",
                    whiteSpace: "nowrap",
                  }}>
                    #{b.batchId}
                  </span>
                  {b.brand && (
                    <span style={{ fontSize: 13, fontWeight: 600 }}>{b.brand}</span>
                  )}
                  {b.lotNumber && (
                    <span style={{ fontSize: 12, color: "#555" }}>Lot: {b.lotNumber}</span>
                  )}
                  {b.expiryDate ? (
                    <span style={{ fontSize: 12, fontWeight: 500, color: expiryColor }}>
                      Exp: {b.expiryDate}
                      {expired ? " ⚠ Expired" : nearExpiry ? " ⚠ Expiring soon" : ""}
                    </span>
                  ) : null}
                </div>

                {/* available qty badge */}
                <span style={{
                  fontSize: 12, color: "#444", background: "#e8eaf6",
                  borderRadius: 4, padding: "2px 8px", whiteSpace: "nowrap",
                }}>
                  Available: <strong>{b.qtyRemaining}</strong>
                </span>

                {/* qty input */}
                <input
                  type="number"
                  min="0"
                  step="any"
                  placeholder="Qty"
                  value={qty}
                  onWheel={(e) => e.target.blur()}
                  onChange={(e) => {
                    const val = e.target.value;
                    onQtyChange(b.batchId, val);
                  }}
                  style={{
                    width: 80, padding: "5px 8px", borderRadius: 4,
                    border: "1px solid #ccc", fontSize: 13, textAlign: "right",
                  }}
                />
              </div>
            );
          })}
        </div>
      </div>
    );
  }

  renderBatchSection() {
    const { entryType, availableBatches, batchQtyMap, excessQtyMap, existingBatchMode } = this.state;
    if (availableBatches.length === 0) return null;

    const totalQty = Number(this.formData.quantity) || 0;

    if (entryType === "LOST_DAMAGED") {
      return (
        <div style={{ marginTop: 12 }}>
          <div style={{
            fontSize: 12, color: "#666", fontWeight: 600, marginBottom: 8,
            textTransform: "uppercase", letterSpacing: "0.4px",
          }}>
            Distribute loss across batches
          </div>
          {this.renderBatchAllocCards({
            batches: availableBatches,
            qtyMap: batchQtyMap,
            totalQty,
            onQtyChange: (batchId, val) => {
              this.setState((prev) => ({
                batchQtyMap: { ...prev.batchQtyMap, [batchId]: val },
              }));
            },
          })}
        </div>
      );
    }

    if (entryType === "EXCESS_FOUND") {
      return (
        <div style={{ marginTop: 12 }}>
          <div style={{
            fontSize: 12, color: "#666", fontWeight: 600, marginBottom: 8,
            textTransform: "uppercase", letterSpacing: "0.4px",
          }}>
            Batch Details
          </div>

          {/* Toggle: add to existing vs create new */}
          <div style={{ display: "flex", gap: 12, marginBottom: 14 }}>
            {[
              { value: true,  title: "Add to existing batch",  desc: "Select a batch already in this warehouse" },
              { value: false, title: "Create new batch",       desc: "Record this stock as a brand-new batch" },
            ].map(({ value, title, desc }) => {
              const active = existingBatchMode === value;
              return (
                <label
                  key={String(value)}
                  style={{
                    flex: 1, display: "flex", gap: 10,
                    padding: "10px 14px",
                    border: active ? "2px solid #1976d2" : "1px solid #ccc",
                    borderRadius: 6, background: active ? "#e3f2fd" : "#fff",
                    cursor: "pointer", userSelect: "none",
                  }}
                >
                  <input
                    type="radio"
                    name="excessBatchMode"
                    checked={active}
                    onChange={() => {
                      this.formData.existingBatchId = null;
                      this.formData.excessBatchEntries = null;
                      this.formData.brand = null;
                      this.formData.lotNumber = null;
                      this.formData.expiryDate = null;
                      this.setState({ existingBatchMode: value, excessQtyMap: {} });
                    }}
                    style={{ marginTop: 3, flexShrink: 0 }}
                  />
                  <div>
                    <div style={{ fontWeight: 600, fontSize: 13 }}>{title}</div>
                    <div style={{ fontSize: 11, color: "#777", marginTop: 2 }}>{desc}</div>
                  </div>
                </label>
              );
            })}
          </div>

          {existingBatchMode ? (
            this.renderBatchAllocCards({
              batches: availableBatches,
              qtyMap: excessQtyMap,
              totalQty,
              onQtyChange: (batchId, val) => {
                this.setState((prev) => ({
                  excessQtyMap: { ...prev.excessQtyMap, [batchId]: val },
                }));
              },
            })
          ) : (
            <div>
              <div className="flex width50">
                {this.renderTextField({
                  fieldname: "brand",
                  placeholder: "Brand / Supplier",
                  onChange: (value) => { this.formData.brand = value; },
                })}
                {this.renderTextField({
                  fieldname: "lotNumber",
                  placeholder: "Lot / Batch Number",
                  onChange: (value) => { this.formData.lotNumber = value; },
                })}
              </div>
              {this.state.selectedProductBatchMode === 'BATCH_WITH_EXPIRY' && (
                <div className="flex width50">
                  {this.renderDate({
                    fieldname: "expiryDate",
                    label: "Expiry Date",
                    minDate: moment(),
                    required: true,
                  })}
                </div>
              )}
            </div>
          )}
        </div>
      );
    }

    return null;
  }

  render() {
    const { entryType, currentStockValue, closing } = this.state;
    const isLost = entryType === "LOST_DAMAGED";
    const unit = this.props.units[this.formData.productId];
    const bothSelected = !!(this.formData.productId && this.formData.warehouseId);
    const qty = Number(this.formData.quantity) || 0;
    const stockKnown = bothSelected && currentStockValue !== null;
    const noStock = stockKnown && currentStockValue <= 0 && isLost;
    const overQty = stockKnown && isLost && qty > currentStockValue && currentStockValue > 0;

    const sectionHeader = (label, color = '#1976d2') => (
      <div style={{
        fontSize: 11, fontWeight: 700, color, textTransform: 'uppercase',
        letterSpacing: '0.6px', marginBottom: 8, marginTop: 4,
        paddingBottom: 4, borderBottom: `2px solid ${color}22`,
      }}>
        {label}
      </div>
    );

    return (
      <div className="list-section add">
        {this.renderHeading()}
        <form onSubmit={(e) => this.add(e)}>

          {/* ── TYPE TOGGLE ──────────────────────────────────── */}
          <div style={{ display: 'flex', gap: 10, marginBottom: 20 }}>
            {[
              { value: 'LOST_DAMAGED', label: '↓ Lost / Damaged', color: '#c62828', bg: '#ffebee', border: '#ef9a9a' },
              { value: 'EXCESS_FOUND', label: '↑ Excess Found',   color: '#2e7d32', bg: '#e8f5e9', border: '#a5d6a7' },
            ].map(({ value, label, color, bg, border }) => {
              const active = entryType === value;
              return (
                <button
                  key={value}
                  type="button"
                  onClick={() => {
                    this.formData.entryType = value;
                    this.formData.batchId = null;
                    this.formData.batchEntries = null;
                    this.formData.existingBatchId = null;
                    this.formData.excessBatchEntries = null;
                    this.formData.brand = null;
                    this.formData.lotNumber = null;
                    this.formData.expiryDate = null;
                    this.setState(
                      { entryType: value, existingBatchMode: false, batchQtyMap: {}, excessQtyMap: {}, selectedProductBatchMode: null },
                      () => this.getCurrentStock()
                    );
                  }}
                  style={{
                    padding: '10px 28px', borderRadius: 6, cursor: 'pointer',
                    fontWeight: 600, fontSize: 14,
                    border: active ? `2px solid ${color}` : `2px solid #e0e0e0`,
                    background: active ? bg : '#fafafa',
                    color: active ? color : '#888',
                    transition: 'all 0.15s',
                  }}
                >
                  {label}
                </button>
              );
            })}
          </div>

          {/* ── SECTION 1: PRODUCT & WAREHOUSE ───────────────── */}
          {sectionHeader('1. Select Product & Warehouse')}
          <div className="flex width50">
            {this.renderAutoComplete({
              fieldname: "warehouseId",
              placeholder: "Warehouse *",
              options: this.props.dropdowns?.warehouse || [],
              disableClearable: true,
              required: true,
              getOption: (option) => option["name"],
              onChange: (e, value) => {
                if (value) {
                  this.formData.warehouseId = value.id;
                  this.setState({ currentStockValue: null, closing: null });
                  this.getCurrentStock();
                  this.loadBatches();
                }
              },
            })}
            {this.renderAutoComplete({
              fieldname: "productId",
              placeholder: "Inventory *",
              options: this.props.dropdowns?.product || [],
              disableClearable: true,
              required: true,
              getOption: (option) => option["name"],
              onChange: (e, value) => {
                if (value) {
                  this.formData.productId = value.id;
                  this.setState({ selectedProductBatchMode: null, currentStockValue: null, closing: null });
                  this.getCurrentStock();
                  this.loadBatches();
                  API.GET(`/api/inventory/product/${value.id}`).then(r => {
                    if (r.success && r.data) {
                      this.setState({ selectedProductBatchMode: r.data.batchMode || 'BATCH_ONLY' });
                    }
                  });
                }
              },
            })}
          </div>

          {/* Stock info card — shown after both selected */}
          {bothSelected && (
            <div style={{
              marginBottom: 16, padding: '10px 16px',
              borderRadius: 8, border: '1px solid',
              borderColor: noStock ? '#ef9a9a' : '#c8e6c9',
              background: noStock ? '#ffebee' : '#f1f8e9',
              display: 'flex', alignItems: 'center', gap: 20, flexWrap: 'wrap',
            }}>
              {currentStockValue === null ? (
                <span style={{ fontSize: 13, color: '#888' }}>Loading stock…</span>
              ) : (
                <>
                  <span style={{ fontSize: 13 }}>
                    <span style={{ color: '#777' }}>Current stock in warehouse: </span>
                    <strong style={{ fontSize: 16, color: noStock ? '#c62828' : '#2e7d32' }}>
                      {currentStockValue} {unit || ''}
                    </strong>
                  </span>
                  {noStock && (
                    <span style={{
                      fontSize: 12, color: '#c62828', fontWeight: 600,
                      background: '#ffcdd2', padding: '2px 10px', borderRadius: 10,
                    }}>
                      ⚠ No stock in this warehouse
                    </span>
                  )}
                  {overQty && (
                    <span style={{
                      fontSize: 12, color: '#e65100', fontWeight: 600,
                      background: '#fff3e0', padding: '2px 10px', borderRadius: 10,
                    }}>
                      ⚠ Quantity exceeds available stock
                    </span>
                  )}
                  {closing !== null && qty > 0 && (
                    <span style={{ fontSize: 13, marginLeft: 'auto' }}>
                      <span style={{ color: '#777' }}>After this entry: </span>
                      <strong style={{ color: closing < 0 ? '#c62828' : '#1976d2' }}>
                        {closing} {unit || ''}
                      </strong>
                    </span>
                  )}
                </>
              )}
            </div>
          )}

          {/* ── SECTION 2: QUANTITY & DATE ────────────────────── */}
          {sectionHeader('2. Quantity & Details')}
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', flex: 1 }}>
            <div style={{ width: 160, flexShrink: 0 }}>
              {this.renderTextField({
                fieldname: "quantity",
                placeholder: isLost ? "Qty Lost *" : "Qty Found *",
                type: "number",
                required: true,
                validation: "nonegative",
                onChange: () => { this.getCurrentStock(); },
              })}
            </div>
            {unit && (
              <span style={{
                fontSize: 15, fontWeight: 700, color: '#3949ab',
                background: '#e8eaf6', borderRadius: 6, padding: '6px 14px',
                whiteSpace: 'nowrap', flexShrink: 0, alignSelf: 'center',
              }}>
                {unit}
              </span>
            )}
            {this.renderDate({
              fieldname: "date",
              label: "Date *",
              maxDate: moment(),
              minDate: moment().add(-7, "d"),
            })}
            {this.renderTextField({
              fieldname: "theftLocation",
              placeholder: "Remarks *",
              required: true,
            })}
          </div>

          {/* ── SECTION 3: BATCH ALLOCATION ───────────────────── */}
          {this.renderBatchSection()}

          {/* ── SECTION 4: ADDITIONAL INFO ────────────────────── */}
          {sectionHeader('Additional Information', '#555')}
          <div className="flex" style={{ marginTop: 4 }}>
            {this.renderTextArea({
              fieldname: "additionalComment",
              placeholder: "Additional Comments (optional)",
            })}
          </div>

          {this.renderFileArea()}
          {this.renderFooter()}
        </form>
      </div>
    );
  }
}

const mapStateToProps = (state) => ({
  units: state.units.units,
});

export default connect(mapStateToProps, null, null, { forwardRef: true })(
  withSnackbar(Add)
);
