//react
import React from "react";
//third party
import { Slide } from "@material-ui/core";
//components
import List from "./list";
import Add from "./add";
import Edit from "./edit";
import Common from "./../../Shared/CommonIndex";
import Button from "./../../Shared/Button";
//misc
import { messages } from "./../../messages";
import { connect } from "react-redux";

class Indent extends Common {
  constructor(props) {
    super(props);
    this.addRef = React.createRef();
    this.editRef = React.createRef();
  }
  state = {
    add: false,
    list: true,
    edit: false,
    prefillData: null,
    validationUpdateKey: 0, // Force re-render when child validation changes
  };
  title = messages.common.indent;
  renderIndentButtons() {
    if (this.state.add) {
      const disabled = this.addRef.current && this.addRef.current.getSaveButtonDisabled ? this.addRef.current.getSaveButtonDisabled() : false;
      return (
        <div className="indent-action-buttons" key={`add-buttons-${this.state.validationUpdateKey}`}>
          <Button
            onClick={() => {
              this.setState({ add: false, list: true });
            }}
            buttonClass="grey"
            label={messages.common.cancel}
          />
          <Button
            onClick={() => {
              if (this.addRef.current && this.addRef.current.submitForm) {
                this.addRef.current.submitForm();
              }
            }}
            buttonClass="blue"
            label={messages.common.save}
            disabled={disabled}
          />
        </div>
      );
    }
    if (this.state.edit) {
      const disabled = this.editRef.current && this.editRef.current.getSaveButtonDisabled ? this.editRef.current.getSaveButtonDisabled() : false;
      return (
        <div className="indent-action-buttons" key={`edit-buttons-${this.state.validationUpdateKey}`}>
          <Button
            onClick={() => {
              this.setState({ edit: false, list: true });
            }}
            buttonClass="grey"
            label={messages.common.cancel}
          />
          <Button
            onClick={() => {
              if (this.editRef.current && this.editRef.current.submitForm) {
                this.editRef.current.submitForm();
              }
            }}
            buttonClass="blue"
            label={messages.common.save}
            disabled={disabled}
          />
        </div>
      );
    }
    return null;
  }
  render() {
    return (
      <div className="page indent-page">
        {this.state.list && (
          <div className="header-info">
            <div>
              {this.renderHeading()}
              {this.renderBreadcrums(messages.common.inventory)}
            </div>
            {this.renderIndentButtons()}
          </div>
        )}
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
            edit={(data) => {
              this.id = data.indentId;
              this.setState({ edit: true, list: false });
            }}
            onAdd={() => {
              this.setState({ add: true, list: false, prefillData: null });
            }}
            onResubmit={(prefillData) => {
              this.setState({ add: true, list: false, prefillData });
            }}
            isGlobal={this.props.isGlobal}
            allTenant={this.props.allTenant}
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
            ref={this.addRef}
            back={() => {
              this.setState({ add: false, list: true, prefillData: null });
            }}
            prefillData={this.state.prefillData}
            dropdowns={this.dropdowns || {}}
            onValidationChange={() => {
              // Trigger re-render when validation state changes
              this.setState({ validationUpdateKey: this.state.validationUpdateKey + 1 });
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
            ref={this.editRef}
            id={this.id}
            back={() => {
              this.setState({ edit: false, list: true });
            }}
            dropdowns={this.dropdowns || {}}
            onValidationChange={() => {
              // Trigger re-render when validation state changes
              this.setState({ validationUpdateKey: this.state.validationUpdateKey + 1 });
            }}
          />
        </Slide>
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    allTenant: state.allTennant.tennants,
  };
};

export default connect(mapStateToProps)(Indent);





