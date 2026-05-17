//react
import React from "react";
//third party
import { Slide } from "@material-ui/core";
//components
import List from "./list";
import Common from "./../../Shared/CommonIndex";
//misc
import { messages } from "./../../messages";

class BOQStatus extends Common {
  state = {
    list: true,
  };
  render() {
    this.title = this.props.pageTitle || messages.common.boqStatus;
    const breadcrumbTitle = this.props.breadcrumbTitle || messages.common.boq;
    return (
      <div className="page">
        <div className="header-info">
          <div>
            {this.renderHeading()}
            {this.renderBreadcrums(breadcrumbTitle)}
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

export default BOQStatus;
