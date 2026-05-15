//react
import React from "react";
//third party
import { withSnackbar } from "notistack";
import AddForm from "./../../Shared/AddForm";

//misc
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";
import Checkbox from "@material-ui/core/Checkbox";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import { API } from "./../../axios";
import "./style.scss";
class Add extends AddForm {
  title = messages.common.location;
  addurl = apiEndpoints.createLocation;
  state = { isAdding: false, isTag: false, options: [] };
  async componentDidMount() {
    const response = await API.GET(apiEndpoints.buildingType);
    if (response.success) {
      this.setState({ options: response.data });
    }
  }
  render() {
    return (
      <div className="list-section add">
        {this.renderHeading()}
        <form onSubmit={(e) => this.add(e)}>
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
      </div>
    );
  }
}

export default withSnackbar(Add);
