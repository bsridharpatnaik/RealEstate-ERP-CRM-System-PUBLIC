//react
import React from "react";
//third party
import { withSnackbar } from "notistack";
import EditForm from "./../../../Shared/EditForm";
//misc
import { API } from "./../../../axios";
import { apiEndpoints } from "./../../../endpoints";
import { messages } from "./../../../messages";

class Edit extends EditForm {
  updateUrl = apiEndpoints.individualLead;
  title = messages.common.lead;
  source_broker = 0;
  state = { isLoaded: false, disableBroker: true };
  assigneeDetails =
    this.props?.dropdowns?.assigneeDetails?.assigneeDetails?.map((item) => {
      return {
        name: item.userName,
        id: item.userid,
      };
    });
  componentDidMount() {
    console.log(this.updateUrl);
    this.updateUrl = this.updateUrl + this.props?.id;
    this.search();
    this.source_broker = this.props.dropdowns?.sourceDetails.filter(
      (x) => x.name.toLowerCase() === "broker"
    )[0]?.id;
  }
  async search() {
    const response = await API.GET(this.updateUrl);
    if (response.success) {
      const data = response.data;
      this.formData.customerName = data.customerName;
      this.formData.primaryMobile = data.primaryMobile;
      this.formData.secondaryMobile = data.secondaryMobile;
      this.formData.occupation = data.occupation;
      this.formData.emailId = data.emailId;
      this.formData.purpose = data.purpose;
      this.formData.dateOfBirth = data.dateOfBirth;
      this.formData.brokerId = this.props.dropdowns?.brokerDetails.filter(
        (x) => x.name.toLowerCase() === data.broker.toLowerCase()
      )[0]?.id;
      this.formData.sourceId = data.source
        ? this.props.dropdowns?.sourceDetails.filter(
          (x) => x.name.toLowerCase() === data.source.toLowerCase()
        )[0]?.id
        : undefined;
      this.formData.addressLine1 = data.addr_line1;
      this.formData.addressLine2 = data.addr_line2;
      this.formData.city = data.city;
      this.formData.pincode = data.pincode;
      this.formData.sentiment = data.sentiment;
      this.formData.propertyType = data.propertyType;
      this.formData.isProspectLead = data.isProspectLead;
      this.formData.assigneeId =
        this.props.dropdowns?.assigneeDetails?.assigneeDetails.filter(
          (x) => x.userName.toLowerCase() === data.asigneeId.toLowerCase()
        )[0]?.userid;
      this.setState({
        isLoaded: true,
        disableBroker:
          this.formData.sourceId === this.source_broker ? false : true,
      });
    }
  }

  render() {
    return (
      <div className="list-section add">
        {this.renderHeading()}
        {this.state.isLoaded && (
          <form onSubmit={(e) => this.update(e)}>
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
                options: this.assigneeDetails,
                disableClearable: true,
                getOption: (option) => {
                  return option["name"];
                },
                onChange: (e, value) => {
                  if (value) {
                    this.formData[messages.formParams.lead.assigneeId] =
                      value.id;
                  }
                },
                skipAdd: true,
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
                getDefaultValue: () => this.formData.purpose,
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
                getDefaultValue: () => this.formData.propertyType,
                getOption: (option) => {
                  return option;
                },
                onChange: (e, value) => {
                  if (value) {
                    this.formData[messages.formParams.lead.propertyType] =
                      value;
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
                skipAdd: true,
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
                getDefaultValue: () => this.formData.sentiment,
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
        )}
      </div>
    );
  }
}

export default withSnackbar(Edit);
