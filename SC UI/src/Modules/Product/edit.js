//react
import React from "react";
import { connect } from "react-redux";

//third party
import { withSnackbar } from "notistack";
import EditForm from "./../../Shared/EditForm";
//misc
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";

class Edit extends EditForm {
  updateUrl = apiEndpoints.individualProduct;
  title = messages.common.product;

  state = {
    data: {},
    isLoaded: false,
    categoriesLoaded: false,
    batchMode: 'NONE',
    originalBatchMode: 'NONE',
    batchModeChanged: false,
    categories: [],
  };

  componentDidMount() {
    this.updateUrl = this.updateUrl + this.props.id;
    this.search();
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
        categoriesLoaded: true,
      });
    }
  }

  async search() {
    const response = await API.GET(this.updateUrl);
    if (response.success) {
      const data = response.data;
      this.formData.productName = data.productName;
      this.formData.reorderQuantity = data.reorderQuantity;
      this.formData.productDescription = data.productDescription;
      this.formData.measurementUnit = data.measurementUnit;
      this.formData.categoryId = data.category.categoryId;
      this.formData.showOnDashboard = data.showOnDashboard;
      this.formData.isManagedInventory = data.isManagedInventory !== undefined ? data.isManagedInventory : true;
      // batchMode supersedes the old isExpirable boolean
      this.formData.batchMode = data.batchMode || (data.isExpirable ? 'BATCH_WITH_EXPIRY' : 'NONE');
      this.setState({ isLoaded: true, batchMode: this.formData.batchMode, originalBatchMode: this.formData.batchMode });
    }
  }

  async update(event) {
    event.preventDefault();
    if (this.state.batchModeChanged) {
      const confirmed = window.confirm(
        'Warning: Changing the batch tracking mode for an existing product may cause inconsistencies with existing inventory.\n\n' +
        'Existing stock without batch data will remain untracked, and FIFO ordering may be affected for in-progress batches.\n\n' +
        'Are you sure you want to continue?'
      );
      if (!confirmed) return;
    }
    this.setState({ isUpdating: true });
    const response = await API.PUT(this.updateUrl, this.formData);
    this.showToaster(response);
    this.setState({ isUpdating: false });
  }

  render() {
    return (
      <div className="list-section add">
        {this.renderHeading()}
        {this.state.isLoaded && this.state.categoriesLoaded && (
          <form onSubmit={(e) => this.update(e)}>
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
                  return option.name;
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
                  value={this.state.batchMode}
                  onChange={(e) => {
                    this.formData.batchMode = e.target.value;
                    this.setState({
                      batchMode: e.target.value,
                      batchModeChanged: e.target.value !== this.state.originalBatchMode,
                    });
                  }}
                >
                  <option value="NONE">No Batch Tracking</option>
                  <option value="BATCH_ONLY">Track by Identifier / Lot (no expiry)</option>
                  <option value="BATCH_WITH_EXPIRY">Track by Identifier / Lot + Expiry Date</option>
                </select>
                {this.state.batchModeChanged && (
                  <span style={{ fontSize: '11px', color: '#e65100', marginTop: '3px' }}>
                    ⚠ Changing this may affect existing inventory records
                  </span>
                )}
              </div>
              {this.renderFooter()}
            </div>
          </form>
        )}
      </div>
    );
  }
}

export default connect(null, null, null, { forwardRef: true })(
  withSnackbar(Edit)
);
