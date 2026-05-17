//react
import React from "react";
//third party
import { withSnackbar } from "notistack";
import EditForm from "./../../Shared/EditForm";
import { API } from "./../../axios";

//misc
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";

class Edit extends EditForm {
  title = messages.common.firm;
  updateUrl = apiEndpoints.updateFirm;
  state = {
    data: {},
    isLoaded: false,
  };

  componentDidMount() {
    const id = this.props.data.firmId ?? this.props.data.id;
    this.updateUrl = this.updateUrl + id;
    this.search();
  }

  async search() {
    const id = this.props.data.firmId ?? this.props.data.id;
    const response = await API.GET(apiEndpoints.getFirmDetail + id);
    if (response.success) {
      const data = response.data;
      this.formData.firmName = data.firmName;
      this.formData.firmDescription = data.firmDescription || "";
      this.formData.firmAddress = data.firmAddress || "";
      this.formData.firmGstNumber = data.firmGstNumber || "";
      this.formData.firmPanNumber = data.firmPanNumber || "";
      this.formData.firmContactNumber = data.firmContactNumber || "";
      this.setState({ isLoaded: true });
    }
  }

  render() {
    return (
      <div className="list-section add">
        {this.renderHeading()}
        {this.state.isLoaded && (
          <form
            onSubmit={(e) => {
              this.update(e);
            }}
          >
            <div className="flex">
              {this.renderTextField({
                fieldname: "firmName",
                placeholder: "Firm Name",
                required: true,
              })}
            </div>
            <div className="flex">
              {this.renderTextField({
                fieldname: "firmDescription",
                placeholder: messages.common.description,
              })}
            </div>
            <div className="flex">
              {this.renderTextField({
                fieldname: "firmAddress",
                placeholder: "Address",
              })}
            </div>
            <div className="flex">
              {this.renderTextField({
                fieldname: "firmGstNumber",
                placeholder: "GST Number",
              })}
            </div>
            <div className="flex">
              {this.renderTextField({
                fieldname: "firmPanNumber",
                placeholder: "PAN Number",
              })}
            </div>
            <div className="flex">
              {this.renderTextField({
                fieldname: "firmContactNumber",
                placeholder: "Contact Number",
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
