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
    selectedBatchId: null,
    existingBatchMode: false,
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
      this.setState({ availableBatches: [], selectedBatchId: null });
      return;
    }
    try {
      const response = await API.GET(
        apiEndpoints.getBatchesForProduct(productId, warehouseId)
      );
      if (response.success && Array.isArray(response.data)) {
        const batches = response.data.filter((b) => b.qtyRemaining > 0);
        this.setState({ availableBatches: batches, selectedBatchId: null });
        this.formData.batchId = null;
      } else {
        this.setState({ availableBatches: [], selectedBatchId: null });
      }
    } catch (e) {
      this.setState({ availableBatches: [] });
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
    super.add(event);
  }

  // ── Reusable batch card list ─────────────────────────────────────────────
  renderBatchCards({ selectedId, radioName, onSelect }) {
    const { availableBatches } = this.state;
    return (
      <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
        {availableBatches.map((b) => {
          const selected = selectedId === b.batchId;
          const expired = isExpiredDate(b.expiryDate);
          const nearExpiry = !expired && isNearExpiryDate(b.expiryDate);
          const expiryColor = expired ? "#c62828" : nearExpiry ? "#e65100" : "#2e7d32";
          return (
            <label
              key={b.batchId}
              style={{
                display: "flex",
                alignItems: "center",
                gap: 12,
                padding: "10px 14px",
                border: selected ? "2px solid #1976d2" : "1px solid #ddd",
                borderRadius: 6,
                background: selected ? "#e3f2fd" : "#fafafa",
                cursor: "pointer",
                userSelect: "none",
              }}
            >
              <input
                type="radio"
                name={radioName}
                value={b.batchId}
                checked={selected}
                onChange={() => onSelect(b.batchId)}
                style={{ flexShrink: 0, marginTop: 1 }}
              />
              <div style={{ flex: 1, display: "flex", gap: 16, flexWrap: "wrap", alignItems: "center" }}>
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
                <span
                  style={{
                    marginLeft: "auto",
                    fontSize: 12,
                    color: "#444",
                    background: "#e8eaf6",
                    borderRadius: 4,
                    padding: "2px 8px",
                    whiteSpace: "nowrap",
                  }}
                >
                  Available: <strong>{b.qtyRemaining}</strong>
                </span>
              </div>
            </label>
          );
        })}
      </div>
    );
  }

  renderBatchSection() {
    const { entryType, availableBatches, selectedBatchId, existingBatchMode } = this.state;
    if (availableBatches.length === 0) return null;

    if (entryType === "LOST_DAMAGED") {
      return (
        <div style={{ marginTop: 12 }}>
          <div
            style={{
              fontSize: 12,
              color: "#666",
              fontWeight: 600,
              marginBottom: 8,
              textTransform: "uppercase",
              letterSpacing: "0.4px",
            }}
          >
            Which batch did the loss come from?
          </div>
          {this.renderBatchCards({
            selectedId: selectedBatchId,
            radioName: "lostBatch",
            onSelect: (id) => {
              this.formData.batchId = id;
              this.setState({ selectedBatchId: id });
            },
          })}
        </div>
      );
    }

    if (entryType === "EXCESS_FOUND") {
      return (
        <div style={{ marginTop: 12 }}>
          <div
            style={{
              fontSize: 12,
              color: "#666",
              fontWeight: 600,
              marginBottom: 8,
              textTransform: "uppercase",
              letterSpacing: "0.4px",
            }}
          >
            Batch Details
          </div>

          {/* Toggle cards */}
          <div style={{ display: "flex", gap: 12, marginBottom: 14 }}>
            {[
              {
                value: true,
                title: "Add to existing batch",
                desc: "Select a batch already in this warehouse",
              },
              {
                value: false,
                title: "Create new batch",
                desc: "Record this stock as a brand-new batch",
              },
            ].map(({ value, title, desc }) => {
              const active = existingBatchMode === value;
              return (
                <label
                  key={String(value)}
                  style={{
                    flex: 1,
                    display: "flex",
                    gap: 10,
                    padding: "10px 14px",
                    border: active ? "2px solid #1976d2" : "1px solid #ccc",
                    borderRadius: 6,
                    background: active ? "#e3f2fd" : "#fff",
                    cursor: "pointer",
                    userSelect: "none",
                  }}
                >
                  <input
                    type="radio"
                    name="excessBatchMode"
                    checked={active}
                    onChange={() => {
                      this.formData.existingBatchId = null;
                      this.formData.brand = null;
                      this.formData.lotNumber = null;
                      this.formData.expiryDate = null;
                      this.setState({
                        existingBatchMode: value,
                        selectedBatchId: null,
                      });
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
            this.renderBatchCards({
              selectedId: selectedBatchId,
              radioName: "excessExistingBatch",
              onSelect: (id) => {
                this.formData.existingBatchId = id;
                this.setState({ selectedBatchId: id });
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
                  this.formData.entryType = e.target.value;
                  this.formData.batchId = null;
                  this.formData.existingBatchId = null;
                  this.formData.brand = null;
                  this.formData.lotNumber = null;
                  this.formData.expiryDate = null;
                  this.setState(
                    { entryType: e.target.value, selectedBatchId: null, existingBatchMode: false },
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
          <div className="flex">
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
