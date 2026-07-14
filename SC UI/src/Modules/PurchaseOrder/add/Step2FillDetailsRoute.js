//react
import React, { Component } from "react";
import { withRouter } from "react-router-dom";
import { connect } from "react-redux";
import { withSnackbar } from "notistack";
import moment from "moment";
import AddForm from "./../../../Shared/AddForm";
import Breadcrumbs from "@material-ui/core/Breadcrumbs";
import NavigateNextIcon from "@material-ui/icons/NavigateNext";
import Step2FillDetails from "./step2FillDetails";
import Button from "./../../../Shared/Button";
import { messages, constants } from "./../../../messages";
import { appRoutes } from "./../../../endpoints";
import { apiEndpoints } from "./../../../endpoints";
import { API } from "./../../../axios";
import { getSession, setSession, clearSession } from "./../../../helper";
import FileList from "./../../../Shared/FileList";
import "../style.scss";
import "./stepStyles.scss";

const FORM_DATA_KEY = "purchaseOrderFormData";

class Step2FillDetailsRoute extends AddForm {
  title = messages.common.purchaseOrder;
  addurl = ""; // Will be set when API is ready
  state = {
    orderTo: null,
    orderFrom: null,
    poSubject: "",
    items: [],
    noteText: "",
    overridePhoneNumber: "",
    overrideEmail: "",
    showOverridePhoneEmail: false,
    projectName: "",
    poNumber: "", // Generated PO number
    isAdding: false,
    isSpecialPo: false,
  };

  componentDidMount() {
    // Load persisted form data from step 1
    const savedData = getSession(FORM_DATA_KEY);
    if (savedData) {
      this.setState({
        selectedIndents: savedData.selectedIndents || [],
        indentItems: savedData.indentItems || [],
        items: savedData.items || [],
        // Load step 2 specific data if available
        orderTo: savedData.orderTo || null,
        orderFrom: savedData.orderFrom || null,
        poSubject: savedData.poSubject || "",
        isSpecialPo: savedData.isSpecialPo || false,
        noteText: savedData.noteText || "",
        overridePhoneNumber: savedData.overridePhoneNumber || "",
        overrideEmail: savedData.overrideEmail || "",
        showOverridePhoneEmail: savedData.showOverridePhoneEmail || false,
        projectName: savedData.projectName || "",
        poNumber: savedData.poNumber || this.state.poNumber,
      });
      this.formData.fileInformations = savedData.fileInformations || [];
    } else {
      // If no saved data, redirect back to step 1
      this.props.history.push(appRoutes.purchaseOrderAddSelectIndents);
    }
  }

  renderFileArea() {
    if (!this.formData.fileInformations) {
      this.formData.fileInformations = [];
    }
    return (
      <FileList
        files={this.formData.fileInformations}
        remove={(removedFile) => {
          let fileList = [...this.formData.fileInformations];
          fileList = fileList.filter((file) => file.fileUUId !== removedFile.fileUUId);
          this.formData.fileInformations = fileList;

          const savedData = getSession(FORM_DATA_KEY) || {};
          setSession(FORM_DATA_KEY, { ...savedData, fileInformations: fileList });

          this.setState({ i: 1 });
        }}
        onChange={(files) => {
          const savedData = getSession(FORM_DATA_KEY) || {};
          setSession(FORM_DATA_KEY, { ...savedData, fileInformations: files || [] });
        }}
      />
    );
  }

  getSaveButtonDisabled() {
    const hasMissingRate = this.state.items.some(
      (item) =>
        item.rate === "" ||
        item.rate === undefined ||
        item.rate === null ||
        isNaN(parseFloat(item.rate))
    );
    return (
      !this.state.orderTo ||
      !this.state.orderFrom ||
      this.state.items.length === 0 ||
      hasMissingRate ||
      this.state.isAdding
    );
  }

  getConfirmDisabledReason() {
    if (this.state.isAdding) return "Please wait...";
    if (!this.state.orderTo) return "Please select Order To supplier";
    if (!this.state.orderFrom) return "Please select Order From firm";
    if (!this.state.projectName) return "Please select a Project";
    if (this.state.items.length === 0) return "Please add at least one item";
    const hasMissingRate = this.state.items.some(
      (item) =>
        item.rate === "" ||
        item.rate === undefined ||
        item.rate === null ||
        isNaN(parseFloat(item.rate))
    );
    if (hasMissingRate) return "Rate is required for all items";
    return null;
  }

  handleConfirmClick = () => {
    const reason = this.getConfirmDisabledReason();
    if (reason) {
      this.props.enqueueSnackbar(reason, { variant: "error" });
      return;
    }
    this.handleConfirm();
  };

