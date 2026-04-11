//react
import React from "react";
import { connect } from "react-redux";

//third party
import { Slide } from "@material-ui/core";
//components
import List from "./list";
import Add from "./add";
import Edit from "./edit";
import Common from "./../../../Shared/CommonIndex";
//misc
import { messages } from "./../../../messages";
import { getDealLostReasons } from "./../../../actions/dealLostReasons";
import { API } from "../../../axios";
import { apiEndpoints } from "../../../endpoints";

class Lead extends Common {
  state = {
    add: false,
    list: true,
    edit: false,
    purposeDropdown: [],
    dropdowns: undefined,
  };
  title = messages.common.lead;
  async componentDidMount() {
    if (!this.props.dealLostReasons) {
      await this.props.getDealLostReasons();
    }
    const response = await API.GET(apiEndpoints.getPurposeDropdownValues);
    if (response.success) {
      this.setState({ purposeDropdown: response.data });
    }
  }
  render() {
    return (
      <div className="page">
        <div className="header-info">
          <div>
            {this.renderHeading()}
            {this.renderBreadcrums(messages.common.crm)}
          </div>
          {this.renderAdd(!this.state.dropdowns)}
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
              this.id = data.leadId;
              this.setState({ edit: true, list: false });
            }}
            setOptions={(options) => (this.setState({dropdowns: options}))}
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
            dropdowns={this.state.dropdowns}
            purposeDropdown={this.state.purposeDropdown}
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
            dropdowns={this.state.dropdowns}
            purposeDropdown={this.state.purposeDropdown}
          />
        </Slide>
      </div>
    );
  }
}
const mapStateToProps = (state) => {
  return {
    dealLostReasons: state.dealLostReasons.dealLostReasons,
  };
};
export default connect(mapStateToProps, { getDealLostReasons }, null, {
  forwardRef: true,
})(Lead);
