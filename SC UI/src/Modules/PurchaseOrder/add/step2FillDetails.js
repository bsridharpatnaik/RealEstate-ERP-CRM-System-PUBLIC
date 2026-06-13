//react
import React from "react";
import Autocomplete from "@material-ui/lab/Autocomplete";
import TextField from "@material-ui/core/TextField";
import ReactQuill from 'react-quill';
import 'react-quill/dist/quill.snow.css';
import Table from "@material-ui/core/Table";
import TableBody from "@material-ui/core/TableBody";
import TableCell from "@material-ui/core/TableCell";
import TableHead from "@material-ui/core/TableHead";
import TableRow from "@material-ui/core/TableRow";
import IconButton from "@material-ui/core/IconButton";
import trashOutlineIcon from "./../../../Shared/Icons/trash-outline.png";
import Button from "@material-ui/core/Button";
import CircularProgress from "@material-ui/core/CircularProgress";
import AddNewSupplierModal from "./addNewSupplierModal";
import AddNewFirmModal from "./addNewFirmModal";
import { API } from "./../../../axios";
import { apiEndpoints } from "./../../../endpoints";
import Checkbox from "@material-ui/core/Checkbox";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import Popover from "@material-ui/core/Popover";
import moment from "moment";
import DatePicker from "./../../../Shared/Date";
import { constants } from "./../../../messages";

class Step2FillDetails extends React.Component {
  lineFileInputRefs = {};

  state = {
    showSupplierModal: false,
    showFirmModal: false,
    editFirmId: null,
    orderToOptions: [],
    orderFromOptions: [],
    isLoadingSuppliers: false,
    isLoadingFirms: false,
    supplierDetailsCache: {}, // Cache for supplier details by ID
    firmDetailsCache: {}, // Cache for firm details by ID
    // PO rate history tooltip
    ratesCache: {},           // productId -> previousRates[]
    ratesLoading: {},         // productId -> bool
    tooltipAnchorEl: null,    // DOM element for Popover anchor
    tooltipProductId: null,   // which product is hovered
    projectList: [],
    unitConversionsCache: {}, // productId -> [{id, unitName, conversionFactor}]
  };

  componentDidMount() {
    this.fetchSupplierNames();
    this.fetchFirmList();
    this.fetchUnitConversionsForItems(this.props.items || []);
    API.GET(apiEndpoints.getTenants)
      .then((res) => {
        if (res.success && Array.isArray(res.data)) {
          const names = res.data
            .filter((t) => t.inventory === true)
            .map((t) => t.name || t.tenantName || "")
            .filter(Boolean);
          this.setState({ projectList: names });
        }
      })
      .catch(() => {});
    // If orderTo is already set (edit mode), fetch its details
    if (this.props.orderTo && this.props.orderTo.id) {
      this.loadSupplierDetails(this.props.orderTo.id);
    }
    // If orderFrom is already set (edit mode), fetch its details
    if (this.props.orderFrom && this.props.orderFrom.id) {
      this.loadFirmDetails(this.props.orderFrom.id);
    }
  }

  componentDidUpdate(prevProps) {
    // Fetch unit conversions for newly added items
    if (prevProps.items !== this.props.items) {
      this.fetchUnitConversionsForItems(this.props.items || []);
    }
    // If orderTo changes and has an ID, fetch its details if not already loaded
    if (
      this.props.orderTo &&
      this.props.orderTo.id &&
      (!prevProps.orderTo || prevProps.orderTo.id !== this.props.orderTo.id)
    ) {
      this.loadSupplierDetails(this.props.orderTo.id);
    }
    // If orderFrom changes and has an ID, fetch its details if not already loaded
    if (
      this.props.orderFrom &&
      this.props.orderFrom.id &&
      (!prevProps.orderFrom || prevProps.orderFrom.id !== this.props.orderFrom.id)
    ) {
      this.loadFirmDetails(this.props.orderFrom.id);
    }
  }

  fetchUnitConversionsForItems = async (items) => {
    const { unitConversionsCache } = this.state;
    const newProductIds = [...new Set((items || []).map((i) => i.productId))]
      .filter((id) => id && !(id in unitConversionsCache));

    if (newProductIds.length === 0) return;

    // Mark as loading (empty array) to avoid duplicate fetches
    const pending = {};
    newProductIds.forEach((id) => { pending[id] = []; });
    this.setState((prev) => ({
      unitConversionsCache: { ...prev.unitConversionsCache, ...pending },
    }));

    await Promise.all(
      newProductIds.map(async (productId) => {
        try {
          const res = await API.GET(apiEndpoints.getUnitConversions(productId));
          const conversions = res.success ? res.data : [];
          this.setState((prev) => ({
            unitConversionsCache: { ...prev.unitConversionsCache, [productId]: conversions },
          }));
        } catch (_) {
          // leave as empty array on error
        }
      })
    );
  };

  handleBillingUnitChange = (index, selectedUnit, conversions, baseUnit) => {
    const mergedItems = this.groupItemsByProductId();
    const targetItem = mergedItems[index];
    if (!targetItem) return;

    const productId = targetItem.productId;
    const baseQty = parseFloat(targetItem.quantity || 0);

    if (!selectedUnit || selectedUnit === baseUnit) {
      // Revert to base unit — clear billing fields, clear rate, recalc on base qty
      const updatedItems = this.props.items.map((item) => {
        if (item.productId === productId) {
          const updated = {
            ...item,
            billingUnit: null,
            billingQuantity: null,
            billingConversionFactor: null,
            rate: "",
          };
          updated.netRate = "0.00";
          updated.totalAmt = "0.00";
          return updated;
        }
        return item;
      });
      this.props.onItemsChange(updatedItems);
      return;
    }

    const conversion = conversions.find((c) => c.unitName === selectedUnit);
    if (!conversion) return;

    // Billing qty = base qty / factor (auto-computed, read-only)
    const billingQty = conversion.conversionFactor > 0
      ? parseFloat((baseQty / conversion.conversionFactor).toFixed(2))
      : 0;

    // Clear rate — user must re-enter rate in new unit; reset totals
    const updatedItems = this.props.items.map((item) => {
      if (item.productId === productId) {
        return {
          ...item,
          billingUnit: selectedUnit,
          billingQuantity: billingQty,
          billingConversionFactor: conversion.conversionFactor,
          rate: "",
          netRate: "0.00",
          totalAmt: "0.00",
        };
      }
      return item;
    });
    this.props.onItemsChange(updatedItems);
  };

