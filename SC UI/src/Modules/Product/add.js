//react
import React from "react";
import { connect } from "react-redux";

//third party
import { withSnackbar } from "notistack";
import AddForm from "./../../Shared/AddForm";
//misc
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";
import "./style.scss";

const BATCH_OPTIONS = [
  {
    value: "NONE",
    label: "No Tracking",
    desc: "Basic products. No batch data captured.",
  },
  {
    value: "BATCH_ONLY",
    label: "Lot / Brand Tracking",
    desc: "Track by supplier lot or brand. FIFO ordering. No expiry date.",
  },
  {
    value: "BATCH_WITH_EXPIRY",
    label: "Lot + Expiry Date",
    desc: "Track lot and expiry date. FEFO (nearest-expiry-first) ordering.",
  },
];

class Add extends AddForm {
  title = messages.common.inventory;
  addurl = apiEndpoints.createProduct;

  state = {
    categories: [],
    batchMode: "NONE",
  };

  constructor(props) {
    super(props);
    this.formData.isManagedInventory = true;
    this.formData.batchMode = "NONE";
  }

  componentDidMount() {
    this.formData.isManagedInventory = true;
    this.fetchCategories();
  }

  async fetchCategories() {
    const response = await API.GET(apiEndpoints.getCategoryIdAndNames);
    if (response.success && Array.isArray(response.data)) {
      this.setState({
        categories: response.data.map((cat) => ({
          id: cat.categoryId ?? cat.id,
          name: cat.categoryName ?? cat.name,
        })),
      });
    }
  }

  renderBatchModeCards() {
    const { batchMode } = this.state;
    return (
      <div style={{ marginBottom: 8, width: "100%" }}>
        <label
          style={{
            fontSize: 12,
            color: "#666",
            fontWeight: 500,
            display: "block",
            marginBottom: 8,
          }}
        >
          Batch Tracking
        </label>
        <div style={{ display: "flex", gap: 12 }}>
          {BATCH_OPTIONS.map(({ value, label, desc }) => {
            const selected = batchMode === value;
            return (
              <label
                key={value}
                style={{
                  flex: 1,
                  display: "flex",
                  gap: 10,
                  padding: "10px 14px",
                  border: selected ? "2px solid #1976d2" : "1px solid #ccc",
                  borderRadius: 6,
                  background: selected ? "#e3f2fd" : "#fff",
                  cursor: "pointer",
                }}
              >
                <input
                  type="radio"
                  name="batchMode"
                  value={value}
                  checked={selected}
                  onChange={() => {
                    this.formData.batchMode = value;
                    this.setState({ batchMode: value });
                  }}
                  style={{ marginTop: 3, flexShrink: 0 }}
                />
                <div>
                  <div style={{ fontWeight: 600, fontSize: 13 }}>{label}</div>
                  <div style={{ fontSize: 11, color: "#666", marginTop: 3 }}>
                    {desc}
                  </div>
                </div>
              </label>
            );
          })}
        </div>
      </div>
    );
  }

  render() {
    return (
      <div className="list-section add">
        {this.renderHeading()}
        <form onSubmit={(e) => this.add(e)}>
          <div className="flex">
            {this.renderTextField({
              fieldname: "productName",
              placeholder: messages.common.inventory,
              required: true,
            })}
          </div>
          <div className="flex">
            {this.renderTextField({
              fieldname: "productDescription",
              placeholder: messages.common.description,
            })}
          </div>
          <div className="flex width50">
            {this.renderTextField({
              fieldname: "reorderQuantity",
              placeholder: "Reorder Level",
              required: true,
              type: "number",
              validation: "nonegative",
            })}
            {this.renderTextField({
              fieldname: "measurementUnit",
              placeholder: messages.fields.measurementUnit,
              required: true,
            })}
            {this.renderAutoComplete({
              fieldname: "categoryId",
              placeholder: "Category",
              options: this.state.categories,
              disableClearable: true,
              required: true,
              getOption: (option) => option["name"],
              onChange: (e, value) => {
                this.formData.categoryId = value.id;
              },
            })}
            {this.renderTextField({
              fieldname: "leadTimeDays",
              placeholder: "Lead Time (Days)",
              type: "number",
            })}
          </div>
          <div className="flex">{this.renderBatchModeCards()}</div>
          <div className="flex">
            {this.renderToggle("Show in Dashboard", "showOnDashboard")}
            {this.renderToggle("Is Managed Inventory", "isManagedInventory")}
          </div>
          {this.renderFooter()}
        </form>
      </div>
    );
  }
}

export default connect(null, null, null, { forwardRef: true })(
  withSnackbar(Add)
);
