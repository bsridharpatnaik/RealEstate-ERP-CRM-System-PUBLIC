//react
import React from "react";
//third party
import { Slide } from "@material-ui/core";
//components
import List from "./list";
import Common from "./../../Shared/CommonIndex";
//misc
import { messages } from "./../../messages";

class InventoryTransaction extends Common {
  state = {
    list: true,
  };
  render() {
    const filter = this.props.match.params.filter || "";
    this.title = this.props.pageTitle || messages.common.allInventory;
    const breadcrumbTitle = this.props.breadcrumbTitle || messages.common.inventory;
    return (
      <div className="page">
        <div className="header-info">
          <div>
            {this.renderHeading()}
            {this.renderBreadcrums(breadcrumbTitle)}
          </div>
        </div>
        <Slide
          filter={filter}
          direction="right"
          in={this.state.list}
          mountOnEnter
          unmountOnExit
          timeout={{ exit: 0 }}
        >
          <List />
        </Slide>
      </div>
    );
  }
}

export default InventoryTransaction;
