import React, { Component } from "react";
import { connect } from "react-redux";
import "./style.scss";

class DebitNotePrint extends Component {
  /* ─── MOR-specific row renderers (unchanged) ─── */
  rendermor() {
    const data = this.props.data;
    const mode = data.mode;
    let diffVal = "";
    let diffText = "";
    if (mode === "TripCount") {
      diffText = "No of Trips";
      diffVal = data.noOfTrips;
    } else if (mode === "Daily") {
      diffText = "DAYS DIFFERENCE";
      diffVal = new Date(data["endDate"]) - new Date(data["startDate"]);
      diffVal = diffVal / (24 * 3600 * 1000);
      diffVal = diffVal + 1;
    } else if (mode === "Hourly") {
      diffText = "TIME DIFFERENCE";
      diffVal = new Date(data["endDateTime"]) - new Date(data["startDateTime"]);
      diffVal = diffVal / (3600 * 1000);
    } else if (mode === "MeterReading") {
      diffText = "METER DIFFERENCE";
      diffVal = data["endMeterReading"] - data["initialMeterReading"];
    }
    return (
      <>
        <tr>
          <th className="no">SR. NO.</th>
          <th>{diffText}</th>
          <th className="notes">Additional Notes</th>
          <th className="rate">RATE</th>
          <th className="amount">AMOUNT</th>
          <th className="remarks">REMARKS</th>
        </tr>
        <tr>
          <td className="no">{1}</td>
          <td>{diffVal}</td>
          <td>{data.additionalNotes}</td>
          <td className="rate">{data.rate}</td>
          <td className="amount">{data.amountCharged}</td>
          <td className="remarks"></td>
        </tr>
      </>
    );
  }

  getAddress(client) {
    let address = "";
    if (client.city) address += " " + client.city;
    if (client.state) address += " " + client.state;
    if (client.zip) address += " " + client.zip;
    return address;
  }

  renderMahavirRow() {
    let type = "";
    let selectenant = this.props.allTenant.filter(
      (t) => t.tenantCode === this.props.tennant
    );
    selectenant = selectenant[0] ? selectenant[0] : this.props.tennant;
    if (this.props.mor) type = "Machinery On Rent";
    return (
      <>
        <tr><td colSpan="7" className="middle">{process.env.REACT_APP_NAME}</td></tr>
        <tr><td colSpan="7" className="middle">{selectenant.tenantName}</td></tr>
        <tr><td colSpan="7" className="middle">{type}</td></tr>
      </>
    );
  }

  renderAddRow() {
    let client = {},
      row1Text = "", row2Text = "", row3Text = "", row4Text = "", row5Text = "",
      row1Value = "", row2Value = "", row3Value = "", row4Value = "", row5Value = "";
    const data = this.props.data;
    if (this.props.mor) {
      client = data.contractor || {};
      row1Text = "Machinery"; row2Text = "Vehicle No"; row3Text = "Mode";
      row4Text = data.mode === "TripCount" ? "No of Trips" : "To ";
      row5Text = data.mode === "TripCount" ? "" : "From";
      row1Value = data.machinery.machineryName;
      row2Value = data.vehicleNo;
      row3Value = data.mode;
      row4Value = data.noOfTrips || data.startDate || data.startDateTime || data.initialMeterReading;
      row5Value = data.endDate || data.endDateTime || data.endMeterReading;
    }
    const add = this.getAddress(client);
    return (
      <>
        <tr>
          <td className="width40">Contractor: {client.name}</td>
          <td className="width20"></td>
          <td className="width20">{row1Text}</td>
          <td className="width20">{row1Value}</td>
        </tr>
        <tr>
          <td className="width40">{client.addr_line1}</td>
          <td className="width20"></td>
          <td className="width20">{row2Text}</td>
          <td className="width20">{row2Value}</td>
        </tr>
        <tr>
          <td className="width40">{client.addr_line2}</td>
          <td className="width20"></td>
          <td className="width20">{row3Text}</td>
          <td className="width20">{row3Value}</td>
        </tr>
        <tr>
          <td className="width40">{add}</td>
          <td className="width20"></td>
          <td className="width20">{row4Text}</td>
          <td className="width20">{row4Value}</td>
        </tr>
        <tr>
          <td className="width40"></td>
          <td className="width20"></td>
          <td className="width20">{row5Text}</td>
          <td className="width20">{row5Value}</td>
        </tr>
      </>
    );
  }

  renderMorData() {
    const data = this.props.data;
    return (
      <>
        <table className="top-table">{this.renderMahavirRow()}</table>
        <table>{this.renderAddRow()}</table>
        <table>
          {this.rendermor()}
        </table>
      </>
    );
  }

  /* ─── Professional layout for Reject / Return prints ─── */
  getDocTitle() {
    if (this.props.inwardReject) return "INWARD REJECT NOTE";
    if (this.props.ourwardReject) return "DEBIT NOTE";
    if (this.props.outwardReturn) return "OUTWARD RETURN NOTE";
    return "DEBIT NOTE";
  }

