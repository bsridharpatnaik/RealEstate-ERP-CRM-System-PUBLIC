//react
import React from "react";
//third party
import { Slide } from "@material-ui/core";
//components
import List from "./list";
import Button from "./../../Shared/Button";
import Common from "./../../Shared/CommonIndex";
//misc
import { messages } from "./../../messages";

class DeadStock extends Common {
  state = {
    list: true,
    syncTriggerAt: null,
  };
  title = messages.common.stockSummary;

  renderAdd() {
    return (
      <Button
        onClick={() => this.setState({ syncTriggerAt: Date.now() })}
        label={messages.common.syncNow}
        buttonClass="add-button"
      />
    );
  }

  render() {
    return (
      <div className="page dead-stock-page">
        <div className="header-info">
          <div>
            {this.renderHeading()}
            {this.renderBreadcrums(messages.common.inventory)}
          </div>
          {this.renderAdd()}
        </div>
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
                syncTriggerAt={this.state.syncTriggerAt}
                isLoading={(bIsLoading) => {
                  if (this.state.list) {
                    this.setState({ isLoading: bIsLoading });
                  }
                }}
              />
            </div>
          </Slide>
        )}
      </div>
    );
  }
}

export default DeadStock;
