import React, { Component } from "react";
import TextField from "@material-ui/core/TextField";
import Button from "@material-ui/core/Button";
import MenuItem from "@material-ui/core/MenuItem";

const STATUS_OPTIONS = [
  { value: "", label: "All" },
  { value: "DRAFT", label: "Draft" },
  { value: "OPEN", label: "Open" },
  { value: "PARTIALLY_FINALIZED", label: "Partially Finalized" },
  { value: "FINALIZED", label: "Finalized" },
  { value: "PARTIALLY_ORDERED", label: "Partially Ordered" },
  { value: "PO_COMPLETED", label: "PO Completed" },
  { value: "CLOSED", label: "Closed" },
  { value: "CANCELLED", label: "Cancelled" },
];

class QuoteComparisonFilter extends Component {
  state = {
    qcId: "",
    project: "",
    indentId: "",
    status: "",
    createdBy: "",
    supplierName: "",
    dateFrom: "",
    dateTo: "",
  };

  handleChange = (key) => (e) => this.setState({ [key]: e.target.value });

  handleApply = () => this.props.onApply(this.state);

  handleReset = () => {
    const empty = {
      qcId: "", project: "", indentId: "", status: "",
      createdBy: "", supplierName: "", dateFrom: "", dateTo: "",
    };
    this.setState(empty, () => this.props.onApply(empty));
  };

  render() {
    const { qcId, project, indentId, status, createdBy, supplierName, dateFrom, dateTo } = this.state;
    const f = { fullWidth: true, size: "small", variant: "outlined", style: { marginBottom: 12 } };
    return (
      <div style={{ padding: 16, minWidth: 280 }}>
        <TextField label="QC Number" value={qcId} onChange={this.handleChange("qcId")} {...f} />
        <TextField label="Project Code" value={project} onChange={this.handleChange("project")} {...f} />
        <TextField label="Indent ID" value={indentId} onChange={this.handleChange("indentId")} {...f} />
        <TextField select label="Status" value={status} onChange={this.handleChange("status")} {...f}>
          {STATUS_OPTIONS.map(o => <MenuItem key={o.value} value={o.value}>{o.label}</MenuItem>)}
        </TextField>
        <TextField label="Supplier Name" value={supplierName} onChange={this.handleChange("supplierName")} {...f} />
        <TextField label="Created By" value={createdBy} onChange={this.handleChange("createdBy")} {...f} />
        <TextField label="Date From (dd-MM-yyyy)" value={dateFrom} onChange={this.handleChange("dateFrom")} {...f} />
        <TextField label="Date To (dd-MM-yyyy)" value={dateTo} onChange={this.handleChange("dateTo")} {...f} />
        <div style={{ display: "flex", gap: 8 }}>
          <Button variant="contained" color="primary" onClick={this.handleApply} style={{ flex: 1 }}>Apply</Button>
          <Button variant="outlined" onClick={this.handleReset} style={{ flex: 1 }}>Reset</Button>
        </div>
      </div>
    );
  }
}

export default QuoteComparisonFilter;
