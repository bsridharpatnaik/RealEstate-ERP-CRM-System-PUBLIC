import React from "react";
import { Slide, Button } from "@material-ui/core";
import List from "./list";
import Common from "./../../Shared/CommonIndex";
import AddNewFirmModal from "./../PurchaseOrder/add/addNewFirmModal";
import { withSnackbar } from "notistack";
import "./style.scss";
import { messages } from "./../../messages";
import { canEditInventoryModules } from "./../../helper";

class Firm extends Common {
  state = {
    list: true,
    modalOpen: false,
    listKey: 0,
  };
  title = messages.common.firm;
  firmModalRef = null;

  openAddModal = () => {
    this.setState({ modalOpen: true });
  };

  openEditModal = (data) => {
    this.setState({ modalOpen: true }, () => {
      if (this.firmModalRef) {
        this.firmModalRef.loadFirmForEdit(data.firmId ?? data.id);
      }
    });
  };

  handleModalClose = () => {
    this.setState({ modalOpen: false });
  };

  handleModalSave = () => {
    this.setState({ modalOpen: false, listKey: this.state.listKey + 1 });
  };

  render() {
    return (
      <div className="page">
        <div className="header-info">
          <div>
            {this.renderHeading()}
            {this.renderBreadcrums(messages.common.inventory)}
          </div>
          {canEditInventoryModules() && (
            <Button variant="contained" color="primary" onClick={this.openAddModal}>
              + {messages.common.add}
            </Button>
          )}
        </div>
        <Slide direction="right" in={this.state.list} mountOnEnter unmountOnExit timeout={{ exit: 0 }}>
          <List
            key={this.state.listKey}
            isLoading={(bIsLoading) => this.setState({ isLoading: bIsLoading })}
            canEdit={canEditInventoryModules()}
            edit={(data) => this.openEditModal(data)}
          />
        </Slide>
        <AddNewFirmModal
          innerRef={(el) => { this.firmModalRef = el; }}
          open={this.state.modalOpen}
          onClose={this.handleModalClose}
          onSave={this.handleModalSave}
          enqueueSnackbar={this.props.enqueueSnackbar}
        />
      </div>
    );
  }
}

export default withSnackbar(Firm);