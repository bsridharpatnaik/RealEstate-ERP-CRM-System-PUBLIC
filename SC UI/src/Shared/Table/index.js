//react
import React from "react";
//third party
import ArrowDropUpIcon from "@material-ui/icons/ArrowDropUp";
import ArrowDropDownIcon from "@material-ui/icons/ArrowDropDown";

//import shared icons
import { DeleteIcon } from "./../Icons/Index.js";
import pencilIcon from "./../Icons/pencil.png";
//style
import "./style.scss";
//misc
import { messages } from "./../../messages";
import IconButton from "@material-ui/core/IconButton";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import Dialog from "@material-ui/core/Dialog";
import Button from "./../../Shared/Button";

class Table extends React.Component {
  state = {
    rows: [],
    headers: [],
    keys: [],
    deleteConfirmOpen: false,
  };
  deleteRow = null;
  componentDidMount() {
    this.keys = this.props.tableData.keys;
    this.headers = this.props.tableData.headers;
    this.hidedelete = this.props.hidedelete;
    this.hideedit = this.props.hideedit;
    this.sortKey = this.props.sortkey;
    this.sortBy = this.props.sortby;
    this.setState({
      rows: this.props.rows,
      keys: this.keys,
      headers: this.headers,
    });
  }
  componentDidUpdate(prevProps) {
    if (this.props.rows && prevProps.rows !== this.props.rows) {
      this.setState({ rows: this.props.rows });
    }
    if (prevProps.hideedit !== this.props.hideedit) {
      this.hideedit = this.props.hideedit;
    }
    if (prevProps.hidedelete !== this.props.hidedelete) {
      this.hidedelete = this.props.hidedelete;
    }
  }
  render() {
    return (
      <div className="table-wrapper">
        <table className="update-table">
          <thead>{this.renderHeader()}</thead>
          <tbody>{this.renderBody()}</tbody>
        </table>
        <Dialog
          disableBackdropClick
          disableEscapeKeyDown
          maxWidth="xs"
          aria-labelledby="confirmation-dialog-title"
          open={this.state.deleteConfirmOpen}
        >
          <DialogTitle id="confirmation-dialog-title">Confirm</DialogTitle>
          <DialogContent dividers>
            Are you sure you want to delete this record?
          </DialogContent>
          <DialogActions>
            <Button
              onClick={() => {
                this.deleteRow = null;
                this.setState({ deleteConfirmOpen: false });
              }}
              buttonClass="grey"
              label="Cancel"
            ></Button>
            <Button
              onClick={() => {
                this.props.delete(this.deleteRow);
                this.setState({ deleteConfirmOpen: false });
              }}
              buttonClass="blue"
              label="Confirm"
            ></Button>
          </DialogActions>
        </Dialog>
      </div>
    );
  }
  renderSortIcon(index) {
    const header = this.keys[index];
    if (header === this.props.sortkey && this.props.sortby === "desc") {
      return <ArrowDropDownIcon />;
    } else if (header === this.props.sortkey && this.props.sortby === "asc") {
      return <ArrowDropUpIcon />;
    }
  }
  sort(index) {
    const key = this.keys[index];
    if (key === this.sortKey) {
      if (!this.sortBy) {
        this.sortBy = "asc";
      } else if (this.sortBy === "asc") {
        this.sortBy = "desc";
      } else if (this.sortBy === "desc") {
        this.sortBy = null;
        this.sortKey = null;
      }
    } else {
      this.sortKey = key;
      this.sortBy = "asc";
    }
    this.props.search(this.sortKey, this.sortBy);
  }
  renderHeader() {
    const headers = this.state.headers || [];
    return (
      <tr>
        {headers.map((header, index) => (
          <th key={index} onClick={() => this.sort(index)} className="sortable">
            <span>
              {header} {this.renderSortIcon(index)}
            </span>
          </th>
        ))}
        {(!this.hideedit || !this.hidedelete || this.props.showDetail || (this.props.customActions && this.props.customActions.length > 0)) && (
          <React.Fragment>
            <th
              colSpan={
                (this.props.showDetail ? 3 : 0) +
                (!this.hideedit ? 1 : 0) +
                (!this.hidedelete ? 1 : 0) +
                ((this.props.customActions && this.props.customActions.length) || 0) || 1
              }
              className="action"
            >
              {messages.common.action}
            </th>
          </React.Fragment>
        )}
      </tr>
    );
  }
  renderCell(key, row, name) {
    const value = row[key] !== null ? row[key] : "";
    return <td data-label={(this.state.headers || [])[name] || key}>{`${value}`}</td>;
  }
  renderAction(row) {
    const customActions = this.props.customActions || [];
    return (
      <React.Fragment>
        {!this.hideedit && (

          <td data-label='Edit' className="action-edit">

            <IconButton
              aria-label="back"
              onClick={() => this.props.edit(row)}
              className="back-icon"
              disabled={this.checkDelete ? this.checkDelete(row) : undefined}
            >
              <img src={pencilIcon} alt="Edit" style={{ width: 15, height: 15 }} />
            </IconButton>
          </td>
        )}
        {!this.hidedelete && (
          <td data-label='Delete' className="action-delete">
            <IconButton
              aria-label="back"
              onClick={() => {
                this.deleteRow = row;
                this.setState({ deleteConfirmOpen: true });
              }}
              disabled={this.checkDelete ? this.checkDelete(row) : undefined}
              className="back-icon"
            >
              {DeleteIcon({ fontSize: "medium" })}
            </IconButton>
          </td>
        )}
        {customActions.map((action) => (
          <td key={action.key} data-label={action.title} className="action-custom">
            <IconButton
              aria-label={action.title}
              title={action.title}
              onClick={() => action.onClick(row)}
              className="back-icon"
            >
              {action.icon}
            </IconButton>
          </td>
        ))}
      </React.Fragment>
    );
  }
  renderBody() {
    const rows = this.state.rows || [];
    const keys = this.state.keys || [];
    if (rows.length === 0) {
      return (
        <tr>
          <td colSpan={keys.length + 1}>{messages.common.noRecords}</td>
        </tr>
      );
    }
    return rows.map((row, index) => (
      <tr
        key={index}
        className={index === rows.length - 1 ? "row last" : "row"}
      >
        {keys.map((key,index) => {
          return this.renderCell(key, row, index);
        })}
        {this.renderAction(row)}
      </tr>
    ));
  }
}
export default Table;
