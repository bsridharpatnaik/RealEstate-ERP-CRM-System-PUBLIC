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
  updateUrl = apiEndpoints.individualCategory;
  title = messages.common.category;

  state = { isLoaded: false };
  componentDidMount() {
    this.updateUrl = this.updateUrl + this.props.id;

    this.search();
  }
  async search() {
    const response = await API.GET(this.updateUrl);
    if (response.success) {
      const data = response.data;
      this.formData.categoryName = data.categoryName;
      this.formData.categoryDescription = data.categoryDescription;
      this.formData.leadTimeDays = data.leadTimeDays != null ? String(data.leadTimeDays) : "";
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
              fieldname: "categoryName",
              placeholder: messages.fields.categoryName,
              required: true,
            })}
            </div>
            <div class="flex">
            {this.renderTextField({
              fieldname: "categoryDescription",
              placeholder: messages.common.description,
            })}
            </div>
            <div class="flex">
            {this.renderTextField({
              fieldname: "leadTimeDays",
              placeholder: "Lead Time (Days)",
              type: "number",
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
