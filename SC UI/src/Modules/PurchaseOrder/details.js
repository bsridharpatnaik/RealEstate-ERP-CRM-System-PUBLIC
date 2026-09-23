import React from "react";
import 'react-quill/dist/quill.core.css';
import CommonDetails from "./../../Shared/Details";
import IconButton from "@material-ui/core/IconButton";
import TextField from "@material-ui/core/TextField";
import CheckIcon from "@material-ui/icons/Check";
import CloseIconMui from "@material-ui/icons/Close";
import { messages } from "./../../messages";
import {
  MoreIcon,
  CloseIcon,
} from "./../../Shared/Icons/Index.js";
import DeleteConfirm from "./../../Shared//DeleteConfirm";
import ShortCloseConfirm from "./../../Shared/ShortCloseConfirm";
import Print from "./purchaseOrderPrint";
import Typography from "@material-ui/core/Typography";
import Box from "@material-ui/core/Box";
import Table from "@material-ui/core/Table";
import TableBody from "@material-ui/core/TableBody";
import TableCell from "@material-ui/core/TableCell";
import TableHead from "@material-ui/core/TableHead";
import TableRow from "@material-ui/core/TableRow";
import Menu from "@material-ui/core/Menu";
import MenuItem from "@material-ui/core/MenuItem";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import Checkbox from "@material-ui/core/Checkbox";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import Button from "@material-ui/core/Button";
import ArrowBackIosIcon from "@material-ui/icons/ArrowBackIos";
import ArrowForwardIosIcon from "@material-ui/icons/ArrowForwardIos";
import PrintIcon from "@material-ui/icons/Print";
import DeleteOutlineIcon from "@material-ui/icons/DeleteOutline";
import SwapHorizIcon from "@material-ui/icons/SwapHoriz";
import AddIcon from "@material-ui/icons/Add";
import POLineBillingDialog from "./POLineBillingDialog";
import CircularProgress from "@material-ui/core/CircularProgress";
import { withSnackbar } from "notistack";
import { canEditInventoryModules, canViewMoneyFields, getRole, isAdmin } from "./../../helper";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";

function TabPanel(props) {
  const { children, value, index, ...other } = props;
  return (
    <Typography
      component="div"
      role="tabpanel"
      hidden={value !== index}
      id={`simple-tabpanel-${index}`}
      aria-labelledby={`simple-tab-${index}`}
      {...other}
    >
      {value === index && <Box p={2}>{children}</Box>}
    </Typography>
  );
}

class Details extends CommonDetails {
  state = {
    value: 0,
    deleteConfirmOpen: false,
    shortCloseConfirmOpen: false,
    anchorEl: null,
    printAnchorEl: null,
    printDialogOpen: false,
    printWithRates: false,
    printWithIndents: false,
    indentsList: null,
    indentsLoading: false,
    indentsError: null,
    indentsPoId: null,
    statusHistory: null,
    statusHistoryLoading: false,
    statusHistoryError: null,
    statusHistoryPoId: null,
    lineImageUrls: {},  // lineId → object-URL for sample images (loaded via axios to carry auth headers)
    lightboxUrl: null,  // URL of image to show in lightbox, null = closed
    // local PO data override — refreshed after add/remove line operations
    localData: null,
    // Remove line
    removeLineConfirmOpen: false,
    lineToRemove: null,
    removeLineLoading: false,
    // Inline tolerance % edit (allowed in NEW/PARTIAL, blocked once terminal)
    editingToleranceLineId: null,
    editingToleranceValue: "",
    savingTolerance: false,

    // Billing unit/rate edit dialog (allowed in any non-terminal state)
    billingDialogLine: null,
  };

