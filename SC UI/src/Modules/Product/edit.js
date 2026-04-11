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
  };

  componentDidMount() {
    this.updateUrl = this.updateUrl + this.props.id;
    this.search();
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
      this.setState({ isLoaded: true });
    }
  }

  render() {
    return (
      <div className="list-section add">
        {this.renderHeading()}
        {this.state.isLoaded && (
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
                options: this.props.categories,
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
              {this.renderFooter()}
            </div>
          </form>
        )}
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    categories: state.categories.categories,
  };
};
export default connect(mapStateToProps, null, null, { forwardRef: true })(
  withSnackbar(Edit)
);
