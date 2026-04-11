//react
import React from "react";
//third party
import { withSnackbar } from "notistack";
//misc
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";

import EditForm from "./../../Shared/EditForm";
class Edit extends EditForm {
  title = messages.common.warehouse;
  updateUrl = apiEndpoints.updateWarehouse;
  state = {
    data: {},
    isLoaded: false,
  };

  componentDidMount() {
    this.updateUrl = this.updateUrl + this.props.data.id;
    this.search();
  }
  async search() {
    const data = this.props.data;
    this.formData.warehouseName = data.name;
    this.setState({
      isLoaded: true,
    });
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
            <div class="flex">
            {this.renderTextField({
              fieldname: "warehouseName",
              placeholder: messages.common.warehouse,
              required: true,
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
