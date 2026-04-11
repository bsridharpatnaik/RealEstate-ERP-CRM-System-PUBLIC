import React from "react";
import { Slide } from "@material-ui/core";
import List from "./list";
import Common from "../../Shared/CommonIndex";
import { messages } from "../../messages";

class StockAgingReport extends Common {
  state = {
    list: true,
  };

  title = messages.common.stockAgingReport;

  render() {
    return (
      <div className="page">
        <div className="header-info">
          <div>
            {this.renderHeading()}
            {this.renderBreadcrums(messages.common.inventory)}
          </div>
        </div>
        <Slide
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

export default StockAgingReport;
