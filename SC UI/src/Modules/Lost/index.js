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
import { canCreateInward } from "./../../helper";

class Lost extends Common {
  state = {
    add: false,
    list: true,
  };
  title = messages.common.lost;
  render() {
    return (
      <div className="page">
        <div className="header-info">
          <div>
            {this.renderHeading()}
            {this.renderBreadcrums(messages.common.inventory)}
          </div>
          {canCreateInward() && this.renderAdd()}
        </div>
        <Slide
          direction="right"
          in={this.state.list}
          mountOnEnter
          unmountOnExit
          timeout={{ exit: 0 }}
        >
          <List
            isLoading={(bIsLoading) => this.setState({ isLoading: bIsLoading })}
            setOptions={(options) => (this.dropdowns = options)}
          />
        </Slide>
        <Slide
          direction="left"
          in={this.state.add}
          mountOnEnter
          unmountOnExit
          timeout={{ exit: 0 }}
        >
          <Add
            dropdowns={this.dropdowns}
            back={() => {
              this.setState({ add: false, list: true });
            }}
          />
        </Slide>
      </div>
    );
  }
}

export default Lost;
