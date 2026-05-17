//react
import React from "react";
//third party
import { Slide } from "@material-ui/core";
//components
import List from "./list";
import Add from "./add";
import Edit from "./edit";
//misc
import { messages } from "./../../messages";
import { canEditContactModules } from "./../../helper";
//style
import "./style.scss";
import Common from "./../../Shared/CommonIndex";
class Contact extends Common {
  state = {
    add: false,
    list: true,
    edit: false,
  };
  title = messages.common.contact;
  render() {
    return (
      <div className="page">
        <div className="header-info">
          <div>{this.renderHeading()}</div>
          {canEditContactModules() && this.renderAdd()}
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
              this.id = data.contactId;
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

export default Contact;
