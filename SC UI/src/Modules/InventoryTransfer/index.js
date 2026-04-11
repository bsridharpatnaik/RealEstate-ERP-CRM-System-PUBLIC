//react
import React from "react";
//third party
import { Slide } from "@material-ui/core";
//components
import List from "./list";
import Add from "./add";
import Common from "./../../Shared/CommonIndex";
//misc
import { messages } from "./../../messages";

class InventoryTransfer extends Common {
  state = {
    add: false,
    list: true,
  };
  title = messages.common.inventoryTransfer;
  render() {
    return (
      <div className="page inventory-transfer-page">
        {this.state.list && (
          <div className="header-info">
            <div>
              {this.renderHeading()}
              {this.renderBreadcrums(messages.common.inventory)}
            </div>
            {this.renderAdd()}
          </div>
        )}
        {this.state.list && (
          <Slide
            direction="right"
            in={this.state.list}
            mountOnEnter
            unmountOnExit
            timeout={{ exit: 0 }}
          >
            <div>
              <List
                isLoading={(bIsLoading) => {
                  if (this.state.list) {
                    this.setState({ isLoading: bIsLoading });
                  }
                }}
                setOptions={(options) => (this.dropdowns = options)}
                onAdd={() => {
                  this.setState({ add: true, list: false });
                }}
              />
            </div>
          </Slide>
        )}
        {this.state.add && (
          <Slide
            direction="left"
            in={this.state.add}
            mountOnEnter
            unmountOnExit
            timeout={{ exit: 0 }}
          >
            <div>
              <Add
                back={() => {
                  this.setState({ add: false, list: true });
                }}
                dropdowns={this.dropdowns || {}}
              />
            </div>
          </Slide>
        )}
      </div>
    );
  }
}

export default InventoryTransfer;
