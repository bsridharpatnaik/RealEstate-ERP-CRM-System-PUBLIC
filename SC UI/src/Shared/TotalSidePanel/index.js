import React, { Component } from "react";
import IconButton from "@material-ui/core/IconButton";
import Clear from "@material-ui/icons/Clear";
import { messages } from "./../../messages";
import converter from "json-2-csv";
import IconButtons from "../Button/IconButtons";

class Total extends Component {
  async exportToCSV() {
    let json2csvCallback = (err, csv) => {
      if (err) throw err;

      var exportedFilenmae = "Total.csv";

      var blob = new Blob([csv], { type: "text/csv;charset=utf-8;" });
      if (navigator.msSaveBlob) {
        // IE 10+
        navigator.msSaveBlob(blob, exportedFilenmae);
      } else {
        var link = document.createElement("a");
        if (link.download !== undefined) {
          // feature detection
          // Browsers that support HTML5 download attribute
          var url = URL.createObjectURL(blob);
          link.setAttribute("href", url);
          link.setAttribute("download", exportedFilenmae);
          link.style.visibility = "hidden";
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
        }
      }
    };

    converter.json2csv(this.props.totals.map(({ productname, measurementUnit, quantity }) => ({ productname, measurementUnit, quantity })), json2csvCallback);
  }
  renderExport() {
    return (
      <IconButtons
        onClick={() => {
          this.exportToCSV();
        }}
        buttonClass="download"
        label={messages.common.export}
        icon={"DownloadSVG"}
      />
    );
  }
  render() {
    return (
      <div className="list-section detail-section">
        <div className="details-header">
          {messages.common.total}
          <div>
            {this.renderExport()}
            <IconButton
              aria-label="back"
              onClick={() => this.props.close()}
              className="back-icon"
            >
              <Clear />
            </IconButton>
          </div>
        </div>
        <div className="details-main-content" style={{ maxHeight: 'calc(100vh - 150px)' }}>
          <table>
            <thead>
              <tr>
                <th>{messages.common.inventory}</th>
                <th>{messages.common.unit}</th>
                <th>{messages.common.total}</th>
              </tr>
            </thead>{" "}
            {this.props.totals.map((total) => {
              return (
                <tr>
                  <td>{total.productname}</td>
                  <td>{total.measurementUnit}</td>
                  <td>{total.quantity}</td>
                </tr>
              );
            })}
          </table>
        </div>
      </div>
    );
  }
}

export default Total;
