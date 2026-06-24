//react
import React from "react";
import { connect } from "react-redux";
//third party
import { withSnackbar } from "notistack";
import moment from "moment";
import AddForm from "./../../Shared/AddForm";
import { API } from "./../../axios";
import Breadcrumbs from "@material-ui/core/Breadcrumbs";
import NavigateNextIcon from "@material-ui/icons/NavigateNext";
import IconButton from "@material-ui/core/IconButton";
import KeyboardBackspaceIcon from "@material-ui/icons/KeyboardBackspace";
import CircularProgress from "@material-ui/core/CircularProgress";
//components
import Step1SelectIndents from "./add/step1SelectIndents";
import Step2FillDetails from "./add/step2FillDetails";
import Step3ReviewPO from "./add/step3ReviewPO";
//misc
import { apiEndpoints } from "./../../endpoints";
import { messages, constants } from "./../../messages";
import { getRole } from "./../../helper";
import Button from "./../../Shared/Button";
import trashRedIcon from "./../../Shared/Icons/trash-red.png";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import TextField from "@material-ui/core/TextField";
//style
import "./style.scss";
import "./add/stepStyles.scss";

class Add extends AddForm {
  title = messages.common.purchaseOrder;
  addurl = apiEndpoints.createPurchaseOrder;
  updateUrl = ""; // Will be set when API is ready
  state = {
    currentStep: 1,
    selectedIndents: [],
    indentItems: [],
    orderTo: null,
    orderFrom: null,
    poSubject: "",
    items: [],
    noteText: "",
    overridePhoneNumber: "",
    overrideEmail: "",
    showOverridePhoneEmail: false,
    projectName: "",
    isSpecialPo: false,
    poDate: moment().format(constants.dateFormat),
    freightCharges: "",
    freightGstPercent: 18,
    poDiscount: "",
    customCharges: [],
    poNumber: "", // Generated PO number
    isEditMode: false,
    isLoaded: false,
    selectedFileName: "",
    selectedFilePreview: null,
    isFileUploading: false,
    saveDraftNameDialogOpen: false,
    draftNameInput: "",
    draftNameError: "",
    loadedDraftId: null,
    isSavingDraft: false,
  };
  fileInputRef = React.createRef();

  componentDidMount() {
    if (!this.formData.fileInformations) {
      this.formData.fileInformations = [];
    }
    if (this.props.editData) {
      this.loadEditData(this.props.editData);
    } else if (this.props.draftData) {
      this.loadDraftData(this.props.draftData, this.props.draftId);
    } else if (this.props.quotePrefill) {
      this.loadQuotePrefill(this.props.quotePrefill);
    }
  }

  // Arrived from Quote Comparison's "Create PO" button — skip indent selection (step 1) since
  // the awarded supplier, products, rates and terms are already known from the finalized quote.
  loadQuotePrefill = (prefill) => {
    this.setState({
      isLoaded: true,
      currentStep: 2,
      items: prefill.items || [],
      orderTo: prefill.orderTo || null,
      projectName: prefill.projectName || "",
    });
  };

  componentWillUnmount() {
    // Clean up blob URLs to prevent memory leaks
    if (this.state.selectedFilePreview) {
      URL.revokeObjectURL(this.state.selectedFilePreview);
    }
    if (this.formData.fileInformations) {
      this.formData.fileInformations.forEach((file) => {
        if (file.previewUrl) {
          URL.revokeObjectURL(file.previewUrl);
        }
      });
    }
  }

