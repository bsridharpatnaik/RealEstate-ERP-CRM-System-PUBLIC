//react
import React from "react";
import { connect } from "react-redux";

//third party
import { withSnackbar } from "notistack";
import EditForm from "./../../Shared/EditForm";
import FormControl from "@material-ui/core/FormControl";
import RadioGroup from "@material-ui/core/RadioGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import Radio from "@material-ui/core/Radio";
//misc
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";
import { fetchUnit } from "./../../actions/measurementUnit";
import moment from "moment";

// ── date helpers ──────────────────────────────────────────────────────────
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

class Edit extends EditForm {
  updateUrl = apiEndpoints.individualLost;
  title = messages.common.lost;

  state = {
    isLoaded: false,
    entryType: "LOST_DAMAGED",
    availableBatches: [],
    existingBatchMode: false,
    // multi-batch qty maps: batchId (number) → qty string
    batchQtyMap: {},    // LOST_DAMAGED allocation
    excessQtyMap: {},   // EXCESS_FOUND add-to-existing allocation
  };

  componentDidMount() {
    this.updateUrl = this.updateUrl + this.props.id;
    this.search();
    const { dispatch } = this.props;
    dispatch(fetchUnit());
  }

  update(event) {
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
    const isBatchTracked = availableBatches.length > 0 || !!this.formData.batchId;

    if (isBatchTracked) {
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

    super.update(event);
  }

  async search() {
    const response = await API.GET(this.updateUrl);
    if (response.success) {
      const data = response.data;
      this.formData.warehouseId = data.warehouse.warehouseId;
      this.formData.quantity = data.quantity;
      this.formData.productId = data.product.productId;
      this.formData.theftLocation = data.locationOfTheft;
      this.formData["Additional Comments"] = data["Additional Comments"];
      this.formData.date = data.date;
      this.formData.fileInformations = data.fileInformations;
      this.formData.entryType = data.entryType || "LOST_DAMAGED";

      const entryType = data.entryType || "LOST_DAMAGED";
      const savedBatch = data.batch;
      let existingBatchMode = false;
      let batchQtyMap = {};
      let excessQtyMap = {};

      // Pre-populate batch allocation from saved batchEntriesJson (multi-batch)
      // or fall back to single-batch record
      if (data.batchEntriesJson) {
        try {
          const entries = JSON.parse(data.batchEntriesJson);
          if (entryType === "LOST_DAMAGED") {
            entries.forEach((e) => { batchQtyMap[e.batchId] = String(e.qty); });
          } else if (entryType === "EXCESS_FOUND") {
            entries.forEach((e) => { excessQtyMap[e.batchId] = String(e.qty); });
            existingBatchMode = true;
          }
        } catch (e) { /* fall back to single-batch below */ }
      }

      if (savedBatch) {
        this.formData.batchId = savedBatch.batchId;
        if (entryType === "LOST_DAMAGED" && Object.keys(batchQtyMap).length === 0) {
          // Legacy single-batch: pre-fill that batch with full qty
          batchQtyMap[savedBatch.batchId] = String(data.quantity);
        }
        if (entryType === "EXCESS_FOUND") {
          existingBatchMode = true;
          if (Object.keys(excessQtyMap).length === 0) {
            excessQtyMap[savedBatch.batchId] = String(data.quantity);
          }
        }
      }

      this.setState({
        isLoaded: true,
        closing: data.closingStock,
        entryType,
        existingBatchMode,
        batchQtyMap,
        excessQtyMap,
      });
      this.loadBatches();
    }
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
      const closing = this.formData.entryType === "EXCESS_FOUND"
        ? currentStock + qty
        : currentStock - qty;
      this.setState({ closing });
    }
  }

  /** Load all batches (no qty filter — pre-selected batch must always appear). */
  async loadBatches() {
    const { productId, warehouseId } = this.formData;
    if (!productId || !warehouseId) return;
    try {
      const response = await API.GET(
        apiEndpoints.getBatchesForProduct(productId, warehouseId)
      );
      if (response.success && Array.isArray(response.data)) {
        this.setState({ availableBatches: response.data });
      }
    } catch (e) {
      this.setState({ availableBatches: [] });
    }
  }

