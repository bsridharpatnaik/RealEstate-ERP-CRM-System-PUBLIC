//react
import React from "react";
//third party
import { withSnackbar } from "notistack";
import EditForm from "./../../../Shared/EditForm";
//misc
import { API } from "./../../../axios";
import { apiEndpoints } from "./../../../endpoints";
import { messages } from "./../../../messages";

class Edit extends EditForm {
  updateUrl = apiEndpoints.individualSentiment;
  title = messages.common.sentiment;

  state = { isLoaded: false };
  componentDidMount() {
    this.updateUrl = this.updateUrl + this.props.id;

    this.search();
  }
  async search() {
    const response = await API.GET(this.updateUrl);
    if (response.success) {
      const data = response.data;
      this.formData.name = data.name;
      this.formData.description = data.description;

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
              fieldname: messages.formParams.sentiment.name,
              placeholder: messages.common.sentiment,
              required: true,
            })}
            </div>
            <div class="flex">
            {this.renderTextArea({
              fieldname: messages.formParams.sentiment.description,
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
