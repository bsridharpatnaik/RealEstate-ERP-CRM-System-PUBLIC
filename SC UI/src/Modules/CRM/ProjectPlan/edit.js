//react
import React from "react";
//third party
import { withSnackbar } from "notistack";
import EditForm from "./../../../Shared/EditForm";
//misc
import { apiEndpoints } from "./../../../endpoints";
import { messages } from "./../../../messages";

class Edit extends EditForm {
  title = messages.common.propertyType;
  updateUrl = apiEndpoints.properyTypes;
  state = { isLoaded: false };
  componentDidMount() {
    this.updateUrl = this.updateUrl + this.props.data.propertyTypeId;
    this.formData.name = this.props.data.propertyType;
    this.setState({ isLoaded: true });
  }

  render() {
    return (
      <div className="list-section add project-property-add">
        {this.renderHeading()}
        {this.state.isLoaded && (
          <form onSubmit={(e) => this.update(e)}>
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
        )}
      </div>
    );
  }
}

export default withSnackbar(Edit);
