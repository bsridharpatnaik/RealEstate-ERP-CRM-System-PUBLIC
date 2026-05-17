//react
import React from "react";
//third party
import { withSnackbar } from "notistack";
import EditForm from "./../../Shared/EditForm";
//misc
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";
import Checkbox from "@material-ui/core/Checkbox";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import "./style.scss";
class Edit extends EditForm {
  updateUrl = apiEndpoints.individualLocation;
  title = messages.common.location;

  state = { isLoaded: false, isTag: false, options: [] };
  componentDidMount() {
    this.updateUrl = this.updateUrl + this.props.id;
    this.getOptions();

    this.search();
  }
  async getOptions() {
    const response = await API.GET(apiEndpoints.buildingType);
    if (response.success) {
      this.setState({ options: response.data });
    }
  }
  async search() {
    const response = await API.GET(this.updateUrl);
    if (response.success) {
      const data = response.data;
      this.formData.locationName = data.locationName;
      this.formData.locationDescription = data.locationDescription;
      if (data.buildingType) {
        this.formData.typeId = data.buildingType.typeId;
        this.setState({ isTag: true });
      }
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
                fieldname: "locationName",
                placeholder: messages.common.location,
                required: true,
              })}
            </div>
            <div class="flex">
              {this.renderTextField({
                fieldname: "locationDescription",
                placeholder: messages.common.description,
              })}
            </div>
            <div className="tag-type flex">
              <FormControlLabel
                className="password-checkbox"
                control={
                  <Checkbox
                    checked={this.state.isTag}
                    name="checkedB"
                    color="primary"
                    onChange={(event) => {
                      this.setState({
                        isTag: event.target.checked,
                      });
                      if (!event.target.checked) {
                        delete this.formData["typeId"];
                      }
                    }}
                  />
                }
                label={"Tag to Structure Type"}
              />
              {this.state.isTag &&
                this.renderAutoComplete({
                  fieldname: "typeId",
                  placeholder: "Structure Type",
                  options: this.state.options,
                  multiple: false,
                  disabled: !this.state.isTag,
                  getOption: (option) => {
                    return option["name"];
                  },
                  onChange: (e, value) => {
                    if (value) {
                      this.formData["typeId"] = value.id;
                    }
                  },
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
