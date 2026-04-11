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
  updateUrl = apiEndpoints.individualBroker;
  title = messages.common.broker;

  state = { isLoaded: false };
  componentDidMount() {
    this.updateUrl = this.updateUrl + this.props.id;

    this.search();
  }
  async search() {
    const response = await API.GET(this.updateUrl);
    if (response.success) {
      const data = response.data;
      this.formData.brokerName = data.brokerName;
      this.formData.brokerAddress = data.brokerAddress;
      this.formData.brokerPhoneno = data.brokerPhoneno;

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
              fieldname: messages.formParams.broker.brokerName,
              placeholder: messages.common.broker,
              required: true,
            })}
            </div>
            <div class="flex">
            {this.renderTextField({
              fieldname: messages.formParams.broker.brokerPhoneno,
              placeholder: messages.fields.mobileNo,
              lengthConstraint: 10,
              type: "number",
              validation: "length",
              required: true,
            })}
            </div>
            <div class="flex">
            {this.renderTextArea({
              fieldname: messages.formParams.broker.brokerAddress,
              placeholder: messages.fields.address,
            })}
            </div>
            {this.renderFooter()}
          </form>
        )}
      </div>
    );
  }
}

export default withSnackbar(Edit);
