import React from "react";
import Table from "./../Table";
import { messages } from "./../../messages";
import ChevronRightIcon from "@material-ui/icons/ChevronRight";
import ExpandMoreIcon from "@material-ui/icons/ExpandMore";

class ExpandTable extends Table {
  renderHeader() {
    const headers = this.state.headers;
    return (
      <tr>
        <th></th>
        {headers.map((header, index) => (
          <th  key={index} onClick={() => this.sort(index)} className="sortable" >
            <span>
              {header} {this.renderSortIcon(index)}
            </span>
          </th>
        ))}
        {(!this.hideedit || !this.hidedelete) && (
          <React.Fragment>
            <th colSpan="2" className="action">
              {messages.common.action}
            </th>
          </React.Fragment>
        )}
      </tr>
    );
  }
  toggleExpand(index) {
    const expand = this.state.expand || [];
    if (expand[index]) {
      expand[index] = false;
      this.setState({ expand });
    } else {
      expand[index] = true;
      this.setState({ expand });
    }
  }
  renderBody() {
    const rows = this.state.rows || [];
    const keys = this.state.keys;
    const expand = this.state.expand || [];

    if (rows.length === 0) {
      return (
        <tr>
          <td colSpan={keys.length + 3}>{messages.common.noRecords}</td>
        </tr>
      );
    }
    return rows.map((row, index) => (
      <React.Fragment>
        <tr
          key={index}
          className={
            index === rows.length - 1
              ? "row last"
              : expand[index]
              ? "row expand"
              : "row"
          }
        >
          <td onClick={() => this.toggleExpand(index)}>
            <span className="expand-icon">
              {expand[index] ? <ExpandMoreIcon /> : <ChevronRightIcon />}
            </span>
          </td>
          {keys.map((key) => {
            return this.renderCell(key, row);
          })}
          {this.renderAction(row)}
        </tr>
        {expand[index] && this.renderExpandTable(row)}
      </React.Fragment>
    ));
  }
}

export default ExpandTable;
