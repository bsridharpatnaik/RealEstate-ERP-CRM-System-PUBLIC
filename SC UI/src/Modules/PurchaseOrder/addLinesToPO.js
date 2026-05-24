import React from "react";
import { withSnackbar } from "notistack";
import IconButton from "@material-ui/core/IconButton";
import KeyboardBackspaceIcon from "@material-ui/icons/KeyboardBackspace";
import Breadcrumbs from "@material-ui/core/Breadcrumbs";
import NavigateNextIcon from "@material-ui/icons/NavigateNext";
import CircularProgress from "@material-ui/core/CircularProgress";
import Button from "./../../Shared/Button";
import Step1SelectIndents from "./add/step1SelectIndents";
import Step2FillDetails from "./add/step2FillDetails";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import "./style.scss";
import "./add/stepStyles.scss";

class AddLinesToPO extends React.Component {
  state = {
    currentStep: 1,
    selectedIndents: [],
    items: [],
    isAdding: false,
  };

  handleNext = () => {
    if (this.state.selectedIndents.length === 0) {
      this.props.enqueueSnackbar("Please select at least one indent line item", { variant: "error" });
      return;
    }

    const existingItemsMap = new Map(this.state.items.map((item) => [item.indentId, item]));

    const items = this.state.selectedIndents.map((indent) => {
      const existing = existingItemsMap.get(indent.indentId);
      if (existing) {
        return {
          ...existing,
          inventoryName: indent.inventoryName,
          unit: indent.unit || existing.unit || "NOS",
          specification: indent.specification || existing.specification || "",
        };
      }
      return {
        indentId: indent.indentId,
        productId: indent.productId,
        inventoryName: indent.inventoryName,
        quantity: indent.quantity || "",
        unit: indent.unit || "NOS",
        specification: indent.specification || "",
        remarks: indent.remarks || "",
        diameter: "",
        brandName: "",
        grade: "",
        rate: "",
        gst: "",
        discount: "",
        tolerance: "",
        netRate: "",
        totalAmt: "",
      };
    });

    this.setState({ currentStep: 2, items });
  };

  handlePrevious = () => {
    this.setState({ currentStep: 1 });
  };

  handleConfirm = async () => {
    const { items } = this.state;
    const missingRate = items.find(
      (item) => !item.rate || isNaN(parseFloat(item.rate)) || parseFloat(item.rate) <= 0
    );
    if (missingRate) {
      this.props.enqueueSnackbar("Rate is required for all items", { variant: "error" });
      return;
    }

    // Group items by productId (same as PO creation logic)
    const groupedItems = {};
    items.forEach((item) => {
      const productId = item.productId;
      if (!groupedItems[productId]) {
        groupedItems[productId] = {
          productId: productId || null,
          quantity: 0,
          brand: item.brandName || "",
          grade: item.grade || "",
          diameter: item.diameter || "",
          specification: item.specification || "",
          rate: parseFloat(item.rate || 0),
          discountPercent: parseFloat(item.discount || 0),
          tolerancePercent: parseFloat(item.tolerance || 0),
          gstPercent: parseFloat(item.gst || 0),
          indentRefs: [],
        };
      }
      groupedItems[productId].quantity += parseFloat(item.quantity || 0);
      if (item.indentId) {
        groupedItems[productId].indentRefs.push({ indentLineItemCode: item.indentId });
      }
    });

    const roundQty = (q) => {
      const n = parseFloat(q);
      return isNaN(n) ? 0 : (Number.isInteger(n) ? n : parseFloat(n.toFixed(2)));
    };

    const lineItems = Object.values(groupedItems).map((item) => {
      const quantity = roundQty(item.quantity);
      const rate = item.rate;
      const discount = item.discountPercent || 0;
      const gstPercent = item.gstPercent;
      const discountedRate = rate - (rate * discount / 100);
      const netRate = discountedRate * quantity;
      const totalAmount = netRate + (netRate * gstPercent / 100);
      return {
        productId: item.productId,
        quantity,
        brand: item.brand,
        grade: item.grade,
        diameter: item.diameter,
        specification: item.specification,
        rate,
        discountPercent: discount,
        tolerancePercent: item.tolerancePercent || 0,
        gstPercent,
        netRate,
        totalAmount,
        indentRefs: item.indentRefs,
      };
    });

    this.setState({ isAdding: true });
    try {
      const response = await API.POST(apiEndpoints.addPOLines(this.props.poId), lineItems);
      this.setState({ isAdding: false });
      if (response.success) {
        this.props.enqueueSnackbar("Line item(s) added successfully", { variant: "success" });
        this.props.back();
      } else {
        this.props.enqueueSnackbar(response.errorMessage || "Failed to add line items", { variant: "error" });
      }
    } catch (err) {
      this.setState({ isAdding: false });
      this.props.enqueueSnackbar("An error occurred", { variant: "error" });
    }
  };

