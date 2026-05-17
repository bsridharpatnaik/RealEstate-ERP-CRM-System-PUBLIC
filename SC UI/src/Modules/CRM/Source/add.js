//react
import React from "react";
//third party
import { withSnackbar } from "notistack";
import AddForm from "./../../../Shared/AddForm";

//misc
import { apiEndpoints } from "./../../../endpoints";
import { messages } from "./../../../messages";

class Add extends AddForm {
  title = messages.common.source;
  addurl = apiEndpoints.createSource;
  render() {
    return (
      <div className="list-section add">
        {this.renderHeading()}
        <form onSubmit={(e) => this.add(e)}>
          <div class="flex">
          {this.renderTextField({
            fieldname: messages.formParams.source.name,
            placeholder: messages.common.source,
            required: true,
          })}
          </div>
          <div class="flex">
          {this.renderTextArea({
            fieldname: messages.formParams.source.description,
            placeholder: messages.common.description,
          })}
          </div>
          {this.renderFooter()}
        </form>
      </div>
    );
  }
}

export default withSnackbar(Add);
