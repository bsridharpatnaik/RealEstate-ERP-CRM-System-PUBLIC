//react
import React from "react";
//third party
import { Slide } from "@material-ui/core";
//components
import List from "./list";
import Add from "./add";
import Edit from "./edit";
import Common from "./../../Shared/CommonIndex";
//misc
import { messages } from "./../../messages";
import { canEditInventoryModules } from "./../../helper";
class Category extends Common {
  state = {
    add: false,
    list: true,
    edit: false,
  };
  title = messages.common.category;
  render() {
    const canEdit = canEditInventoryModules();
    return (
      <div className="page">
        <div className="header-info">
          <div>
            {this.renderHeading()}
            {this.renderBreadcrums(messages.common.inventory)}
          </div>
          {canEdit && this.renderAdd()}
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
            edit={(data) => {
              this.id = data.categoryId;
              this.setState({ edit: true, list: false });
            }}
            hideedit={!canEdit}
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
            back={() => {
              this.setState({ add: false, list: true });
            }}
          />
        </Slide>
        <Slide
          direction="left"
          in={this.state.edit}
          mountOnEnter
          unmountOnExit
          timeout={{ exit: 0 }}
        >
          <Edit
            id={this.id}
            back={() => {
              this.setState({ edit: false, list: true });
            }}
          />
        </Slide>
      </div>
    );
  }
}

export default Category;