  loadSupplierDetails = async (supplierId) => {
    // Only fetch if details are missing
    if (
      this.props.orderTo &&
      !this.props.orderTo.contactPerson &&
      !this.props.orderTo.mobileNumber &&
      !this.props.orderTo.address
    ) {
      const details = await this.fetchSupplierDetails(supplierId);
      if (details) {
        // Update the orderTo in parent component with fetched details
        const updatedOrderTo = {
          ...this.props.orderTo,
          ...details,
        };
        this.props.onOrderToChange(updatedOrderTo);
      }
    }
  };

  loadFirmDetails = async (firmId) => {
    // Only fetch if details are missing
    if (
      this.props.orderFrom &&
      !this.props.orderFrom.address &&
      !this.props.orderFrom.gst &&
      !this.props.orderFrom.pan &&
      !this.props.orderFrom.contactNumber &&
      !this.props.orderFrom.description
    ) {
      const details = await this.fetchFirmDetails(firmId);
      if (details) {
        // Update the orderFrom in parent component with fetched details
        const updatedOrderFrom = {
          ...this.props.orderFrom,
          ...details,
        };
        this.props.onOrderFromChange(updatedOrderFrom);
      }
    }
  };

  fetchSupplierNames = async () => {
    this.setState({ isLoadingSuppliers: true });
    
    try {
      const response = await API.GET(apiEndpoints.getSupplierNames);
      
      if (response.success && response.data && Array.isArray(response.data)) {
        // Transform API response to match component's expected format
        const suppliers = response.data.map((supplier) => ({
          id: supplier.id,
          name: supplier.name.trim(), // Trim whitespace from name
          contactPerson: null, // Will be fetched if needed
          mobileNumber: null, // Will be fetched if needed
          address: null, // Will be fetched if needed
        }));
        
        this.setState({
          orderToOptions: suppliers,
          isLoadingSuppliers: false,
        });
      } else {
        this.setState({
          isLoadingSuppliers: false,
        });
      }
    } catch (error) {
      this.setState({
        isLoadingSuppliers: false,
      });
    }
  };

  fetchFirmList = async () => {
    this.setState({ isLoadingFirms: true });
    
    try {
      const response = await API.GET(apiEndpoints.getFirmIdAndNames);
      
      if (response.success && response.data) {
        // Handle different response formats
        let firms = [];
        
        if (Array.isArray(response.data)) {
          // If response is directly an array
          firms = response.data;
        } else if (response.data.firms && Array.isArray(response.data.firms)) {
          // If response has a firms property
          firms = response.data.firms;
        } else if (response.data.content && Array.isArray(response.data.content)) {
          // If response is paginated
          firms = response.data.content;
        }
        
        // Transform API response to match component's expected format
        const firmOptions = firms.map((firm) => ({
          id: firm.id || firm.firmId || null,
          name: (firm.name || firm.firmName || "").trim() || null,
          address: firm.address || firm.firmAddress || null,
        }));
        
        this.setState({
          orderFromOptions: firmOptions,
          isLoadingFirms: false,
        });
      } else {
        this.setState({
          isLoadingFirms: false,
        });
      }
    } catch (error) {
      this.setState({
        isLoadingFirms: false,
      });
    }
  };

  fetchSupplierDetails = async (supplierId) => {
    // Check cache first
    if (this.state.supplierDetailsCache[supplierId]) {
      return this.state.supplierDetailsCache[supplierId];
    }

    try {
      // Try to fetch full supplier details from contact endpoint
      // Note: This assumes supplier ID maps to contact ID
      const response = await API.GET(apiEndpoints.getContactDetail + supplierId);
      
      if (response.success && response.data) {
        const contact = response.data;
        // Build address from available fields
        const addressParts = [];
        if (contact.addr_line1) addressParts.push(contact.addr_line1);
        if (contact.addr_line2) addressParts.push(contact.addr_line2);
        if (contact.city) addressParts.push(contact.city);
        if (contact.state) addressParts.push(contact.state);
        if (contact.zip) addressParts.push(contact.zip);
        
        const supplierDetails = {
          contactPerson: contact.contactPerson || null,
          mobileNumber: contact.contactPersonMobileNo || contact.mobileNo || null,
          address: addressParts.length > 0 ? addressParts.join(", ") : null,
        };
        
        // Cache the details
        this.setState((prevState) => ({
          supplierDetailsCache: {
            ...prevState.supplierDetailsCache,
            [supplierId]: supplierDetails,
          },
        }));
        
        return supplierDetails;
      }
    } catch (error) {
      // If fetching details fails, return null values
      return null;
    }
    
    return null;
  };

  fetchFirmDetails = async (firmId) => {
    // Check cache first
    if (this.state.firmDetailsCache[firmId]) {
      return this.state.firmDetailsCache[firmId];
    }

    try {
      const response = await API.GET(apiEndpoints.getFirmDetail + firmId);
      
      if (response.success && response.data) {
        const firm = response.data;
        
const firmDetails = {
          address: firm.firmAddress || null,
          addr_line1: firm.addr_line1 || null,
          addr_line2: firm.addr_line2 || null,
          city: firm.city || null,
          state: firm.state || null,
          zip: firm.zip || null,
          gst: firm.firmGstNumber || null,
          pan: firm.firmPanNumber || null,
          contactNumber: firm.firmContactNumber || null,
          description: firm.firmDescription || null,
        };
        
        // Cache the details
        this.setState((prevState) => ({
          firmDetailsCache: {
            ...prevState.firmDetailsCache,
            [firmId]: firmDetails,
          },
        }));
        
        return firmDetails;
      }
    } catch (error) {
      // If fetching details fails, return null values
      return null;
    }
    
    return null;
  };

  handleOrderToChange = async (event, value) => {
    if (value) {
      // Fetch supplier details if not already available
      if (!value.contactPerson && !value.mobileNumber && !value.address) {
        const details = await this.fetchSupplierDetails(value.id);
        if (details) {
          value = {
            ...value,
            ...details,
          };
          // Update the option in the list
          const updatedOptions = this.state.orderToOptions.map((option) =>
            option.id === value.id ? value : option
          );
          this.setState({ orderToOptions: updatedOptions });
        }
      }
    }
    this.props.onOrderToChange(value);
  };

  handleOrderFromChange = async (event, value) => {
    if (value) {
      // Fetch firm details if not already available
      if (!value.address && !value.gst && !value.pan && !value.contactNumber && !value.description) {
        const details = await this.fetchFirmDetails(value.id);
        if (details) {
          value = {
            ...value,
            ...details,
          };
          // Update the option in the list
          const updatedOptions = this.state.orderFromOptions.map((option) =>
            option.id === value.id ? value : option
          );
          this.setState({ orderFromOptions: updatedOptions });
        }
      }
    }
    this.props.onOrderFromChange(value);
  };

