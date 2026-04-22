//react
import React from "react";
//third party
import { withSnackbar } from "notistack";
import EditForm from "./../../../Shared/EditForm";
//misc
import { apiEndpoints } from "./../../../endpoints";
import { messages } from "./../../../messages";

class Edit extends EditForm {
  title = messages.common.propertySubTypes;
  updateUrl = apiEndpoints.properySubTypes;
  state = { isLoaded: false };
  componentDidMount() {
    this.updateUrl = this.updateUrl + this.props.data.propertyNameId;
    this.formData.name = this.props.data.name;
    this.formData.unitDetails = this.props.data.unitDetails;
    this.formData.phase = this.props.data.phase;
    this.formData.plotSize = this.props.data.plotSize;
    this.formData.superBuiltupArea = this.props.data.superBuiltupArea;
    this.setState({ isLoaded: true });
  }

  render() {
    console.log(this.props.data);
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
            <div className="flex width50">
              {this.renderTextField({
                fieldname: "unitDetails",
                placeholder: "Plot Size",
                required: false,
                lengthConstraint: 10,
                validation: "maxlength",
              })}
            </div>

            <div className="flex width50">
              {this.renderTextField({
                fieldname: "plotSize",
                placeholder: "Enter Plot Area",
                required: false,
                lengthConstraint: 10,
                validation: "maxlength",
              })}
            </div>
            <div className="flex width50">
              {this.renderTextField({
                fieldname: "superBuiltupArea",
                placeholder: "Enter Super Builtup Area",
                required: false,
                lengthConstraint: 10,
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
