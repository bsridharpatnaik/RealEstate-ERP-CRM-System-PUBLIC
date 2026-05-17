//react
import React from "react";
//third party
import { withSnackbar } from "notistack";
import AddForm from "./../../../Shared/AddForm";

//misc
import { apiEndpoints } from "./../../../endpoints";
import { messages } from "./../../../messages";

class Add extends AddForm {
  title = messages.common.propertyType
  addurl = apiEndpoints.properyTypes;
  render() {
    return (
      <div className="list-section add project-property-add">
        {this.renderHeading()}
        <form onSubmit={(e) => this.add(e)}>
          <div className="flex width50">
            {this.renderTextField({
              fieldname: "name",
              placeholder: "Enter Property Type Name",
              required: true,
              lengthConstraint: 120,
              validation: "maxlength",
            })}
          </div>
          {this.renderFooter()}
        </form>
      </div>
    );
  }
}

export default withSnackbar(Add);
