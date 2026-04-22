//react
import React from "react";
import { connect } from "react-redux";

//third party
import { withSnackbar } from "notistack";
import AddForm from "./../../Shared/AddForm";
import { API } from "./../../axios";

//misc
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";
import { fetchUnit } from "./../../actions/measurementUnit";
import moment from "moment";

class Add extends AddForm {
  title = messages.common.lost;
  addurl = apiEndpoints.createLost;
  state = { closing: null };
  componentDidMount() {
    const { dispatch } = this.props;
    dispatch(fetchUnit());
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
      closing = Number(response.data) - Number(this.formData.quantity);
      this.setState({ closing: closing });
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
  render() {
    return (
      <div className="list-section add">
        {this.renderHeading()}
        <form onSubmit={(e) => this.add(e)}>
         <div class="flex">
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
              }
            },
          })}
          </div>
          <div class="flex">
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
              placeholder: "Location",
              required: true,
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
