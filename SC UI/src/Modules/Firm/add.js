//react
import React from "react";
//third party
import { withSnackbar } from "notistack";
import AddForm from "./../../Shared/AddForm";

//misc
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";

class Add extends AddForm {
  title = messages.common.firm;
  addurl = apiEndpoints.createFirm;
  render() {
    return (
      <div className="list-section add">
        {this.renderHeading()}
        <form onSubmit={(e) => this.add(e)}>
          <div className="flex">
            {this.renderTextField({
              fieldname: "firmName",
              placeholder: "Firm Name",
              required: true,
            })}
          </div>
          <div className="flex">
            {this.renderTextField({
              fieldname: "firmDescription",
              placeholder: messages.common.description,
            })}
          </div>
          <div className="flex">
            {this.renderTextField({
              fieldname: "firmAddress",
              placeholder: "Address",
            })}
          </div>
          <div className="flex">
            {this.renderTextField({
              fieldname: "firmGstNumber",
              placeholder: "GST Number",
            })}
          </div>
          <div className="flex">
            {this.renderTextField({
              fieldname: "firmPanNumber",
              placeholder: "PAN Number",
            })}
          </div>
          <div className="flex">
            {this.renderTextField({
              fieldname: "firmContactNumber",
              placeholder: "Contact Number",
            })}
          </div>
          {this.renderFooter()}
        </form>
      </div>
    );
  }
}
export default withSnackbar(Add);
