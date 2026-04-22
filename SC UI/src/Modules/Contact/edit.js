//react
import React from "react";
import { connect } from "react-redux";
//third party
import { withSnackbar } from "notistack";
//misc
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";
//style
import "./style.scss";
import FormLabel from "@material-ui/core/FormLabel";
import EditForm from "./../../Shared/EditForm";
import { fields } from "./../../fieldnames";

class Edit extends EditForm {
  state = { isLoaded: false };
  title = messages.common.contact;
  updateUrl = apiEndpoints.updateContact;
  radioOptions = [
    // { label: "Customer", value: "CUSTOMER" },
    { label: "Contractor", value: "CONTRACTOR" },
    { label: "Supplier", value: "SUPPLIER" },
  ];
  componentDidMount() {
    this.updateUrl = this.updateUrl + this.props.id;
    this.search();
  }
  async search() {
    const response = await API.GET(
      apiEndpoints.getContactDetail + this.props.id
    );
    if (response.success) {
      const data = response.data;
      this.formData = { ...data };
      this.setState({
        isLoaded: true,
      });
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
                fieldname: "name",
                placeholder: messages.common.contactCompanyName,
                required: true,
                validation: "maxlength",
                lengthConstraint: 50,
                errorMessage: messages.common.contactNameError,
              })}
            </div>
            <div className="flex width50">
              {this.renderRadio({
                label: "Type of Vendor",
                options: this.radioOptions,
                fieldname: "contactType",
                defaultValue: this.formData.contactType,
                classname: "radio",
                onChange: (value) => {
                  if (value === "CUSTOMER") {
                    this.fieldRefs.contactPerson.setValue("");
                    this.fieldRefs.gstNumber.setValue("");
                    this.fieldRefs.contactPersonMobileNo.setValue("");
                  }
                  this.setState({ i: 1 });
                },
              })}

              {this.renderTextField({
                fieldname: "contactPerson",
                placeholder: "Contact Person Name",
                compRef: true,
                disabled: !(
                  this.formData.contactType === "CONTRACTOR" ||
                  this.formData.contactType === "SUPPLIER"
                ),
              })}
            </div>
            <div className="flex width50">
          {this.renderTextField({
            fieldname: "gstNumber",
            placeholder: "GST",
            compRef: true,
            required: false,
            disabled: !(
              this.formData.contactType === "CONTRACTOR" ||
              this.formData.contactType === "SUPPLIER"
            ),
          })}
              {this.renderTextField({
                fieldname: "contactPersonMobileNo",
                placeholder: "Contact Person Mobile Number",
                lengthConstraint: 10,
                compRef: true,
                type: "number",
                validation: "length",
                errorMessage: messages.common.mobileError,
                disabled: !(
                  this.formData.contactType === "CONTRACTOR" ||
                  this.formData.contactType === "SUPPLIER"
                ),
              })}
            </div>
            <div className="flex width50">
              {this.renderTextField({
                fieldname: "emailId",
                placeholder: "Email Id",
              })}
              {this.renderTextField({
                fieldname: "mobileNo",
                placeholder:
                  this.formData.contactType === "CONTRACTOR" ||
                  this.formData.contactType === "SUPPLIER"
                    ? "Office Number"
                    : "Mobile Number",
                lengthConstraint: 10,
                type: "number",
                validation: "length",
                errorMessage: messages.common.mobileError,
                required: false,
              })}
            </div>
            {/* <FormLabel component="legend">Address</FormLabel> */}
            <div className="flex width50">
              {this.renderTextField({
                fieldname: fields.addr_line1,
                placeholder: "Address Line 1",
              })}
              {this.renderTextField({
                fieldname: fields.addr_line2,
                placeholder: "Address Line 2",
              })}
            </div>
<div className="flex width25">
              {this.renderTextField({ fieldname: "city", placeholder: "City" })}
              {this.renderTextField({
                fieldname: "state",
                placeholder: "State",
              })}
              {this.renderTextField({
                fieldname: fields.zip,
                placeholder: messages.common.zip,
                lengthConstraint: 6,
                type: "number",
                validation: "length",
                errorMessage: messages.common.zipError,
              })}
            </div>

            {/* Bank Details */}
            <div className="flex width50">
              {this.renderTextField({
                fieldname: "accountName",
                placeholder: "Account Holder Name",
                validation: "maxlength",
                lengthConstraint: 100,
              })}
              {this.renderTextField({
                fieldname: "accountNumber",
                placeholder: "Account Number",
                validation: "maxlength",
                lengthConstraint: 18
              })}
            </div>
            <div className="flex width25">
              {this.renderTextField({
                fieldname: "bankName",
                placeholder: "Bank Name",
                validation: "maxlength",
                lengthConstraint: 50,
              })}
              {this.renderTextField({
                fieldname: "branchName",
                placeholder: "Branch Name",
                validation: "maxlength",
                lengthConstraint: 50,
              })}
              {this.renderTextField({
                fieldname: "ifscCode",
                placeholder: "IFSC Code (e.g. KKBK0000561)",
                validation: "length",
                lengthConstraint: 11,
                errorMessage: "IFSC Code must be exactly 11 characters",
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
    roles: state.roles.roles,
  };
};
export default connect(mapStateToProps, null, null, { forwardRef: true })(
  withSnackbar(Edit)
);
