//react
import React from "react";
//third party
import { withSnackbar } from "notistack";
import AddForm from "./../../../Shared/AddForm";

//misc
import { apiEndpoints } from "./../../../endpoints";
import { messages } from "./../../../messages";

class Add extends AddForm {
  title = messages.common.broker;
  addurl = apiEndpoints.createBroker;
  render() {
    return (
      <div className="list-section add">
        {this.renderHeading()}
        <form onSubmit={(e) => this.add(e)}>
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
      </div>
    );
  }
}

export default withSnackbar(Add);
