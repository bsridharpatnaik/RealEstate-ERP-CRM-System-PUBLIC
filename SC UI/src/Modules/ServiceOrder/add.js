import React from "react";
import AddForm from "./../../Shared/AddForm";
import Autocomplete from "@material-ui/lab/Autocomplete";
import TextField from "@material-ui/core/TextField";
import IconButton from "@material-ui/core/IconButton";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import EditIcon from "@material-ui/icons/Edit";
import AddIcon from "@material-ui/icons/Add";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import Checkbox from "@material-ui/core/Checkbox";
import trashRedIcon from "./../../Shared/Icons/trash-red.png";
import Button from "./../../Shared/Button";
import DatePicker from "./../../Shared/Date";
import ReactQuill from "react-quill";
import "react-quill/dist/quill.snow.css";
import moment from "moment";
import { withSnackbar } from "notistack";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import { messages, constants } from "./../../messages";
import CircularProgress from "@material-ui/core/CircularProgress";
import MuiButton from "@material-ui/core/Button";
import "./style.scss";

const SERVICE_TYPE_OPTIONS = ["ROUTINE", "PREVENTIVE", "BREAKDOWN", "EMERGENCY", "AMC"];

// Two-column grid for the full-width page (the "item-dialog-row two-col" class is tuned for
// the narrower Dialog content box — reusing it at full page width gave uneven column splits).
const twoColGrid = { display: "grid", gridTemplateColumns: "1fr 1fr", gap: 16, marginTop: 8 };

// Inline styles for the vendor/firm contact-detail boxes — written standalone rather than via the
// "order-to-details"/"detail-row" classNames, because those rules in PurchaseOrder's stepStyles.scss
// are nested under ".step2-fill-details" and never match outside that component's DOM.
const detailBoxStyle = { marginTop: 16, padding: 16, backgroundColor: "#f9f9f9", borderRadius: 4 };
const detailRowStyle = { display: "flex", marginBottom: 8, fontSize: 13 };
const detailLabelStyle = { fontWeight: 600, minWidth: 150, color: "#323c47" };
const detailValueStyle = { color: "#666" };

class Add extends AddForm {
  state = {
    serviceDate: moment().format(constants.dateFormat),
    vendor: null,
    firm: null,
    subject: "",
    notes: "",
    projectName: "",
    specialDiscount: "",
    overridePhoneNumber: "",
    overrideEmail: "",
    showOverridePhoneEmail: false,
    lines: [],
    vendorOptions: [],
    firmOptions: [],
    projectOptions: [],
    descriptionOptions: [],
    customFieldLabelOptions: [],
    vendorDetails: null,
    firmDetails: null,
    isSaving: false,
    isEditMode: false,
    soId: null,
    // item dialog
    itemDialogOpen: false,
    dialogItem: {},
    dialogEditingKey: null,
    dialogValidation: false,
    descDropdownOpen: false,
    customFieldDropdownOpen: {},
    // file upload
    fileInformations: [],
    selectedFileName: "",
    selectedFilePreview: null,
    isFileUploading: false,
  };

  fileInputRef = React.createRef();

  componentDidMount() {
    this.fetchVendors();
    this.fetchFirms();
    this.fetchProjects();
    this.fetchDescriptionOptions();
    this.fetchCustomFieldLabelOptions();
    if (this.props.editData) {
      this.loadEditData(this.props.editData);
    }
  }

  componentWillUnmount() {
    if (this.state.selectedFilePreview) URL.revokeObjectURL(this.state.selectedFilePreview);
    (this.state.fileInformations || []).forEach((f) => f.previewUrl && URL.revokeObjectURL(f.previewUrl));
  }

  componentDidUpdate(prevProps, prevState) {
    if (this.state.vendor?.id && this.state.vendor.id !== prevState.vendor?.id) {
      this.fetchVendorDetails(this.state.vendor.id);
    }
    if (!this.state.vendor && prevState.vendor) {
      this.setState({ vendorDetails: null });
    }
    if (this.state.firm?.id && this.state.firm.id !== prevState.firm?.id) {
      this.fetchFirmDetails(this.state.firm.id);
    }
    if (!this.state.firm && prevState.firm) {
      this.setState({ firmDetails: null });
    }
  }

  fetchVendorDetails = async (vendorId) => {
    this.setState({ vendorDetails: null });
    const response = await API.GET(apiEndpoints.getContactDetail + vendorId);
    if (response.success && response.data) {
      const c = response.data;
      const addressParts = [c.addr_line1, c.addr_line2, c.city, c.state, c.zip].filter(Boolean);
      this.setState({
        vendorDetails: {
          contactPerson: c.contactPerson || null,
          mobileNumber: c.contactPersonMobileNo || c.mobileNo || null,
          address: addressParts.length > 0 ? addressParts.join(", ") : null,
        },
      });
    }
  };

