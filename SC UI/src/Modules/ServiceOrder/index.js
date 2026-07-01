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

class ServiceOrder extends Common {
  state = {
    list: true,
    add: false,
    editData: null,
  };
  title = "Service Order";

  render() {
    return (
      <div className="page service-order-page">
        {this.state.list && (
          <div className="header-info">
            <div>
              {this.renderHeading()}
              {this.renderBreadcrums(messages.common.inventory)}
            </div>
          </div>
        )}
        <Slide direction="right" in={this.state.list} mountOnEnter unmountOnExit timeout={{ exit: 0 }} appear={false}>
          <div>
            <List
              edit={(data) => this.setState({ add: true, list: false, editData: data })}
              onAdd={() => this.setState({ add: true, list: false, editData: null })}
            />
          </div>
        </Slide>
        <Slide direction="left" in={this.state.add} mountOnEnter unmountOnExit timeout={{ exit: 0 }} appear={false}>
          <div>
            <Add
              editData={this.state.editData}
              back={() => this.setState({ add: false, list: true, editData: null })}
            />
          </div>
        </Slide>
      </div>
    );
  }
}

export default ServiceOrder;
