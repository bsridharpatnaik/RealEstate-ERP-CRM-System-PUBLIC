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
    entryType: "LOST_DAMAGED",
    availableBatches: [],
    existingBatchMode: false,
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
      this.setState({ closing });
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
                  {!b.brand && !b.lotNumber && !b.expiryDate && (
                    <span style={{ fontSize: 12, color: "#888" }}>Batch #{b.batchId}</span>
                  )}
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
              <div className="flex width50">
                {this.renderDate({
                  fieldname: "expiryDate",
                  label: "Expiry Date (if applicable)",
                  minDate: moment(),
                  required: false,
                })}
              </div>
            </div>
          )}
        </div>
      );
    }

    return null;
  }

  render() {
    return (
      <div className="list-section add">
        {this.renderHeading()}
        <form onSubmit={(e) => this.add(e)}>

          {/* Type toggle */}
          <div className="flex">
            <FormControl component="fieldset">
              <RadioGroup
                row
                value={this.state.entryType}
                onChange={(e) => {
                  const newType = e.target.value;
                  this.formData.entryType = newType;
                  this.formData.batchId = null;
                  this.formData.batchEntries = null;
                  this.formData.existingBatchId = null;
                  this.formData.excessBatchEntries = null;
                  this.formData.brand = null;
                  this.formData.lotNumber = null;
                  this.formData.expiryDate = null;
                  this.setState(
                    {
                      entryType: newType,
                      existingBatchMode: false,
                      batchQtyMap: {},
                      excessQtyMap: {},
                    },
                    () => this.getCurrentStock()
                  );
                }}
              >
                <FormControlLabel
                  value="LOST_DAMAGED"
                  control={<Radio color="primary" />}
                  label="Lost / Damaged"
                />
                <FormControlLabel
                  value="EXCESS_FOUND"
                  control={<Radio color="primary" />}
                  label="Excess Found"
                />
              </RadioGroup>
            </FormControl>
          </div>

          {/* Warehouse + Inventory — same row */}
          <div className="flex width50">
            {this.renderAutoComplete({
              fieldname: "warehouseId",
              placeholder: "Warehouse",
              options: this.props.dropdowns?.warehouse || [],
              disableClearable: true,
              required: true,
              getOption: (option) => option["name"],
              onChange: (e, value) => {
                if (value) {
                  this.formData.warehouseId = value.id;
                  this.getCurrentStock();
                  this.loadBatches();
                }
              },
            })}
            {this.renderAutoComplete({
              fieldname: "productId",
              placeholder: messages.common.inventory,
              options: this.props.dropdowns?.product || [],
              disableClearable: true,
              required: true,
              getOption: (option) => option["name"],
              onChange: (e, value) => {
                if (value) {
                  this.formData.productId = value.id;
                  this.getCurrentStock();
                  this.loadBatches();
                }
              },
            })}
          </div>

          {/* Quantity + Measurement Unit + Closing Stock — same row */}
          <div className="flex width50">
            {this.renderTextField({
              fieldname: "quantity",
              placeholder: "Quantity",
              type: "number",
              required: true,
              validation: "nonegative",
              onChange: () => { this.getCurrentStock(); },
            })}
            {this.renderTextField({
              fieldname: "measurementUnit",
              placeholder: "Measurement Unit",
              disabled: true,
              value: this.props.units[this.formData.productId],
            })}
            {this.renderTextField({
              fieldname: "Closing Stock",
              placeholder: "Closing Stock",
              disabled: true,
              value: this.state.closing,
            })}
          </div>

          {/* Date + Location/Remarks */}
          <div className="flex width50">
            {this.renderDate({
              fieldname: "date",
              label: messages.fields.date,
              maxDate: moment(),
              minDate: moment().add(-7, "d"),
            })}
            {this.renderTextField({
              fieldname: "theftLocation",
              placeholder:
                this.state.entryType === "EXCESS_FOUND" ? "Remarks" : "Location",
              required: true,
            })}
          </div>

          {/* Batch section */}
          {this.renderBatchSection()}

          {/* Additional Comments */}
          <div className="flex" style={{ marginTop: 16 }}>
            {this.renderTextArea({
              fieldname: "Additional Comments",
              placeholder: "Additional Comments",
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