  fetchFirmDetails = async (firmId) => {
    this.setState({ firmDetails: null });
    const response = await API.GET(apiEndpoints.getFirmDetail + firmId);
    if (response.success && response.data) {
      const f = response.data;
      const addressParts = [f.addr_line1, f.addr_line2, f.city, f.state, f.zip].filter(Boolean);
      this.setState({
        firmDetails: {
          address: addressParts.length > 0 ? addressParts.join(", ") : (f.firmAddress || null),
          gst: f.firmGstNumber || null,
          pan: f.firmPanNumber || null,
          contactNumber: f.firmContactNumber || null,
        },
      });
    }
  };

  loadEditData = (data) => {
    this.setState({
      isEditMode: true,
      soId: data.serviceOrderId,
      serviceDate: data.serviceDate || moment().format(constants.dateFormat),
      vendor: data.vendor ? { id: data.vendor.contactId, name: data.vendor.name } : null,
      firm: data.firm ? { id: data.firm.firmId, name: data.firm.firmName } : null,
      subject: data.subject || "",
      notes: data.notes || "",
      projectName: data.projectName || "",
      specialDiscount: data.specialDiscount != null ? String(data.specialDiscount) : "",
      overridePhoneNumber: data.overridePhoneNumber || "",
      overrideEmail: data.overrideEmail || "",
      showOverridePhoneEmail: !!(data.overridePhoneNumber || data.overrideEmail),
      lines: (data.lines || []).map((l, i) => ({
        key: i,
        lineId: l.id,
        description: l.description || "",
        quantity: l.quantity != null ? String(l.quantity) : "",
        rate: l.rate != null ? String(l.rate) : "",
        discount: l.discountPercent != null ? String(l.discountPercent) : "",
        gst: l.gstPercent != null ? String(l.gstPercent) : "",
        serviceType: l.serviceType || "",
        assetTag: l.assetTag || "",
        warrantyTill: l.warrantyTill || null,
        customFields: (l.customFields || []).map((cf) => ({ label: cf.fieldLabel || "", value: cf.fieldValue || "" })),
      })),
      fileInformations: Array.from(data.fileInformations || []),
    });
    this.nextKey = (data.lines || []).length;
  };

  // ── File upload ──────────────────────────────────────────────────────────

  handleFileSelect = (e) => {
    const file = e.target.files[0];
    if (!file) {
      this.setState({ selectedFileName: "", selectedFilePreview: null });
      return;
    }
    const fileSize = file.size / 1024 / 1024;
    if (fileSize > 2) {
      this.props.enqueueSnackbar("File upload is restricted to 2MB", { variant: "error" });
      this.setState({ selectedFileName: "", selectedFilePreview: null });
      if (this.fileInputRef.current) this.fileInputRef.current.value = "";
      return;
    }
    const validTypes = ["image/jpeg", "image/jpg", "image/png", "application/pdf"];
    if (!validTypes.includes(file.type)) {
      this.props.enqueueSnackbar("Only JPG, PNG and PDF files are allowed", { variant: "error" });
      this.setState({ selectedFileName: "", selectedFilePreview: null });
      if (this.fileInputRef.current) this.fileInputRef.current.value = "";
      return;
    }
    const previewUrl = URL.createObjectURL(file);
    this.setState({ selectedFileName: file.name, selectedFilePreview: previewUrl });
  };

  handleFileSubmit = async () => {
    if (!this.fileInputRef.current || !this.fileInputRef.current.files[0]) {
      this.props.enqueueSnackbar("Please select a file first", { variant: "error" });
      return;
    }
    const file = this.fileInputRef.current.files[0];
    const formData = new FormData();
    formData.append("file", file, file.name);
    this.setState({ isFileUploading: true });
    try {
      const response = await API.POST(apiEndpoints.masterFileUpload, formData);
      if (response.success) {
        const fileData = { ...response.data, previewUrl: this.state.selectedFilePreview };
        this.setState((prev) => ({
          fileInformations: [...prev.fileInformations, fileData],
          selectedFileName: "",
          selectedFilePreview: null,
          isFileUploading: false,
        }));
        this.props.enqueueSnackbar("File uploaded successfully", { variant: "success" });
        if (this.fileInputRef.current) this.fileInputRef.current.value = "";
      } else {
        if (this.state.selectedFilePreview) URL.revokeObjectURL(this.state.selectedFilePreview);
        this.setState({ isFileUploading: false });
        this.props.enqueueSnackbar(response.errorMessage || "Upload failed", { variant: "error" });
      }
    } catch (error) {
      if (this.state.selectedFilePreview) URL.revokeObjectURL(this.state.selectedFilePreview);
      this.setState({ isFileUploading: false });
      this.props.enqueueSnackbar("Upload failed", { variant: "error" });
    }
  };

