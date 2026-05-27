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

class Add extends AddForm {
  title = messages.common.lost;
  addurl = apiEndpoints.createLost;
  state = {
    closing: null,
    entryType: "LOST_DAMAGED",
    // Batches available for selected product+warehouse
    availableBatches: [],
    selectedBatchId: null,
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
    if (!productId || !warehouseId) {
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

  /** Load available batches for the currently selected product+warehouse. */
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
      this.props.enqueueSnackbar("Add atleast one file", {
        variant: "error",
      });
      return;
    }
    super.add(event);
  }

  renderBatchSection() {
    const { entryType, availableBatches } = this.state;
    const isBatchTracked = availableBatches.length > 0;

    if (!isBatchTracked) return null;

    if (entryType === "LOST_DAMAGED") {
      // User must select which batch the loss came from
      return (
        <div className="flex" style={{ marginTop: 8 }}>
          <div style={{ display: "flex", flexDirection: "column", gap: 4, flex: 1 }}>
            <label style={{ fontSize: 12, color: "#666", fontWeight: 500 }}>
              Batch (select which batch the loss came from)
            </label>
            <select
              style={{
                padding: "8px 10px",
                border: "1px solid #ccc",
                borderRadius: 4,
                fontSize: 13,
              }}
              value={this.state.selectedBatchId || ""}
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
                  b.expiryDate
                    ? "Exp: " + moment(b.expiryDate).format("DD-MM-YYYY")
                    : null,
                  `Qty: ${b.qtyRemaining}`,
                ]
                  .filter(Boolean)
                  .join(" | ");
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
      // User enters batch details for the new stock being added
      return (
        <div style={{ marginTop: 8 }}>
          <div style={{ fontSize: 12, color: "#666", fontWeight: 500, marginBottom: 4 }}>
            Batch Details (optional — fill to track this excess stock as a batch)
          </div>
          <div className="flex width50">
            {this.renderTextField({
              fieldname: "brand",
              placeholder: "Identifier / Lot",
              onChange: (value) => {
                this.formData.brand = value;
              },
            })}
            {this.renderTextField({
              fieldname: "lotNumber",
              placeholder: "Lot Number",
              onChange: (value) => {
                this.formData.lotNumber = value;
              },
            })}
          </div>
          {/* Show expiry date only if the product is BATCH_WITH_EXPIRY.
              Since we can't easily distinguish from batch list alone,
              we show it as optional — backend will validate if needed. */}
          <div className="flex width50">
            {this.renderDate({
              fieldname: "expiryDate",
              label: "Expiry Date (if applicable)",
              minDate: moment(),
              required: false,
            })}
          </div>
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
          <div className="flex">
            <FormControl component="fieldset">
              <RadioGroup
                row
                value={this.state.entryType}
                onChange={(e) => {
                  this.formData.entryType = e.target.value;
                  // Clear batch fields when type changes
                  this.formData.batchId = null;
                  this.formData.brand = null;
                  this.formData.lotNumber = null;
                  this.formData.expiryDate = null;
                  this.setState(
                    { entryType: e.target.value, selectedBatchId: null },
                    () => this.getCurrentStock()
                  );
                }}
              >
                <FormControlLabel value="LOST_DAMAGED" control={<Radio color="primary" />} label="Lost / Damaged" />
                <FormControlLabel value="EXCESS_FOUND" control={<Radio color="primary" />} label="Excess Found" />
              </RadioGroup>
            </FormControl>
          </div>
          <div className="flex">
            {this.renderAutoComplete({
              fieldname: "warehouseId",
              placeholder: "Warehouse",
              options: this.props.dropdowns?.warehouse || [],
              disableClearable: true,
              required: true,
              getOption: (option) => {
                return option["name"];
              },
              onChange: (e, value) => {
                if (value) {
                  this.formData.warehouseId = value.id;
                  this.getCurrentStock();
                  this.loadBatches();
                }
              },
            })}
          </div>
          <div className="flex">
            {this.renderAutoComplete({
              fieldname: "productId",
              placeholder: messages.common.inventory,
              options: this.props.dropdowns?.product || [],
              disableClearable: true,
              required: true,
              getOption: (option) => {
                return option["name"];
              },
              onChange: (e, value) => {
                if (value) {
                  this.formData.productId = value.id;
                  this.getCurrentStock();
                  this.loadBatches();
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
              validation: "nonegative",
              onChange: (value) => {
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
              maxDate: moment(),
              minDate: moment().add(-7, 'd'),
            })}
            {this.renderTextField({
              fieldname: "theftLocation",
              placeholder: this.state.entryType === "EXCESS_FOUND" ? "Remarks" : "Location",
              required: true,
            })}
          </div>

          {/* Batch section — shown only when product+warehouse selected and product is batch-tracked */}
          {this.renderBatchSection()}

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
const mapStateToProps = (state) => {
  return {
    units: state.units.units,
  };
};
export default connect(mapStateToProps, null, null, { forwardRef: true })(
  withSnackbar(Add)
);