  async download(file) {
    this.setState({ isLoading: true });
    const response = await API.GET(apiEndpoints.masterFileDownload + file.fileUUId, {
      responseType: "blob",
    });
    this.setState({ isLoading: false });
    if (response.status === 200) {
      var a = document.createElement("a");
      var url = window.URL.createObjectURL(response.data);
      a.href = url;
      a.download = file.fileName;
      document.body.append(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    }
  }

  printRef = React.createRef();
  deleteRow = null;

  componentDidMount() {
    this.loadLineImages(this.props.data?.lines);
  }

  componentDidUpdate(prevProps) {
    if (prevProps.data?.purchaseOrderId !== this.props.data?.purchaseOrderId) {
      // Revoke previous blob URLs before loading new ones
      Object.values(this.state.lineImageUrls).forEach((u) => u && URL.revokeObjectURL(u));
      this.setState({
        statusHistory: null,
        statusHistoryError: null,
        statusHistoryPoId: null,
        indentsList: null,
        indentsError: null,
        indentsPoId: null,
        lineImageUrls: {},
      });
      if (this.state.value === 1) {
        this.fetchStatusHistory();
      }
      this.loadLineImages(this.props.data?.lines);
    }
  }

  componentWillUnmount() {
    Object.values(this.state.lineImageUrls).forEach((u) => u && URL.revokeObjectURL(u));
  }

  // Fetches each line's sample image via axios (carries auth headers) and stores a blob URL.
  loadLineImages = async (lines) => {
    if (!lines || lines.length === 0) return;
    const urls = {};
    await Promise.all(
      lines.map(async (line) => {
        if (!line.sampleImageFileId) return;
        try {
          const res = await API.GET(
            `${apiEndpoints.masterFileDownload}${line.sampleImageFileId}`,
            { responseType: "blob" }
          );
          if (res.status === 200) {
            urls[line.id] = URL.createObjectURL(res.data);
          }
        } catch (_) { /* non-critical */ }
      })
    );
    this.setState({ lineImageUrls: urls });
  };

  // ─── Role helpers ──────────────────────────────────────────────────────────

  // Returns true for admin, purchase-manager, and management — all get two print options (with/without rates).
  canPrintWithRates = () => {
    const role = (getRole() || "").toLowerCase();
    if (!role) return false;
    return role === "admin" || role === "purchase-manager" || role === "management";
  };

  // ─── More-menu handlers ────────────────────────────────────────────────────

  handleCloseMenu = () => {
    this.setState({ anchorEl: null });
  };

  // ─── Print dialog handlers ─────────────────────────────────────────────────

  handleOpenPrintDialog = () => {
    this.setState({
      printDialogOpen: true,
      printWithRates: false,
      printWithIndents: false,
      anchorEl: null,
      printAnchorEl: null,
    });
  };

  handleClosePrintDialog = () => {
    this.setState({ printDialogOpen: false });
  };

  handlePrintFromDialog = () => {
    const { printWithRates, printWithIndents } = this.state;
    const hideMoneyFields = !printWithRates;
    this.setState({ printDialogOpen: false });
    this.handlePrintPdf(hideMoneyFields, printWithIndents);
  };

  handlePrintPdf = async (hideMoneyFields = true, includeIndents = false) => {
    this.handleCloseMenu();
    const poId = this.props.data?.purchaseOrderId;
    if (!poId) return;
    try {
      const url = apiEndpoints.printPurchaseOrder(poId, hideMoneyFields, includeIndents);
      const response = await API.GETBlob(url);
      if (response.success) {
        const blobUrl = window.URL.createObjectURL(
          new Blob([response.data], { type: "application/pdf" })
        );
        window.open(blobUrl, "_blank");
      } else {
        this.props.enqueueSnackbar("Failed to generate PDF", { variant: "error" });
      }
    } catch (e) {
      this.props.enqueueSnackbar("Failed to generate PDF", { variant: "error" });
    }
  };

  // ─── Status history ────────────────────────────────────────────────────────

  fetchStatusHistory = async () => {
    const data = this.props.data;
    const poId = data?.purchaseOrderId;
    if (!poId) return;
    if (this.state.statusHistoryPoId === poId && this.state.statusHistory !== null) return;
    this.setState({ statusHistoryLoading: true, statusHistoryError: null });
    try {
      const url = apiEndpoints.getPurchaseOrderStatusHistory(poId);
      const response = await API.GET(url);
      if (response.success) {
        const list = Array.isArray(response.data) ? response.data : [];
        this.setState({
          statusHistory: list,
          statusHistoryLoading: false,
          statusHistoryPoId: poId,
        });
      } else {
        this.setState({
          statusHistoryError: response.errorMessage || "Failed to load history",
          statusHistoryLoading: false,
        });
      }
    } catch (error) {
      this.setState({
        statusHistoryError: "An error occurred while loading history",
        statusHistoryLoading: false,
      });
    }
  };

  handleHistoryTabClick = () => {
    this.setState({ value: 1 });
    this.fetchStatusHistory();
  };

  handleIndentsTabClick = () => {
    this.setState({ value: 2 });
    this.fetchIndentsForPo();
  };

  fetchIndentsForPo = async () => {
    const poId = this.props.data?.purchaseOrderId;
    if (!poId) return;
    if (this.state.indentsPoId === poId && this.state.indentsList !== null) return;
    this.setState({ indentsLoading: true, indentsError: null });
    try {
      const response = await API.GET(apiEndpoints.getIndentsForPo(poId));
      if (response.success) {
        const list = Array.isArray(response.data) ? response.data : [];
        this.setState({ indentsList: list, indentsLoading: false, indentsPoId: poId });
      } else {
        this.setState({
          indentsError: response.errorMessage || "Failed to load indents",
          indentsLoading: false,
        });
      }
    } catch (error) {
      this.setState({ indentsError: "An error occurred while loading indents", indentsLoading: false });
    }
  };

  // ─── Short-close ───────────────────────────────────────────────────────────

  handleShortClose = async (reason) => {
    const data = this.props.data;
    const requestBody = {
      purchaseOrderNo: data.purchaseOrderId,
      reason: reason,
    };
    try {
      const response = await API.POST(apiEndpoints.shortClosePurchaseOrder, requestBody);
      if (response.success) {
        this.props.enqueueSnackbar("Purchase Order short closed successfully", {
          variant: "success",
        });
        this.setState({ shortCloseConfirmOpen: false });
        if (this.props.onRefresh) this.props.onRefresh();
        this.props.close();
      } else {
        this.props.enqueueSnackbar(
          response.errorMessage || "Failed to short close purchase order",
          { variant: "error" }
        );
      }
    } catch (error) {
      this.props.enqueueSnackbar(
        "An error occurred while short closing the purchase order",
        { variant: "error" }
      );
    }
  };

  handleShortCloseCancel = () => {
    this.setState({ shortCloseConfirmOpen: false });
  };

  // ─── Remove line ────────────────────────────────────────────────────────────

  openRemoveLineConfirm = (line) => {
    this.setState({ removeLineConfirmOpen: true, lineToRemove: line });
  };

  closeRemoveLineConfirm = () => {
    this.setState({ removeLineConfirmOpen: false, lineToRemove: null });
  };

  handleRemoveLine = async () => {
    const data = this.getEffectiveData();
    const { lineToRemove } = this.state;
    if (!data || !lineToRemove) return;
    this.setState({ removeLineLoading: true });
    try {
      const url = apiEndpoints.removePOLine(data.purchaseOrderId, lineToRemove.id);
      const response = await API.DELETE(url);
      if (response.success) {
        this.props.enqueueSnackbar("Line item removed successfully", { variant: "success" });
        this.setState({ removeLineConfirmOpen: false, lineToRemove: null, localData: response.data });
        if (this.props.onRefresh) this.props.onRefresh();
      } else {
        this.props.enqueueSnackbar(response.errorMessage || "Failed to remove line item", { variant: "error" });
      }
    } catch (e) {
      this.props.enqueueSnackbar("An error occurred while removing the line item", { variant: "error" });
    } finally {
      this.setState({ removeLineLoading: false });
    }
  };


  /** Returns localData if set (after add/remove), otherwise falls back to props.data. */
  getEffectiveData = () => this.state.localData || this.props.data;

  startEditTolerance = (line) => {
    this.setState({
      editingToleranceLineId: line.id,
      editingToleranceValue: line.tolerancePercent != null ? String(line.tolerancePercent) : "",
    });
  };

  cancelEditTolerance = () => {
    this.setState({ editingToleranceLineId: null, editingToleranceValue: "" });
  };

  onBillingSaved = (updatedPo) => {
    this.setState({ localData: updatedPo, billingDialogLine: null });
    if (this.props.onRefresh) this.props.onRefresh();
  };

  saveTolerance = async (lineId) => {
    const data = this.getEffectiveData();
    if (!data) return;
    const value = parseFloat(this.state.editingToleranceValue || 0);
    this.setState({ savingTolerance: true });
    try {
      const response = await API.PUT(
        apiEndpoints.updatePOLineTolerance(data.purchaseOrderId, lineId),
        { tolerancePercent: isNaN(value) ? 0 : value }
      );
      if (response.success) {
        this.props.enqueueSnackbar("Tolerance % updated successfully", { variant: "success" });
        this.setState({
          localData: response.data,
          editingToleranceLineId: null,
          editingToleranceValue: "",
        });
        if (this.props.onRefresh) this.props.onRefresh();
      } else {
        this.props.enqueueSnackbar(response.errorMessage || "Failed to update tolerance %", { variant: "error" });
      }
    } catch (e) {
      this.props.enqueueSnackbar("An error occurred while updating tolerance %", { variant: "error" });
    } finally {
      this.setState({ savingTolerance: false });
    }
  };

  // ─── Navigation ────────────────────────────────────────────────────────────

  handlePrevious = () => {
    const { currentIndex, onNavigate } = this.props;
    if (currentIndex > 0 && onNavigate) onNavigate(currentIndex - 1);
  };

  handleNext = () => {
    const { currentIndex, allEntries, onNavigate } = this.props;
    if (allEntries && currentIndex < allEntries.length - 1 && onNavigate)
      onNavigate(currentIndex + 1);
  };

  // ─── Indents tab ───────────────────────────────────────────────────────────

  renderIndentsList = () => {
    const { indentsList, indentsLoading, indentsError } = this.state;

    if (indentsLoading) {
      return (
        <div className="po-indents-loading">
          <CircularProgress size={24} />
          <span>Loading indents…</span>
        </div>
      );
    }
    if (indentsError) {
      return <div className="po-indents-error">{indentsError}</div>;
    }
    if (!indentsList || indentsList.length === 0) {
      return <div className="po-indents-empty">No indents associated with this Purchase Order.</div>;
    }

    return (
      <div className="po-indents-list">
        {indentsList.map((indent) => {
          const lineItems = Array.isArray(indent.inventoryList)
            ? indent.inventoryList
            : Array.from(indent.inventoryList || []);

          return (
            <div key={indent.indentId} className="po-indent-card">
              {/* Indent header */}
              <div className="po-indent-card-header">
                <div className="po-indent-header-fields">
                  <span className="po-indent-header-item">
                    <span className="po-indent-header-label">Indent No:</span>
                    <span className="po-indent-header-value">{indent.indentId || "-"}</span>
                  </span>
                  <span className="po-indent-header-item">
                    <span className="po-indent-header-label">Date:</span>
                    <span className="po-indent-header-value">{indent.indentDate || "-"}</span>
                  </span>
                  <span className="po-indent-header-item">
                    <span className="po-indent-header-label">Status:</span>
                    <span className={`po-indent-status-badge status-${(indent.indentStatus || "").toLowerCase()}`}>
                      {indent.indentStatus || "-"}
                    </span>
                  </span>
                  <span className="po-indent-header-item">
                    <span className="po-indent-header-label">Project:</span>
                    <span className="po-indent-header-value">{indent.tenant || "-"}</span>
                  </span>
                </div>
              </div>

              {/* Line items table */}
              <div className="po-indent-table-wrapper">
                <table className="po-indent-table">
                  <thead>
                    <tr>
                      <th>Product</th>
                      <th>Specification</th>
                      <th>UOM</th>
                      <th>Qty Ordered</th>
                      <th>Qty Received</th>
                      <th>Qty Pending</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {lineItems.length === 0 ? (
                      <tr>
                        <td colSpan={8} className="po-indent-no-data">No line items</td>
                      </tr>
                    ) : (
                      lineItems.map((item, idx) => (
                        <tr key={item.entryid || idx}>
                          <td>
                            <div className="po-indent-product-name">
                              {item.product?.productName || "-"}
                            </div>
                            {item.product?.productCode && (
                              <div className="po-indent-product-code">{item.product.productCode}</div>
                            )}
                          </td>
                          <td>{item.specification || "-"}</td>
                          <td>{item.measurementUnit || "-"}</td>
                          <td>{item.quantity != null ? Number(item.quantity).toFixed(2) : "-"}</td>
                          <td>{item.quantityReceived != null ? Number(item.quantityReceived).toFixed(2) : "-"}</td>
                          <td>{item.quantityPending != null ? Number(item.quantityPending).toFixed(2) : "-"}</td>
                          <td>
                            <span className={`po-indent-line-status status-${(item.lineItemStatus || "").toLowerCase().replace(/\s+/g, "-")}`}>
                              {item.lineItemStatus || "-"}
                            </span>
                          </td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          );
        })}
      </div>
    );
  };

  // ─── Print button (used in fromRelation view) ──────────────────────────────
  // Opens the unified print dialog for all roles.

  renderPrintButton = () => (
    <IconButton
      className="back-icon"
      aria-label="Print options"
      onClick={this.handleOpenPrintDialog}
    >
      <PrintIcon fontSize="medium" />
    </IconButton>
  );

  // ─── Print dialog ──────────────────────────────────────────────────────────

  renderPrintDialog = () => {
    const { printDialogOpen, printWithRates, printWithIndents } = this.state;
    const showRatesOption = this.canPrintWithRates();

    return (
      <Dialog
        open={printDialogOpen}
        onClose={this.handleClosePrintDialog}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle>Print Options</DialogTitle>
        <DialogContent>
          <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
            {showRatesOption && (
              <FormControlLabel
                control={
                  <Checkbox
                    checked={printWithRates}
                    onChange={(e) => this.setState({ printWithRates: e.target.checked })}
                    color="primary"
                  />
                }
                label="Include Rates"
              />
            )}
            <FormControlLabel
              control={
                <Checkbox
                  checked={printWithIndents}
                  onChange={(e) => this.setState({ printWithIndents: e.target.checked })}
                  color="primary"
                />
              }
              label="Include Indents"
            />
          </div>
        </DialogContent>
        <DialogActions>
          <Button onClick={this.handleClosePrintDialog} color="default">
            Cancel
          </Button>
          <Button onClick={this.handlePrintFromDialog} color="primary" variant="contained">
            Print
          </Button>
        </DialogActions>
      </Dialog>
    );
  };

  // ─── Render ────────────────────────────────────────────────────────────────

  render() {
    const data = this.getEffectiveData();
    if (!data) return null;

    const { currentIndex = 0, allEntries = [], fromRelation } = this.props;
    const canGoPrevious = currentIndex > 0;
    const canGoNext = currentIndex < allEntries.length - 1;
    const isFromRelation = fromRelation === true;

    const { statusHistory, statusHistoryLoading, statusHistoryError } = this.state;

    const statusNormalized = (data.status || "").toLowerCase().trim();

    const items = data.lines || [];
    const hasImages = items.some(item => item.sampleImageFileId);
    const total =
      data.grandTotal ||
      items.reduce((sum, item) => sum + parseFloat(item.totalAmount || 0), 0);

    // project-manager and store-incharge must NOT see any money-related fields
    const showMoneyFields = canViewMoneyFields();

    return (
      <div className="list-section detail-section purchase-order-detail-section">
        {this.renderPrintDialog()}

        {/* Hidden print template */}
        <div className="print-content">
          <Print ref={this.printRef} data={data} />
        </div>

        <div className="po-detail-card">
          {/* ── Header ── */}
          <div className="details-header">
            <div className="po-header-left">
              <div className="po-number">PO Number: {data.purchaseOrderId || ""}</div>
              {data.specialPo && <span className="spl-po-badge">SPL PO</span>}
              {!isFromRelation && (
                <div className="po-navigation">
                  <IconButton
                    size="small"
                    className="nav-arrow"
                    onClick={this.handlePrevious}
                    disabled={!canGoPrevious}
                  >
                    <ArrowBackIosIcon style={{ fontSize: 14 }} />
                  </IconButton>
                  <IconButton
                    size="small"
                    className="nav-arrow"
                    onClick={this.handleNext}
                    disabled={!canGoNext}
                  >
                    <ArrowForwardIosIcon style={{ fontSize: 14 }} />
                  </IconButton>
                </div>
              )}
            </div>

            <div className="po-header-right">
              {isFromRelation ? (
                // Relation view: standalone print button (no more-menu)
                this.renderPrintButton()
              ) : (
                <>
                  {/* ── More (⋮) menu ── */}
                  <IconButton
                    onClick={(event) =>
                      this.setState({ anchorEl: event.currentTarget })
                    }
                    className="back-icon"
                  >
                    {MoreIcon({ fontSize: "medium" })}
                  </IconButton>
                  <Menu
                    anchorEl={this.state.anchorEl}
                    keepMounted
                    open={Boolean(this.state.anchorEl)}
                    onClose={this.handleCloseMenu}
                    getContentAnchorEl={null}
                    anchorOrigin={{ vertical: "bottom", horizontal: "center" }}
                    transformOrigin={{ vertical: "top", horizontal: "center" }}
                    classes={{ paper: "po-detail-dropdown-menu" }}
                  >
                    <MenuItem
                      key="print"
                      onClick={() => {
                        this.handleCloseMenu();
                        this.handleOpenPrintDialog();
                      }}
                    >
                      Print
                    </MenuItem>

                    {canEditInventoryModules() && (isAdmin() || data.status === "NEW") && (
                      <MenuItem
                        onClick={() => {
                          this.handleCloseMenu();
                          this.props.edit(data);
                        }}
                      >
                        Edit
                      </MenuItem>
                    )}

                    {canEditInventoryModules() && data.status === "PARTIAL" && (
                      <MenuItem
                        onClick={() => {
                          this.handleCloseMenu();
                          this.setState({ shortCloseConfirmOpen: true });
                        }}
                      >
                        Short Close
                      </MenuItem>
                    )}

                    {canEditInventoryModules() && statusNormalized !== "cancelled" && (
                      <MenuItem
                        onClick={() => {
                          this.handleCloseMenu();
                          this.setState({ deleteConfirmOpen: true });
                        }}
                      >
                        Delete
                      </MenuItem>
                    )}
                  </Menu>
                </>
              )}

              <IconButton onClick={this.props.close} className="back-icon">
                {CloseIcon({ fontSize: "medium" })}
              </IconButton>
            </div>
          </div>

          {/* ── Tabs header ── */}
          <div className="po-tabs-header">
            <div className="po-tabs-container">
              <div
                className={`po-tab ${this.state.value === 0 ? "active" : ""}`}
                onClick={() => this.setState({ value: 0 })}
              >
                Details
              </div>
              <div
                className={`po-tab ${this.state.value === 1 ? "active" : ""}`}
                onClick={this.handleHistoryTabClick}
              >
                History
              </div>
              <div
                className={`po-tab ${this.state.value === 2 ? "active" : ""}`}
                onClick={this.handleIndentsTabClick}
              >
                Indents
              </div>
            </div>
          </div>

          {/* ── Details tab ── */}
          <TabPanel value={this.state.value} index={0} className="detail-content-panel">
            <div className="detail-content">

              {/* General Information */}
              <div className="detail-section-group">
                <h3 className="section-title">General Information</h3>
                <div className="detail-item">
                  <span className="detail-label">Date Creation:</span>
                  <span className="detail-value">{data.poDate || "-"}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Subject:</span>
                  <span className="detail-value">{data.subject || "-"}</span>
                </div>
                {data.projectName && (
                  <div className="detail-item">
                    <span className="detail-label">Project:</span>
                    <span className="detail-value">{data.projectName}</span>
                  </div>
                )}
                {showMoneyFields && (
                  <div className="detail-item">
                    <span className="detail-label">Grand Total:</span>
                    <span className="detail-value">
                      Rs.{" "}
                      {data.grandTotal
                        ? data.grandTotal.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })
                        : "-"}
                    </span>
                  </div>
                )}
              </div>

              {/* Order To */}
              <div className="detail-section-group">
                <h3 className="section-title">Order To</h3>
                <div className="detail-item">
                  <span className="detail-label">Supplier Name:</span>
                  <span className="detail-value">{data.supplier?.name || "-"}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Contact Type:</span>
                  <span className="detail-value">{data.supplier?.contactType || "-"}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">GST Number:</span>
                  <span className="detail-value">{data.supplier?.gstNumber || "-"}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Contact Person:</span>
                  <span className="detail-value">{data.supplier?.contactPerson || "-"}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Mobile Number:</span>
                  <span className="detail-value">
                    {data.supplier?.mobileNo ||
                      data.supplier?.contactPersonMobileNo ||
                      "-"}
                  </span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Email:</span>
                  <span className="detail-value">{data.supplier?.emailId || "-"}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Address:</span>
                  <span className="detail-value">
                    {[
                      data.supplier?.addr_line1,
                      data.supplier?.addr_line2,
                      data.supplier?.city,
                      data.supplier?.state,
                      data.supplier?.zip,
                    ]
                      .filter(Boolean)
                      .join(", ") || "-"}
                  </span>
                </div>
              </div>

              {/* Order From */}
              <div className="detail-section-group">
                <h3 className="section-title">Order From</h3>
                <div className="detail-item">
                  <span className="detail-label">Firm Name:</span>
                  <span className="detail-value">{data.firm?.firmName || "-"}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Description:</span>
                  <span className="detail-value">{data.firm?.firmDescription || "-"}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">GST Number:</span>
                  <span className="detail-value">{data.firm?.firmGstNumber || "-"}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">PAN Number:</span>
                  <span className="detail-value">{data.firm?.firmPanNumber || "-"}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Contact Number:</span>
                  <span className="detail-value">
                    {data.overridePhoneNumber || data.firm?.firmContactNumber || "-"}
                  </span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Contact Email:</span>
                  <span className="detail-value">
                    {data.overrideEmail || data.firm?.firmEmail || "-"}
                  </span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Address:</span>
                  <span className="detail-value">
                    {[
                      data.firm?.addr_line1,
                      data.firm?.addr_line2,
                      data.firm?.city,
                      data.firm?.state,
                      data.firm?.zip,
                    ]
                      .filter(Boolean)
                      .join(", ") ||
                      data.firm?.firmAddress ||
                      "-"}
                  </span>
                </div>
              </div>

              {/* Line items */}
              {items.length > 0 && (
                <div className="detail-section-group">
                  <h3 className="section-title purchase-orders-title" style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
                    <span>Purchase Orders</span>
                    {canEditInventoryModules() &&
                      (data.status === "NEW" || data.status === "PARTIAL") && (
                      <Button
                        size="small"
                        variant="outlined"
                        color="primary"
                        startIcon={<AddIcon />}
                        onClick={() => this.props.onAddLineToPO && this.props.onAddLineToPO(data.purchaseOrderId)}
                        style={{ fontSize: 12 }}
                      >
                        Add Line
                      </Button>
                    )}
                  </h3>
                  <div
                    className={
                      showMoneyFields
                        ? "inventory-table-wrapper"
                        : "inventory-table-wrapper no-money-columns"
                    }
                  >
                    <Table>
                      <TableHead>
                        <TableRow>
                          <TableCell>Inventory</TableCell>
                          {hasImages && <TableCell style={{ textAlign: 'center' }}>Sample Image</TableCell>}
                          <TableCell style={{ whiteSpace: 'nowrap' }}>Qty / UOM</TableCell>
                          <TableCell>Line Status</TableCell>
                          {data.status === "PARTIAL" && <TableCell style={{ whiteSpace: 'nowrap' }}>Received Qty</TableCell>}
                          {data.status === "PARTIAL" && <TableCell style={{ whiteSpace: 'nowrap' }}>Balance Qty</TableCell>}
                          {showMoneyFields && <TableCell style={{ whiteSpace: 'nowrap' }}>Rate</TableCell>}
                          {showMoneyFields && <TableCell style={{ whiteSpace: 'nowrap' }}>Total</TableCell>}
                          {showMoneyFields && <TableCell style={{ whiteSpace: 'nowrap' }}>Discount %</TableCell>}
                          <TableCell style={{ whiteSpace: 'nowrap' }}>Tolerance %</TableCell>
                          <TableCell style={{ whiteSpace: 'nowrap' }}>Days Left</TableCell>
                          <TableCell style={{ whiteSpace: 'nowrap' }}>Exp. Date</TableCell>
                          {showMoneyFields && <TableCell style={{ whiteSpace: 'nowrap' }}>Net Value</TableCell>}
                          {showMoneyFields && <TableCell style={{ whiteSpace: 'nowrap' }}>Net Value/Unit</TableCell>}
                          <TableCell style={{ whiteSpace: 'nowrap' }}>GST %</TableCell>
                          {showMoneyFields && <TableCell style={{ whiteSpace: 'nowrap' }}>GST Amt</TableCell>}
                          {showMoneyFields && <TableCell style={{ whiteSpace: 'nowrap' }}>Amt Incl Tax</TableCell>}
                          {canEditInventoryModules() &&
                            (data.status === "NEW" || data.status === "PARTIAL") && (
                            <TableCell style={{ whiteSpace: 'nowrap', width: 48 }}></TableCell>
                          )}
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {items.map((item, index) => {
                          const productName =
                            item.product?.name ||
                            item.product?.productName ||
                            "-";
                          const quantity = item.quantity || 0;
                          const rate = parseFloat(item.rate || 0);
                          const discountPercent = parseFloat(
                            item.discountPercent || 0
                          );
                          const gstPercent = parseFloat(item.gstPercent || 0);
                          const tolerancePercent = parseFloat(item.tolerancePercent || 0);
                          const netRate = parseFloat(item.netRate || 0);
                          const grossTotal = rate * quantity;
                          const gstAmt = netRate * gstPercent / 100;
                          const totalAmount = parseFloat(item.totalAmount || 0);

                          // Show billing unit/qty when PO was created in alternate unit
                          const baseUnit = item.product?.measurementUnit || item.unit || "";
                          const displayQty = item.billingUnit
                            ? item.billingQuantity
                            : quantity;
                          const displayUnit = item.billingUnit || baseUnit;
                          const quantityText = displayQty
                            ? `${displayQty} ${displayUnit}`.trim()
                            : "";
                          const baseQtyNote = item.billingUnit
                            ? ` (= ${quantity} ${baseUnit})`.trim()
                            : "";
                          const details = [
                            item.brand && `Brand Name: ${item.brand}`,
                            item.grade && `Grade: ${item.grade}`,
                            item.diameter && `Diameter: ${item.diameter}`,
                            item.size && `Size: ${item.size}`,
                            item.specification &&
                              `Spec: ${item.specification}`,
                          ].filter(Boolean);
                          const inventoryDetails = quantityText
                            ? [quantityText + baseQtyNote, ...details].join(", ")
                            : details.join(", ");

                          const status = data.status || "Complete Inward";

                          const lineStatus = item.lineItemStatus || data.status || "";
                          const isRemovable = canEditInventoryModules() &&
                            (data.status === "NEW" || data.status === "PARTIAL") &&
                            lineStatus === "PO CREATED";
                          const isRemoveBlocked = canEditInventoryModules() &&
                            (data.status === "NEW" || data.status === "PARTIAL") &&
                            lineStatus === "INWARD PARTIAL";

                          return (
                            <TableRow key={index}>
                              <TableCell className="inventory-cell">
                                <div className="inventory-name">{productName}</div>
                                {details.length > 0 && (
                                  <div className="inventory-details">
                                    {details.join(", ")}
                                  </div>
                                )}
                              </TableCell>
                              {hasImages && (
                                <TableCell style={{ textAlign: "center", minWidth: 110 }}>
                                  {this.state.lineImageUrls[item.id] ? (
                                    <img
                                      src={this.state.lineImageUrls[item.id]}
                                      alt="sample"
                                      title="Click to enlarge"
                                      onClick={() => this.setState({ lightboxUrl: this.state.lineImageUrls[item.id] })}
                                      style={{ width: 80, height: 80, objectFit: "contain", border: "1px solid #ddd", borderRadius: 4, cursor: "zoom-in" }}
                                    />
                                  ) : (item.sampleImageFileId ? "Loading…" : "-")}
                                </TableCell>
                              )}
                              <TableCell style={{ whiteSpace: 'nowrap' }}>
                                <div>{displayQty} {displayUnit}</div>
                                {item.billingUnit && (
                                  <div style={{ fontSize: 11, color: '#888' }}>= {quantity} {baseUnit}</div>
                                )}
                              </TableCell>
                              <TableCell>
                                <span
                                  className={`status-badge-table status-${(lineStatus)
                                    .toLowerCase()
                                    .replace(/\s+/g, "-")}`}
                                >
                                  {lineStatus || "-"}
                                </span>
                              </TableCell>
                              {data.status === "PARTIAL" && (
                                <TableCell style={{ whiteSpace: 'nowrap' }}>
                                  {item.receivedQuantity != null ? Number(item.receivedQuantity).toFixed(2) : "-"}
                                </TableCell>
                              )}
                              {data.status === "PARTIAL" && (
                                <TableCell style={{ whiteSpace: 'nowrap' }}>
                                  {item.balanceQuantity != null ? Number(item.balanceQuantity).toFixed(2) : "-"}
                                </TableCell>
                              )}
                              {showMoneyFields && (
                                <>
                                  <TableCell style={{ whiteSpace: 'nowrap' }}>
                                    Rs. {rate.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                  </TableCell>
                                  <TableCell style={{ whiteSpace: 'nowrap' }}>
                                    Rs. {grossTotal.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                  </TableCell>
                                  <TableCell style={{ whiteSpace: 'nowrap' }}>
                                    {discountPercent > 0 ? `${discountPercent}%` : "-"}
                                  </TableCell>
                                </>
                              )}
                              <TableCell style={{ whiteSpace: 'nowrap' }}>
                                {canEditInventoryModules() && (data.status === "NEW" || data.status === "PARTIAL") ? (
                                  this.state.editingToleranceLineId === item.id ? (
                                    <div style={{ display: "flex", alignItems: "center", gap: 4 }}>
                                      <TextField
                                        type="number"
                                        size="small"
                                        variant="outlined"
                                        value={this.state.editingToleranceValue}
                                        onChange={(e) => this.setState({ editingToleranceValue: e.target.value })}
                                        inputProps={{ min: 0, max: 100, step: 0.01, style: { fontSize: 12, padding: "4px 6px", width: 50 } }}
                                        disabled={this.state.savingTolerance}
                                      />
                                      <IconButton
                                        size="small"
                                        onClick={() => this.saveTolerance(item.id)}
                                        disabled={this.state.savingTolerance}
                                        title="Save"
                                      >
                                        <CheckIcon fontSize="small" />
                                      </IconButton>
                                      <IconButton
                                        size="small"
                                        onClick={this.cancelEditTolerance}
                                        disabled={this.state.savingTolerance}
                                        title="Cancel"
                                      >
                                        <CloseIconMui fontSize="small" />
                                      </IconButton>
                                    </div>
                                  ) : (
                                    <span
                                      onClick={() => this.startEditTolerance(item)}
                                      style={{ cursor: "pointer", borderBottom: "1px dashed #999" }}
                                      title="Click to edit tolerance %"
                                    >
                                      {tolerancePercent > 0 ? `${tolerancePercent}%` : "Set %"}
                                    </span>
                                  )
                                ) : (
                                  tolerancePercent > 0 ? `${tolerancePercent}%` : "-"
                                )}
                              </TableCell>
                              <TableCell style={{ whiteSpace: 'nowrap', textAlign: 'center' }}>
                                {item.daysLeft != null ? (
                                  <span
                                    className={`days-left-chip ${item.isOverdue ? "days-left-overdue" : "days-left-ok"}`}
                                  >
                                    {item.isOverdue
                                      ? `⚠ ${Math.abs(item.daysLeft)}d overdue`
                                      : `${item.daysLeft}d left`}
                                  </span>
                                ) : (
                                  <span style={{ color: '#aaa' }}>—</span>
                                )}
                              </TableCell>
                              <TableCell style={{ whiteSpace: 'nowrap' }}>{item.needByDate || "-"}</TableCell>
                              {showMoneyFields && (
                                <>
                                  <TableCell className="net-rate-cell" style={{ whiteSpace: 'nowrap' }}>
                                    Rs. {netRate.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                  </TableCell>
                                  <TableCell style={{ whiteSpace: 'nowrap' }}>
                                    Rs. {(displayQty > 0 ? netRate / displayQty : 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                  </TableCell>
                                </>
                              )}
                              <TableCell style={{ whiteSpace: 'nowrap' }}>{gstPercent}%</TableCell>
                              {showMoneyFields && (
                                <>
                                  <TableCell style={{ whiteSpace: 'nowrap' }}>
                                    Rs. {gstAmt.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                  </TableCell>
                                  <TableCell style={{ whiteSpace: 'nowrap' }}>
                                    Rs. {totalAmount.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                                  </TableCell>
                                </>
                              )}
                              {canEditInventoryModules() &&
                                (data.status === "NEW" || data.status === "PARTIAL") && (
                                <TableCell style={{ padding: "0 4px", whiteSpace: "nowrap" }}>
                                  {showMoneyFields && (
                                    <IconButton
                                      size="small"
                                      title="Edit billing unit / rate"
                                      onClick={() => this.setState({ billingDialogLine: item })}
                                    >
                                      <SwapHorizIcon fontSize="small" />
                                    </IconButton>
                                  )}
                                  {isRemovable ? (
                                    <IconButton
                                      size="small"
                                      title="Remove this line item"
                                      onClick={() => this.openRemoveLineConfirm(item)}
                                      style={{ color: "#c62828" }}
                                    >
                                      <DeleteOutlineIcon fontSize="small" />
                                    </IconButton>
                                  ) : isRemoveBlocked ? (
                                    <IconButton size="small" disabled title="Inward already started — cannot remove">
                                      <DeleteOutlineIcon fontSize="small" />
                                    </IconButton>
                                  ) : null}
                                </TableCell>
                              )}
                            </TableRow>
                          );
                        })}
                      </TableBody>
                    </Table>

                    {showMoneyFields && (
                      <div className="total-row-wrapper">
                        <div className="total-row">
                          <span className="total-label">Total: </span>
                          <span className="total-amount">
                            Rs. {items.reduce((sum, item) => sum + parseFloat(item.totalAmount || 0), 0)
                              .toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                          </span>
                        </div>
                        {data.freightCharges > 0 && (
                          <div className="total-row">
                            <span className="total-label">
                              Freight Charges: Rs. {data.freightCharges.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                              &nbsp;&nbsp;|&nbsp;&nbsp;
                              GST: {data.freightGstPercent != null ? data.freightGstPercent.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + "%" : "-"}
                              &nbsp;&nbsp;|&nbsp;&nbsp;
                              Total Freight:
                            </span>
                            <span className="total-amount">
                              Rs. {(data.totalFreightCharges || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                            </span>
                          </div>
                        )}
                        {(data.customCharges || []).filter(c => c.chargeAmount > 0).map((cc, idx) => (
                          <div key={idx} className="total-row">
                            <span className="total-label">
                              {cc.chargeName || "Additional Charges"}: Rs. {cc.chargeAmount.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                              &nbsp;&nbsp;|&nbsp;&nbsp;
                              GST: {cc.chargeGstPercent != null ? cc.chargeGstPercent.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + "%" : "-"}
                              &nbsp;&nbsp;|&nbsp;&nbsp;
                              Total:
                            </span>
                            <span className="total-amount">
                              Rs. {(cc.totalChargeAmount || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                            </span>
                          </div>
                        ))}
                        {data.poDiscount > 0 && (
                          <div className="total-row">
                            <span className="total-label">PO Discount: </span>
                            <span className="total-amount">
                              - Rs. {data.poDiscount.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                            </span>
                          </div>
                        )}
                        <div className="total-row">
                          <span className="total-label">Grand Total: </span>
                          <span className="total-amount">
                            Rs. {total.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                          </span>
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Uploaded documents */}
              {data.fileInformations && data.fileInformations.length > 0 && (
                <div className="detail-section-group">
                  <h3 className="section-title">
                    {messages.common.uploadedDocuments}
                  </h3>
                  {this.renderFileList(data.fileInformations)}
                </div>
              )}

              {/* Notes */}
              {data.notes && data.notes.trim().length > 0 && (
                <div className="detail-section-group">
                  <h3 className="section-title">Notes</h3>
                  <div
                    className="notes-content ql-editor"
                    dangerouslySetInnerHTML={{ __html: data.notes }}
                  />
                </div>
              )}
            </div>
          </TabPanel>

          {/* ── Indents tab ── */}
          <TabPanel value={this.state.value} index={2} className="indents-content-panel">
            {this.renderIndentsList()}
          </TabPanel>

          {/* ── History tab ── */}
          <TabPanel value={this.state.value} index={1} className="history-content-panel">
            <div className="history-content">
              {statusHistoryLoading ? (
                <div className="history-loading">Loading...</div>
              ) : statusHistoryError ? (
                <div className="history-error">{statusHistoryError}</div>
              ) : statusHistory && statusHistory.length > 0 ? (
                statusHistory.map((item) => (
                  <div key={item.id} className="history-item">
                    <div className="history-date-time">{item.changedAt}</div>
                    <div className="history-description">{item.changeMessage}</div>
                    {item.changedBy && (
                      <div className="history-changed-by">
                        Changed by: {item.changedBy}
                      </div>
                    )}
                    {item.relations &&
                      item.relations.length > 0 &&
                      this.props.onOpenRelation && (
                        <div className="history-relations">
                          <Table size="small" className="relations-table">
                            <TableHead>
                              <TableRow>
                                <TableCell>Relation Type</TableCell>
                                <TableCell>Reference ID</TableCell>
                              </TableRow>
                            </TableHead>
                            <TableBody>
                              {item.relations.map((rel) => (
                                <TableRow key={rel.id}>
                                  <TableCell>{rel.relationType}</TableCell>
                                  <TableCell>
                                    <button
                                      type="button"
                                      className="relation-link"
                                      onClick={() =>
                                        this.props.onOpenRelation({
                                          relationType: rel.relationType,
                                          referenceId: rel.referenceId,
                                          tenant: rel.tenant || "",
                                        })
                                      }
                                    >
                                      {rel.referenceId}
                                    </button>
                                  </TableCell>
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                        </div>
                      )}
                  </div>
                ))
              ) : (
                <div className="history-empty">No history available</div>
              )}
            </div>
          </TabPanel>
        </div>

        {/* ── Dialogs ── */}
        <DeleteConfirm
          open={this.state.deleteConfirmOpen}
          onCancel={() => this.setState({ deleteConfirmOpen: false })}
          onConfirm={() => {
            this.props.delete(data);
            this.setState({ deleteConfirmOpen: false });
          }}
        />
        <ShortCloseConfirm
          open={this.state.shortCloseConfirmOpen}
          purchaseOrderNo={data.purchaseOrderId}
          onCancel={this.handleShortCloseCancel}
          onConfirm={this.handleShortClose}
        />

        {/* ── Remove Line Confirmation ── */}
        <Dialog
          open={this.state.removeLineConfirmOpen}
          onClose={this.closeRemoveLineConfirm}
          maxWidth="xs"
          fullWidth
        >
          <DialogTitle>Remove Line Item</DialogTitle>
          <DialogContent>
            <p style={{ margin: 0, fontSize: 14 }}>
              This will remove the selected line item and revert the linked indent back to
              <strong> NEW</strong> status. This action cannot be undone. Continue?
            </p>
          </DialogContent>
          <DialogActions>
            <Button onClick={this.closeRemoveLineConfirm} disabled={this.state.removeLineLoading}>
              Cancel
            </Button>
            <Button
              onClick={this.handleRemoveLine}
              color="secondary"
              variant="contained"
              disabled={this.state.removeLineLoading}
            >
              {this.state.removeLineLoading ? <CircularProgress size={18} /> : "Remove"}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Image lightbox */}
        {this.state.lightboxUrl && (
          <div
            onClick={() => this.setState({ lightboxUrl: null })}
            style={{
              position: "fixed", inset: 0, zIndex: 9999,
              background: "rgba(0,0,0,0.82)",
              display: "flex", alignItems: "center", justifyContent: "center",
              cursor: "zoom-out",
            }}
          >
            <img
              src={this.state.lightboxUrl}
              alt="sample enlarged"
              onClick={(e) => e.stopPropagation()}
              style={{
                maxWidth: "90vw", maxHeight: "90vh",
                objectFit: "contain",
                borderRadius: 6,
                boxShadow: "0 8px 40px rgba(0,0,0,0.5)",
              }}
            />
            <span
              onClick={() => this.setState({ lightboxUrl: null })}
              style={{
                position: "fixed", top: 18, right: 24,
                color: "#fff", fontSize: 28, cursor: "pointer",
                lineHeight: 1, userSelect: "none",
              }}
            >✕</span>
          </div>
        )}

        {this.state.billingDialogLine && (
          <POLineBillingDialog
            poId={this.getEffectiveData().purchaseOrderId}
            line={this.state.billingDialogLine}
            productId={this.state.billingDialogLine.product?.productId}
            onClose={() => this.setState({ billingDialogLine: null })}
            onSaved={this.onBillingSaved}
          />
        )}
      </div>
    );
  }
}

export default withSnackbar(Details);