  handlePrevious = () => {
    // Save current step 2 data to sessionStorage
    const formData = {
      ...getSession(FORM_DATA_KEY),
      orderTo: this.state.orderTo,
      orderFrom: this.state.orderFrom,
      poSubject: this.state.poSubject,
      items: this.state.items,
      noteText: this.state.noteText,
      overridePhoneNumber: this.state.overridePhoneNumber,
      overrideEmail: this.state.overrideEmail,
      showOverridePhoneEmail: this.state.showOverridePhoneEmail,
      projectName: this.state.projectName,
      poNumber: this.state.poNumber,
      fileInformations: this.formData.fileInformations || [],
      specialPo: this.state.isSpecialPo || false,
    };
    setSession(FORM_DATA_KEY, formData);

    // Navigate back to step 1
    this.props.history.push(appRoutes.purchaseOrderAddSelectIndents);
  };

  handleCancel = () => {
    // Clear form data
    clearSession(FORM_DATA_KEY);
    // Navigate back to list
    this.props.history.push(appRoutes.purchaseOrder);
  };

  handleConfirm = async () => {
    // Validate step 2
    if (!this.state.orderTo) {
      this.props.enqueueSnackbar("Please select Order To supplier", {
        variant: "error",
      });
      return;
    }
    if (!this.state.orderFrom) {
      this.props.enqueueSnackbar("Please select Order From firm", {
        variant: "error",
      });
      return;
    }
    if (!this.state.projectName) {
      this.props.enqueueSnackbar("Please select a Project", { variant: "error" });
      return;
    }
    if (this.state.items.length === 0) {
      this.props.enqueueSnackbar("Please add at least one item", {
        variant: "error",
      });
      return;
    }
    const itemWithMissingRate = this.state.items.find(
      (item) =>
        item.rate === "" ||
        item.rate === undefined ||
        item.rate === null ||
        isNaN(parseFloat(item.rate))
    );
    if (itemWithMissingRate) {
      this.props.enqueueSnackbar("Rate is required for all items", {
        variant: "error",
      });
      return;
    }

    // Group items by productId to club items with same product
    const groupedItems = {};
    this.state.items.forEach((item) => {
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
          sampleImageFileId: item.sampleImageFileId || null,
          indentRefs: []
        };
      }
      
      // Sum quantities
      groupedItems[productId].quantity += parseFloat(item.quantity || 0);
      
      // Collect all indent line item codes
      if (item.indentId) {
        groupedItems[productId].indentRefs.push({
          indentLineItemCode: item.indentId
        });
      }
    });

    // Transform grouped items to lineItems format expected by API
    // Recalculate netRate and totalAmount based on summed quantity
    const lineItems = Object.values(groupedItems).map((item) => {
      const quantity = roundQuantity(item.quantity);
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
        sampleImageFileId: item.sampleImageFileId || null,
        indentRefs: item.indentRefs,
        linkedQcLineId: item._linkedQcLineId || null,
        linkedSupplierQuoteLineId: item._linkedSupplierQuoteLineId || null,
        linkedQcId: item._linkedQcId || null,
      };
    });

    // Calculate grandTotal from lineItems
    const grandTotal = lineItems.reduce((sum, item) => sum + (item.totalAmount || 0), 0);

    // Prepare payload matching API structure
    const payload = {
      poDate: moment().format(constants.dateFormat), // Current date in DD-MM-YYYY format
      supplierId: this.state.orderTo?.id || null,
      firmId: this.state.orderFrom?.id || null,
      subject: this.state.poSubject || "",
      notes: this.state.noteText || "",
      overridePhoneNumber: this.state.overridePhoneNumber || null,
      overrideEmail: this.state.overrideEmail || null,
      specialPo: this.state.isSpecialPo || false,
      projectName: this.state.projectName || null,
      fileInformations: (this.formData.fileInformations || []).map((f) => ({
        fileUUId: f.fileUUId,
        fileName: f.fileName,
      })),
      grandTotal: grandTotal,
      lineItems: lineItems
    };

    this.setState({ isAdding: true });

    try {
      const response = await API.POST(apiEndpoints.createPurchaseOrder, payload);
      this.setState({ isAdding: false });

      if (response.success) {
        // Link any quote-comparison lines to this PO (optional, non-blocking)
        const poId = response.data?.purchaseOrderId;
        if (poId) {
          const linkedItems = lineItems.filter(l => l.linkedQcLineId && l.linkedSupplierQuoteLineId && l.linkedQcId);
          for (const li of linkedItems) {
            try {
              await API.POST(apiEndpoints.quoteComparisonLinkToPo, {
                supplierQuoteLineId: li.linkedSupplierQuoteLineId,
                qcLineId: li.linkedQcLineId,
                qcId: li.linkedQcId,
                purchaseOrderId: poId,
                poLineId: null,
              });
            } catch (e) { /* non-critical */ }
          }
        }
        this.props.enqueueSnackbar("Purchase Order created successfully", {
          variant: "success",
        });
        clearSession(FORM_DATA_KEY);
        this.props.history.push(appRoutes.purchaseOrder);
      } else {
        this.props.enqueueSnackbar(response.errorMessage || "Failed to create Purchase Order", {
          variant: "error",
        });
      }
    } catch (error) {
      this.setState({ isAdding: false });
      this.props.enqueueSnackbar("An error occurred while creating Purchase Order", {
        variant: "error",
      });
    }
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
        <span>Fill Details</span>
      </Breadcrumbs>
    );
  }

  renderHeaderActions() {
    return (
      <div className="po-action-buttons">
        <Button
          onClick={this.handlePrevious}
          buttonClass="grey"
          label="Previous"
        />
        <Button
          onClick={this.handleCancel}
          buttonClass="grey"
          label={messages.common.cancel}
        />
        <Button
          onClick={this.handleConfirmClick}
          buttonClass={this.getSaveButtonDisabled() ? "grey confirm-disabled" : "blue"}
          label="Confirm"
          disabled={this.state.isAdding}
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
            <div className="po-number-display">
              PO Number: {this.state.poNumber}
            </div>
          </div>
          {this.renderHeaderActions()}
        </div>
        <div className="create-po-content">
          <Step2FillDetails
            orderTo={this.state.orderTo}
            orderFrom={this.state.orderFrom}
            poSubject={this.state.poSubject}
            items={this.state.items}
            noteText={this.state.noteText}
            onOrderToChange={(orderTo) => {
              this.setState({ orderTo });
              const savedData = getSession(FORM_DATA_KEY) || {};
              setSession(FORM_DATA_KEY, { ...savedData, orderTo });
            }}
            onOrderFromChange={(orderFrom) => {
              this.setState({ orderFrom });
              const savedData = getSession(FORM_DATA_KEY) || {};
              setSession(FORM_DATA_KEY, { ...savedData, orderFrom });
            }}
            onPoSubjectChange={(poSubject) => {
              this.setState({ poSubject });
              const savedData = getSession(FORM_DATA_KEY) || {};
              setSession(FORM_DATA_KEY, { ...savedData, poSubject });
            }}
            onItemsChange={(items) => {
              this.setState({ items });
              const savedData = getSession(FORM_DATA_KEY) || {};
              setSession(FORM_DATA_KEY, { ...savedData, items });
            }}
            onNoteTextChange={(text) => {
              this.setState({ noteText: text });
              const savedData = getSession(FORM_DATA_KEY) || {};
              setSession(FORM_DATA_KEY, { ...savedData, noteText: text });
            }}
            overridePhoneNumber={this.state.overridePhoneNumber}
            onOverridePhoneNumberChange={(val) => {
              this.setState({ overridePhoneNumber: val });
              const savedData = getSession(FORM_DATA_KEY) || {};
              setSession(FORM_DATA_KEY, { ...savedData, overridePhoneNumber: val });
            }}

            overrideEmail={this.state.overrideEmail}
            onOverrideEmailChange={(val) => {
              this.setState({ overrideEmail: val });
              const savedData = getSession(FORM_DATA_KEY) || {};
              setSession(FORM_DATA_KEY, { ...savedData, overrideEmail: val });
            }}

            isSpecialPo={this.state.isSpecialPo}
            onIsSpecialPoChange={(val) => {
              this.setState({ isSpecialPo: val });
              const savedData = getSession(FORM_DATA_KEY) || {};
              setSession(FORM_DATA_KEY, { ...savedData, isSpecialPo: val });
            }}
            showOverridePhoneEmail={this.state.showOverridePhoneEmail}
            onShowOverridePhoneEmailChange={(val) => {
              this.setState({ showOverridePhoneEmail: val });
              const savedData = getSession(FORM_DATA_KEY) || {};
              setSession(FORM_DATA_KEY, { ...savedData, showOverridePhoneEmail: val });
            }}
            projectName={this.state.projectName}
            onProjectNameChange={(val) => {
              this.setState({ projectName: val });
              const savedData = getSession(FORM_DATA_KEY) || {};
              setSession(FORM_DATA_KEY, { ...savedData, projectName: val });
            }}
            fileArea={this.renderFileArea()}
            dropdowns={this.props.dropdowns || {}}
          />
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state) => ({
  dropdowns: state.dropdowns || {},
});

export default connect(mapStateToProps)(withRouter(withSnackbar(Step2FillDetailsRoute)));
