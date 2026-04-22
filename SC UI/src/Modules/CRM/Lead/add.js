//react
import React from "react";
//third party
import { withSnackbar } from "notistack";
import AddForm from "./../../../Shared/AddForm";

//misc
import { apiEndpoints } from "./../../../endpoints";
import { messages } from "./../../../messages";

class Add extends AddForm {
  title = messages.common.lead;
  addurl = apiEndpoints.createLead;
  importurl = apiEndpoints.importLead;
  source_broker = this.props.dropdowns?.sourceDetails.filter(
    (x) => x.name.toLowerCase() === "broker"
  )[0]?.id;
  state = { disableBroker: true };
  render() {
    return (
      <div className="list-section add">
        {this.renderHeading()}
        <form onSubmit={(e) => this.add(e)}>
          <div className="flex width50">
            {this.renderTextField({
              fieldname: messages.formParams.lead.customerName,
              placeholder: messages.common.customerName,
              required: true,
              lengthConstraint: 20,
              validation: "maxlength",
            })}
            {this.renderTextField({
              fieldname: messages.formParams.lead.primaryMobile,
              placeholder: messages.fields.mobileNo,
              required: true,
              lengthConstraint: 10,
              type: "number",
              validation: "length",
              errorMessage: messages.common.mobileError,
            })}
            {this.renderAutoComplete({
              fieldname: messages.formParams.lead.assigneeId,
              placeholder: messages.common.assignee,
              options: this.props.dropdowns?.assigneeDetails?.assigneeDetails,
              disableClearable: true,
              required: true,
              getOption: (option) => {
                return option["userName"];
              },
              onChange: (e, value) => {
                if (value) {
                  this.formData[messages.formParams.lead.assigneeId] =
                    value.userid;
                }
              },
            })}
          </div>
          <div className="flex width50">
            {this.renderTextField({
              fieldname: messages.formParams.lead.emailId,
              placeholder: messages.fields.email,
            })}
            {this.renderTextField({
              fieldname: messages.formParams.lead.occupation,
              placeholder: messages.common.occupation,
            })}
            {this.renderDate({
              fieldname: messages.formParams.lead.dateOfBirth,
              label: messages.common.dateOfBirth,
              emptyDate: true,
              type: "date",
            })}
          </div>
          <div className="flex width50">
            {this.renderAutoComplete({
              fieldname: messages.formParams.lead.purpose,
              placeholder: messages.common.purpose,
              options: this.props.purposeDropdown,
              getOption: (option) => {
                return option;
              },
              onChange: (e, value) => {
                if (value) {
                  this.formData[messages.formParams.lead.purpose] = value;
                }
              },
            })}
            {this.renderAutoComplete({
              fieldname: messages.formParams.lead.propertyType,
              placeholder: messages.common.propertyType,
              options: this.props.dropdowns?.validPropertyType,
              disableClearable: true,
              getOption: (option) => {
                return option;
              },
              onChange: (e, value) => {
                if (value) {
                  this.formData[messages.formParams.lead.propertyType] = value;
                }
              },
            })}

            {this.renderAutoComplete({
              fieldname: messages.formParams.lead.sourceId,
              placeholder: messages.common.source,
              options: this.props.dropdowns?.sourceDetails,
              disableClearable: true,
              getOption: (option) => {
                return option["name"];
              },
              onChange: (e, value) => {
                if (value) {
                  this.formData[messages.formParams.lead.sourceId] = value.id;
                  if (value.id === this.source_broker) {
                    this.setState({ disableBroker: false });
                  } else {
                    this.formData[messages.formParams.lead.brokerId] = null;
                    this.setState({ disableBroker: true });
                  }
                }
              },
            })}
          </div>

          <div className="flex width25">
            {this.renderAutoComplete({
              fieldname: messages.formParams.lead.brokerId,
              placeholder: messages.common.broker,
              options: this.props.dropdowns?.brokerDetails,
              disableClearable: true,
              getOption: (option) => {
                return option["name"];
              },
              onChange: (e, value) => {
                if (value) {
                  this.formData[messages.formParams.lead.brokerId] = value.id;
                }
              },
              disabled: this.state.disableBroker,
            })}

            {this.renderAutoComplete({
              fieldname: messages.formParams.lead.sentimentId,
              placeholder: messages.common.sentiment,
              options: this.props.dropdowns?.validSentiments,
              disableClearable: true,
              getOption: (option) => {
                return option;
              },
              onChange: (e, value) => {
                if (value) {
                  this.formData[messages.formParams.lead.sentimentId] = value;
                }
              },
            })}
            {this.renderTextField({
              fieldname: messages.formParams.lead.secondaryMobile,
              placeholder: messages.common.secondaryMobile,
              lengthConstraint: 10,
              type: "number",
              validation: "length",
              errorMessage: messages.common.mobileError,
            })}
          </div>
          <div className="flex width25">
            {this.renderTextField({
              fieldname: messages.formParams.lead.addressLine1,
              placeholder: messages.common.add1,
            })}
            {this.renderTextField({
              fieldname: messages.formParams.lead.addressLine2,
              placeholder: messages.common.add2,
            })}
          </div>
          <div className="flex width25">
            {this.renderTextField({
              fieldname: messages.formParams.lead.city,
              placeholder: "City",
            })}
            {this.renderTextField({
              fieldname: messages.formParams.lead.pincode,
              placeholder: messages.common.zip,
              validation: "length",
              lengthConstraint: 6,
              type: "number",
              errorMessage: messages.common.zipError,
            })}
          </div>
          <div class="flex flex-space-between">
            {this.renderToggle('Prospect Lead', 'isProspectLead')}
            {this.renderFooter()}
          </div>
        </form>
      </div>
    );
  }
}

export default withSnackbar(Add);