  renderBreadcrumbs() {
    const steps = ["Purchase Order", this.props.poId, "Add Line Items"];
    if (this.state.currentStep === 2) steps.push("Fill Details");
    return (
      <Breadcrumbs separator={<NavigateNextIcon />} aria-label="breadcrumb" className="breadcrumbs">
        {steps.map((step, index) => <span key={index}>{step}</span>)}
      </Breadcrumbs>
    );
  }

  renderHeaderActions() {
    const { currentStep, isAdding } = this.state;
    const noSelection = this.state.selectedIndents.length === 0;
    if (currentStep === 1) {
      return (
        <div className="po-action-buttons">
          <Button onClick={this.props.back} buttonClass="grey" label="Cancel" />
          <Button onClick={this.handleNext} buttonClass="blue" label="Next" disabled={noSelection} />
        </div>
      );
    }
    const missingRate = this.state.items.some(
      (item) => !item.rate || isNaN(parseFloat(item.rate)) || parseFloat(item.rate) <= 0
    );
    return (
      <div className="po-action-buttons">
        <Button onClick={this.handlePrevious} buttonClass="grey" label="Previous" />
        <Button onClick={this.props.back} buttonClass="grey" label="Cancel" />
        <Button
          onClick={this.handleConfirm}
          buttonClass={missingRate ? "grey" : "blue"}
          label={isAdding ? "Adding..." : "Confirm & Add"}
          disabled={isAdding || missingRate}
        />
      </div>
    );
  }

  render() {
    const { currentStep, selectedIndents, items } = this.state;
    return (
      <div className="list-section add create-po-wrapper">
        <div className="create-po-header">
          <div className="create-po-header-row">
            <div className="add-heading-wrapper">
              <IconButton aria-label="back" onClick={this.props.back} className="back-icon">
                <KeyboardBackspaceIcon />
              </IconButton>
              <span className="add-heading">Add Line Items — {this.props.poId}</span>
            </div>
            {this.renderHeaderActions()}
          </div>
          {this.renderBreadcrumbs()}
        </div>
        <div className="create-po-content">
          {currentStep === 1 && (
            <Step1SelectIndents
              selectedIndents={selectedIndents}
              indentItems={[]}
              onSelectIndents={(indents) => this.setState({ selectedIndents: indents })}
              onIndentItemsChange={() => {}}
            />
          )}
          {currentStep === 2 && (
            <Step2FillDetails
              addLinesMode={true}
              items={items}
              onItemsChange={(updated) => this.setState({ items: updated })}
              /* supply no-op callbacks for props Step2 still accesses */
              orderTo={null}
              orderFrom={null}
              poSubject=""
              noteText=""
              onOrderToChange={() => {}}
              onOrderFromChange={() => {}}
              onPoSubjectChange={() => {}}
              onNoteTextChange={() => {}}
              overridePhoneNumber=""
              onOverridePhoneNumberChange={() => {}}
              overrideEmail=""
              onOverrideEmailChange={() => {}}
              showOverridePhoneEmail={false}
              onShowOverridePhoneEmailChange={() => {}}
              projectName=""
              onProjectNameChange={() => {}}
              isSpecialPo={false}
              onIsSpecialPoChange={() => {}}
              poDate=""
              onPoDateChange={() => {}}
              isAdmin={false}
              freightCharges=""
              freightGstPercent={18}
              onFreightChargesChange={() => {}}
              onFreightGstPercentChange={() => {}}
              customCharges={[]}
              onCustomChargesChange={() => {}}
              fileArea={null}
              dropdowns={{}}
              isEditMode={false}
              enqueueSnackbar={this.props.enqueueSnackbar}
            />
          )}
        </div>
      </div>
    );
  }
}

export default withSnackbar(AddLinesToPO);
