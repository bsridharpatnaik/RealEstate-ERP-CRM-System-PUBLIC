import React, { Component } from "react";
import Button from "@material-ui/core/Button";
import { messages } from "./../../messages";
import Breadcrumbs from "@material-ui/core/Breadcrumbs";
import NavigateNextIcon from "@material-ui/icons/NavigateNext";
import Skeleton from "@material-ui/lab/Skeleton";

const height = 35;
class Common extends Component {
  renderHeading() {
    return <span className="page-heading">{this.title}</span>;
  }

  renderLoader() {
    return (
      <React.Fragment>
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
      </React.Fragment>
    );
  }

  renderBreadcrums(br1) {
    return (
      <Breadcrumbs
        separator={<NavigateNextIcon />}
        aria-label="breadcrumb"
        className="breadcrumbs"
      >
        <span>{br1}</span>
        <span>{this.title}</span>
      </Breadcrumbs>
    );
  }
  renderAdd(isDisabled = false) {
    return (
      this.state.list && (
        <Button
          onClick={() => this.setState({ add: true, list: false })}
          color="primary"
          variant="contained"
          disabled={this.state.isLoading || isDisabled}
          classes={{
            root: "add-button",
            label: "add-label",
          }}
        >
          {this.customAdd
            ? this.customAdd
            : `${messages.common.add} ${this.title}`}
        </Button>
      )
    );
  }
}

export default Common;