  handleLineImageUpload = async (index, e) => {
    const inputEl = e.target; // capture before async — React recycles synthetic events
    const file = inputEl.files[0];
    if (!file) return;

    if (file.size / 1024 / 1024 > 2) {
      this.props.enqueueSnackbar && this.props.enqueueSnackbar("File upload is restricted to 2MB", { variant: "error" });
      inputEl.value = "";
      return;
    }
    const validTypes = ["image/jpeg", "image/jpg", "image/png", "application/pdf"];
    if (!validTypes.includes(file.type)) {
      this.props.enqueueSnackbar && this.props.enqueueSnackbar("Only JPG, PNG and PDF files are allowed", { variant: "error" });
      inputEl.value = "";
      return;
    }

    const formData = new FormData();
    formData.append("file", file, file.name);
    try {
      const response = await API.POST(apiEndpoints.masterFileUpload, formData);
      if (response.success) {
        const previewUrl = URL.createObjectURL(file);
        this.handleItemChange(index, "sampleImageFileId", response.data.fileUUId);
        this.handleItemChange(index, "sampleImagePreview", previewUrl);
        this.props.enqueueSnackbar && this.props.enqueueSnackbar("Sample image uploaded", { variant: "success" });
      } else {
        this.props.enqueueSnackbar && this.props.enqueueSnackbar(response.errorMessage || "Upload failed", { variant: "error" });
      }
    } catch {
      this.props.enqueueSnackbar && this.props.enqueueSnackbar("Upload failed", { variant: "error" });
    }
    if (inputEl) inputEl.value = "";
  };

  handleLineImageRemove = (index) => {
    const mergedItems = this.groupItemsByProductId();
    const target = mergedItems[index];
    if (!target) return;
    if (target.sampleImagePreview) URL.revokeObjectURL(target.sampleImagePreview);
    this.handleItemChange(index, "sampleImageFileId", null);
    this.handleItemChange(index, "sampleImagePreview", null);
  };

  handleItemChange = (index, field, value) => {
    const mergedItems = this.groupItemsByProductId();
    const targetItem = mergedItems[index];

    if (!targetItem) return;

    // Use billing qty when a billing unit is active, otherwise base qty from indent
    const totalGroupQuantity = targetItem.billingUnit
      ? parseFloat(targetItem.billingQuantity || 0)
      : parseFloat(targetItem.quantity || 0);

    const updatedItems = this.props.items.map(originalItem => {
      if (originalItem.productId === targetItem.productId) {
        const updatedItem = { ...originalItem, [field]: value };

        if (field === "rate" || field === "gst" || field === "quantity" || field === "discount") {
          const rate = parseFloat(updatedItem.rate || 0);
          const gst = parseFloat(updatedItem.gst || 0);
          const discount = parseFloat(updatedItem.discount || 0);
          const discountedRate = rate - (rate * discount / 100);

          const netRate = discountedRate * totalGroupQuantity;
          const totalAmt = netRate + (netRate * gst / 100);
          updatedItem.netRate = netRate.toFixed(2);
          updatedItem.totalAmt = totalAmt.toFixed(2);
        }

        return updatedItem;
      }
      return originalItem;
    });

    this.props.onItemsChange(updatedItems);
  };

  handleRemoveItem = (index) => () => {
    const mergedItems = this.groupItemsByProductId();
    const targetItem = mergedItems[index];

    if (!targetItem) return;

    // Remove all original items that belong to this product group
    const updatedItems = this.props.items.filter(originalItem =>
      originalItem.productId !== targetItem.productId
    );

    this.props.onItemsChange(updatedItems);
  };

  groupItemsByProductId = () => {
    const groupedItems = {};
    const mergedItems = [];

    // Group items by productId
    this.props.items.forEach((item) => {
      const productId = item.productId;

      if (!groupedItems[productId]) {
        groupedItems[productId] = {
          ...item,
          quantity: parseFloat(item.quantity || 0),
          originalItems: [item],
          projectNames: [item.projectName],
          remarks: [item.remarks || ""],
          indentNos: [item.indentNo || ""],
          lineItemCodes: [item.lineItemCode || ""],
          // Initialize other fields
          diameter: item.diameter || "",
          size: item.size || "",
          brandName: item.brandName || "",
          grade: item.grade || "",
          specification: item.specification || "",
          rate: item.rate || "",
          gst: item.gst || "",
          netRate: item.netRate || "",
          totalAmt: item.totalAmt || "",
          sampleImageFileId: item.sampleImageFileId || null,
          sampleImagePreview: item.sampleImagePreview || null,
        };
      } else {
        // Merge quantities and collect project information
        groupedItems[productId].quantity += parseFloat(item.quantity || 0);
        groupedItems[productId].originalItems.push(item);

        // Collect unique project names
        if (!groupedItems[productId].projectNames.includes(item.projectName)) {
          groupedItems[productId].projectNames.push(item.projectName);
        }

        // Collect remarks from different projects
        if (item.remarks && !groupedItems[productId].remarks.includes(item.remarks)) {
          groupedItems[productId].remarks.push(item.remarks);
        }

        // Collect indent numbers and line item codes
        if (item.indentNo && !groupedItems[productId].indentNos.includes(item.indentNo)) {
          groupedItems[productId].indentNos.push(item.indentNo);
        }

        if (item.lineItemCode && !groupedItems[productId].lineItemCodes.includes(item.lineItemCode)) {
          groupedItems[productId].lineItemCodes.push(item.lineItemCode);
        }
      }
    });

    // Convert grouped items to array and update quantity strings
    Object.keys(groupedItems).forEach((productId) => {
      const item = groupedItems[productId];
      item.quantity = item.quantity.toString();

      // Combine remarks from different projects
      item.combinedRemarks = item.remarks.filter(r => r.trim()).join("; ");

      // Store project information for display
      item.projectInfo = item.projectNames.join(", ");

      mergedItems.push(item);
    });

    return mergedItems;
  };

  calculateTotal = () => {
    const mergedItems = this.groupItemsByProductId();
    return mergedItems.reduce((sum, item) => {
      return sum + parseFloat(item.totalAmt || 0);
    }, 0);
  };