  handleFileRemove = (fileToRemove) => {
    if (fileToRemove.previewUrl) URL.revokeObjectURL(fileToRemove.previewUrl);
    this.setState((prev) => ({
      fileInformations: prev.fileInformations.filter((f) => f.fileUUId !== fileToRemove.fileUUId),
    }));
  };

  nextKey = 0;

  fetchVendors = async () => {
    const response = await API.GET(apiEndpoints.getSupplierNames);
    if (response.success && Array.isArray(response.data)) {
      this.setState({ vendorOptions: response.data.map((v) => ({ id: v.id, name: (v.name || "").trim() })) });
    }
  };

  fetchFirms = async () => {
    const response = await API.GET(apiEndpoints.getFirmIdAndNames);
    if (response.success && response.data) {
      let firms = Array.isArray(response.data) ? response.data : (response.data.content || []);
      this.setState({ firmOptions: firms.map((f) => ({ id: f.id || f.firmId, name: (f.name || f.firmName || "").trim() })) });
    }
  };

  fetchProjects = async () => {
    const response = await API.GET(apiEndpoints.getTenants);
    if (response.success && Array.isArray(response.data)) {
      const names = response.data
        .filter((t) => t.inventory === true)
        .map((t) => t.name || t.tenantName || "")
        .filter(Boolean);
      this.setState({ projectOptions: names });
    }
  };

  fetchDescriptionOptions = async () => {
    const response = await API.GET(apiEndpoints.getServiceOrderLineDescriptions);
    if (response.success && Array.isArray(response.data)) {
      this.setState({ descriptionOptions: response.data });
    }
  };

  fetchCustomFieldLabelOptions = async () => {
    const response = await API.GET(apiEndpoints.getServiceOrderCustomFieldLabels);
    if (response.success && Array.isArray(response.data)) {
      this.setState({ customFieldLabelOptions: response.data });
    }
  };

