import React, { useState, useRef, useLayoutEffect } from "react";
import CommonDetails from "./../../Shared/Details";
import IconButton from "@material-ui/core/IconButton";
import { messages } from "./../../messages";
import {
  EditIcon,
  DeleteIcon,
  MoreIcon,
  CloseIcon,
} from "./../../Shared/Icons/Index.js";
import DeleteConfirm from "./../../Shared//DeleteConfirm";
import Print from "./indentPrint";
import Typography from "@material-ui/core/Typography";
import Box from "@material-ui/core/Box";
import Table from "@material-ui/core/Table";
import TableBody from "@material-ui/core/TableBody";
import TableCell from "@material-ui/core/TableCell";
import TableHead from "@material-ui/core/TableHead";
import TableRow from "@material-ui/core/TableRow";
import Tooltip from "@material-ui/core/Tooltip";
import Menu from "@material-ui/core/Menu";
import MenuItem from "@material-ui/core/MenuItem";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import ArrowBackIosIcon from "@material-ui/icons/ArrowBackIos";
import ArrowForwardIosIcon from "@material-ui/icons/ArrowForwardIos";
import CheckIcon from "@material-ui/icons/Check";
import CloseIconMui from "@material-ui/icons/Close";
import PrintIcon from "@material-ui/icons/Print";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import { withSnackbar } from "notistack";
import {
  canApproveIndentRecord,
  canManagerRejectIndentRecord,
  canCancelIndentRecord,
  canResubmitIndentRecord,
} from "./../../helper";
import TextField from "@material-ui/core/TextField";
import Button from "./../../Shared/Button";

const INDENT_ACTION_STYLES = {
  CREATED:     { color: '#2e7d32', background: '#e8f5e9' },
  UPDATED:     { color: '#e65100', background: '#fff3e0' },
  APPROVED:    { color: '#1565c0', background: '#e3f2fd' },
  CANCELLED:   { color: '#c62828', background: '#ffebee' },
  DELETED:     { color: '#c62828', background: '#ffebee' },
  SPLIT:       { color: '#4e342e', background: '#efebe9' },
};

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

function StatusBadgeWithTooltip({ lineItemStatus, statusClass }) {
  const [truncated, setTruncated] = useState(false);
  const ref = useRef(null);
  useLayoutEffect(() => {
    if (ref.current && ref.current.scrollWidth > ref.current.clientWidth) {
      setTruncated(true);
    }
  }, [lineItemStatus]);
  const badge = (
    <span ref={ref} className={`status-badge-table ${statusClass}`}>
      {lineItemStatus}
    </span>
  );
  return truncated ? (
    <Tooltip title={lineItemStatus} placement="top">
      {badge}
    </Tooltip>
  ) : (
    badge
  );
}

class Details extends CommonDetails {
  // Ref to the Print component instance so we can call handlePrint() on it
  printRef = React.createRef();