  handleAddSupplier = async (supplier) => {
    // Supplier is already created via API, just add it to the list and select it
    const newSupplier = {
      id: supplier.id,
      name: supplier.name || "",
      contactPerson: supplier.contactPerson || null,
      mobileNumber: supplier.mobileNumber || null,
      address: supplier.address || null,
    };
    
    // Check if supplier already exists in the list
    const existingIndex = this.state.orderToOptions.findIndex(
      (s) => s.id === newSupplier.id
    );
    
    if (existingIndex >= 0) {
      // Update existing supplier
      const updatedOptions = [...this.state.orderToOptions];
      updatedOptions[existingIndex] = newSupplier;
      this.setState({ orderToOptions: updatedOptions });
    } else {
      // Add new supplier to the list
      this.setState({
        orderToOptions: [...this.state.orderToOptions, newSupplier],
      });
    }
    
    // Select the newly created supplier
    this.props.onOrderToChange(newSupplier);
    this.handleCloseSupplierModal();
    
    // Refresh supplier list to ensure it's up to date
    this.fetchSupplierNames();
  };

handleAddFirm = async (firm) => {
    // Clear cache for this firm so fresh data is fetched
    this.setState((prevState) => {
      const updatedCache = { ...prevState.firmDetailsCache };
      delete updatedCache[firm.id];
      return { firmDetailsCache: updatedCache };
    });

    const newFirm = {
      id: firm.id,
      name: firm.name || "",
      address: firm.address || null,
    };
    
    // Fetch firm details to get all firm information
    const details = await this.fetchFirmDetails(newFirm.id);
    if (details) {
      newFirm.address = details.address || newFirm.address;
      newFirm.gst = details.gst || null;
      newFirm.pan = details.pan || null;
      newFirm.contactNumber = details.contactNumber || null;
      newFirm.description = details.description || null;
    }
    
    // Check if firm already exists in the list
    const existingIndex = this.state.orderFromOptions.findIndex(
      (f) => f.id === newFirm.id
    );
    
    if (existingIndex >= 0) {
      // Update existing firm
      const updatedOptions = [...this.state.orderFromOptions];
      updatedOptions[existingIndex] = newFirm;
      this.setState({ orderFromOptions: updatedOptions });
    } else {
      // Add new firm to the list
      this.setState({
        orderFromOptions: [...this.state.orderFromOptions, newFirm],
      });
    }
    
// Select the newly created firm
    this.handleCloseFirmModal();

    // Fetch fresh details and update the selected firm with full data
    const freshDetails = await this.fetchFirmDetails(newFirm.id);
    if (freshDetails) {
      newFirm.address = freshDetails.address;
      newFirm.addr_line1 = freshDetails.addr_line1;
      newFirm.addr_line2 = freshDetails.addr_line2;
      newFirm.city = freshDetails.city;
      newFirm.state = freshDetails.state;
      newFirm.zip = freshDetails.zip;
      newFirm.gst = freshDetails.gst;
      newFirm.pan = freshDetails.pan;
      newFirm.contactNumber = freshDetails.contactNumber;
      newFirm.description = freshDetails.description;
    }

    this.props.onOrderFromChange(newFirm);

    // Refresh firm list to ensure it's up to date
    this.fetchFirmList();
  };

  handleCloseSupplierModal = () => {
    this.setState({ showSupplierModal: false });
  };

  handleCloseFirmModal = () => {
    this.setState({ showFirmModal: false });
  };

  // ── PO Rate History Tooltip ──────────────────────────────────────────────
  handleProductHover = async (e, productId) => {
    this.setState({ tooltipAnchorEl: e.currentTarget, tooltipProductId: productId });
    if (this.state.ratesCache[productId] !== undefined) return; // already cached
    this.setState((prev) => ({ ratesLoading: { ...prev.ratesLoading, [productId]: true } }));
    try {
      const response = await API.GET(apiEndpoints.getPurchaseOrderPreviousRates(productId));
      const rates = response.success ? (response.data || []) : [];
      this.setState((prev) => ({
        ratesCache: { ...prev.ratesCache, [productId]: rates },
        ratesLoading: { ...prev.ratesLoading, [productId]: false },
      }));
    } catch {
      this.setState((prev) => ({
        ratesCache: { ...prev.ratesCache, [productId]: [] },
        ratesLoading: { ...prev.ratesLoading, [productId]: false },
      }));
    }
  };

  handleProductLeave = () => {
    this.setState({ tooltipAnchorEl: null, tooltipProductId: null });
  };

