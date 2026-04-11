//react
import React from "react";
//third party
import { withSnackbar } from "notistack";
import EditForm from "./../../Shared/EditForm";
//misc
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";

class Edit extends EditForm {
  updateUrl = apiEndpoints.individualBuildingType;
  title = messages.common.buildingType;

  state = { isLoaded: false };
  componentDidMount() {
    this.updateUrl = this.updateUrl + this.props.id;

    this.search();
  }
  async search() {
    const response = await API.GET(this.updateUrl);
    if (response.success) {
      const data = response.data;
      this.formData.typeName = data.typeName;
      this.formData.typeDescription = data.typeDescription;
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
                fieldname: "typeName",
                placeholder: messages.fields.buildingTypeName,
                required: true,
              })}
            </div>
            <div class="flex">
              {this.renderTextField({
                fieldname: "typeDescription",
                placeholder: messages.common.description,
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
