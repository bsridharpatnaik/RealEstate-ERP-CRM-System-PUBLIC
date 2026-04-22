//react
import React, { Component } from "react";
import { withRouter } from "react-router-dom";
import { withSnackbar } from "notistack";
import AddForm from "./../../../Shared/AddForm";
import Breadcrumbs from "@material-ui/core/Breadcrumbs";
import NavigateNextIcon from "@material-ui/icons/NavigateNext";
import Step1SelectIndents from "./step1SelectIndents";
import Button from "./../../../Shared/Button";
import { messages } from "./../../../messages";
import { appRoutes } from "./../../../endpoints";
import { getSession, setSession, clearSession } from "./../../../helper";
import "../style.scss";
import "./stepStyles.scss";

const FORM_DATA_KEY = "purchaseOrderFormData";

class Step1SelectIndentsRoute extends AddForm {
  title = messages.common.purchaseOrder;
  state = {
    selectedIndents: [],
    indentItems: [],
  };

  componentDidMount() {
    // Load persisted form data if available
    const savedData = getSession(FORM_DATA_KEY);
    if (savedData && savedData.selectedIndents) {
      this.setState({
        selectedIndents: savedData.selectedIndents || [],
        indentItems: savedData.indentItems || [],
      });
    }
  }

  getSaveButtonDisabled() {
    return this.state.selectedIndents.length === 0;
  }

  handleNext = () => {
    // Validate step 1
    if (this.state.selectedIndents.length === 0) {
      this.props.enqueueSnackbar("Please select at least one indent", {
        variant: "error",
      });
      return;
    }

    // Process selected indents to create items (no hardcoded defaults; values from indent only)
    const items = this.state.selectedIndents.map((indent) => {
      const quantity = indent.quantity || "";
      const rate = parseFloat("" || 0);
      const qty = parseFloat(quantity || 0);
      const gst = parseFloat("" || 0);
      const netRate = rate * qty;
      const totalAmt = netRate + (netRate * gst / 100);

      return {
        indentId: indent.indentId,
        inventoryName: indent.inventoryName,
        quantity,
        unit: indent.unit || "NOS",
        specification: indent.specification || "",
        remarks: indent.remarks || "",
        diameter: "",
        size: "",
        brandName: "",
        grade: "",
        rate: "",
        discount: "",
        tolerance: "",
        gst: "",
        netRate: netRate > 0 ? netRate.toFixed(2) : "",
        totalAmt: totalAmt > 0 ? totalAmt.toFixed(2) : "",
      };
    });

    // Save form data to sessionStorage
    const formData = {
      selectedIndents: this.state.selectedIndents,
      indentItems: this.state.indentItems,
      items,
      currentStep: 2,
    };
    setSession(FORM_DATA_KEY, formData);

    // Navigate to step 2
    this.props.history.push(appRoutes.purchaseOrderAddFillDetails);
  };

  handleCancel = () => {
    // Clear form data
    clearSession(FORM_DATA_KEY);
    // Navigate back to list
    this.props.history.push(appRoutes.purchaseOrder);
  };

  renderBreadcrumbs() {
    return (
      <Breadcrumbs
        separator={<NavigateNextIcon />}
        aria-label="breadcrumb"
        className="breadcrumbs"
      >
        <span>{messages.common.inventory}</span>
        <span>{messages.common.purchaseOrder}</span>
        <span>Create Purchase Order</span>
      </Breadcrumbs>
    );
  }

  renderHeaderActions() {
    return (
      <div className="po-action-buttons">
        <Button
          onClick={this.handleCancel}
          buttonClass="grey"
          label={messages.common.cancel}
        />
        <Button
          onClick={this.handleNext}
          buttonClass="blue"
          label="Next"
          disabled={this.getSaveButtonDisabled()}
        />
      </div>
    );
  }

  render() {
    return (
      <div className="list-section add create-po-wrapper">
        <div className="create-po-header">
          <div>
            {this.renderHeading()}
            {this.renderBreadcrumbs()}
          </div>
          {this.renderHeaderActions()}
        </div>
        <div className="create-po-content">
          <Step1SelectIndents
            selectedIndents={this.state.selectedIndents}
            indentItems={this.state.indentItems}
            onSelectIndents={(indents) => {
              this.setState({ selectedIndents: indents });
              // Update sessionStorage
              const savedData = getSession(FORM_DATA_KEY) || {};
              setSession(FORM_DATA_KEY, {
                ...savedData,
                selectedIndents: indents,
              });
            }}
            onIndentItemsChange={(items) => {
              this.setState({ indentItems: items });
              // Update sessionStorage
              const savedData = getSession(FORM_DATA_KEY) || {};
              setSession(FORM_DATA_KEY, {
                ...savedData,
                indentItems: items,
              });
            }}
          />
        </div>
      </div>
    );
  }
}

export default withRouter(withSnackbar(Step1SelectIndentsRoute));