  loadEditData = (data) => {
    // Map backend PO lines to form items
    const items = Array.from(data.lines || []).map((line) => ({
      lineId: line.id,
      productId: line.product?.productId,
      inventoryName: line.product?.productName || "",
      unit: line.product?.measurementUnit || "NOS",
      quantity: line.quantity != null ? String(line.quantity) : "",
      rate: line.rate != null ? String(line.rate) : "",
      gst: line.gstPercent != null ? String(line.gstPercent) : "",
      discount: line.discountPercent != null ? String(line.discountPercent) : "",
      tolerance: line.tolerancePercent != null ? String(line.tolerancePercent) : "",
      brandName: line.brand || "",
      grade: line.grade || "",
      diameter: line.diameter || "",
      specification: line.specification || "",
      netRate: line.netRate != null ? String(line.netRate) : "",
      totalAmt: line.totalAmount != null ? String(line.totalAmount) : "",
      sampleImageFileId: line.sampleImageFileId || null,
      sampleImagePreview: null, // will fall back to download URL in the UI
      leadTimeDays: line.leadTimeDays ?? null,
      billingUnit: line.billingUnit || null,
      billingQuantity: line.billingQuantity != null ? line.billingQuantity : null,
      billingConversionFactor: line.billingConversionFactor != null ? line.billingConversionFactor : null,
    }));

    this.formData.fileInformations = Array.from(data.fileInformations || []);

    this.setState({
      isEditMode: true,
      isLoaded: true,
      currentStep: 2, // Skip step 1 — line items cannot be changed
      items,
      orderTo: data.supplier ? { id: data.supplier.contactId, name: data.supplier.name } : null,
      orderFrom: data.firm ? { id: data.firm.firmId, name: data.firm.firmName } : null,
      poSubject: data.subject || "",
      noteText: data.notes || "",
      overridePhoneNumber: data.overridePhoneNumber || "",
      overrideEmail: data.overrideEmail || "",
      showOverridePhoneEmail: !!(data.overridePhoneNumber || data.overrideEmail),
      projectName: data.projectName || "",
      isSpecialPo: data.specialPo || false,
      freightCharges: data.freightCharges != null ? String(data.freightCharges) : "",
      freightGstPercent: data.freightGstPercent != null ? data.freightGstPercent : 18,
      poDiscount: data.poDiscount != null ? String(data.poDiscount) : "",
      customCharges: (data.customCharges || []).map(c => ({
        chargeName: c.chargeName || "",
        chargeAmount: c.chargeAmount != null ? String(c.chargeAmount) : "",
        chargeGstPercent: c.chargeGstPercent != null ? c.chargeGstPercent : 18,
      })),
    });
  };

  buildDraftPayload() {
    return {
      currentStep: this.state.currentStep,
      selectedIndents: this.state.selectedIndents,
      indentItems: this.state.indentItems,
      orderTo: this.state.orderTo,
      orderFrom: this.state.orderFrom,
      poSubject: this.state.poSubject,
      items: this.state.items,
      noteText: this.state.noteText,
      overridePhoneNumber: this.state.overridePhoneNumber,
      overrideEmail: this.state.overrideEmail,
      showOverridePhoneEmail: this.state.showOverridePhoneEmail,
      projectName: this.state.projectName,
      isSpecialPo: this.state.isSpecialPo,
      freightCharges: this.state.freightCharges,
      freightGstPercent: this.state.freightGstPercent,
      poDiscount: this.state.poDiscount,
      customCharges: this.state.customCharges,
      fileInformations: (this.formData.fileInformations || []).map((f) => ({
        fileUUId: f.fileUUId,
        fileName: f.fileName || "file",
      })),
    };
  }

  saveDraft = (e) => {
    if (e && e.preventDefault) e.preventDefault();
    this.setState({ saveDraftNameDialogOpen: true, draftNameInput: "", draftNameError: "" });
  };

  submitSaveDraft = async () => {
    const name = this.state.draftNameInput.trim();
    if (!name) {
      this.setState({ draftNameError: "Draft name is required" });
      return;
    }
    const payload = this.buildDraftPayload();
    this.setState({ isSavingDraft: true });
    const response = await API.POST(apiEndpoints.saveDraft("PO", name), payload);
    this.setState({ isSavingDraft: false });
    if (response.success) {
      this.setState({ saveDraftNameDialogOpen: false });
      this.props.enqueueSnackbar("Draft saved successfully.", { variant: "success" });
      this.props.back();
    } else {
      const errorMsg = response.errorMessage || "Failed to save draft";
      // Show name conflict inline; other errors as snackbar
      if (errorMsg.toLowerCase().includes("already exists")) {
        this.setState({ draftNameError: errorMsg });
      } else {
        this.setState({ saveDraftNameDialogOpen: false });
        this.props.enqueueSnackbar(errorMsg, { variant: "error" });
      }
    }
  };

  autoSaveDraftSilently = async () => {
    try {
      const payload = this.buildDraftPayload();
      const autoName = `Auto-save ${new Date().toLocaleString('en-IN', { timeZone: 'Asia/Kolkata' })}`;
      await API.POST(apiEndpoints.saveDraft("PO", autoName), payload);
    } catch (_) {
      // best-effort — ignore draft save errors
    }
  };

  loadDraftData = (draft, draftId = null) => {
    if (!draft) return;
    this.formData.fileInformations = draft.fileInformations || [];
    this.setState({
      loadedDraftId: draftId,
      currentStep: draft.currentStep ?? 1,
      selectedIndents: draft.selectedIndents || [],
      indentItems: draft.indentItems || [],
      orderTo: draft.orderTo || null,
      orderFrom: draft.orderFrom || null,
      poSubject: draft.poSubject || "",
      items: draft.items || [],
      noteText: draft.noteText || "",
      overridePhoneNumber: draft.overridePhoneNumber || "",
      overrideEmail: draft.overrideEmail || "",
      showOverridePhoneEmail: draft.showOverridePhoneEmail || false,
      projectName: draft.projectName || "",
      isSpecialPo: draft.isSpecialPo || false,
      freightCharges: draft.freightCharges || "",
      freightGstPercent: draft.freightGstPercent !== undefined ? draft.freightGstPercent : 18,
      poDiscount: draft.poDiscount || "",
      customCharges: draft.customCharges || [],
    });
    this.forceUpdate();
  };