  // ── Batch allocation cards — each batch has a qty input ───────────────────
  renderBatchAllocCards({ batches, qtyMap, onQtyChange, totalQty }) {
    const allocated = Object.values(qtyMap).reduce(
      (s, v) => s + (Number(v) || 0), 0
    );
    const remaining = Math.round((totalQty - allocated) * 1000) / 1000;
    const overAllocated = remaining < -0.001;
    const fullyAllocated = Math.abs(remaining) <= 0.001;

    return (
      <div>
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
                <div style={{ flex: 1, display: "flex", gap: 12, flexWrap: "wrap", alignItems: "center" }}>
                  {b.brand && <span style={{ fontSize: 13, fontWeight: 600 }}>{b.brand}</span>}
                  {b.lotNumber && <span style={{ fontSize: 12, color: "#555" }}>Lot: {b.lotNumber}</span>}
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
                <span style={{
                  fontSize: 12, color: "#444", background: "#e8eaf6",
                  borderRadius: 4, padding: "2px 8px", whiteSpace: "nowrap",
                }}>
                  Qty: <strong>{b.qtyRemaining}</strong>
                </span>
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
    const isBatchTracked = availableBatches.length > 0 || !!this.formData.batchId;
    if (!isBatchTracked) return null;

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
          <div style={{ fontSize: 11, color: "#888", marginBottom: 8 }}>
            Note: current batch quantities shown before reversal of this entry.
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
      const activeBatches = availableBatches.filter((b) => b.qtyRemaining > 0);
      return (
        <div style={{ marginTop: 12 }}>
          <div style={{
            fontSize: 12, color: "#666", fontWeight: 600, marginBottom: 8,
            textTransform: "uppercase", letterSpacing: "0.4px",
          }}>
            Batch Details
          </div>

          {activeBatches.length > 0 && (
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
                      name="excessBatchModeEdit"
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
          )}

          {existingBatchMode ? (
            <>
              <div style={{ fontSize: 11, color: "#888", marginBottom: 8 }}>
                Note: quantities shown before reversal of this entry.
              </div>
              {this.renderBatchAllocCards({
                batches: activeBatches,
                qtyMap: excessQtyMap,
                totalQty,
                onQtyChange: (batchId, val) => {
                  this.setState((prev) => ({
                    excessQtyMap: { ...prev.excessQtyMap, [batchId]: val },
                  }));
                },
              })}
            </>
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
        {this.state.isLoaded && (
          <form onSubmit={(e) => this.update(e)}>
            {/* Entry type — read-only in edit */}
            <div className="flex">
              <FormControl component="fieldset">
                <RadioGroup row value={this.state.entryType}>
                  <FormControlLabel value="LOST_DAMAGED" control={<Radio color="primary" disabled />} label="Lost / Damaged" />
                  <FormControlLabel value="EXCESS_FOUND" control={<Radio color="primary" disabled />} label="Excess Found" />
                </RadioGroup>
              </FormControl>
            </div>

            <div className="flex">
              {this.renderAutoComplete({
                fieldname: "warehouseId",
                placeholder: "Warehouse",
                defaultKey: "warehouseId",
                options: this.props.dropdowns?.warehouse || [],
                disableClearable: true,
                required: true,
                disabled: !this.isAdmin,
                getOption: (option) => option["name"],
                onChange: (e, value) => {
                  if (value) {
                    this.formData.warehouseId = value.id;
                    this.getCurrentStock();
                  }
                },
              })}
            </div>

            <div className="flex">
              {this.renderAutoComplete({
                fieldname: "productId",
                placeholder: messages.common.inventory,
                defaultKey: "productId",
                options: this.props.dropdowns?.product || [],
                disableClearable: true,
                required: true,
                disabled: !this.isAdmin,
                getOption: (option) => option["name"],
                skipAdd: true,
                onChange: (e, value) => {
                  if (value) {
                    this.formData.productId = value.id;
                    this.getCurrentStock();
                  }
                },
              })}
            </div>

            <div className="flex">
              {this.renderTextField({
                fieldname: "measurementUnit",
                placeholder: "Measurement Unit",
                disabled: true,
                value: this.props.units[this.formData.productId],
              })}
            </div>

            <div className="flex width50">
              {this.renderTextField({
                fieldname: "quantity",
                placeholder: "Quantity",
                type: "number",
                required: true,
                skipAdd: true,
                validation: "nonegative",
                onChange: (value) => {
                  this.formData.quantity = value;
                  this.getCurrentStock();
                },
              })}
              {this.renderTextField({
                fieldname: "Closing Stock",
                placeholder: "Closing Stock",
                disabled: true,
                value: this.state.closing,
              })}
            </div>

            <div className="flex width50">
              {this.renderDate({
                fieldname: "date",
                label: messages.fields.date,
                defaultKey: "date",
                disabled: true,
                maxDate: moment(),
              })}
              {this.renderTextField({
                fieldname: "theftLocation",
                placeholder: this.state.entryType === "EXCESS_FOUND" ? "Remarks" : "Location",
                required: true,
                disabled: !this.isAdmin,
              })}
            </div>

            {/* Batch allocation section */}
            {this.renderBatchSection()}

            <div className="flex" style={{ marginTop: 16 }}>
              {this.renderTextArea({
                fieldname: "Additional Comments",
                placeholder: "Additional Comments",
              })}
            </div>

            {this.renderFileArea()}
            {this.renderFooter()}
          </form>
        )}
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return { units: state.units.units };
};

export default connect(mapStateToProps, null, null, { forwardRef: true })(
  withSnackbar(Edit)
);