  renderRateTooltip() {
    const { tooltipAnchorEl, tooltipProductId, ratesCache, ratesLoading } = this.state;
    const open = Boolean(tooltipAnchorEl);
    if (!open || !tooltipProductId) return null;
    const rates = ratesCache[tooltipProductId];
    const loading = ratesLoading[tooltipProductId];

    return (
      <Popover
        open={open}
        anchorEl={tooltipAnchorEl}
        onClose={this.handleProductLeave}
        anchorOrigin={{ vertical: "bottom", horizontal: "left" }}
        transformOrigin={{ vertical: "top", horizontal: "left" }}
        disableRestoreFocus
        style={{ pointerEvents: "none" }}
        PaperProps={{ style: { pointerEvents: "none", borderRadius: 8, boxShadow: "0 4px 20px rgba(0,0,0,0.15)" } }}
      >
        <div className="po-rate-history-tooltip">
          <div className="po-rate-history-title">PO Date Rate Details</div>
          {loading ? (
            <div className="po-rate-history-loading">
              <CircularProgress size={16} />
            </div>
          ) : !rates || rates.length === 0 ? (
            <div className="po-rate-history-empty">No previous rates</div>
          ) : (
            <table className="po-rate-history-table">
              <thead>
                <tr>
                  <th className="po-rate-th">PO No</th>
                  <th className="po-rate-th">PO Date</th>
                  <th className="po-rate-th">Supplier</th>
                  <th className="po-rate-th text-right">Qty</th>
                  <th className="po-rate-th text-right">Rate</th>
                </tr>
              </thead>
              <tbody>
                {rates.slice(0, 5).map((r, i) => (
                  <tr key={i} className={i === 0 ? "latest-row" : ""}>
                    <td className="po-rate-pono">{r.purchaseOrderId ?? "-"}</td>
                    <td className="po-rate-date">{r.poDate}</td>
                    <td className="po-rate-supplier">{r.supplierName}</td>
                    <td className="po-rate-qty">{r.quantity ?? "-"}</td>
                    <td className={`po-rate-value ${i === 0 ? "latest-rate" : ""}`}>
                      Rs {r.rate}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </Popover>
    );
  }
  // ─────────────────────────────────────────────────────────────────────────

  // Helper function to format null/undefined/empty values as '-'
  formatValue = (value) => {
    if (value === null || value === undefined || value === "") {
      return "-";
    }
    return value;
  };

  render() {
    const mergedItems = this.groupItemsByProductId();
    const total = this.calculateTotal();

    return (
      <div className="step2-fill-details">
        <AddNewSupplierModal
          open={this.state.showSupplierModal}
          onClose={this.handleCloseSupplierModal}
          onSave={this.handleAddSupplier}
        />
        <AddNewFirmModal
          open={this.state.showFirmModal}
          onClose={this.handleCloseFirmModal}
          onSave={this.handleAddFirm}
          ref={(ref) => { this.firmModalRef = ref; }}
        />

        {/* Order To and Order From Sections - Side by Side */}
        {!this.props.addLinesMode && <div className="order-sections-container">
          {/* Order To Section */}
          <div className="form-section">
            <h3 className="section-title">Order To</h3>
            <div className="order-to-content">
              {this.state.isLoadingSuppliers ? (
                <div style={{ display: "flex", alignItems: "center", flex: 1, marginRight: "8px" }}>
                  <CircularProgress size={20} style={{ marginRight: "8px" }} />
                  <TextField
                    variant="outlined"
                    placeholder="Loading suppliers..."
                    size="small"
                    disabled
                    style={{ flex: 1 }}
                  />
                </div>
              ) : (
              <Autocomplete
                options={this.state.orderToOptions}
                getOptionLabel={(option) => this.formatValue(option?.name)}
                value={this.props.orderTo}
                onChange={this.handleOrderToChange}
                loading={this.state.isLoadingSuppliers}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    variant="outlined"
                    placeholder="Select Supplier"
                    size="small"
                  />
                )}
                style={{ flex: 1, marginRight: "8px" }}
              />
              )}
              <span className="or-separator">Or</span>
              <Button
                variant="outlined"
                color="primary"
                onClick={() => this.setState({ showSupplierModal: true })}
                style={{ marginLeft: "8px" }}
              >
                + Add New
              </Button>
            </div>
            {this.props.orderTo && (
              <div className="order-to-details">
                <div className="detail-row">
                  <span className="detail-label">Key Contact Person:</span>
                  <span className="detail-value">
                    {this.formatValue(this.props.orderTo.contactPerson)}
                  </span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Mobile Number:</span>
                  <span className="detail-value">
                    {this.formatValue(this.props.orderTo.mobileNumber)}
                  </span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Supplier Address:</span>
                  <span className="detail-value">
                    {this.formatValue(this.props.orderTo.address)}
                  </span>
                </div>
              </div>
            )}
          </div>

          {/* Order From Section */}
          <div className="form-section">
            <h3 className="section-title">Order From</h3>
            {this.state.isLoadingFirms ? (
              <div style={{ display: "flex", alignItems: "center", width: "100%" }}>
                <CircularProgress size={20} style={{ marginRight: "8px" }} />
                <TextField
                  variant="outlined"
                  placeholder="Loading firms..."
                  size="small"
                  disabled
                  style={{ flex: 1 }}
                />
              </div>
            ) : (
              <div className="order-to-content">
                <Autocomplete
                  options={this.state.orderFromOptions}
                  getOptionLabel={(option) => this.formatValue(option?.name)}
                  value={this.props.orderFrom}
                  onChange={this.handleOrderFromChange}
                  loading={this.state.isLoadingFirms}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      variant="outlined"
                      placeholder="Select Firm"
                      size="small"
                    />
                  )}
                  style={{ flex: 1, marginRight: "8px" }}
                />
                <span className="or-separator">Or</span>
                <Button
                  variant="outlined"
                  color="primary"
                  onClick={() => this.setState({ showFirmModal: true })}
                  style={{ marginLeft: "8px" }}
                >
                  + Add New
                </Button>
              </div>
            )}
            {this.props.orderFrom && (
              <div className="order-from-details">
                <div className="detail-row" style={{ justifyContent: "flex-end", paddingBottom: "4px" }}>
                  <Button
                    size="small"
                    variant="outlined"
                    color="primary"
                    style={{ fontSize: "11px", padding: "2px 8px" }}
                    onClick={() => {
                      this.setState({ showFirmModal: true }, () => {
                        if (this.firmModalRef && this.props.orderFrom?.id) {
                          this.firmModalRef.loadFirmForEdit(this.props.orderFrom.id);
                        }
                      });
                    }}
                  >
                    ✏️ Edit Firm
                  </Button>
                </div>
                <div className="detail-row">
                                  <span className="detail-label">Firm Address:</span>
                                  <span className="detail-value">
                                    {[
                                      this.props.orderFrom.addr_line1,
                                      this.props.orderFrom.addr_line2,
                                      this.props.orderFrom.city,
                                      this.props.orderFrom.state,
                                      this.props.orderFrom.zip,
                                    ].filter(Boolean).join(", ") || this.formatValue(this.props.orderFrom.address)}
                                  </span>
                                </div>
                <div className="detail-row">
                  <span className="detail-label">GST:</span>
                  <span className="detail-value">
                    {this.formatValue(this.props.orderFrom.gst)}
                  </span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">PAN:</span>
                  <span className="detail-value">
                    {this.formatValue(this.props.orderFrom.pan)}
                  </span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Contact Number:</span>
                  <span className="detail-value">
                    {this.formatValue(this.props.orderFrom.contactNumber)}
                  </span>
                </div>
                <div className="detail-row">
                  <span className="detail-label">Description:</span>
                  <span className="detail-value">
                    {this.formatValue(this.props.orderFrom.description)}
                  </span>
                </div>
                <div className="detail-row">
                  <FormControlLabel
                    control={
                      <Checkbox
                        checked={this.props.showOverridePhoneEmail || false}
                        onChange={(e) => this.props.onShowOverridePhoneEmailChange?.(e.target.checked)}
                        color="primary"
                        size="small"
                      />
                    }
                    label={<span className="detail-label" style={{ fontWeight: "normal" }}>Override Phone/Email</span>}
                  />
                </div>
                {this.props.showOverridePhoneEmail && (
                  <>
                    <div className="detail-row" style={{ alignItems: "flex-start", flexDirection: "column", gap: "4px" }}>
                      <span className="detail-label">Override Phone Number</span>
                      <TextField
                        variant="outlined"
                        fullWidth
                        placeholder="e.g. 9876543210, nani-8600033031"
                        value={this.props.overridePhoneNumber || ""}
                        onChange={(e) => this.props.onOverridePhoneNumberChange?.(e.target.value)}
                        size="small"
                        inputProps={{ maxLength: 35 }}
                        helperText={`${(this.props.overridePhoneNumber || "").length}/35 — overrides firm's phone on PO`}
                      />
                    </div>
                    <div className="detail-row" style={{ alignItems: "flex-start", flexDirection: "column", gap: "4px" }}>
                      <span className="detail-label">Override Email</span>
                      <TextField
                        variant="outlined"
                        fullWidth
                        placeholder="e.g. contact@company.com"
                        value={this.props.overrideEmail || ""}
                        onChange={(e) => this.props.onOverrideEmailChange?.(e.target.value)}
                        size="small"
                        inputProps={{ maxLength: 100 }}
                        helperText="If filled, overrides firm's email on PO"
                      />
                    </div>
                  </>
                )}
              </div>
            )}
          </div>
        </div>}

        {/* Purchase Order Subject */}
        {/* Purchase Order Subject + SPL PO inline */}
        {!this.props.addLinesMode && <div className="form-section">
          <div style={{ display: "flex", alignItems: "center", gap: "16px" }}>
            <div style={{ flex: 1 }}>
              <h3 className="section-title">Purchase order Subject</h3>
              <TextField
                variant="outlined"
                fullWidth
                placeholder="Enter subject"
                value={this.props.poSubject}
                onChange={(e) => this.props.onPoSubjectChange(e.target.value)}
                size="small"
              />
            </div>
            <div style={{ paddingTop: "28px", flexShrink: 0 }}>
              <FormControlLabel
                control={
                  <Checkbox
                    checked={this.props.isSpecialPo || false}
                    onChange={(e) => this.props.onIsSpecialPoChange(e.target.checked)}
                    color="primary"
                  />
                }
                label="SPL PO"
              />
            </div>
          </div>
          {/* PO Date — admin only */}
          {this.props.isAdmin && (
            <div style={{ marginTop: "12px" }}>
              <label style={{ fontSize: "13px", color: "#555", marginBottom: "2px", display: "block" }}>PO Date</label>
              <DatePicker
                label="PO Date"
                maxDate={moment()}
                defaultValue={this.props.poDate}
                onChange={(date) => this.props.onPoDateChange && this.props.onPoDateChange(date)}
              />
            </div>
          )}
          {/* Project (required) */}
          <div style={{ marginTop: "12px" }}>
            <Autocomplete
              options={this.state.projectList}
              value={this.props.projectName || null}
              onChange={(e, v) => this.props.onProjectNameChange?.(v || "")}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Project *"
                  size="small"
                  variant="outlined"
                  placeholder="Select project"
                />
              )}
            />
          </div>
        </div>}
        {/* Purchase Order Items Table */}
        <div className="form-section items-section">
          <h3 className="section-title">Purchase order Items</h3>
          <div className="items-table-wrapper">
            <div className="table-scroll-container">
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Inventory</TableCell>
                    <TableCell>Diameter</TableCell>
                    <TableCell>Size</TableCell>
                    <TableCell>Brand Name</TableCell>
                    <TableCell>Grade</TableCell>
                    <TableCell>Specification</TableCell>
                    <TableCell>Qty / Unit</TableCell>
                    <TableCell>Tolerance %</TableCell>
                    <TableCell>Rate *</TableCell>
                    <TableCell>Discount %</TableCell>
                    <TableCell>GST</TableCell>
                    <TableCell>Net Rate</TableCell>
                    <TableCell>Total Amt.</TableCell>
                    <TableCell>Sample Image</TableCell>
                    {!this.props.isEditMode && <TableCell>Action</TableCell>}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {this.renderRateTooltip()}
                  {mergedItems.map((item, index) => (
                    <TableRow key={item.productId || index}>
                      <TableCell
                        className="inventory-name-cell"
                        onMouseEnter={(e) => this.handleProductHover(e, item.productId)}
                        onMouseLeave={this.handleProductLeave}
                        style={{ cursor: "default" }}
                      >
                        <span className="inventory-name-hoverable">
                          <span className="rate-history-trigger">
                            {this.formatValue(item.inventoryName)} ({this.formatValue(item.unit)})
                          </span>
                        </span>
                        {item.leadTimeDays != null && (
                          <div className="lead-time-chip" style={{ marginTop: '4px' }}>
                            <span className="lead-time-chip-icon">⏱</span>
                            Lead Time: <strong>{item.leadTimeDays} days</strong>
                          </div>
                        )}
                        {item.projectInfo && (
                          <div style={{ fontSize: '11px', color: '#666', marginTop: '2px' }}>
                            Projects: {item.projectInfo}
                          </div>
                        )}
                      </TableCell>
                      <TableCell>
                        <TextField
                          value={item.diameter || ""}
                          onChange={(e) =>
                            this.handleItemChange(index, "diameter", e.target.value)
                          }
                          size="small"
                          variant="outlined"
                          inputProps={{ style: { fontSize: "12px", padding: "8px" } }}
                        />
                      </TableCell>
                      <TableCell>
                        <TextField
                          value={item.size || ""}
                          onChange={(e) =>
                            this.handleItemChange(index, "size", e.target.value)
                          }
                          size="small"
                          variant="outlined"
                          inputProps={{ style: { fontSize: "12px", padding: "8px" } }}
                        />
                      </TableCell>
                      <TableCell>
                        <TextField
                          value={item.brandName || ""}
                          onChange={(e) =>
                            this.handleItemChange(index, "brandName", e.target.value)
                          }
                          size="small"
                          variant="outlined"
                          inputProps={{ style: { fontSize: "12px", padding: "8px" } }}
                        />
                      </TableCell>
                      <TableCell>
                        <TextField
                          value={item.grade || ""}
                          onChange={(e) =>
                            this.handleItemChange(index, "grade", e.target.value)
                          }
                          size="small"
                          variant="outlined"
                          inputProps={{ style: { fontSize: "12px", padding: "8px" } }}
                        />
                      </TableCell>
                      <TableCell className="specification-cell">
                        <TextField
                          value={item.specification || ""}
                          onChange={(e) =>
                            this.handleItemChange(
                              index,
                              "specification",
                              e.target.value
                            )
                          }
                          size="small"
                          variant="outlined"
                          inputProps={{ style: { fontSize: "12px", padding: "8px" } }}
                        />
                      </TableCell>
                      <TableCell style={{ minWidth: 130 }}>
                        {(() => {
                          const conversions = this.state.unitConversionsCache[item.productId] || [];
                          const baseUnit = item.unit || "";
                          const selectedBillingUnit = item.billingUnit || baseUnit;
                          const hasAlternateUnits = conversions.length > 0;

                          return (
                            <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
                              {/* Unit selector — only when alternate units configured */}
                              {hasAlternateUnits ? (
                                <select
                                  value={selectedBillingUnit}
                                  onChange={(e) =>
                                    this.handleBillingUnitChange(index, e.target.value, conversions, baseUnit)
                                  }
                                  style={{
                                    fontSize: 12,
                                    padding: "5px 6px",
                                    border: "1px solid #ccc",
                                    borderRadius: 4,
                                    width: "100%",
                                    height: 34,
                                    background: "#fff",
                                  }}
                                >
                                  <option value={baseUnit}>{baseUnit}</option>
                                  {conversions.map((c) => (
                                    <option key={c.id} value={c.unitName}>{c.unitName}</option>
                                  ))}
                                </select>
                              ) : (
                                <span style={{ fontSize: 11, color: "#666", paddingBottom: 2 }}>{baseUnit}</span>
                              )}
                              {/* Quantity — billing qty when unit selected, base qty otherwise */}
                              <TextField
                                value={item.billingUnit ? (item.billingQuantity || "") : (item.quantity || "")}
                                size="small"
                                variant="outlined"
                                type="number"
                                disabled
                                inputProps={{ style: { fontSize: "12px", padding: "6px 8px" } }}
                              />
                              {/* Base qty reference when billing unit is active */}
                              {item.billingUnit && (
                                <div style={{ fontSize: 10, color: "#888", lineHeight: 1.2 }}>
                                  = {item.quantity || 0} {baseUnit}
                                </div>
                              )}
                            </div>
                          );
                        })()}
                      </TableCell>
                      <TableCell>
                        <TextField
                          type="number"
                          size="small"
                          variant="outlined"
                          placeholder="0"
                          value={item.tolerance || ""}
                          inputProps={{ min: 0, max: 100, step: 0.01 }}
                          onChange={(e) => this.handleItemChange(index, "tolerance", e.target.value)}
                          style={{ width: "70px" }}
                        />
                      </TableCell>
                      <TableCell className="rate-cell">
                        {item.willBeClubbed ? (
                          <div style={{ fontSize: "12px" }}>
                            <div>Rs. {item.rate}</div>
                            <div style={{ color: "#1976d2", fontStyle: "italic", fontSize: "11px", marginTop: "2px" }}>
                              Clubbed
                            </div>
                          </div>
                        ) : (
                          <TextField
                            value={item.rate || ""}
                            onChange={(e) =>
                              this.handleItemChange(index, "rate", e.target.value)
                            }
                            size="small"
                            variant="outlined"
                            type="number"
                            required
                            inputProps={{ style: { fontSize: "12px", padding: "8px" } }}
                          />
                        )}
                      </TableCell>
                      <TableCell>
                        {item.willBeClubbed ? (
                          <span style={{ fontSize: "12px" }}>{item.discount || "0"}%</span>
                        ) : (
                          <TextField
                            type="number"
                            size="small"
                            variant="outlined"
                            placeholder="0"
                            value={item.discount || ""}
                            inputProps={{ min: 0, max: 100, step: 0.01 }}
                            onChange={(e) => this.handleItemChange(index, "discount", e.target.value)}
                            style={{ width: "80px" }}
                          />
                        )}
                      </TableCell>
                      <TableCell>
                        {item.willBeClubbed ? (
                          <span style={{ fontSize: "12px" }}>{item.gst || "0"}%</span>
                        ) : (
                          <TextField
                            value={item.gst ?? ""}
                            onChange={(e) =>
                              this.handleItemChange(index, "gst", e.target.value)
                            }
                            size="small"
                            variant="outlined"
                            type="number"
                            placeholder="%"
                            inputProps={{ style: { fontSize: "12px", padding: "8px" } }}
                          />
                        )}
                      </TableCell>
                      <TableCell className="net-rate-cell calculated-cell">
                        {item.netRate ? `Rs. ${parseFloat(item.netRate).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` : "-"}
                      </TableCell>
                      <TableCell className="total-amt-cell calculated-cell">
                        {item.totalAmt ? `Rs. ${parseFloat(item.totalAmt).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` : "-"}
                      </TableCell>
                      <TableCell style={{ minWidth: "90px", textAlign: "center" }}>
                        {item.sampleImageFileId ? (
                          <div style={{ position: "relative", display: "inline-block" }}>
                            <img
                              src={item.sampleImagePreview || `${apiEndpoints.masterFileDownload}${item.sampleImageFileId}`}
                              alt="sample"
                              style={{ width: 64, height: 64, objectFit: "contain", border: "1px solid #ddd", borderRadius: 4 }}
                            />
                            <span
                              onClick={() => this.handleLineImageRemove(index)}
                              style={{ position: "absolute", top: -6, right: -6, background: "#f44336", color: "#fff", borderRadius: "50%", width: 16, height: 16, fontSize: 11, cursor: "pointer", display: "flex", alignItems: "center", justifyContent: "center", lineHeight: 1 }}
                            >×</span>
                          </div>
                        ) : (
                          <>
                            <input
                              type="file"
                              accept="image/jpeg,image/jpg,image/png,application/pdf"
                              style={{ display: "none" }}
                              ref={(el) => { this.lineFileInputRefs[index] = el; }}
                              onChange={(e) => this.handleLineImageUpload(index, e)}
                            />
                            <button
                              type="button"
                              onClick={() => this.lineFileInputRefs[index] && this.lineFileInputRefs[index].click()}
                              style={{ fontSize: 11, padding: "3px 8px", cursor: "pointer", border: "1px solid #1976d2", borderRadius: 4, background: "#fff", color: "#1976d2" }}
                            >+ Image</button>
                          </>
                        )}
                      </TableCell>
                      {!this.props.isEditMode && (
                        <TableCell>
                          <IconButton
                            size="small"
                            className="delete-item-button"
                            onClick={this.handleRemoveItem(index)}
                            disableRipple
                          >
                            <img src={trashOutlineIcon} alt="Delete" style={{ width: 15, height: 15 }} />
                          </IconButton>
                        </TableCell>
                      )}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
            <div className="table-total">
              <strong>Total: Rs. {total.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</strong>
            </div>
          </div>
        </div>



        {/* Freight Charges Section */}
        {!this.props.addLinesMode && <div className="form-section">
          <h3 className="section-title">Freight Charges</h3>
          <div style={{ display: "flex", gap: "16px", alignItems: "flex-start", flexWrap: "wrap" }}>
            <div style={{ flex: "1", minWidth: "160px" }}>
              <div style={{ fontSize: "12px", marginBottom: "4px", color: "#555" }}>Freight Charges (₹)</div>
              <TextField
                type="number"
                variant="outlined"
                size="small"
                placeholder="0.00"
                value={this.props.freightCharges || ""}
                onChange={(e) => this.props.onFreightChargesChange(e.target.value)}
                inputProps={{ min: 0, step: 0.01, style: { fontSize: "12px", padding: "8px" } }}
                style={{ width: "100%" }}
              />
            </div>
            <div style={{ flex: "1", minWidth: "120px" }}>
              <div style={{ fontSize: "12px", marginBottom: "4px", color: "#555" }}>GST %</div>
              <TextField
                type="number"
                variant="outlined"
                size="small"
                placeholder="18"
                value={this.props.freightGstPercent !== "" && this.props.freightGstPercent !== undefined ? this.props.freightGstPercent : 18}
                onChange={(e) => this.props.onFreightGstPercentChange(e.target.value)}
                inputProps={{ min: 0, max: 100, step: 0.01, style: { fontSize: "12px", padding: "8px" } }}
                style={{ width: "100%" }}
              />
            </div>
            <div style={{ flex: "1", minWidth: "180px" }}>
              <div style={{ fontSize: "12px", marginBottom: "4px", color: "#555" }}>Total Freight Charges (₹)</div>
              <div style={{
                border: "1px solid #ccc",
                borderRadius: "4px",
                padding: "8px 10px",
                fontSize: "12px",
                background: "#f5f5f5",
                minHeight: "36px",
                display: "flex",
                alignItems: "center",
                fontWeight: "600",
              }}>
                {(() => {
                  const fc = parseFloat(this.props.freightCharges || 0);
                  const gst = parseFloat(this.props.freightGstPercent !== "" && this.props.freightGstPercent !== undefined ? this.props.freightGstPercent : 18);
                  const total = fc + fc * gst / 100;
                  return fc > 0
                    ? `Rs. ${total.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
                    : "-";
                })()}
              </div>
            </div>
          </div>
        </div>}

        {/* Custom / Additional Charges Section */}
        {!this.props.addLinesMode && <div className="form-section">
          <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", marginBottom: "8px" }}>
            <h3 className="section-title" style={{ margin: 0 }}>Additional Charges</h3>
            <button
              type="button"
              onClick={() => {
                const updated = [...(this.props.customCharges || []), { chargeName: "", chargeAmount: "", chargeGstPercent: 18 }];
                this.props.onCustomChargesChange(updated);
              }}
              style={{ fontSize: "12px", padding: "4px 12px", cursor: "pointer", border: "1px solid #1976d2", borderRadius: "4px", background: "#fff", color: "#1976d2", fontWeight: 600 }}
            >
              + Add Charge
            </button>
          </div>
          {(this.props.customCharges || []).length === 0 && (
            <div style={{ fontSize: "12px", color: "#999", padding: "4px 0" }}>No additional charges added.</div>
          )}
          {(this.props.customCharges || []).map((charge, idx) => {
            const amt = parseFloat(charge.chargeAmount || 0);
            const gst = parseFloat(charge.chargeGstPercent !== "" && charge.chargeGstPercent !== undefined ? charge.chargeGstPercent : 18);
            const total = amt + amt * gst / 100;
            const updateCharge = (field, value) => {
              const updated = (this.props.customCharges || []).map((c, i) => i === idx ? { ...c, [field]: value } : c);
              this.props.onCustomChargesChange(updated);
            };
            return (
              <div key={idx} style={{ marginBottom: "10px", background: "#fafafa", borderRadius: "4px", border: "1px solid #e0e0e0", overflow: "hidden" }}>
                {/* Remove button row */}
                <div style={{ display: "flex", justifyContent: "flex-end", borderBottom: "1px solid #f0f0f0", padding: "4px 8px" }}>
                  <button
                    type="button"
                    onClick={() => {
                      const updated = (this.props.customCharges || []).filter((_, i) => i !== idx);
                      this.props.onCustomChargesChange(updated);
                    }}
                    style={{ fontSize: "11px", padding: "2px 8px", cursor: "pointer", border: "1px solid #d32f2f", borderRadius: "4px", background: "#fff", color: "#d32f2f", fontWeight: 700 }}
                    title="Remove"
                  >
                    ✕ Remove
                  </button>
                </div>
                {/* Fields row */}
                <div style={{ display: "flex", gap: "12px", alignItems: "flex-start", flexWrap: "wrap", padding: "10px" }}>
                  <div style={{ flex: "2", minWidth: "180px" }}>
                    <div style={{ fontSize: "12px", marginBottom: "4px", color: "#555" }}>Charge Name</div>
                    <input
                      type="text"
                      placeholder="e.g. Insurance, Porter Charges"
                      value={charge.chargeName || ""}
                      onChange={(e) => updateCharge("chargeName", e.target.value)}
                      style={{ width: "100%", fontSize: "12px", padding: "7px 8px", border: "1px solid #ccc", borderRadius: "4px", boxSizing: "border-box" }}
                    />
                  </div>
                  <div style={{ flex: "1", minWidth: "140px" }}>
                    <div style={{ fontSize: "12px", marginBottom: "4px", color: "#555" }}>Amount (₹)</div>
                    <input
                      type="number"
                      placeholder="0.00"
                      min="0"
                      step="0.01"
                      value={charge.chargeAmount || ""}
                      onChange={(e) => updateCharge("chargeAmount", e.target.value)}
                      style={{ width: "100%", fontSize: "12px", padding: "7px 8px", border: "1px solid #ccc", borderRadius: "4px", boxSizing: "border-box" }}
                    />
                  </div>
                  <div style={{ flex: "1", minWidth: "100px" }}>
                    <div style={{ fontSize: "12px", marginBottom: "4px", color: "#555" }}>GST %</div>
                    <input
                      type="number"
                      placeholder="18"
                      min="0"
                      max="100"
                      step="0.01"
                      value={charge.chargeGstPercent !== "" && charge.chargeGstPercent !== undefined ? charge.chargeGstPercent : 18}
                      onChange={(e) => updateCharge("chargeGstPercent", e.target.value)}
                      style={{ width: "100%", fontSize: "12px", padding: "7px 8px", border: "1px solid #ccc", borderRadius: "4px", boxSizing: "border-box" }}
                    />
                  </div>
                  <div style={{ flex: "1", minWidth: "140px" }}>
                    <div style={{ fontSize: "12px", marginBottom: "4px", color: "#555" }}>Total (₹)</div>
                    <div style={{ border: "1px solid #ccc", borderRadius: "4px", padding: "7px 8px", fontSize: "12px", background: "#f5f5f5", minHeight: "34px", display: "flex", alignItems: "center", fontWeight: "600" }}>
                      {amt > 0 ? `Rs. ${total.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` : "-"}
                    </div>
                  </div>
                </div>
              </div>
            );
          })}
        </div>}

        {/* Notes Section */}
        {!this.props.addLinesMode && (this.props.fileArea ? (
          <div className="form-section">
            {this.props.fileArea}
          </div>
        ) : null)}
        {!this.props.addLinesMode && <div className="form-section">
          <h3 className="section-title">Notes</h3>
          <ReactQuill
            value={this.props.noteText}
            onChange={(value) => this.props.onNoteTextChange(value)}
            modules={{
              toolbar: [
                ['bold', 'italic', 'underline'],
                [{ list: 'ordered' }, { list: 'bullet' }],
                ['clean'],
              ],
            }}
            formats={['bold', 'italic', 'underline', 'list', 'bullet']}
            placeholder="Enter notes..."
            style={{ background: '#fff' }}
          />
        </div>}
      </div>
    );
  }
}

export default Step2FillDetails;