  getSaveButtonDisabled() {
    if (this.state.currentStep === 1) {
      return this.state.selectedIndents.length === 0;
    }
    if (this.state.currentStep === 2) {
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
        !this.state.projectName ||
        this.state.items.length === 0 ||
        hasMissingRate
      );
    }
    return false;
  }

  getConfirmDisabledReason() {
    if (this.state.currentStep !== 2) return null;
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
    // Move to review step instead of saving directly
    this.setState({ currentStep: 3 });
  };

  handleNext = () => {
    if (this.state.currentStep === 1) {
      // Validate step 1
      if (this.state.selectedIndents.length === 0) {
        this.props.enqueueSnackbar("Please select at least one indent", {
          variant: "error",
        });
        return;
      }
      
      // Check if items already exist (preserving filled details from Step 2)
      const existingIndentIds = new Set(
        this.state.items.map((item) => item.indentId)
      );
      const selectedIndentIds = new Set(
        this.state.selectedIndents.map((indent) => indent.indentId)
      );
      
      // If indents haven't changed, preserve existing items
      const indentIdsMatch = 
        existingIndentIds.size === selectedIndentIds.size &&
        [...selectedIndentIds].every((id) => existingIndentIds.has(id));
      
      if (indentIdsMatch && this.state.items.length > 0) {
        // Indents haven't changed, preserve existing items and just move to step 2
        this.setState({ currentStep: 2 });
        return;
      }
      
      // Indents have changed - merge existing items with new ones
      const existingItemsMap = new Map(
        this.state.items.map((item) => [item.indentId, item])
      );
      
      const items = this.state.selectedIndents.map((indent) => {
        // Check if this indent already has a filled item
        const existingItem = existingItemsMap.get(indent.indentId);
        if (existingItem) {
          // Preserve existing filled details, but update basic indent info
          return {
            ...existingItem,
            inventoryName: indent.inventoryName,
            unit: indent.unit || existingItem.unit || "NOS",
            specification: indent.specification || existingItem.specification || "",
            remarks: indent.remarks || existingItem.remarks || "",
          };
        }
        
        // New indent - create item (no hardcoded defaults; values from indent only)
        const quantity = indent.quantity || "";
        const rate = parseFloat("" || 0);
        const qty = parseFloat(quantity || 0);
        const gst = parseFloat("" || 0);
        const netRate = rate * qty;
        const totalAmt = netRate + (netRate * gst / 100);

        return {
          indentId: indent.indentId,
          productId: indent.productId,
          inventoryName: indent.inventoryName,
          quantity,
          unit: indent.unit || "NOS",
          leadTimeDays: indent.leadTimeDays ?? null,
          specification: indent.specification || "",
          remarks: indent.remarks || "",
          diameter: "",
          size: "",
          brandName: "",
          grade: "",
          rate: "",
          gst: "",
          netRate: netRate > 0 ? netRate.toFixed(2) : "",
          totalAmt: totalAmt > 0 ? totalAmt.toFixed(2) : "",
        };
      });
      
      this.setState({ currentStep: 2, items });
    }
  };

  handlePrevious = () => {
    if (this.state.currentStep === 3) {
      this.setState({ currentStep: 2 });
    } else if (this.state.currentStep === 2 && !this.state.isEditMode) {
      this.setState({ currentStep: 1 });
    }
  };

  handleCancel = () => {
    this.props.back();
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
          billingUnit: item.billingUnit || null,
          billingQuantity: item.billingQuantity || null,
          billingConversionFactor: item.billingConversionFactor || null,
          indentRefs: [],
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
    // Round quantity to avoid floating-point artifacts (e.g. 154.67000000000002 -> 154.67)
    const roundQuantity = (q) => {
      const n = parseFloat(q);
      return isNaN(n) ? 0 : (Number.isInteger(n) ? n : parseFloat(n.toFixed(2)));
    };
    const lineItems = Object.values(groupedItems).map((item) => {
      const quantity = roundQuantity(item.quantity);
      // When billing unit selected, rate is per billing unit — calculate on billing qty
      const calcQty = item.billingUnit && item.billingQuantity
        ? parseFloat(item.billingQuantity)
        : quantity;
      const rate = item.rate;
      const discount = item.discountPercent || 0;
      const gstPercent = item.gstPercent;
      const discountedRate = rate - (rate * discount / 100);
      const netRate = discountedRate * calcQty;
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
        billingUnit: item.billingUnit || null,
        billingQuantity: item.billingQuantity ? parseFloat(item.billingQuantity) : null,
        billingConversionFactor: item.billingConversionFactor || null,
        indentRefs: item.indentRefs,
      };
    });

    // PO Discount is a flat, special, post-tax PO-level deduction — it must not change line
    // item rates, GST, or totals. It's subtracted once from the grand total below.
    const poDiscountAmt = parseFloat(this.state.poDiscount || 0);

    // Calculate grandTotal from lineItems
    const lineItemsTotal = lineItems.reduce((sum, item) => sum + (item.totalAmount || 0), 0);

    // Calculate freight totals
    const freightCharges = parseFloat(this.state.freightCharges || 0);
    const freightGstPercent = parseFloat(this.state.freightGstPercent !== "" ? this.state.freightGstPercent : 18);
    const totalFreightCharges = Math.round((freightCharges + freightCharges * freightGstPercent / 100) * 100) / 100;

    // Calculate custom charge totals
    const customChargesPayload = (this.state.customCharges || [])
      .filter(c => parseFloat(c.chargeAmount || 0) > 0)
      .map(c => {
        const amt = parseFloat(c.chargeAmount || 0);
        const gst = parseFloat(c.chargeGstPercent !== "" && c.chargeGstPercent !== undefined ? c.chargeGstPercent : 18);
        const total = Math.round((amt + amt * gst / 100) * 100) / 100;
        return { chargeName: c.chargeName || null, chargeAmount: amt, chargeGstPercent: gst, totalChargeAmount: total };
      });
    const totalCustomCharges = customChargesPayload.reduce((sum, c) => sum + c.totalChargeAmount, 0);

    const grandTotal = Math.round((lineItemsTotal + totalFreightCharges + totalCustomCharges - poDiscountAmt) * 100) / 100;

    // Prepare payload matching API structure
    const payload = {
      poDate: this.state.poDate,
      supplierId: this.state.orderTo?.id || null,
      firmId: this.state.orderFrom?.id || null,
      subject: this.state.poSubject || "",
      notes: this.state.noteText || "",
      overridePhoneNumber: this.state.showOverridePhoneEmail ? (this.state.overridePhoneNumber || null) : null,
      overrideEmail: this.state.showOverridePhoneEmail ? (this.state.overrideEmail || null) : null,
      projectName: this.state.projectName || null,
      fileInformations: (this.formData.fileInformations || []).map((f) => ({
        fileUUId: f.fileUUId,
        fileName: f.fileName,
      })),
      grandTotal: grandTotal,
      freightCharges: freightCharges || null,
      freightGstPercent: freightCharges > 0 ? freightGstPercent : null,
      totalFreightCharges: freightCharges > 0 ? totalFreightCharges : null,
      poDiscount: poDiscountAmt || null,
      customCharges: customChargesPayload,
      specialPo: this.state.isSpecialPo || false,
      lineItems: lineItems
    };

    this.setState({ isAdding: true });

    try {
      if (this.state.isEditMode) {
        // Build line updates — quantity and indent refs are preserved by the backend
        const lineUpdates = this.state.items.map((item) => {
          const qty = parseFloat(item.quantity || 0);
          const billingQty = item.billingUnit && item.billingQuantity
            ? parseFloat(item.billingQuantity) : null;
          const calcQty = billingQty !== null ? billingQty : qty;
          const rate = parseFloat(item.rate || 0);
          const discount = parseFloat(item.discount || 0);
          const gst = parseFloat(item.gst || 0);
          const discountedRate = rate - (rate * discount / 100);
          const netRate = discountedRate * calcQty;
          const totalAmount = netRate + (netRate * gst / 100);
          return {
            lineId: item.lineId,
            rate,
            discountPercent: discount,
            tolerancePercent: parseFloat(item.tolerance || 0),
            gstPercent: gst,
            netRate,
            totalAmount,
            brand: item.brandName || "",
            grade: item.grade || "",
            diameter: item.diameter || "",
            specification: item.specification || "",
            sampleImageFileId: item.sampleImageFileId || null,
            billingUnit: item.billingUnit || null,
            billingQuantity: billingQty,
            billingConversionFactor: item.billingConversionFactor || null,
          };
        });

        // PO Discount is a flat, special, post-tax PO-level deduction — does not touch line items.
        const poDiscountEdit = parseFloat(this.state.poDiscount || 0);

        const lineItemsTotal = lineUpdates.reduce((sum, l) => sum + (l.totalAmount || 0), 0);
        const freightChargesEdit = parseFloat(this.state.freightCharges || 0);
        const freightGstEdit = parseFloat(this.state.freightGstPercent !== "" ? this.state.freightGstPercent : 18);
        const totalFreightEdit = Math.round((freightChargesEdit + freightChargesEdit * freightGstEdit / 100) * 100) / 100;
        const customChargesEdit = (this.state.customCharges || [])
          .filter(c => parseFloat(c.chargeAmount || 0) > 0)
          .map(c => {
            const amt = parseFloat(c.chargeAmount || 0);
            const gst = parseFloat(c.chargeGstPercent !== "" && c.chargeGstPercent !== undefined ? c.chargeGstPercent : 18);
            const total = Math.round((amt + amt * gst / 100) * 100) / 100;
            return { chargeName: c.chargeName || null, chargeAmount: amt, chargeGstPercent: gst, totalChargeAmount: total };
          });
        const totalCustomEdit = customChargesEdit.reduce((sum, c) => sum + c.totalChargeAmount, 0);
        const grandTotalEdit = Math.round((lineItemsTotal + totalFreightEdit + totalCustomEdit - poDiscountEdit) * 100) / 100;

        const updatePayload = {
          poDate: this.state.poDate,
          supplierId: this.state.orderTo?.id || null,
          firmId: this.state.orderFrom?.id || null,
          subject: this.state.poSubject || "",
          notes: this.state.noteText || "",
          overridePhoneNumber: this.state.showOverridePhoneEmail ? (this.state.overridePhoneNumber || null) : null,
          overrideEmail: this.state.showOverridePhoneEmail ? (this.state.overrideEmail || null) : null,
          projectName: this.state.projectName || null,
          specialPo: this.state.isSpecialPo || false,
          freightCharges: freightChargesEdit || null,
          freightGstPercent: freightChargesEdit > 0 ? freightGstEdit : null,
          totalFreightCharges: freightChargesEdit > 0 ? totalFreightEdit : null,
          poDiscount: poDiscountEdit || null,
          customCharges: customChargesEdit,
          grandTotal: grandTotalEdit,
          fileInformations: (this.formData.fileInformations || []).map((f) => ({
            fileUUId: f.fileUUId,
            fileName: f.fileName,
          })),
          lineUpdates,
        };

        const response = await API.PUT(
          apiEndpoints.updatePurchaseOrder(this.props.editData.purchaseOrderId),
          updatePayload
        );
        this.setState({ isAdding: false });
        if (response.success) {
          this.props.enqueueSnackbar("Purchase Order updated successfully", { variant: "success" });
          this.props.back();
        } else {
          this.props.enqueueSnackbar(response.errorMessage || "Failed to update Purchase Order", { variant: "error" });
        }
      } else {
        // Create new Purchase Order
        const response = await API.POST(this.addurl, payload);
        this.setState({ isAdding: false });

        if (response.success) {
          this.props.enqueueSnackbar("Purchase Order created successfully", {
            variant: "success",
          });
          if (this.state.loadedDraftId) {
            API.DELETE(apiEndpoints.deleteDraftById(this.state.loadedDraftId)).catch(() => {});
          }
          // If this PO was created from a Quote Comparison "Create PO" action, mark those
          // quote lines as PO-linked so the comparison reflects that it's been ordered.
          const poId = response.data?.purchaseOrderId;
          const linkedItems = this.state.items.filter(i => i._linkedQcLineId && i._linkedSupplierQuoteLineId && i._linkedQcId);
          if (poId && linkedItems.length > 0) {
            await Promise.all(linkedItems.map(li => API.POST(apiEndpoints.quoteComparisonLinkToPo, {
              supplierQuoteLineId: li._linkedSupplierQuoteLineId,
              qcLineId: li._linkedQcLineId,
              qcId: li._linkedQcId,
              purchaseOrderId: poId,
              poLineId: null,
            }).catch(() => {})));
          }
          this.props.back();
        } else {
          await this.autoSaveDraftSilently();
          this.props.enqueueSnackbar(
            (response.errorMessage || "Save failed.") + " Your data has been saved as a draft automatically.",
            { variant: "warning" }
          );
          this.setState({ currentStep: 3 });
        }
      }
    } catch (error) {
      this.setState({ isAdding: false });
      await this.autoSaveDraftSilently();
      this.props.enqueueSnackbar(
        "An error occurred. Your data has been saved as a draft automatically.",
        { variant: "warning" }
      );
      this.setState({ currentStep: 3 });
    }
  };

  renderBreadcrumbs() {
    const steps = ["Purchase Order", this.state.isEditMode ? "Edit Purchase Order" : "Create Purchase Order"];
    if (this.state.currentStep === 2) {
      steps.push("Fill Details");
    } else if (this.state.currentStep === 3) {
      steps.push("Fill Details");
      steps.push("Review");
    }
    return (
      <Breadcrumbs
        separator={<NavigateNextIcon />}
        aria-label="breadcrumb"
        className="breadcrumbs"
      >
        {steps.map((step, index) => (
          <span key={index}>{step}</span>
        ))}
      </Breadcrumbs>
    );
  }

  renderHeaderActions() {
    if (this.state.currentStep === 1) {
      return (
        <div className="po-action-buttons">
          <Button
            onClick={this.handleCancel}
            buttonClass="grey"
            label={messages.common.cancel}
          />
          <Button
            onClick={this.saveDraft}
            buttonClass="grey"
            label="Save as Draft"
            disabled={this.state.isSavingDraft}
          />
          <Button
            onClick={this.handleNext}
            buttonClass="blue"
            label="Next"
            disabled={this.getSaveButtonDisabled()}
          />
        </div>
      );
    } else if (this.state.currentStep === 2) {
      return (
        <div className="po-action-buttons">
          {!this.state.isEditMode && (
            <Button
              onClick={this.handlePrevious}
              buttonClass="grey"
              label="Previous"
            />
          )}
          <Button
            onClick={this.handleCancel}
            buttonClass="grey"
            label={messages.common.cancel}
          />
          {!this.state.isEditMode && (
            <Button
              onClick={this.saveDraft}
              buttonClass="grey"
              label="Save as Draft"
              disabled={this.state.isSavingDraft}
            />
          )}
          <Button
            onClick={this.handleConfirmClick}
            buttonClass={this.getSaveButtonDisabled() ? "grey confirm-disabled" : "blue"}
            label="Review"
            disabled={this.state.isAdding}
          />
        </div>
      );
    } else {
      // Step 3 — Review
      return (
        <div className="po-action-buttons">
          <Button
            onClick={this.handlePrevious}
            buttonClass="grey"
            label="Back to Edit"
          />
          <Button
            onClick={this.handleCancel}
            buttonClass="grey"
            label={messages.common.cancel}
          />
          <Button
            onClick={this.handleConfirm}
            buttonClass="blue"
            label={this.state.isEditMode ? "Confirm & Update" : "Confirm & Save"}
            disabled={this.state.isAdding}
          />
        </div>
      );
    }
  }

  renderSaveDraftNameDialog() {
    return (
      <Dialog
        open={this.state.saveDraftNameDialogOpen}
        onClose={() => this.setState({ saveDraftNameDialogOpen: false })}
        maxWidth="xs"
        fullWidth
        aria-labelledby="save-draft-dialog-title"
      >
        <DialogTitle id="save-draft-dialog-title">Save as Draft</DialogTitle>
        <DialogContent>
          <TextField
            label="Draft Name"
            fullWidth
            autoFocus
            value={this.state.draftNameInput}
            onChange={(e) => this.setState({ draftNameInput: e.target.value, draftNameError: "" })}
            onKeyPress={(e) => { if (e.key === "Enter") this.submitSaveDraft(); }}
            error={!!this.state.draftNameError}
            helperText={this.state.draftNameError || "Give this draft a unique name for easy identification"}
            style={{ marginTop: 8 }}
          />
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => this.setState({ saveDraftNameDialogOpen: false })}
            buttonClass="grey"
            label="Cancel"
          />
          <Button
            onClick={this.submitSaveDraft}
            buttonClass="blue"
            label="Save Draft"
            disabled={this.state.isSavingDraft}
          />
        </DialogActions>
      </Dialog>
    );
  }

  renderHeading() {
    return (
      <div className="add-heading-wrapper">
        <IconButton
          aria-label="back"
          onClick={this.handleCancel}
          className="back-icon"
        >
          <KeyboardBackspaceIcon />
        </IconButton>
        <span className="add-heading">
          {this.state.isEditMode ? "Edit " : "Add "}{messages.common.purchaseOrder}
        </span>
      </div>
    );
  }

  handleFileSelect(e) {
    const file = e.target.files[0];
    if (!file) {
      this.setState({ selectedFileName: "", selectedFilePreview: null });
      return;
    }

    // Validate file size (2MB max)
    const fileSize = file.size / 1024 / 1024; // in MB
    if (fileSize > 2) {
      this.props.enqueueSnackbar("File upload is restricted to 2MB", {
        variant: "error",
      });
      this.setState({ selectedFileName: "", selectedFilePreview: null });
      if (this.fileInputRef.current) {
        this.fileInputRef.current.value = "";
      }
      return;
    }

    // Validate file type (JPG, PNG, PDF only)
    const validTypes = ["image/jpeg", "image/jpg", "image/png", "application/pdf"];
    if (!validTypes.includes(file.type)) {
      this.props.enqueueSnackbar("Only JPG, PNG and PDF files are allowed", {
        variant: "error",
      });
      this.setState({ selectedFileName: "", selectedFilePreview: null });
      if (this.fileInputRef.current) {
        this.fileInputRef.current.value = "";
      }
      return;
    }

    // Create preview URL for the selected file
    const previewUrl = URL.createObjectURL(file);

    this.setState({
      selectedFileName: file.name,
      selectedFilePreview: previewUrl,
    });
  }

  async handleFileSubmit() {
    if (!this.fileInputRef.current || !this.fileInputRef.current.files[0]) {
      this.props.enqueueSnackbar("Please select a file first", {
        variant: "error",
      });
      return;
    }

    const file = this.fileInputRef.current.files[0];
    const formData = new FormData();
    formData.append("file", file, file.name);

    this.setState({ isFileUploading: true });

    try {
      const response = await API.POST(apiEndpoints.masterFileUpload, formData);
      if (response.success) {
        if (!this.formData.fileInformations) {
          this.formData.fileInformations = [];
        }
        const fileData = {
          ...response.data,
          previewUrl: this.state.selectedFilePreview,
        };
        this.formData.fileInformations.push(fileData);
        this.setState({
          fileUploadKey: Date.now(),
          selectedFileName: "",
          selectedFilePreview: null,
          isFileUploading: false,
        });
        this.props.enqueueSnackbar("File uploaded successfully", {
          variant: "success",
        });
        if (this.fileInputRef.current) {
          this.fileInputRef.current.value = "";
        }
      } else {
        if (this.state.selectedFilePreview) {
          URL.revokeObjectURL(this.state.selectedFilePreview);
        }
        this.setState({ isFileUploading: false });
        this.props.enqueueSnackbar(response.errorMessage || "Upload failed", {
          variant: "error",
        });
      }
    } catch (error) {
      if (this.state.selectedFilePreview) {
        URL.revokeObjectURL(this.state.selectedFilePreview);
      }
      this.setState({ isFileUploading: false });
      this.props.enqueueSnackbar("Upload failed", {
        variant: "error",
      });
    }
  }

  handleFileRemove(fileToRemove) {
    if (!this.formData.fileInformations) {
      this.formData.fileInformations = [];
    }
    if (fileToRemove.previewUrl) {
      URL.revokeObjectURL(fileToRemove.previewUrl);
    }
    this.formData.fileInformations = this.formData.fileInformations.filter(
      (file) => file.fileUUId !== fileToRemove.fileUUId
    );
    this.forceUpdate();
  }

  renderIndentStyleFileArea() {
    if (!this.formData.fileInformations) {
      this.formData.fileInformations = [];
    }
    const files = this.formData.fileInformations || [];

    return (
      <div className="indent-upload-section">
        <div className="upload-documents-heading">Upload Documents</div>
        <div className="upload-controls">
          <div className="upload-left">
            <div className="upload-buttons-row">
              <input
                ref={this.fileInputRef}
                type="file"
                id="po-file-upload"
                accept="image/jpeg,image/jpg,image/png,application/pdf"
                onChange={(e) => this.handleFileSelect(e)}
                style={{ display: "none" }}
              />
              <label
                htmlFor="po-file-upload"
                className="file-name-input-label"
                onClick={(e) => {
                  e.preventDefault();
                  if (this.fileInputRef.current) {
                    this.fileInputRef.current.click();
                  }
                }}
              >
                <input
                  type="text"
                  className="file-name-input"
                  value={this.state.selectedFileName}
                  placeholder="choose file"
                  readOnly
                />
              </label>
              <Button
                buttonClass="blue"
                label="Submit"
                onClick={() => this.handleFileSubmit()}
                disabled={this.state.isFileUploading}
              />
              {this.state.isFileUploading && (
                <CircularProgress
                  size={20}
                  style={{ marginLeft: "10px", color: "#1976d2" }}
                />
              )}
              <div className="upload-thumbnails">
                {files.map((file, index) => (
                  <div
                    key={file.fileUUId || index}
                    className="upload-thumbnail"
                  >
                    <IconButton
                      className="thumbnail-remove"
                      onClick={() => this.handleFileRemove(file)}
                    >
                      <img
                        src={trashRedIcon}
                        alt="Remove"
                        className="trash-red-icon"
                      />
                    </IconButton>
                    <div className="thumbnail-preview">
                      {file.previewUrl ? (
                        <img src={file.previewUrl} alt={file.fileName} />
                      ) : (
                        <div className="thumbnail-placeholder">
                          <svg
                            width="40"
                            height="40"
                            viewBox="0 0 24 24"
                            fill="none"
                            xmlns="http://www.w3.org/2000/svg"
                          >
                            <path
                              d="M14 2H6C4.9 2 4 2.9 4 4V20C4 21.1 4.89 22 5.99 22H18C19.1 22 20 21.1 20 20V8L14 2ZM18 20H6V4H13V9H18V20Z"
                              fill="#999"
                            />
                          </svg>
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <div className="upload-hint">
              Upload a file here, Max 2 MB allow (JPG, PNG, PDF only)
            </div>
          </div>
        </div>
      </div>
    );
  }

  render() {
    if (this.state.isEditMode && !this.state.isLoaded) {
      return <div>Loading...</div>;
    }

    return (
      <div className="list-section add create-po-wrapper">
        {this.renderSaveDraftNameDialog()}
        <div className="create-po-header">
          <div className="create-po-header-row">
            {this.renderHeading()}
            {this.renderHeaderActions()}
          </div>
          {this.renderBreadcrumbs()}
        </div>
        <div className="create-po-content">
          {this.state.currentStep === 1 && (
            <Step1SelectIndents
              selectedIndents={this.state.selectedIndents}
              indentItems={this.state.indentItems}
              onSelectIndents={(indents) => {
                this.setState({ selectedIndents: indents });
              }}
              onIndentItemsChange={(items) => {
                this.setState({ indentItems: items });
              }}
            />
          )}
          {this.state.currentStep === 2 && (
            <Step2FillDetails
              orderTo={this.state.orderTo}
              orderFrom={this.state.orderFrom}
              poSubject={this.state.poSubject}
              items={this.state.items}
              noteText={this.state.noteText}
              onOrderToChange={(orderTo) => this.setState({ orderTo })}
              onOrderFromChange={(orderFrom) => this.setState({ orderFrom })}
              onPoSubjectChange={(poSubject) => this.setState({ poSubject })}
              onItemsChange={(items) => this.setState({ items })}
              onNoteTextChange={(text) => this.setState({ noteText: text })}
              overridePhoneNumber={this.state.overridePhoneNumber}
              onOverridePhoneNumberChange={(val) => this.setState({ overridePhoneNumber: val })}
              overrideEmail={this.state.overrideEmail}
              onOverrideEmailChange={(val) => this.setState({ overrideEmail: val })}
              showOverridePhoneEmail={this.state.showOverridePhoneEmail}
              onShowOverridePhoneEmailChange={(val) => this.setState({ showOverridePhoneEmail: val })}
              projectName={this.state.projectName}
              onProjectNameChange={(val) => this.setState({ projectName: val })}
              isSpecialPo={this.state.isSpecialPo}
                onIsSpecialPoChange={(val) => this.setState({ isSpecialPo: val })}
              poDate={this.state.poDate}
              onPoDateChange={(date) => this.setState({ poDate: date })}
              isAdmin={(getRole() || "").toLowerCase() === "admin"}
              freightCharges={this.state.freightCharges}
              freightGstPercent={this.state.freightGstPercent}
              onFreightChargesChange={(val) => this.setState({ freightCharges: val })}
              onFreightGstPercentChange={(val) => this.setState({ freightGstPercent: val })}
              poDiscount={this.state.poDiscount}
              onPoDiscountChange={(val) => this.setState({ poDiscount: val })}
              customCharges={this.state.customCharges}
              onCustomChargesChange={(val) => this.setState({ customCharges: val })}
              fileArea={this.renderIndentStyleFileArea()}
              dropdowns={this.props.dropdowns || {}}
              isEditMode={this.state.isEditMode}
              enqueueSnackbar={this.props.enqueueSnackbar}
            />
          )}
          {this.state.currentStep === 3 && (
            <Step3ReviewPO
              orderTo={this.state.orderTo}
              orderFrom={this.state.orderFrom}
              poSubject={this.state.poSubject}
              isSpecialPo={this.state.isSpecialPo}
              poDate={this.state.poDate}
              items={this.state.items}
              noteText={this.state.noteText}
              projectName={this.state.projectName}
              overridePhoneNumber={this.state.overridePhoneNumber}
              overrideEmail={this.state.overrideEmail}
              fileInformations={this.formData.fileInformations || []}
              freightCharges={this.state.freightCharges}
              freightGstPercent={this.state.freightGstPercent}
              poDiscount={this.state.poDiscount}
              customCharges={this.state.customCharges}
            />
          )}
        </div>
      </div>
    );
  }
}

export default connect()(withSnackbar(Add));
