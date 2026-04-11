import React, { Component } from "react";
import Button from "@material-ui/core/Button";
import downloadIcon from "../Icons/download.png";
import { ReactComponent as FilterSVG } from "../Icons/FilterIcon.svg"
import { ReactComponent as ListMenuSVG } from "../Icons/list-filter.svg"

import "./style.scss";
class IconButtons extends Component {
  render() {
    const getIcon = () => {
      if (this.props.icon === "DownloadSVG") return <img src={downloadIcon} alt="Download" style={{ width: 20, height: 20 }} />;
      if (this.props.icon === "FilterSVG") return <FilterSVG />;
      if (this.props.icon === "MenuSVG") return <ListMenuSVG />;
      return null;
    };
    
    return (
      <Button
        color="primary"
        variant="contained"
        component={this.props.component}
        classes={{
          root: this.props.buttonClass,
          label: "button-label",
        }}
        onClick={this.props.onClick}
        ref={this.props.innerRef}
        type={this.props.type}
        disabled={this.props.disabled}
        startIcon={getIcon()}
      >
       {this.props.label}
      </Button>
    );
  }
}

export default IconButtons;