  renderProfessionalData(selectenant) {
    const data = this.props.data;
    const docTitle = this.getDocTitle();

    let items = [];
    let dateKey = "rejectDate";
    let qtyLabel = "REJECT QTY";
    let qtyKey = "rejectQuantity";
    let showRemarks = true;

    if (this.props.inwardReject) {
      items = data.rejectInwardList || [];
    } else if (this.props.ourwardReject) {
      items = data.rejectOutwardList || [];
    } else if (this.props.outwardReturn) {
      items = data.returnOutwardList || [];
      dateKey = "returnDate";
      qtyLabel = "RETURNED QTY";
      qtyKey = "returnQuantity";
      showRemarks = false;
    }

    /* Info panel fields */
    let client, clientLabel, leftRows, rightRows;
    if (this.props.inwardReject) {
      client = data.supplier || {};
      clientLabel = "Supplier";
      leftRows = [
        { label: "Supplier", value: client.name },
        client.addr_line1 ? { label: "Address", value: [client.addr_line1, client.addr_line2].filter(Boolean).join(", ") } : null,
      ].filter(Boolean);
      rightRows = [
        { label: "PO Date",        value: data.purchaseOrderDate || data.purchaseOrderdate },
        { label: "PO Number",      value: data.purchaseOrderNo   || data.purchaseOrder },
        { label: "MRN Number",     value: data.ourSlipNo },
        { label: "Receiving Date", value: data.date },
      ];
    } else {
      client = data.contractor || {};
      clientLabel = "Contractor";
      leftRows = [
        { label: "Contractor", value: client.name },
        client.addr_line1 ? { label: "Address", value: [client.addr_line1, client.addr_line2].filter(Boolean).join(", ") } : null,
      ].filter(Boolean);
      rightRows = [
        { label: "Outward Date",  value: data.date },
        { label: "Slip No",       value: data.slipNo },
        { label: "Structure", value: data.usageLocation?.locationName },
      ];
    }

    return (
      <>
        {/* Title */}
        <div className="dn-title-block">
          <div className="dn-app-name">{process.env.REACT_APP_NAME}</div>
          <div className="dn-tenant-name">{selectenant.tenantName}</div>
          <div className="dn-doc-title">{docTitle}</div>
        </div>

        {/* Info Panel */}
        <div className="dn-info-panel">
          <div className="dn-info-col">
            {leftRows.map((r, i) => (
              <div className="dn-info-row" key={i}>
                <span className="dn-label">{r.label}:</span>
                <span className="dn-value">{r.value || "-"}</span>
              </div>
            ))}
          </div>
          <div className="dn-info-col">
            {rightRows.map((r, i) => (
              r.label ? (
                <div className="dn-info-row" key={i}>
                  <span className="dn-label">{r.label}:</span>
                  <span className="dn-value">{r.value || "-"}</span>
                </div>
              ) : null
            ))}
          </div>
        </div>

        {/* Section Title */}
        <div className="dn-section-title">{docTitle}</div>

        {/* Items Table */}
        <table className="dn-table">
          <thead>
            <tr>
              <th className="col-no">SR.</th>
              <th className="col-date">DATE</th>
              <th className="col-desc">DESCRIPTION</th>
              <th className="col-uom">UOM</th>
              <th className="col-qty">OLD QTY</th>
              <th className="col-qty">{qtyLabel}</th>
              {showRemarks && <th className="col-remarks">REMARKS</th>}
            </tr>
          </thead>
          <tbody>
            {items.map((item, index) => (
              <tr key={index}>
                <td className="col-no">{index + 1}</td>
                <td className="col-date">{item[dateKey] || "-"}</td>
                <td className="col-desc">{item.product.productName}</td>
                <td className="col-uom">{item.product.measurementUnit}</td>
                <td className="col-qty">{item.oldQuantity}</td>
                <td className="col-qty">{item[qtyKey]}</td>
                {showRemarks && <td className="col-remarks">{item.remarks || "-"}</td>}
              </tr>
            ))}
          </tbody>
        </table>
      </>
    );
  }

  render() {
    /* MOR: keep original boring-but-functional layout */
    if (this.props.mor) {
      return (
        <div className="debit-note" id="debit-note">
          <div className="data">{this.renderMorData()}</div>
          <div className="footer">
            <div>AUTHORISED SIGNATURE <div>{process.env.REACT_APP_NAME}</div></div>
            <div>STORE KEEPER</div>
            <div>Receiver</div>
            <div></div>
            <div className="footer-note4"></div>
          </div>
        </div>
      );
    }

    let selectenant = this.props.allTenant.filter(
      (t) => t.tenantCode === this.props.tennant
    );
    selectenant = selectenant[0] ? selectenant[0] : this.props.tennant;

    return (
      <div className="debit-note dn-professional" id="debit-note">
        <div className="logo-container">
          <img
            src={`${process.env.PUBLIC_URL}/${process.env.REACT_APP_LOGIN_LOGO}.png`}
            alt="Logo"
            className="logo"
          />
        </div>
        <div className="data">{this.renderProfessionalData(selectenant)}</div>
        <div className="dn-footer">
          <div>AUTHORISED SIGNATURE<div>{process.env.REACT_APP_NAME}</div></div>
          <div>STORE KEEPER</div>
          <div>{this.props.inwardReject ? "SUPPLIER" : "CONTRACTOR"}</div>
          <div></div>
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state) => ({
  tennant: state.tennant.tennant_id,
  allTenant: state.allTennant.tennants,
});

export default connect(mapStateToProps, null, null, { forwardRef: true })(DebitNotePrint);
