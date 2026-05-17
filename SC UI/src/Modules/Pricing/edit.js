//react
import React from "react";
import { connect } from "react-redux";

//third party
import { withSnackbar } from "notistack";
import EditForm from "./../../Shared/EditForm";
import { API } from "./../../axios";

//misc
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";
import Button from "./../../Shared/Button";
//style
import "./style.scss";
import IconButton from "@material-ui/core/IconButton";
import KeyboardBackspaceIcon from "@material-ui/icons/KeyboardBackspace";

class Edit extends EditForm {
  title = messages.common.pricingHeader;
  updateUrl = apiEndpoints.updatePrice;
  state = {
    isLoaded: true
  };
  key = 1;

  componentDidMount() {
    this.search();
  }
  async search() {
    const {data} = this.props;
    this.formData.id = data.id;
    this.formData.price = data.price;
    this.formData.date = data.date;
  }
  async update(event) {
    event.preventDefault();
    if (Object.keys(this.formValidation).length > 0) {
      this.props.enqueueSnackbar("Please fix the errors!", {
        variant: "error",
      });
      return;
    }
    if (!this.formData.price) {
      this.props.enqueueSnackbar("Please add a price!", {
        variant: "error",
      });
      return;
    }
    const params = this.formData;
    params.productId = params.id;
    delete params.id;
    this.setState({ isUpdating: true });
    const response = await API.POST(this.updateUrl, params);
    this.showToaster(response);
    this.setState({ isUpdating: false });
  }
  renderFooter() {
    return (
      <div className="form-footer">
        <Button
          onClick={this.props.back}
          buttonClass="grey"
          label={messages.common.cancel}
        />
        <Button
          type="submit"
          buttonClass="blue"
          label={messages.common.save}
          disabled={this.state.isUpdating}
        />
      </div>
    );
  }

  renderHeading() {
    return (
        <div className="add-heading-wrapper">
          <IconButton
              aria-label="back"
              onClick={this.props.back}
              className="back-icon"
          >
            <KeyboardBackspaceIcon />
          </IconButton>
          <span className="add-heading">
          {(this.props.isEdit ? messages.common.update : messages.common.add) + this.title}
        </span>
        </div>
    );
  }
  render() {
    const {dropdowns, isEdit, data} = this.props;
    return (
      <div className="list-section add">
        {this.renderHeading()}
        {this.state.isLoaded && (
          <form onSubmit={(e) => this.update(e)}>
            <div className="flex width30">
              {this.renderAutoComplete({
                fieldname: "id",
                placeholder: "Product",
                options: dropdowns.product,
                disableClearable: true,
                required: true,
                data: data,
                getOption: (option) => {
                  return option["name"];
                },
                disabled: isEdit,
              })}
              {this.renderTextField({
                fieldname: "date",
                placeholder: "Month (yyyy-MM)",
                validation: "regex",
                regexConstraint: /^(\d{4}-\d{2})$/,
                data: data,
                errorMessage: "yyyy-MM",
                disabled: isEdit,
                required: true,
              })}
              {this.renderTextField({
                fieldname: "price",
                placeholder: "Price",
                type: "number",
                data: data,
                errorMessage: messages.common.contactNameError,
                required: true,
              })}
            </div>

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
