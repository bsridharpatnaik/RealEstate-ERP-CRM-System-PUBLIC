import React, { Component } from "react";
import "./progress.scss";
import { SvgIcon } from "@material-ui/core";
import { ReactComponent as CloseSVG } from "./close.svg";
function CrossIcon(props) {
  return (
    <SvgIcon {...props}>
      <CloseSVG />
    </SvgIcon>
  );
}
class Progress extends Component {
  constructor(props) {
    super(props);
    this.state = {};
  }
  render() {
    const isCompleted = this.props.progress === 100 ? true : false;
    return (
      <div
        className={
          isCompleted ? "completed progress-wrapper" : "progress-wrapper"
        }
      >
        <CrossIcon
          className="cross"
          onClick={() => this.props.removeFile(this.props.file)}
        />
        <div>{this.props.file.name || this.props.file.fileName}</div>
        <div className="ProgressBar">
          <div
            className="Progress"
            style={{ width: this.props.progress + "%" }}
          />
        </div>
        <div className="text">
          {isCompleted ? "Completed" : this.props.progress + "%"}
        </div>
      </div>
    );
  }
}

export default Progress;
