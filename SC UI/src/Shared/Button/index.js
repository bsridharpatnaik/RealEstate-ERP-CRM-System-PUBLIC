import React, { Component } from "react";
import Button from "@material-ui/core/Button";
import "./style.scss";
class CustomButton extends Component {
  render() {
    return (
      <Button
        color="primary"
        variant="contained"
        component={this.props.component}
        startIcon={this.props.startIcon}
        endIcon={this.props.endIcon}
        classes={{
          root: this.props.buttonClass,
          label: "button-label",
        }}
        onClick={this.props.onClick}
        ref={this.props.innerRef}
        type={this.props.type}
        disabled={this.props.disabled}
      >
        {this.props.label}
      </Button>
    );
  }
}

export default CustomButton;