  state = {
    value: 0,
    deleteConfirmOpen: false,
    approveConfirmOpen: false,
    rejectConfirmOpen: false,
    cancelConfirmOpen: false,
    anchorEl: null,
    isApproving: false,
    isRejecting: false,
    isCancelling: false,
    rejectRemarks: "",
    statusHistory: null,
    statusHistoryLoading: false,
    statusHistoryError: null,
    statusHistoryIndentId: null,
    activityLogs: [],
    activityLoading: false,
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

  deleteRow = null;

  componentDidMount() {
    this.detailTabRef = React.createRef();
  }

  componentDidUpdate(prevProps) {
    if (prevProps.data?.indentId !== this.props.data?.indentId) {
      this.setState({
        statusHistory: null,
        statusHistoryError: null,
        statusHistoryIndentId: null,
      });
      if (this.state.value === 1) {
        this.fetchStatusHistory();
        this.loadActivityLog();
      }
    }
  }

  fetchStatusHistory = async () => {
    const data = this.props.data;
    const indentId = data?.indentId;
    if (!indentId) return;
    if (this.state.statusHistoryIndentId === indentId && this.state.statusHistory !== null) return;
    this.setState({ statusHistoryLoading: true, statusHistoryError: null });
    try {
      const url = apiEndpoints.getIndentStatusHistory(indentId);
      const response = await API.GET(url);
      if (response.success) {
        const list = Array.isArray(response.data) ? response.data : [];
        this.setState({
          statusHistory: list,
          statusHistoryLoading: false,
          statusHistoryIndentId: indentId,
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

  loadActivityLog = async () => {
    const data = this.props.data;
    const indentId = data?.indentId;
    if (!indentId) return;
    this.setState({ activityLoading: true });
    const r = await API.GET(apiEndpoints.activityLogByEntity('INDENT', indentId));
    if (r.success) {
      this.setState({ activityLogs: r.data || [] });
    }
    this.setState({ activityLoading: false });
  };

  handleHistoryTabClick = () => {
    this.setState({ value: 1 });
    this.fetchStatusHistory();
    this.loadActivityLog();
  };

  handleCloseMenu = () => {
    this.setState({ anchorEl: null });
  };

  // Calls the Print component's handlePrint() method directly
  triggerPrint = () => {
    this.handleCloseMenu();
    if (this.printRef.current) {
      this.printRef.current.handlePrint();
    }
  };

  handleApprove = async () => {
    const data = this.props.data;
    if (!data || !data.indentId) {
      this.props.enqueueSnackbar("Invalid indent data", { variant: "error" });
      return;
    }
    this.setState({ isApproving: true, approveConfirmOpen: false });
    const response = await API.PATCH(
      apiEndpoints.approveIndent + data.indentId + "/approve"
    );
    if (response.success) {
      this.props.enqueueSnackbar("Indent approved successfully", { variant: "success" });
      if (this.props.goToDetails) this.props.goToDetails();
    } else {
      this.props.enqueueSnackbar(response.errorMessage || "Failed to approve indent", { variant: "error" });
    }
    this.setState({ isApproving: false });
  };

  handleReject = async () => {
    const data = this.props.data;
    if (!data || !data.indentId) {
      this.props.enqueueSnackbar("Invalid indent data", { variant: "error" });
      return;
    }
    this.setState({ isRejecting: true, rejectConfirmOpen: false });
    const body = this.state.rejectRemarks ? { remarks: this.state.rejectRemarks } : undefined;
    const response = await API.DELETE(apiEndpoints.deleteIndent + data.indentId, body);
    if (response.success) {
      this.props.enqueueSnackbar("Indent rejected successfully", { variant: "success" });
      if (this.props.goToDetails) this.props.goToDetails();
    } else {
      this.props.enqueueSnackbar(response.errorMessage || "Failed to reject indent", { variant: "error" });
    }
    this.setState({ isRejecting: false, rejectRemarks: "" });
  };

  handleResubmit = () => {
    const data = this.props.data;
    const inventoryItems = data.inventoryItems || data.inventoryList || [];
    const prefillData = {
      inventoryList: inventoryItems
        .map((item) => ({
          productId: item.product?.productId || item.productId,
          quantity: item.quantity,
          specification: item.specification || "",
          remarks: item.remarks || item.remark || "",
          measurementUnit: item.measurementUnit || item.product?.measurementUnit || "",
          needByDate: item.needByDate || null,
        }))
        .filter((item) => item.productId),
      needByDate: data.needByDate || null,
    };
    if (this.props.onResubmit) this.props.onResubmit(prefillData);
  };

  handleCancel = async () => {
    const data = this.props.data;
    if (!data || !data.indentId) {
      this.props.enqueueSnackbar("Invalid indent data", { variant: "error" });
      return;
    }
    this.setState({ isCancelling: true, cancelConfirmOpen: false });
    const response = await API.DELETE(apiEndpoints.deleteIndent + data.indentId);
    if (response.success) {
      this.props.enqueueSnackbar("Indent cancelled successfully", { variant: "success" });
      if (this.props.goToDetails) this.props.goToDetails();
    } else {
      this.props.enqueueSnackbar(response.errorMessage || "Failed to cancel indent", { variant: "error" });
    }
    this.setState({ isCancelling: false });
  };

  handlePrevious = () => {
    const { currentIndex, onNavigate } = this.props;
    if (currentIndex > 0 && onNavigate) onNavigate(currentIndex - 1);
  };

  handleNext = () => {
    const { currentIndex, allEntries, onNavigate } = this.props;
    if (allEntries && currentIndex < allEntries.length - 1 && onNavigate) onNavigate(currentIndex + 1);
  };

  render() {
    const data = this.props.data;
    if (!data) return null;

    const { currentIndex = 0, allEntries = [] } = this.props;
    const canGoPrevious = currentIndex > 0;
    const canGoNext = currentIndex < allEntries.length - 1;

    const { statusHistory, statusHistoryLoading, statusHistoryError } = this.state;

    const fromRelation = this.props.fromRelation === true;
    const indentStatusValue = data.status || "";
    const indentStatusNormalized = indentStatusValue.toLowerCase().trim();
    const showPOColumn =
      indentStatusNormalized === "po partial" ||
      indentStatusNormalized === "po completed" ||
      indentStatusNormalized === "inward partial" ||
      indentStatusNormalized === "closed";
    const showReceivedPendingColumns =
      indentStatusNormalized === "inward partial" ||
      indentStatusNormalized === "closed";
    const canShowApprove       = !fromRelation && canApproveIndentRecord(data.status);
    const canShowManagerReject = !fromRelation && canManagerRejectIndentRecord(data.status);
    const canShowCancel        = !fromRelation && canCancelIndentRecord(data.status);
    const canShowResubmit      = !fromRelation && canResubmitIndentRecord(data.status);

    return (
      <div className="list-section detail-section indent-detail-section">
        {/*
          Print component renders null but holds handlePrint().
          forwardRef:true in connect() makes this ref point to the class instance.
        */}
        <Print ref={this.printRef} data={data} />

        <div className="po-detail-card">
          <div className="details-header">
            <div className="po-header-left">
              <div className="po-number">
                Indent No: #{data.indentId || ""}
              </div>
              {!fromRelation && (
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
              {fromRelation ? (
                <IconButton
                  className="back-icon"
                  aria-label="Print"
                  onClick={this.triggerPrint}
                >
                  <PrintIcon fontSize="medium" />
                </IconButton>
              ) : (
                <>
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
                    classes={{ paper: "indent-detail-dropdown-menu" }}
                  >
                    <MenuItem onClick={this.triggerPrint}>
                      Print
                    </MenuItem>
                  </Menu>
                </>
              )}
              <IconButton onClick={this.props.close} className="back-icon">
                {CloseIcon({ fontSize: "medium" })}
              </IconButton>
            </div>
          </div>
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
            </div>
            {data.status && (
              <div
                className={`po-status-badge status-${(data.status || "")
                  .toLowerCase()
                  .replace(/\s+/g, "-")}`}
              >
                {data.status}
              </div>
            )}
          </div>
          <TabPanel value={this.state.value} index={0} className="detail-content-panel">
            <div className="detail-content" ref={this.detailTabRef}>
              <div className="detail-section-group">
                <h3 className="section-title">General Information</h3>
                <div className="detail-item">
                  <span className="detail-label">Indent Date:</span>
                  <span className="detail-value">{data.indentDate || "-"}</span>
                </div>
                {data.needByDate && (
                  <div className="detail-item">
                    <span className="detail-label">Expected Delivery:</span>
                    <span className="detail-value">
                      {data.needByDate}
                      {(() => {
                        const normalizedStatus = (data.indentStatus || data.status || "").toLowerCase().trim();
                        const isTerminal = normalizedStatus === "cancelled" || normalizedStatus === "rejected" || normalizedStatus === "po completed" || normalizedStatus === "closed" || normalizedStatus === "short closed";
                        if (isTerminal) return null;
                        const parts = data.needByDate.split("-");
                        if (parts.length !== 3) return null;
                        const deadline = new Date(parts[2], parts[1] - 1, parts[0]);
                        const today = new Date();
                        today.setHours(0, 0, 0, 0);
                        deadline.setHours(0, 0, 0, 0);
                        const days = Math.round((deadline - today) / (1000 * 60 * 60 * 24));
                        let badgeClass = "days-badge-normal";
                        if (days <= 3) badgeClass = "days-badge-critical";
                        else if (days <= 7) badgeClass = "days-badge-high";
                        else if (days <= 14) badgeClass = "days-badge-medium";
                        const label = days < 0 ? `${Math.abs(days)}d overdue` : `${days}d left`;
                        return (
                          <span className={`days-badge ${badgeClass}`} style={{ marginLeft: 8 }}>
                            {label}
                          </span>
                        );
                      })()}
                    </span>
                  </div>
                )}
                <div className="detail-item">
                  <span className="detail-label">Project:</span>
                  <span className="detail-value">{data.projectName || data.tenant || "-"}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Inventory Count:</span>
                  <span className="detail-value">{data.inventoryCount ?? 0}</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Status:</span>
                  <span className="detail-value">
                    {(() => {
                      const statusValue = data.status || "";
                      const normalizedStatus = statusValue.toLowerCase().trim();
                      let statusClass = "";
                      if (normalizedStatus === "new") statusClass = "status-new";
                      else if (normalizedStatus === "approved") statusClass = "status-approved";
                      else if (normalizedStatus === "po partial") statusClass = "status-po-partial";
                      else if (normalizedStatus === "po completed") statusClass = "status-po-completed";
                      else if (normalizedStatus === "cancelled") statusClass = "status-cancelled";
                      else if (normalizedStatus.includes("created")) statusClass = "status-indent-created";
                      else if (normalizedStatus.includes("raised")) statusClass = "status-po-raised";
                      else statusClass = `status-${normalizedStatus.replace(/\s+/g, "-")}`;
                      return (
                        <span className={`status-badge ${statusClass}`}>
                          {statusValue || "-"}
                        </span>
                      );
                    })()}
                  </span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Created By:</span>
                  <span className="detail-value">{data.createdBy || "-"}</span>
                </div>
              </div>
              {((data.inventoryItems || data.inventoryList) || []).length > 0 && (
                <div className="detail-section-group">
                  <h3 className="section-title purchase-orders-title">Inventory List</h3>
                  <div className="inventory-table-wrapper">
                    <Table>
                      <TableHead>
                        <TableRow>
                          <TableCell className="inventory-name-col">Inventory Name</TableCell>
                          <TableCell className="inventory-code-col">Inventory Code</TableCell>
                          <TableCell className="inventory-qty-col">Quantity</TableCell>
                          <TableCell className="inventory-spec-col">Specification</TableCell>
                          <TableCell className="inventory-remark-col">Remark</TableCell>
                          <TableCell className="inventory-expdate-col">Exp. Date</TableCell>
                          {showPOColumn && (
                            <TableCell className="inventory-po-col">PO Number</TableCell>
                          )}
                          <TableCell className="inventory-lineitem-col">Line Item Code</TableCell>
                          {showReceivedPendingColumns && (
                            <>
                              <TableCell className="inventory-received-col">Received Quantity</TableCell>
                              <TableCell className="inventory-pending-col">Pending Quantity</TableCell>
                            </>
                          )}
                          <TableCell className="inventory-status-col">Status</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {(data.inventoryItems || data.inventoryList || []).map((item, index) => (
                          <TableRow key={index}>
                            <TableCell className="inventory-name-cell inventory-name-col">
                              <Tooltip title={item.product?.productName || "-"} placement="top">
                                <span className="inventory-name-truncate">{item.product?.productName || "-"}</span>
                              </Tooltip>
                            </TableCell>
                            <TableCell className="inventory-code-col">
                              {item.product?.productCode || "-"}
                            </TableCell>
                            <TableCell className="inventory-qty-col">
                              {item.quantity} {item.measurementUnit || ""}
                            </TableCell>
                            <TableCell className="inventory-spec-col">
                              {item.specification || "-"}
                            </TableCell>
                            <TableCell className="inventory-remark-col">
                              {item.remarks || item.remark || "-"}
                            </TableCell>
                            <TableCell className="inventory-expdate-col">
                              {item.needByDate || "-"}
                            </TableCell>
                            {showPOColumn && (
                              <TableCell className="inventory-po-col">
                                {item.purchaseOrderId || "-"}
                              </TableCell>
                            )}
                            <TableCell className="inventory-lineitem-col">
                              {item.lineItemCode || "-"}
                            </TableCell>
                            {showReceivedPendingColumns && (
                              <>
                                <TableCell className="inventory-received-col">
                                  {item.quantityReceived !== undefined && item.quantityReceived !== null ? item.quantityReceived : "-"}
                                </TableCell>
                                <TableCell className="inventory-pending-col">
                                  {item.quantityPending !== undefined && item.quantityPending !== null ? item.quantityPending : "-"}
                                </TableCell>
                              </>
                            )}
                            <TableCell>
                              {(() => {
                                const lineItemStatus = item.lineItemStatus || "";
                                const normalizedStatus = lineItemStatus.toLowerCase().trim();
                                let statusClass = "";
                                if (normalizedStatus === "new") statusClass = "status-new";
                                else if (normalizedStatus === "po created") statusClass = "status-po-created";
                                else if (normalizedStatus === "cancelled") statusClass = "status-cancelled";
                                else if (normalizedStatus === "split") statusClass = "status-split";
                                else statusClass = `status-${normalizedStatus.replace(/\s+/g, "-")}`;
                                return lineItemStatus ? (
                                  <StatusBadgeWithTooltip lineItemStatus={lineItemStatus} statusClass={statusClass} />
                                ) : "-";
                              })()}
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                </div>
              )}
              {data.fileInformations && data.fileInformations.length > 0 && (
                <div className="detail-section-group">
                  <h3 className="section-title">{messages.common.uploadedDocuments}</h3>
                  {this.renderFileList(data.fileInformations)}
                </div>
              )}
              {/* Executive: Cancel button (only on NEW status) */}
              {canShowCancel && (
                <div className="indent-detail-tab-actions">
                  <Button
                    buttonClass="indent-reject-btn"
                    label={this.state.isCancelling ? "Cancelling..." : "Cancel Indent"}
                    onClick={() => this.setState({ cancelConfirmOpen: true })}
                    disabled={this.state.isCancelling}
                    startIcon={<CloseIconMui />}
                  />
                </div>
              )}

              {/* Executive: Re-submit button (only on REJECTED status) */}
              {canShowResubmit && (
                <div className="indent-detail-tab-actions">
                  <Button
                    buttonClass="indent-approve-btn"
                    label="Re-submit as New Indent"
                    onClick={this.handleResubmit}
                    startIcon={<CheckIcon />}
                  />
                </div>
              )}

              {/* Admin/Manager: Approve (NEW only) + Reject (any non-terminal) */}
              {(canShowApprove || canShowManagerReject) && (
                <div className="indent-detail-tab-actions">
                  {canShowManagerReject && (
                    <Button
                      buttonClass="indent-reject-btn"
                      label={this.state.isRejecting ? "Rejecting..." : "Reject"}
                      onClick={() => this.setState({ rejectConfirmOpen: true })}
                      disabled={this.state.isApproving || this.state.isRejecting}
                      startIcon={<CloseIconMui />}
                    />
                  )}
                  {canShowApprove && (
                    <Button
                      buttonClass="indent-approve-btn"
                      label={this.state.isApproving ? "Approving..." : "Approve"}
                      onClick={() => this.setState({ approveConfirmOpen: true })}
                      disabled={this.state.isApproving || this.state.isRejecting}
                      startIcon={<CheckIcon />}
                    />
                  )}
                </div>
              )}
            </div>
          </TabPanel>
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
                      <div className="history-changed-by">Changed by: {item.changedBy}</div>
                    )}
                    {item.relations && item.relations.length > 0 && this.props.onOpenRelation && (
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

            {/* Activity Log section */}
            <div style={{ marginTop: '24px', borderTop: '1px solid #e0e0e0', paddingTop: '16px' }}>
              <div style={{ fontWeight: 600, fontSize: '14px', marginBottom: '12px', color: '#444' }}>
                Activity Log
              </div>
              {this.state.activityLoading ? (
                <div className="history-loading">Loading activity log...</div>
              ) : this.state.activityLogs.length === 0 ? (
                <div className="history-empty">No activity recorded.</div>
              ) : (
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell>Time</TableCell>
                      <TableCell>Action</TableCell>
                      <TableCell>Description</TableCell>
                      <TableCell>By</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {this.state.activityLogs.map((log, i) => (
                      <TableRow key={i}>
                        <TableCell style={{ whiteSpace: 'nowrap', fontSize: '12px' }}>
                          {log.activityTime ? new Date(log.activityTime).toLocaleString() : ''}
                        </TableCell>
                        <TableCell>
                          <span style={{
                            padding: '2px 8px', borderRadius: '4px', fontWeight: 600, fontSize: '12px',
                            ...INDENT_ACTION_STYLES[log.action]
                          }}>
                            {log.action}
                          </span>
                        </TableCell>
                        <TableCell style={{ fontSize: '13px', wordBreak: 'break-word', maxWidth: '280px' }}>
                          {log.description}
                        </TableCell>
                        <TableCell style={{ fontSize: '12px' }}>{log.performedBy}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </div>
          </TabPanel>
        </div>
        <DeleteConfirm
          open={this.state.deleteConfirmOpen}
          onClose={() => this.setState({ deleteConfirmOpen: false })}
          onConfirm={() => {
            this.props.delete(data);
            this.setState({ deleteConfirmOpen: false });
          }}
        />
        <Dialog open={this.state.approveConfirmOpen} onClose={() => this.setState({ approveConfirmOpen: false })} maxWidth="xs" aria-labelledby="approve-confirmation-dialog-title" className="indent-approve-confirm-dialog">
          <DialogTitle id="approve-confirmation-dialog-title">Confirm Approval</DialogTitle>
          <DialogContent dividers>Are you sure you want to approve this indent?</DialogContent>
          <DialogActions>
            <Button buttonClass="grey" label="Cancel" onClick={() => this.setState({ approveConfirmOpen: false })} />
            <Button buttonClass="indent-approve-btn-solid" label={this.state.isApproving ? "Approving..." : "Approve"} onClick={this.handleApprove} disabled={this.state.isApproving} startIcon={<CheckIcon />} />
          </DialogActions>
        </Dialog>
        {/* Reject dialog — for Admin/Manager with optional remarks */}
        <Dialog open={this.state.rejectConfirmOpen} onClose={() => this.setState({ rejectConfirmOpen: false, rejectRemarks: "" })} maxWidth="xs" fullWidth aria-labelledby="reject-confirmation-dialog-title" className="indent-reject-confirm-dialog">
          <DialogTitle id="reject-confirmation-dialog-title">Confirm Rejection</DialogTitle>
          <DialogContent dividers>
            <div style={{ marginBottom: 12 }}>Are you sure you want to reject this indent?</div>
            <TextField
              label="Reason for rejection (optional)"
              multiline
              rows={3}
              variant="outlined"
              fullWidth
              value={this.state.rejectRemarks}
              onChange={(e) => this.setState({ rejectRemarks: e.target.value })}
            />
          </DialogContent>
          <DialogActions>
            <Button buttonClass="grey" label="Cancel" onClick={() => this.setState({ rejectConfirmOpen: false, rejectRemarks: "" })} />
            <Button buttonClass="indent-reject-btn-solid" label={this.state.isRejecting ? "Rejecting..." : "Reject"} onClick={this.handleReject} disabled={this.state.isRejecting} startIcon={<CloseIconMui />} />
          </DialogActions>
        </Dialog>

        {/* Cancel dialog — for Executive */}
        <Dialog open={this.state.cancelConfirmOpen} onClose={() => this.setState({ cancelConfirmOpen: false })} maxWidth="xs" aria-labelledby="cancel-confirmation-dialog-title" className="indent-cancel-confirm-dialog">
          <DialogTitle id="cancel-confirmation-dialog-title">Cancel Indent</DialogTitle>
          <DialogContent dividers>Are you sure you want to cancel this indent? This action cannot be undone.</DialogContent>
          <DialogActions>
            <Button buttonClass="grey" label="No" onClick={() => this.setState({ cancelConfirmOpen: false })} />
            <Button buttonClass="indent-reject-btn-solid" label={this.state.isCancelling ? "Cancelling..." : "Yes, Cancel"} onClick={this.handleCancel} disabled={this.state.isCancelling} startIcon={<CloseIconMui />} />
          </DialogActions>
        </Dialog>
      </div>
    );
  }
}

export default withSnackbar(Details);