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

class Edit extends EditForm {
  updateUrl = apiEndpoints.individualLost;
  title = messages.common.lost;

  state = {
    isLoaded: false,
    entryType: "LOST_DAMAGED",
    availableBatches: [],
    selectedBatchId: null,
    existingBatchMode: false,
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
      this.props.enqueueSnackbar("Add atleast one file", {
        variant: "error",
      });
      return;
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

      // Pre-populate batch fields from the saved record
      const savedBatch = data.batch;
      let selectedBatchId = null;
      let existingBatchMode = false;
      if (savedBatch) {
        this.formData.batchId = savedBatch.batchId;
        selectedBatchId = savedBatch.batchId;
        if ((data.entryType || "LOST_DAMAGED") === "EXCESS_FOUND") {
          // Default edit of EXCESS_FOUND to "add to existing batch" using the saved batch
          existingBatchMode = true;
          this.formData.existingBatchId = savedBatch.batchId;
        }
      }

      this.setState({
        isLoaded: true,
        closing: response.data.closingStock,
        entryType: data.entryType || "LOST_DAMAGED",
        selectedBatchId,
        existingBatchMode,
      });
      this.loadBatches();
    }
  }
  async getCurrentStock(index) {
    const warehouseId = this.formData.warehouseId;
    const productId = this.formData.productId;
    if (!productId && !warehouseId) {
      return;
    }
    const response = await API.GET(
      apiEndpoints.getCurrentStock +
        "productId=" +
        productId +
        "&warehouseId=" +
        warehouseId
    );
    if (response.success) {
      const qty = Number(this.formData.quantity) || 0;
      const currentStock = Number(response.data);
      const closing = this.formData.entryType === "EXCESS_FOUND"
        ? currentStock + qty
        : currentStock - qty;
      this.setState({ closing: closing });
    }
  }
  /** Load all batches for current product+warehouse (edit: no qty filter so pre-selected batch always visible). */
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

  renderBatchSection() {
    const { entryType, availableBatches, selectedBatchId, existingBatchMode } = this.state;
    // Show section if any batches exist OR if original record had a batch attached
    const isBatchTracked = availableBatches.length > 0 || !!this.formData.batchId;
    if (!isBatchTracked) return null;

    if (entryType === "LOST_DAMAGED") {
      return (
        <div className="flex" style={{ marginTop: 8 }}>
          <div style={{ display: "flex", flexDirection: "column", gap: 4, flex: 1 }}>
            <label style={{ fontSize: 12, color: "#666", fontWeight: 500 }}>
              Batch (select which batch the loss came from)
            </label>
            <select
              style={{ padding: "8px 10px", border: "1px solid #ccc", borderRadius: 4, fontSize: 13 }}
              value={selectedBatchId || ""}
              onChange={(e) => {
                const val = e.target.value ? Number(e.target.value) : null;
                this.formData.batchId = val;
                this.setState({ selectedBatchId: val });
              }}
            >
              <option value="">-- Select Batch --</option>
              {availableBatches.map((b) => {
                const label = [
                  b.brand,
                  b.lotNumber,
                  b.expiryDate ? "Exp: " + b.expiryDate : null,
                  `Qty: ${b.qtyRemaining}`,
                ].filter(Boolean).join(" | ");
                return (
                  <option key={b.batchId} value={b.batchId}>
                    {label || `Batch #${b.batchId}`}
                  </option>
                );
              })}
            </select>
          </div>
        </div>
      );
    }

    if (entryType === "EXCESS_FOUND") {
      const activeBatches = availableBatches.filter((b) => b.qtyRemaining > 0);
      return (
        <div style={{ marginTop: 8 }}>
          <div style={{ fontSize: 12, color: "#666", fontWeight: 500, marginBottom: 4 }}>
            Batch Details
          </div>
          {activeBatches.length > 0 && (
            <div style={{ display: "flex", gap: 16, marginBottom: 8 }}>
              <label style={{ fontSize: 12, cursor: "pointer" }}>
                <input
                  type="radio"
                  name="excessBatchModeEdit"
                  checked={existingBatchMode}
                  onChange={() => {
                    this.formData.existingBatchId = selectedBatchId;
                    this.formData.brand = null;
                    this.formData.lotNumber = null;
                    this.formData.expiryDate = null;
                    this.setState({ existingBatchMode: true });
                  }}
                />{" "}Add to existing batch
              </label>
              <label style={{ fontSize: 12, cursor: "pointer" }}>
                <input
                  type="radio"
                  name="excessBatchModeEdit"
                  checked={!existingBatchMode}
                  onChange={() => {
                    this.formData.existingBatchId = null;
                    this.setState({ existingBatchMode: false });
                  }}
                />{" "}Create new batch
              </label>
            </div>
          )}
          {existingBatchMode ? (
            <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
              <label style={{ fontSize: 12, color: "#666" }}>Select batch to add excess to:</label>
              <select
                style={{ padding: "8px 10px", border: "1px solid #ccc", borderRadius: 4, fontSize: 13 }}
                value={selectedBatchId || ""}
                onChange={(e) => {
                  const val = e.target.value ? Number(e.target.value) : null;
                  this.formData.existingBatchId = val;
                  this.setState({ selectedBatchId: val });
                }}
              >
                <option value="">-- Select Batch --</option>
                {activeBatches.map((b) => {
                  const label = [
                    b.brand,
                    b.lotNumber,
                    b.expiryDate ? "Exp: " + b.expiryDate : null,
                    `Qty: ${b.qtyRemaining}`,
                  ].filter(Boolean).join(" | ");
                  return (
                    <option key={b.batchId} value={b.batchId}>
                      {label || `Batch #${b.batchId}`}
                    </option>
                  );
                })}
              </select>
            </div>
          ) : (
            <div>
              <div className="flex width50">
                {this.renderTextField({
                  fieldname: "brand",
                  placeholder: "Identifier / Lot",
                  onChange: (value) => { this.formData.brand = value; },
                })}
                {this.renderTextField({
                  fieldname: "lotNumber",
                  placeholder: "Lot Number",
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
            <div className="flex">
              <FormControl component="fieldset">
                <RadioGroup row value={this.state.entryType}>
                  <FormControlLabel value="LOST_DAMAGED" control={<Radio color="primary" disabled />} label="Lost / Damaged" />
                  <FormControlLabel value="EXCESS_FOUND" control={<Radio color="primary" disabled />} label="Excess Found" />
                </RadioGroup>
              </FormControl>
            </div>
            <div class="flex">
            {this.renderAutoComplete({
              fieldname: "warehouseId",
              placeholder: "Warehouse",
              defaultKey: "warehouseId",
              options: this.props.dropdowns?.warehouse || [],
              disableClearable: true,
              required: true,
              disabled: !this.isAdmin,
              getOption: (option) => {
                return option["name"];
              },
              onChange: (e, value) => {
                if (value) {
                  this.formData.warehouseId = value.id;
                  this.getCurrentStock();
                }
              },
            })}
            </div>
            <div class="flex">
            {this.renderAutoComplete({
              fieldname: "productId",
              placeholder: messages.common.inventory,
              defaultKey: "productId",
              options: this.props.dropdowns?.product || [],
              disableClearable: true,
              required: true,
              disabled: !this.isAdmin,
              getOption: (option) => {
                return option["name"];
              },
              skipAdd: true,
              onChange: (e, value) => {
                if (value) {
                  this.formData.productId = value.id;
                  this.getCurrentStock();
                }
              },
            })}
            </div>
            <div class="flex">
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
                //disabled: !this.isAdmin,
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
            {/* Batch section — shown only when product is batch-tracked */}
            {this.renderBatchSection()}

            <div class="flex">
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
  return {
    units: state.units.units,
  };
};
export default connect(mapStateToProps, null, null, { forwardRef: true })(
  withSnackbar(Edit)
);
