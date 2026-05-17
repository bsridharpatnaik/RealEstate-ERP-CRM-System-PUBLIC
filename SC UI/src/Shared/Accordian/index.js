import React, { Component } from "react";
import ExpandMoreIcon from "@material-ui/icons/ExpandMore";
import "./style.scss";
class Accordian extends Component {
  state = { expand: true };
  componentDidMount = () => {
    if (this.props.defaultClosed) {
      this.setState({ expand: false });
    }
  };
  render() {
    return (
      <div className={this.state.expand ? "accordian expand" : "accordian"}>
        <div className="heading">
          <ExpandMoreIcon
            className="accordian-icon"
            onClick={() => this.setState({ expand: !this.state.expand })}
          />
          <div>{this.props.title()}</div>
        </div>
        <div
          className="accordian-content"
          style={{ height: this.state.expand ? "auto" : "0px" }}
        >
          {this.state.expand ? this.props.content() : ""}
        </div>
      </div>
    );
  }
}
export default Accordian;
