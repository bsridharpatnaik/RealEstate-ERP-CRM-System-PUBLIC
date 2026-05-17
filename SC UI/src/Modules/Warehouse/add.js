//react
import React from "react";
//third party
import { withSnackbar } from "notistack";
import AddForm from "./../../Shared/AddForm";

//misc
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";

class Add extends AddForm {
  title = messages.common.warehouse;
  addurl = apiEndpoints.createWarehouse;
  render() {
    return (
      <div className="list-section add">
        {this.renderHeading()}
        <form onSubmit={(e) => this.add(e)}>
        <div class="flex">
          {this.renderTextField({
            fieldname: "warehouseName",
            placeholder: messages.common.warehouse,
            required: true,
          })}
          </div>
          {this.renderFooter()}
        </form>
      </div>
    );
  }
}
export default withSnackbar(Add);
