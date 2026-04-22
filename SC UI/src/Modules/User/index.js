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
//style
import "./style.scss";
class User extends Common {
  state = {
    add: false,
    list: true,
    edit: false,
  };
  title = messages.common.user;
  render() {
    return (
      <div className="page">
        <div className="header-info">
          <div>
            {this.renderHeading()}
            {this.renderBreadcrums(messages.common.admin)}
          </div>
          {this.renderAdd()}
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
            edit={(user) => {
              this.userid = user.userId;
              this.setState({ edit: true, list: false });
            }}
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
            id={this.userid}
            back={() => {
              this.setState({ edit: false, list: true });
            }}
          />
        </Slide>
      </div>
    );
  }
}

export default User;
