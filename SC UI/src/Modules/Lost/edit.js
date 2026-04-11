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
import { fetchUnit } from "./../../actions/measurementUnit";
import moment from "moment";

class Edit extends EditForm {
  updateUrl = apiEndpoints.individualLost;
  title = messages.common.lost;

  state = { isLoaded: false };

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
      this.setState({
        isLoaded: true,
        closing: response.data.closingStock,
      });
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
      let closing = this.state.closing;
      closing = Number(response.data) - Number(this.state.data.quantity);
      this.setState({ closing: closing });
    }
  }
  render() {
    return (
      <div className="list-section add">
        {this.renderHeading()}
        {this.state.isLoaded && (
          <form onSubmit={(e) => this.update(e)}>
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
                placeholder: "Location",
                required: true,
                disabled: !this.isAdmin,
              })}
            </div>
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
