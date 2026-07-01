import React from "react";
import CommonDetails from "./../../Shared/Details";
import IconButton from "@material-ui/core/IconButton";
import { CloseIcon } from "./../../Shared/Icons/Index.js";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import TextField from "@material-ui/core/TextField";
import Button from "@material-ui/core/Button";
import DatePicker from "./../../Shared/Date";
import { withSnackbar } from "notistack";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import { canEditInventoryModules } from "./../../helper";
import PrintIcon from "@material-ui/icons/Print";
import Add from "./add";

const STATUS_COLORS = {
  NEW: { background: "#e3f2fd", color: "#1565c0" },
  COMPLETED: { background: "#e8f5e9", color: "#2e7d32" },
  CANCELLED: { background: "#ffebee", color: "#c62828" },
};

class Details extends CommonDetails {
  state = {
    cancelDialogOpen: false,
    cancelReason: "",
    completeDialogOpen: false,
    completeLineData: {}, // { [lineId]: { warrantyTill, nextServiceDate } }
    actionLoading: false,
    isEditing: false,
  };

  openCompleteDialog = () => {
    const lines = this.props.data?.lines || [];
    const completeLineData = {};
    lines.forEach((l) => {
      completeLineData[l.id] = {
        warrantyTill: l.warrantyTill || null,
        nextServiceDate: l.nextServiceDate || null,
      };
    });
    this.setState({ completeDialogOpen: true, completeLineData });
  };

  setLineField = (lineId, field, value) => {
    this.setState((prev) => ({
      completeLineData: {
        ...prev.completeLineData,
        [lineId]: { ...prev.completeLineData[lineId], [field]: value },
      },
    }));
  };

  handleMarkComplete = async () => {
    this.setState({ actionLoading: true });
    const lineWarranties = Object.entries(this.state.completeLineData).map(([lineId, fields]) => ({
      lineId: Number(lineId),
      warrantyTill: fields.warrantyTill || null,
      nextServiceDate: fields.nextServiceDate || null,
    }));
    const response = await API.POST(apiEndpoints.completeServiceOrder(this.props.data.serviceOrderId), {
      lineWarranties,
    });
    this.setState({ actionLoading: false, completeDialogOpen: false });
    if (response.success) {
      this.props.enqueueSnackbar("Service Order marked complete", { variant: "success" });
      this.props.onRefresh && this.props.onRefresh();
      this.props.close && this.props.close();
    } else {
      this.props.enqueueSnackbar(response.errorMessage || "Failed to mark complete", { variant: "error" });
    }
  };

  handleCancel = async () => {
    this.setState({ actionLoading: true });
    const response = await API.POST(apiEndpoints.cancelServiceOrder(this.props.data.serviceOrderId), {
      reason: this.state.cancelReason || null,
    });
    this.setState({ actionLoading: false, cancelDialogOpen: false });
    if (response.success) {
      this.props.enqueueSnackbar("Service Order cancelled", { variant: "success" });
      this.props.onRefresh && this.props.onRefresh();
      this.props.close && this.props.close();
    } else {
      this.props.enqueueSnackbar(response.errorMessage || "Failed to cancel", { variant: "error" });
    }
  };

  handleEditSaved = () => {
    this.setState({ isEditing: false });
    this.props.onRefresh && this.props.onRefresh();
    this.props.close && this.props.close();
  };

  handlePrintPdf = () => {
    const soId = this.props.data?.serviceOrderId;
    if (!soId) return;
    const url = `${window.location.origin}${apiEndpoints.printServiceOrderPdf(soId)}`;
    const a = document.createElement("a");
    a.href = url;
    a.download = `${soId}.pdf`;
    a.target = "_blank";
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
  };

