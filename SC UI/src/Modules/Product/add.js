//react
import React from "react";
import { connect } from "react-redux";

//third party
import { withSnackbar } from "notistack";
import AddForm from "./../../Shared/AddForm";
//misc
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";
import "./style.scss";
class Add extends AddForm {
  title = messages.common.inventory;
  addurl = apiEndpoints.createProduct;

  constructor(props) {
    super(props);
    this.formData.isManagedInventory = true;
  }

  componentDidMount() {
    // Ensure the value is set even if constructor wasn't called
    this.formData.isManagedInventory = true;
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
              options: this.props.categories,
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
            {this.renderToggle('Track Expiry Date', 'isExpirable')}
            {this.renderFooter()}
          </div>
        </form>
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
  withSnackbar(Add)
);
