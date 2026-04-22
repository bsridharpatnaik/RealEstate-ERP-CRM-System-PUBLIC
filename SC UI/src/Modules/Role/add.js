//react
import React from "react";
//third party
import { withSnackbar } from "notistack";
//misc
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";
import AddForm from "./../../Shared/AddForm";

class Add extends AddForm {
  title = messages.fields.role;
  addurl = apiEndpoints.createRole;
  render() {
    return (
      <div className="list-section add">
        {this.renderHeading()}
        <form onSubmit={(e) => this.add(e)}>
        <div class="flex">
          {this.renderTextField({
            fieldname: "name",
            placeholder: messages.fields.role,
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