  render() {
    if (this.state.isEditing) {
      return (
        <Add
          editData={this.props.data}
          back={() => this.setState({ isEditing: false })}
          onSaved={this.handleEditSaved}
        />
      );
    }

    const data = this.props.data || {};
    const lines = data.lines || [];
    const statusStyle = STATUS_COLORS[data.status] || {};
    const canAct = canEditInventoryModules() && data.status === "NEW";

    return (
      <div className="purchase-order-detail-section">
      <div className="po-detail-card">
        <div className="details-header">
          <div className="po-header-left">
            <div className="po-number">SO Number: {data.serviceOrderId || ""}</div>
            <span className="spl-po-badge" style={{ background: "#f0fbff", color: "#0277bd" }}>SERVICE ORDER</span>
          </div>
          <IconButton onClick={this.props.close}>{CloseIcon({})}</IconButton>
        </div>

        <div style={{ padding: "12px 16px" }}>
          <span style={{ ...statusStyle, padding: "4px 12px", borderRadius: 12, fontSize: 13, fontWeight: 600 }}>
            {data.status}
          </span>
          {data.status === "CANCELLED" && data.cancelReason && (
            <div style={{ marginTop: 8, color: "#c62828", fontSize: 13 }}>Reason: {data.cancelReason}</div>
          )}
        </div>

        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px 32px", padding: "0 16px 16px" }}>
          {[
            { label: "Service Date", value: data.serviceDate || "-" },
            { label: "Project", value: data.projectName || "-" },
            { label: "Subject", value: data.subject || "-" },
            { label: "Created By", value: data.createdBy || "-" },
            { label: "Next Service Date", value: data.nextServiceDate || "-", highlight: data.nextServiceDate && data.status !== "CANCELLED" },
          ].map(({ label, value, highlight }) => (
            <div key={label}>
              <div style={{ fontSize: 11, fontWeight: 600, color: "#8a94a6", textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: 3 }}>{label}</div>
              <div style={{ fontSize: 14, color: highlight ? "#c62828" : "#2d3748", fontWeight: highlight ? 600 : 400 }}>{value}</div>
            </div>
          ))}
          <div>
            <div style={{ fontSize: 11, fontWeight: 600, color: "#8a94a6", textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: 3 }}>Vendor</div>
            <div style={{ fontSize: 14, color: "#2d3748", fontWeight: 500 }}>{data.vendor?.name || "-"}</div>
            {data.vendor && (data.vendor.contactPerson || data.vendor.contactPersonMobileNo || data.vendor.mobileNo) && (
              <div style={{ fontSize: 12, color: "#6b7280", marginTop: 2 }}>
                {[data.vendor.contactPerson, data.vendor.contactPersonMobileNo || data.vendor.mobileNo].filter(Boolean).join(" · ")}
              </div>
            )}
            {data.vendor && [data.vendor.addr_line1, data.vendor.addr_line2, data.vendor.city, data.vendor.state, data.vendor.zip].filter(Boolean).length > 0 && (
              <div style={{ fontSize: 12, color: "#6b7280" }}>
                {[data.vendor.addr_line1, data.vendor.addr_line2, data.vendor.city, data.vendor.state, data.vendor.zip].filter(Boolean).join(", ")}
              </div>
            )}
          </div>
          <div>
            <div style={{ fontSize: 11, fontWeight: 600, color: "#8a94a6", textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: 3 }}>Firm</div>
            <div style={{ fontSize: 14, color: "#2d3748", fontWeight: 500 }}>{data.firm?.firmName || "-"}</div>
            {data.firm && [data.firm.addr_line1, data.firm.addr_line2, data.firm.city, data.firm.state, data.firm.zip].filter(Boolean).length > 0 && (
              <div style={{ fontSize: 12, color: "#6b7280", marginTop: 2 }}>
                {[data.firm.addr_line1, data.firm.addr_line2, data.firm.city, data.firm.state, data.firm.zip].filter(Boolean).join(", ")}
              </div>
            )}
            {data.firm && (data.firm.firmGstNumber || data.firm.firmContactNumber) && (
              <div style={{ fontSize: 12, color: "#6b7280" }}>
                {[data.firm.firmGstNumber && `GST: ${data.firm.firmGstNumber}`, data.firm.firmContactNumber].filter(Boolean).join(" · ")}
              </div>
            )}
          </div>
          {data.specialDiscount > 0 && (
            <div>
              <div style={{ fontSize: 11, fontWeight: 600, color: "#8a94a6", textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: 3 }}>Special Discount</div>
              <div style={{ fontSize: 14, color: "#2d3748" }}>₹ {Number(data.specialDiscount).toFixed(2)}</div>
            </div>
          )}
          {(data.overridePhoneNumber || data.overrideEmail) && (
            <div style={{ gridColumn: "1 / -1" }}>
              <div style={{ fontSize: 11, fontWeight: 600, color: "#8a94a6", textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: 3 }}>Override Contact</div>
              <div style={{ fontSize: 14, color: "#2d3748" }}>{[data.overridePhoneNumber, data.overrideEmail].filter(Boolean).join(" · ")}</div>
            </div>
          )}
          {data.notes && (
            <div style={{ gridColumn: "1 / -1" }}>
              <div style={{ fontSize: 11, fontWeight: 600, color: "#8a94a6", textTransform: "uppercase", letterSpacing: "0.5px", marginBottom: 3 }}>Notes</div>
              <div style={{ fontSize: 14, color: "#2d3748" }} dangerouslySetInnerHTML={{ __html: data.notes }} />
            </div>
          )}
        </div>

        <div style={{ padding: "0 16px 16px" }}>
          <h3 className="section-title">Service Lines</h3>
          <table style={{ width: "100%", borderCollapse: "collapse" }}>
            <thead>
              <tr style={{ borderBottom: "2px solid #e0e0e0", textAlign: "left" }}>
                <th style={{ padding: "8px" }}>Description</th>
                <th style={{ padding: "8px" }}>Type / Asset</th>
                <th style={{ padding: "8px" }}>Qty</th>
                <th style={{ padding: "8px" }}>Rate</th>
                <th style={{ padding: "8px" }}>Discount %</th>
                <th style={{ padding: "8px" }}>GST %</th>
                <th style={{ padding: "8px" }}>Warranty Till</th>
                <th style={{ padding: "8px" }}>Next Service</th>
                <th style={{ padding: "8px", textAlign: "right" }}>Total</th>
              </tr>
            </thead>
            <tbody>
              {lines.map((line) => (
                <tr key={line.id} style={{ borderBottom: "1px solid #f0f0f0" }}>
                  <td style={{ padding: "8px" }}>{line.description}</td>
                  <td style={{ padding: "8px" }}>
                    {[line.serviceType, line.assetTag].filter(Boolean).join(" · ") || "-"}
                    {(line.customFields || []).length > 0 && (
                      <div style={{ display: "flex", flexWrap: "wrap", gap: 4, marginTop: 4 }}>
                        {line.customFields.map((cf, i) => (
                          <span
                            key={i}
                            style={{ fontSize: 11, background: "#eef2f7", color: "#444", borderRadius: 10, padding: "1px 8px" }}
                          >
                            {cf.fieldLabel}: {cf.fieldValue || "-"}
                          </span>
                        ))}
                      </div>
                    )}
                  </td>
                  <td style={{ padding: "8px" }}>{line.quantity ?? "-"}</td>
                  <td style={{ padding: "8px" }}>{line.rate != null ? `₹${Number(line.rate).toFixed(2)}` : "-"}</td>
                  <td style={{ padding: "8px" }}>{line.discountPercent ? `${line.discountPercent}%` : "-"}</td>
                  <td style={{ padding: "8px" }}>{line.gstPercent ? `${line.gstPercent}%` : "-"}</td>
                  <td style={{ padding: "8px" }}>{line.warrantyTill || "-"}</td>
                  <td style={{ padding: "8px" }}>{line.nextServiceDate || "-"}</td>
                  <td style={{ padding: "8px", textAlign: "right" }}>{line.totalAmount != null ? `₹${Number(line.totalAmount).toFixed(2)}` : "-"}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div style={{ textAlign: "right", marginTop: 12, fontWeight: 600 }}>
            Grand Total: ₹ {data.grandTotal != null ? Number(data.grandTotal).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : "0.00"}
          </div>
        </div>

        {Array.from(data.fileInformations || []).length > 0 && (
          <div style={{ padding: "0 16px 16px" }}>
            {this.renderFileList(Array.from(data.fileInformations))}
          </div>
        )}

        <div style={{ padding: "0 16px 16px", display: "flex", gap: 12, flexWrap: "wrap" }}>
          <Button
            variant="outlined"
            startIcon={<PrintIcon />}
            onClick={this.handlePrintPdf}
          >
            Print PDF
          </Button>
          {canAct && (
            <>
              <Button
                variant="contained"
                color="primary"
                disabled={this.state.actionLoading}
                onClick={() => this.setState({ isEditing: true })}
              >
                Edit
              </Button>
              <Button
                variant="outlined"
                style={{ color: "#2e7d32", borderColor: "#2e7d32" }}
                disabled={this.state.actionLoading}
                onClick={this.openCompleteDialog}
              >
                Mark Complete
              </Button>
              <Button
                variant="outlined"
                style={{ color: "#c62828", borderColor: "#c62828" }}
                disabled={this.state.actionLoading}
                onClick={() => this.setState({ cancelDialogOpen: true, cancelReason: "" })}
              >
                Cancel
              </Button>
            </>
          )}
        </div>

        <Dialog open={this.state.completeDialogOpen} onClose={() => this.setState({ completeDialogOpen: false })} maxWidth="sm" fullWidth>
          <DialogTitle>Mark Service Order Complete</DialogTitle>
          <DialogContent>
            <p style={{ fontSize: 13, color: "#666", marginTop: 0 }}>
              Optionally record warranty and next service date per line.
            </p>
            {(this.props.data?.lines || []).map((line) => {
              const lineData = this.state.completeLineData[line.id] || {};
              return (
                <div key={line.id} style={{ marginBottom: 16, padding: "12px 14px", background: "#f8f9fb", borderRadius: 6, border: "1px solid #e8e8e8" }}>
                  <div style={{ fontSize: 13, fontWeight: 600, color: "#2d3748", marginBottom: 10 }}>{line.description}</div>
                  <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12 }}>
                    <DatePicker
                      label="Warranty Till (optional)"
                      type="date"
                      emptyDate
                      defaultValue={lineData.warrantyTill || null}
                      onChange={(date) => this.setLineField(line.id, "warrantyTill", date)}
                    />
                    <DatePicker
                      label="Next Service Date (optional)"
                      type="date"
                      emptyDate
                      defaultValue={lineData.nextServiceDate || null}
                      onChange={(date) => this.setLineField(line.id, "nextServiceDate", date)}
                    />
                  </div>
                </div>
              );
            })}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => this.setState({ completeDialogOpen: false })}>Back</Button>
            <Button color="primary" disabled={this.state.actionLoading} onClick={this.handleMarkComplete}>
              Confirm Complete
            </Button>
          </DialogActions>
        </Dialog>

        <Dialog open={this.state.cancelDialogOpen} onClose={() => this.setState({ cancelDialogOpen: false })}>
          <DialogTitle>Cancel Service Order</DialogTitle>
          <DialogContent>
            <TextField
              autoFocus
              label="Reason (optional)"
              fullWidth
              multiline
              variant="outlined"
              value={this.state.cancelReason}
              onChange={(e) => this.setState({ cancelReason: e.target.value })}
            />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => this.setState({ cancelDialogOpen: false })}>Back</Button>
            <Button color="primary" disabled={this.state.actionLoading} onClick={this.handleCancel}>
              Confirm Cancel
            </Button>
          </DialogActions>
        </Dialog>
      </div>
      </div>
    );
  }
}

export default withSnackbar(Details);
