//react
import React from "react";
//third party
import { Slide } from "@material-ui/core";
//components
import List from "./list";
import Common from "./../../Shared/CommonIndex";
//misc
import { messages } from "./../../messages";

class BOQReport extends Common {
  state = {
    list: true,
  };
  title = "BOQReport";
  render() {
    return (
      <div className="page">
        <div className="header-info">
          <div>
            {this.renderHeading()}
            {this.renderBreadcrums(messages.common.boq)}
          </div>
        </div>
        <Slide
          direction="right"
          in={this.state.list}
          mountOnEnter
          unmountOnExit
          timeout={{ exit: 0 }}
        >
          <List
            setOptions={(options) => (this.dropdowns = options)}
            edit={(data) => {
              this.id = data.ou_inventoryId;
              this.setState({ edit: true, list: false });
            }}
          />
        </Slide>
      </div>
    );
  }
}

export default BOQReport;