  renderFileArea() {
    const files = this.state.fileInformations || [];
    return (
      <div className="indent-upload-section">
        <div className="upload-documents-heading">Upload Documents</div>
        <div className="upload-controls">
          <div className="upload-left">
            <div className="upload-buttons-row">
              <input
                ref={this.fileInputRef}
                type="file"
                id="so-file-upload"
                accept="image/jpeg,image/jpg,image/png,application/pdf"
                onChange={this.handleFileSelect}
                style={{ display: "none" }}
              />
              <label
                htmlFor="so-file-upload"
                className="file-name-input-label"
                onClick={(e) => {
                  e.preventDefault();
                  if (this.fileInputRef.current) this.fileInputRef.current.click();
                }}
              >
                <input type="text" className="file-name-input" value={this.state.selectedFileName} placeholder="choose file" readOnly />
              </label>
              <Button buttonClass="blue" label="Submit" onClick={this.handleFileSubmit} disabled={this.state.isFileUploading} />
              {this.state.isFileUploading && <CircularProgress size={20} style={{ marginLeft: "10px", color: "#1976d2" }} />}
              <div className="upload-thumbnails">
                {files.map((file, index) => (
                  <div key={file.fileUUId || index} className="upload-thumbnail">
                    <IconButton className="thumbnail-remove" onClick={() => this.handleFileRemove(file)}>
                      <img src={trashRedIcon} alt="Remove" className="trash-red-icon" />
                    </IconButton>
                    <div className="thumbnail-preview">
                      {file.previewUrl ? (
                        <img src={file.previewUrl} alt={file.fileName} />
                      ) : (
                        <div className="thumbnail-placeholder">
                          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path d="M14 2H6C4.9 2 4 2.9 4 4V20C4 21.1 4.89 22 5.99 22H18C19.1 22 20 21.1 20 20V8L14 2ZM18 20H6V4H13V9H18V20Z" fill="#999" />
                          </svg>
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <div className="upload-hint">Upload a file here, Max 2 MB (JPG, PNG, PDF only)</div>
          </div>
        </div>
      </div>
    );
  }

  // ── Item dialog ──────────────────────────────────────────────────────────

  openAddItemDialog = () => {
    this.setState({ itemDialogOpen: true, dialogItem: {}, dialogEditingKey: null, dialogValidation: false });
  };

  openEditItemDialog = (key) => {
    const item = this.state.lines.find((l) => l.key === key);
    this.setState({ itemDialogOpen: true, dialogItem: { ...item }, dialogEditingKey: key, dialogValidation: false });
  };

  closeItemDialog = () => {
    this.setState({ itemDialogOpen: false, dialogItem: {}, dialogEditingKey: null, dialogValidation: false });
  };

  saveDialogItem = () => {
    const item = this.state.dialogItem;
    const descriptionMissing = !item.description || !String(item.description).trim();
    const rateMissing = item.rate === "" || item.rate === undefined || item.rate === null || isNaN(parseFloat(item.rate));
    if (descriptionMissing || rateMissing) {
      this.setState({ dialogValidation: true });
      return;
    }
    if (this.state.dialogEditingKey !== null) {
      this.setState((prev) => ({
        lines: prev.lines.map((l) => (l.key === prev.dialogEditingKey ? { ...item, key: l.key } : l)),
        itemDialogOpen: false,
        dialogItem: {},
        dialogEditingKey: null,
        dialogValidation: false,
      }));
    } else {
      const key = this.nextKey++;
      this.setState((prev) => ({
        lines: [...prev.lines, { ...item, key }],
        itemDialogOpen: false,
        dialogItem: {},
        dialogEditingKey: null,
        dialogValidation: false,
      }));
    }
  };

  addCustomFieldRow = () => {
    this.setState((prev) => ({
      dialogItem: { ...prev.dialogItem, customFields: [...(prev.dialogItem.customFields || []), { label: "", value: "" }] },
    }));
  };

  removeCustomFieldRow = (index) => {
    this.setState((prev) => ({
      dialogItem: { ...prev.dialogItem, customFields: (prev.dialogItem.customFields || []).filter((_, i) => i !== index) },
    }));
  };

  updateCustomFieldRow = (index, field, value) => {
    this.setState((prev) => ({
      dialogItem: {
        ...prev.dialogItem,
        customFields: (prev.dialogItem.customFields || []).map((cf, i) => (i === index ? { ...cf, [field]: value } : cf)),
      },
    }));
  };

  deleteLine = (key) => {
    this.setState((prev) => ({ lines: prev.lines.filter((l) => l.key !== key) }));
  };

  computeLineTotal = (line) => {
    const qty = line.quantity ? parseFloat(line.quantity) : 1;
    const rate = parseFloat(line.rate || 0);
    const discount = parseFloat(line.discount || 0);
    const gst = parseFloat(line.gst || 0);
    const discountedRate = rate - (rate * discount) / 100;
    const netRate = discountedRate * qty;
    const totalAmount = netRate + (netRate * gst) / 100;
    return { netRate, totalAmount };
  };

  buildLinePayload = (line) => {
    const { netRate, totalAmount } = this.computeLineTotal(line);
    return {
      description: line.description,
      quantity: line.quantity ? parseFloat(line.quantity) : null,
      rate: parseFloat(line.rate || 0),
      discountPercent: parseFloat(line.discount || 0),
      gstPercent: parseFloat(line.gst || 0),
      netRate,
      totalAmount,
      serviceType: line.serviceType || null,
      assetTag: line.assetTag || null,
      customFields: (line.customFields || [])
        .filter((cf) => cf.label && cf.label.trim())
        .map((cf) => ({ label: cf.label, value: cf.value || null })),
    };
  };

  // ── Save ─────────────────────────────────────────────────────────────────

  validate = () => {
    if (!this.state.vendor) return "Please select a Vendor";
    if (!this.state.projectName) return "Please select a Project";
    if (!this.state.serviceDate) return "Please select a Service Date";
    if (this.state.lines.length === 0) return "Please add at least one service line";
    return null;
  };

  handleSave = async () => {
    const error = this.validate();
    if (error) {
      this.props.enqueueSnackbar(error, { variant: "error" });
      return;
    }

    const lineTotal = this.state.lines.reduce((sum, l) => sum + this.computeLineTotal(l).totalAmount, 0);
    const specialDiscountAmt = parseFloat(this.state.specialDiscount || 0);
    const grandTotal = Math.round((lineTotal - specialDiscountAmt) * 100) / 100;

    this.setState({ isSaving: true });

    const commonFields = {
      serviceDate: this.state.serviceDate,
      vendorId: this.state.vendor?.id || null,
      firmId: this.state.firm?.id || null,
      subject: this.state.subject || "",
      notes: this.state.notes || "",
      projectName: this.state.projectName || null,
      specialDiscount: specialDiscountAmt || null,
      overridePhoneNumber: this.state.showOverridePhoneEmail ? (this.state.overridePhoneNumber || null) : null,
      overrideEmail: this.state.showOverridePhoneEmail ? (this.state.overrideEmail || null) : null,
      grandTotal,
      fileInformations: (this.state.fileInformations || []).map((f) => ({ fileUUId: f.fileUUId, fileName: f.fileName })),
    };

    if (this.state.isEditMode) {
      const existingLines = this.state.lines.filter((l) => l.lineId);
      const newLines = this.state.lines.filter((l) => !l.lineId);
      const originalLineIds = (this.props.editData.lines || []).map((l) => l.id);
      const currentIds = new Set(existingLines.map((l) => l.lineId));
      const removedLineIds = originalLineIds.filter((id) => !currentIds.has(id));

      const payload = {
        ...commonFields,
        lineUpdates: existingLines.map((l) => ({ lineId: l.lineId, ...this.buildLinePayload(l) })),
        newLines: newLines.map((l) => this.buildLinePayload(l)),
        removedLineIds,
      };

      const response = await API.PUT(apiEndpoints.updateServiceOrder(this.state.soId), payload);
      this.setState({ isSaving: false });
      if (response.success) {
        this.props.enqueueSnackbar("Service Order updated successfully", { variant: "success" });
        if (this.props.onSaved) this.props.onSaved();
        else this.props.back();
      } else {
        this.props.enqueueSnackbar(response.errorMessage || "Failed to update Service Order", { variant: "error" });
      }
      return;
    }

    const payload = {
      ...commonFields,
      lineItems: this.state.lines.map((l) => this.buildLinePayload(l)),
    };

    const response = await API.POST(apiEndpoints.createServiceOrder, payload);
    this.setState({ isSaving: false });
    if (response.success) {
      this.props.enqueueSnackbar("Service Order created successfully", { variant: "success" });
      this.props.back();
    } else {
      this.props.enqueueSnackbar(response.errorMessage || "Failed to create Service Order", { variant: "error" });
    }
  };

  // ── Render: line list ────────────────────────────────────────────────────

  renderLineRow(line) {
    const { totalAmount } = this.computeLineTotal(line);
    const truncate = (text) => (text && text.length > 40 ? text.slice(0, 38) + "…" : text);
    const badges = [];
    if (line.serviceType) badges.push(line.serviceType);
    if (line.assetTag) badges.push(line.assetTag);
    if (line.warrantyTill) badges.push(`Warranty till ${line.warrantyTill}`);
    const customFields = (line.customFields || []).filter((cf) => cf.label);

    return (
      <div
        key={line.key}
        onClick={() => this.openEditItemDialog(line.key)}
        style={{
          display: "grid",
          gridTemplateColumns: "2fr 1fr 2.5fr 1fr auto",
          gap: 16,
          alignItems: "center",
          padding: "12px 16px",
          borderBottom: "1px solid #eee",
          cursor: "pointer",
        }}
      >
        <div style={{ fontWeight: 600, fontSize: 13, color: "#323c47" }}>{truncate(line.description) || "—"}</div>
        <div style={{ fontSize: 13, color: "#555" }}>{line.quantity || 1} x ₹{line.rate || 0}</div>
        <div>
          {badges.length > 0 ? (
            <span style={{ fontSize: 12, color: "#666" }}>{badges.join(" · ")}</span>
          ) : (
            <span style={{ fontSize: 12, color: "#bbb" }}>—</span>
          )}
          {customFields.length > 0 && (
            <div style={{ display: "flex", flexWrap: "wrap", gap: 6, marginTop: 6 }}>
              {customFields.map((cf, i) => (
                <span
                  key={i}
                  style={{ fontSize: 11, background: "#eef2f7", color: "#444", borderRadius: 10, padding: "2px 8px" }}
                >
                  {cf.label}: {cf.value || "-"}
                </span>
              ))}
            </div>
          )}
        </div>
        <div style={{ textAlign: "right", fontSize: 13, fontWeight: 600 }}>
          ₹{totalAmount.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
        </div>
        <div style={{ display: "flex", gap: 0 }}>
          <IconButton size="small" aria-label="edit" onClick={(e) => { e.stopPropagation(); this.openEditItemDialog(line.key); }}>
            <EditIcon fontSize="small" />
          </IconButton>
          <IconButton size="small" aria-label="delete" onClick={(e) => { e.stopPropagation(); this.deleteLine(line.key); }}>
            <img src={trashRedIcon} alt="Delete" className="trash-red-icon" />
          </IconButton>
        </div>
      </div>
    );
  }

  renderItemsSection() {
    const { lines } = this.state;
    const grandTotal = lines.reduce((sum, l) => sum + this.computeLineTotal(l).totalAmount, 0);
    return (
      <div className="indent-items-section">
        <div className="indent-items-header-row">
          <div className="indent-items-title">Service Lines{lines.length > 0 ? ` (${lines.length})` : ""}</div>
          {lines.length > 0 && (
            <Button onClick={this.openAddItemDialog} buttonClass="blue" label="Add Line" startIcon={<AddIcon />} />
          )}
        </div>
        {lines.length === 0 ? (
          <div className="indent-items-empty">
            <div className="indent-items-empty-text">No service lines added yet.</div>
            <Button onClick={this.openAddItemDialog} buttonClass="blue" label="+ Add Line" />
          </div>
        ) : (
          <div className="indent-items-table">
            {lines.map((l) => this.renderLineRow(l))}
          </div>
        )}
        {lines.length > 0 && (
          <div style={{ textAlign: "right", padding: 16, fontSize: 14, fontWeight: 600, borderTop: "2px solid #e0e0e0", backgroundColor: "#fafafa" }}>
            Lines Total: Rs. {grandTotal.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </div>
        )}
      </div>
    );
  }

  // ── Render: item dialog ──────────────────────────────────────────────────

  renderItemDialog() {
    const item = this.state.dialogItem || {};
    const showValidation = this.state.dialogValidation;
    const descriptionMissing = showValidation && (!item.description || !String(item.description).trim());
    const rateMissing = showValidation && (item.rate === "" || item.rate === undefined || item.rate === null || isNaN(parseFloat(item.rate)));

    const setDialogField = (field, value) =>
      this.setState((prev) => ({ dialogItem: { ...prev.dialogItem, [field]: value } }));

    const matchesText = (opt, text) => opt.toLowerCase().includes((text || "").toLowerCase());
    const descMatches = this.state.descriptionOptions.filter((o) => matchesText(o, item.description));

    return (
      <Dialog open={this.state.itemDialogOpen} onClose={this.closeItemDialog} maxWidth="md" fullWidth aria-labelledby="so-item-dialog-title">
        <DialogTitle id="so-item-dialog-title">
          {this.state.dialogEditingKey !== null ? "Edit Service Line" : "Add Service Line"}
        </DialogTitle>
        <DialogContent>
          <div className="item-dialog-row">
            <Autocomplete
              freeSolo
              options={descMatches}
              open={this.state.descDropdownOpen && descMatches.length > 0}
              onOpen={() => this.setState({ descDropdownOpen: true })}
              onClose={() => this.setState({ descDropdownOpen: false })}
              inputValue={item.description || ""}
              onChange={(e, value) => setDialogField("description", value || "")}
              onInputChange={(e, value) => setDialogField("description", value || "")}
              renderInput={(params) => (
                <TextField
                  {...params}
                  variant="outlined"
                  margin="normal"
                  label="Description — select existing or type new"
                  required
                  error={descriptionMissing}
                  helperText={descriptionMissing ? "Description is required" : "e.g. Service 20 ACs at site office"}
                  InputLabelProps={{ shrink: true }}
                />
              )}
            />
          </div>
          <div className="item-dialog-row two-col">
            <Autocomplete
              options={SERVICE_TYPE_OPTIONS}
              value={item.serviceType || null}
              onChange={(e, value) => setDialogField("serviceType", value || "")}
              renderInput={(params) => (
                <TextField {...params} variant="outlined" margin="normal" label="Service Type" InputLabelProps={{ shrink: true }} />
              )}
            />
            <TextField
              variant="outlined"
              margin="normal"
              label="Asset / Reference"
              placeholder="e.g. Car KA01AB1234, AC Unit 3F"
              value={item.assetTag || ""}
              onChange={(e) => setDialogField("assetTag", e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
          </div>
          <div className="item-dialog-row two-col">
            <TextField
              type="number"
              variant="outlined"
              margin="normal"
              label="Quantity"
              value={item.quantity || ""}
              onChange={(e) => setDialogField("quantity", e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
            <div className={rateMissing ? "quantity-field-wrapper quantity-error" : ""}>
              <TextField
                type="number"
                variant="outlined"
                margin="normal"
                label="Rate *"
                fullWidth
                error={rateMissing}
                value={item.rate || ""}
                onChange={(e) => setDialogField("rate", e.target.value)}
                InputLabelProps={{ shrink: true }}
              />
              {rateMissing && <span className="quantity-error-msg">Rate is required</span>}
            </div>
          </div>
          <div className="item-dialog-row two-col">
            <TextField
              type="number"
              variant="outlined"
              margin="normal"
              label="Discount %"
              value={item.discount || ""}
              onChange={(e) => setDialogField("discount", e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
            <TextField
              type="number"
              variant="outlined"
              margin="normal"
              label="GST %"
              value={item.gst || ""}
              onChange={(e) => setDialogField("gst", e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
          </div>
          <div style={{ marginTop: 16 }}>
            <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
              <span style={{ fontSize: 13, fontWeight: 600, color: "#555" }}>
                Custom Fields <span style={{ fontWeight: 400, color: "#888" }}>(e.g. Odometer Reading, Lift Capacity)</span>
              </span>
              <MuiButton size="small" variant="outlined" startIcon={<AddIcon />} onClick={this.addCustomFieldRow}>
                Add Field
              </MuiButton>
            </div>
            {(item.customFields || []).map((cf, index) => {
              const keyMatches = this.state.customFieldLabelOptions.filter((o) => matchesText(o, cf.label));
              return (
              <div key={index} style={{ display: "grid", gridTemplateColumns: "1fr 1fr auto", gap: 12, alignItems: "center", marginTop: 10 }}>
                <Autocomplete
                  freeSolo
                  options={keyMatches}
                  open={!!this.state.customFieldDropdownOpen[index] && keyMatches.length > 0}
                  onOpen={() => this.setState((prev) => ({ customFieldDropdownOpen: { ...prev.customFieldDropdownOpen, [index]: true } }))}
                  onClose={() => this.setState((prev) => ({ customFieldDropdownOpen: { ...prev.customFieldDropdownOpen, [index]: false } }))}
                  inputValue={cf.label || ""}
                  onChange={(e, value) => this.updateCustomFieldRow(index, "label", value || "")}
                  onInputChange={(e, value) => this.updateCustomFieldRow(index, "label", value || "")}
                  renderInput={(params) => (
                    <TextField {...params} variant="outlined" margin="normal" label="Key" placeholder="select existing or type new" InputLabelProps={{ shrink: true }} />
                  )}
                />
                <TextField
                  variant="outlined"
                  margin="normal"
                  label="Value"
                  fullWidth
                  value={cf.value || ""}
                  onChange={(e) => this.updateCustomFieldRow(index, "value", e.target.value)}
                  InputLabelProps={{ shrink: true }}
                />
                <IconButton size="small" onClick={() => this.removeCustomFieldRow(index)}>
                  <img src={trashRedIcon} alt="Remove" className="trash-red-icon" />
                </IconButton>
              </div>
              );
            })}
          </div>
        </DialogContent>
        <DialogActions>
          <Button onClick={this.closeItemDialog} buttonClass="grey" label="Cancel" />
          <Button onClick={this.saveDialogItem} buttonClass="blue" label={this.state.dialogEditingKey !== null ? "Save Changes" : "Add Line"} />
        </DialogActions>
      </Dialog>
    );
  }

  // ── Render: main form ────────────────────────────────────────────────────

  render() {
    const lineTotal = this.state.lines.reduce((sum, l) => sum + this.computeLineTotal(l).totalAmount, 0);
    const specialDiscountAmt = parseFloat(this.state.specialDiscount || 0);
    const grandTotal = Math.round((lineTotal - specialDiscountAmt) * 100) / 100;

    return (
      <div className="list-section add create-po-wrapper service-order-wrapper">
        {this.renderItemDialog()}
        <div className="create-po-header">
          <div className="create-po-header-row">
            <h2>{this.state.isEditMode ? "Edit Service Order" : "Add Service Order"}</h2>
            <div className="po-action-buttons">
              <Button onClick={this.props.back} buttonClass="grey" label={messages.common.cancel} />
              <Button onClick={this.handleSave} buttonClass="blue" label={this.state.isSaving ? "Saving..." : "Save"} disabled={this.state.isSaving} />
            </div>
          </div>
        </div>

        <div className="create-po-content">
          <div className="form-section">
            <h3 className="section-title">Vendor &amp; Firm</h3>
            <div style={twoColGrid}>
              <Autocomplete
                options={this.state.vendorOptions}
                getOptionLabel={(option) => option?.name || ""}
                value={this.state.vendor}
                onChange={(e, val) => this.setState({ vendor: val })}
                renderInput={(params) => (
                  <TextField {...params} variant="outlined" margin="normal" label="Vendor *" InputLabelProps={{ shrink: true }} />
                )}
              />
              <Autocomplete
                options={this.state.firmOptions}
                getOptionLabel={(option) => option?.name || ""}
                value={this.state.firm}
                onChange={(e, val) => this.setState({ firm: val })}
                renderInput={(params) => (
                  <TextField {...params} variant="outlined" margin="normal" label="Firm (optional)" InputLabelProps={{ shrink: true }} />
                )}
              />
            </div>
            {(this.state.vendorDetails || this.state.firmDetails) && (
              <div style={twoColGrid}>
                <div>
                  {this.state.vendor && this.state.vendorDetails && (
                    <div style={detailBoxStyle}>
                      <div style={detailRowStyle}>
                        <span style={detailLabelStyle}>Contact Person:</span>
                        <span style={detailValueStyle}>{this.state.vendorDetails.contactPerson || "-"}</span>
                      </div>
                      <div style={detailRowStyle}>
                        <span style={detailLabelStyle}>Mobile Number:</span>
                        <span style={detailValueStyle}>{this.state.vendorDetails.mobileNumber || "-"}</span>
                      </div>
                      <div style={{ ...detailRowStyle, marginBottom: 0 }}>
                        <span style={detailLabelStyle}>Address:</span>
                        <span style={detailValueStyle}>{this.state.vendorDetails.address || "-"}</span>
                      </div>
                    </div>
                  )}
                </div>
                <div>
                  {this.state.firm && this.state.firmDetails && (
                    <div style={detailBoxStyle}>
                      <div style={detailRowStyle}>
                        <span style={detailLabelStyle}>Firm Address:</span>
                        <span style={detailValueStyle}>{this.state.firmDetails.address || "-"}</span>
                      </div>
                      <div style={detailRowStyle}>
                        <span style={detailLabelStyle}>GST:</span>
                        <span style={detailValueStyle}>{this.state.firmDetails.gst || "-"}</span>
                      </div>
                      <div style={detailRowStyle}>
                        <span style={detailLabelStyle}>PAN:</span>
                        <span style={detailValueStyle}>{this.state.firmDetails.pan || "-"}</span>
                      </div>
                      <div style={{ ...detailRowStyle, marginBottom: 0 }}>
                        <span style={detailLabelStyle}>Contact Number:</span>
                        <span style={detailValueStyle}>{this.state.firmDetails.contactNumber || "-"}</span>
                      </div>
                      <div style={{ marginTop: 8 }}>
                        <FormControlLabel
                          control={
                            <Checkbox
                              checked={this.state.showOverridePhoneEmail || false}
                              onChange={(e) => this.setState({ showOverridePhoneEmail: e.target.checked })}
                              color="primary"
                              size="small"
                            />
                          }
                          label={<span style={{ fontSize: 13, color: "#323c47" }}>Override Phone/Email</span>}
                        />
                      </div>
                      {this.state.showOverridePhoneEmail && (
                        <>
                          <div style={{ display: "flex", flexDirection: "column", gap: 4, marginTop: 8 }}>
                            <span style={detailLabelStyle}>Override Phone Number</span>
                            <TextField
                              variant="outlined"
                              fullWidth
                              placeholder="e.g. 9876543210, nani-8600033031"
                              value={this.state.overridePhoneNumber || ""}
                              onChange={(e) => this.setState({ overridePhoneNumber: e.target.value })}
                              size="small"
                              inputProps={{ maxLength: 35 }}
                              helperText={`${(this.state.overridePhoneNumber || "").length}/35 — overrides firm's phone on this order`}
                            />
                          </div>
                          <div style={{ display: "flex", flexDirection: "column", gap: 4, marginTop: 8 }}>
                            <span style={detailLabelStyle}>Override Email</span>
                            <TextField
                              variant="outlined"
                              fullWidth
                              placeholder="e.g. contact@company.com"
                              value={this.state.overrideEmail || ""}
                              onChange={(e) => this.setState({ overrideEmail: e.target.value })}
                              size="small"
                              inputProps={{ maxLength: 100 }}
                              helperText={`${(this.state.overrideEmail || "").length}/100 — overrides firm's email on this order`}
                            />
                          </div>
                        </>
                      )}
                    </div>
                  )}
                </div>
              </div>
            )}
          </div>

          <div className="form-section">
            <h3 className="section-title">Service Date, Project &amp; Subject</h3>
            <div style={twoColGrid}>
              <DatePicker label="Service Date *" maxDate={moment()} defaultValue={this.state.serviceDate} onChange={(date) => this.setState({ serviceDate: date })} />
              <Autocomplete
                options={this.state.projectOptions}
                getOptionLabel={(option) => option || ""}
                value={this.state.projectName || null}
                onChange={(e, val) => this.setState({ projectName: val || "" })}
                renderInput={(params) => (
                  <TextField {...params} variant="outlined" margin="normal" label="Project *" InputLabelProps={{ shrink: true }} />
                )}
              />
            </div>
            <div style={{ marginTop: 8 }}>
              <TextField
                fullWidth
                variant="outlined"
                margin="normal"
                label="Subject"
                value={this.state.subject}
                onChange={(e) => this.setState({ subject: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </div>
          </div>

          {this.renderItemsSection()}

          <div className="form-section">
            <h3 className="section-title">Special Discount</h3>
            <div style={twoColGrid}>
              <TextField
                type="number"
                variant="outlined"
                margin="normal"
                label="Special Discount (₹, flat, applied after lines total)"
                value={this.state.specialDiscount}
                onChange={(e) => this.setState({ specialDiscount: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </div>
            <div className="table-total" style={{ marginTop: 12 }}>
              {specialDiscountAmt > 0 && (
                <div style={{ fontSize: 13, marginBottom: 4 }}>
                  Lines Total: Rs. {lineTotal.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })} − Special Discount: Rs. {specialDiscountAmt.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </div>
              )}
              <strong>Grand Total: Rs. {grandTotal.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</strong>
            </div>
          </div>

          <div className="form-section">
            {this.renderFileArea()}
          </div>

          <div className="form-section">
            <h3 className="section-title">Notes</h3>
            <ReactQuill
              value={this.state.notes}
              onChange={(value) => this.setState({ notes: value })}
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
          </div>
        </div>
      </div>
    );
  }
}

export default withSnackbar(Add);
