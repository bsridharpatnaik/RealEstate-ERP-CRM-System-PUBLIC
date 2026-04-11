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
  updateUrl = apiEndpoints.individualUsageArea;
  title = messages.common.finalLocation;

  state = { isLoaded: false };
  componentDidMount() {
    this.updateUrl = this.updateUrl + this.props.id;

    this.search();
  }
  async search() {
    const response = await API.GET(this.updateUrl);
    if (response.success) {
      const data = response.data;
      this.formData.usageAreaName = data.usageAreaName;
      this.formData.usageAreaDescription = data.usageAreaDescription;

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
              fieldname: "usageAreaName",
              placeholder: messages.fields.usageAreaName,
              required: true,
            })}
            </div>
            <div class="flex">
            {this.renderTextField({
              fieldname: "usageAreaDescription",
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
