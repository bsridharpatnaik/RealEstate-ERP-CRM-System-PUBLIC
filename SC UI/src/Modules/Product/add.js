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
class Add extends AddForm {
  title = messages.common.inventory;
  addurl = apiEndpoints.createProduct;

  state = { categories: [] };

  constructor(props) {
    super(props);
    this.formData.isManagedInventory = true;
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
  render() {
    return (
      <div className="list-section add">
        {this.renderHeading()}
        <form onSubmit={(e) => this.add(e)}>
          <div class="flex">
            {this.renderTextField({
              fieldname: "productName",
              placeholder: messages.common.inventory,
              required: true,
            })}
          </div>
          <div class="flex">
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
              validation: "positive",
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
              getOption: (option) => {
                return option["name"];
              },
              onChange: (e, value) => {
                this.formData.categoryId = value.id;
              },
            })}
          </div>
          <div class="flex flex-space-between">
            {this.renderToggle('Show in Dashboard', 'showOnDashboard')}
            {this.renderToggle('Is Managed Inventory', 'isManagedInventory')}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', minWidth: '220px' }}>
              <label style={{ fontSize: '12px', color: '#666', fontWeight: 500 }}>Batch Tracking</label>
              <select
                style={{ padding: '8px 10px', border: '1px solid #ccc', borderRadius: '4px', fontSize: '13px' }}
                defaultValue="NONE"
                onChange={(e) => { this.formData.batchMode = e.target.value; }}
              >
                <option value="NONE">No Batch Tracking</option>
                <option value="BATCH_ONLY">Track by Identifier / Lot (no expiry)</option>
                <option value="BATCH_WITH_EXPIRY">Track by Identifier / Lot + Expiry Date</option>
              </select>
            </div>
            {this.renderFooter()}
          </div>
        </form>
      </div>
    );
  }
}
export default connect(null, null, null, { forwardRef: true })(
  withSnackbar(Add)
);
