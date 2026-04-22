//react
import React from "react";
//third party
import { Slide } from "@material-ui/core";
import Button from "@material-ui/core/Button";
import Menu from "@material-ui/core/Menu";
import MenuItem from "@material-ui/core/MenuItem";
import ArrowDropDownIcon from "@material-ui/icons/ArrowDropDown";
//components
import List from "./list";
import Add from "./add";
import Common from "./../../Shared/CommonIndex";
//misc
import { messages } from "./../../messages";
import { canCreateInward } from "./../../helper";

class InwardInventory extends Common {
  state = {
    add: false,
    list: true,
    edit: false,
    addFromPO: false,
    addSample: false,
    menuAnchorEl: null,   // controls the dropdown menu open/close
  };
  title = messages.common.inwardInventory;

  handleMenuOpen = (event) => {
    this.setState({ menuAnchorEl: event.currentTarget });
  };

  handleMenuClose = () => {
    this.setState({ menuAnchorEl: null });
  };

  handleSelectMode = (mode) => {
    this.handleMenuClose();
    if (mode === "po") {
      this.setState({ addFromPO: true, list: false });
    } else if (mode === "direct") {
      this.setState({ add: true, list: false });
    } else if (mode === "sample") {
      this.setState({ addSample: true, list: false });
    }
  };

  renderAddButton() {
    if (!this.state.list) return null;
    if (!canCreateInward()) return null;
    return (
      <>
        <Button
          color="primary"
          variant="contained"
          disabled={this.state.isLoading}
          onClick={this.handleMenuOpen}
          endIcon={<ArrowDropDownIcon />}
          classes={{ root: "add-button", label: "add-label" }}
        >
          Add Inward
        </Button>
        <Menu
          anchorEl={this.state.menuAnchorEl}
          keepMounted
          open={Boolean(this.state.menuAnchorEl)}
          onClose={this.handleMenuClose}
          getContentAnchorEl={null}
          anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
          transformOrigin={{ vertical: "top", horizontal: "right" }}
        >
          <MenuItem onClick={() => this.handleSelectMode("po")}>
            Inward from PO
          </MenuItem>
          <MenuItem onClick={() => this.handleSelectMode("direct")}>
            Direct Inward
          </MenuItem>
          <MenuItem onClick={() => this.handleSelectMode("sample")}>
            Sample Inward
          </MenuItem>
        </Menu>
      </>
    );
  }

  render() {
    return (
      <div className="page">
        <div className="header-info">
          <div>
            {this.renderHeading()}
            {this.renderBreadcrums(messages.common.inventory)}
          </div>
          <div className="add-buttons">
            {this.renderAddButton()}
          </div>
        </div>

        <Slide direction="right" in={this.state.list} mountOnEnter unmountOnExit timeout={{ exit: 0 }}>
          <List
            isLoading={(bIsLoading) => this.setState({ isLoading: bIsLoading })}
            setOptions={(options) => (this.dropdowns = options)}
            edit={(data) => {
              this.id = data.inwardId;
              this.setState({ edit: true, list: false });
            }}
          />
        </Slide>

        <Slide direction="left" in={this.state.add} mountOnEnter unmountOnExit timeout={{ exit: 0 }}>
          <Add
            mode="direct"
            back={() => this.setState({ add: false, list: true })}
            dropdowns={this.dropdowns || {}}
          />
        </Slide>

        <Slide direction="left" in={this.state.edit} mountOnEnter unmountOnExit timeout={{ exit: 0 }}>
          <Add
            id={this.id}
            back={() => this.setState({ edit: false, list: true })}
            dropdowns={this.dropdowns || {}}
          />
        </Slide>

        <Slide direction="left" in={this.state.addFromPO} mountOnEnter unmountOnExit timeout={{ exit: 0 }}>
          <Add
            mode="po"
            back={() => this.setState({ addFromPO: false, list: true })}
            dropdowns={this.dropdowns || {}}
          />
        </Slide>

        <Slide direction="left" in={this.state.addSample} mountOnEnter unmountOnExit timeout={{ exit: 0 }}>
          <Add
            mode="sample"
            back={() => this.setState({ addSample: false, list: true })}
            dropdowns={this.dropdowns || {}}
          />
        </Slide>
      </div>
    );
  }
}

export default InwardInventory;