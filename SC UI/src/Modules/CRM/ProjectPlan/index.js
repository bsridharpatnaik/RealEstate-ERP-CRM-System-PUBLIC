import React from "react";
import { Slide } from "@material-ui/core";
import List from './list';
import Add from './add';
import Edit from './edit';
import EditSubType from './editsubtype';
import Common from "../../../Shared/CommonIndex";
import { messages } from "./../../../messages";
import Button from "@material-ui/core/Button";
import "./style.scss";

class ProjectPlan extends Common {
  title = messages.common.projectPlan;
  state = {
    add: false,
    list: true,
    edit: false,
    editsubtype: false,
    editable: false
  };

  render() {
    return (
      <div className="page">
        <div className="header-info projectPlansHeader">
          <div>
            {this.renderHeading()}
            {this.renderBreadcrums(messages.common.crm)}
          </div>
          <div className="right-header">
            {this.state.editable ? (
              <>
                <Button
                  onClick={() => this.setState({ add: true, list: false })}
                  color="primary"
                  variant="contained"
                  classes={{
                    root: "add-button",
                    label: "add-label",
                  }}
                >
                  <span className="add-property-text">ADD PROPERTY TYPE</span>
                  <span className="add-property-plus">+</span>
              </Button>
                <Button
                  onClick={() => this.setState({ editable: false })}
                  variant="contained"
                >
                  <span className="add-property-text">Close</span>
                  <span className="add-property-plus">&times;</span>
                </Button>
              </>
            ) :
              <Button
                onClick={() => this.setState({ editable: true })}
                color="primary"
                variant="contained"
                classes={{
                  root: "add-button",
                  label: "add-label",
                }}
              >
                Edit
              </Button>
            }
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
            isLoading={(bIsLoading) => this.setState({ isLoading: bIsLoading })}
            edit={(data) => {
              this.data = data;
              this.setState({ edit: true, list: false });
            }}
            subTypeEdit={(data) => {
              this.data = data;
              this.setState({ editsubtype: true, list: false });
            }}
            editable={this.state.editable}
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
            data={this.data}
            back={() => {
              this.setState({ edit: false, list: true });
            }}
          />
        </Slide>
        <Slide
          direction="left"
          in={this.state.editsubtype}
          mountOnEnter
          unmountOnExit
          timeout={{ exit: 0 }}
        >
          <EditSubType
            data={this.data}
            back={() => {
              this.setState({ editsubtype: false, list: true });
            }}
          />
        </Slide>
      </div>
    );
  }
}

export default ProjectPlan
