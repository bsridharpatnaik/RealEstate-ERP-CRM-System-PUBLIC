import React, { Component } from "react";
import { connect } from "react-redux";
import "./printstyle.scss";

class Print extends Component {
  renderData() {
    const data = this.props.data;
    const items = data.inwardOutwardList || [];
    const returnList = data.returnOutwardList || [];
    const rejectList = data.rejectOutwardList || [];

    let selectenant = this.props.allTenant.filter(t => t.tenantCode === this.props.tennant);
    selectenant = selectenant[0] ? selectenant[0] : this.props.tennant;

    // Each line carries its own warehouse — show it directly, or "Multiple" when lines
    // disagree. No header-level warehouse exists.
    const distinctWarehouseNames = Array.from(new Set(
      items.map((item) => item.warehouse?.warehouseName).filter(Boolean)
    ));
    const warehouseDisplay = distinctWarehouseNames.length > 1
      ? "Multiple (see items below)"
      : (distinctWarehouseNames[0] || "-");

    return (
      <>
        {/* Title Block */}
        <div className="mis-title-block">
          <div className="mis-app-name">{process.env.REACT_APP_NAME}</div>
          <div className="mis-tenant-name">{selectenant.tenantName}</div>
          <div className="mis-doc-title">MATERIAL ISSUE SLIP</div>
        </div>

        {/* Info Panel */}
        <div className="mis-info-panel">
          <div className="mis-info-col">
            <div className="mis-info-row">
              <span className="mis-label">Contractor:</span>
              <span className="mis-value">{data.contractor?.name || "-"}</span>
            </div>
            <div className="mis-info-row">
              <span className="mis-label">Warehouse:</span>
              <span className="mis-value">{warehouseDisplay}</span>
            </div>
            <div className="mis-info-row">
              <span className="mis-label">Location:</span>
              <span className="mis-value">{data.usageLocation?.locationName || "-"}</span>
            </div>
            <div className="mis-info-row">
              <span className="mis-label">Work Area:</span>
              <span className="mis-value">{data.usageArea?.usageAreaName || "-"}</span>
            </div>
          </div>
          <div className="mis-info-col">
            <div className="mis-info-row">
              <span className="mis-label">Date:</span>
              <span className="mis-value">{data.date || "-"}</span>
            </div>
            <div className="mis-info-row">
              <span className="mis-label">Slip No:</span>
              <span className="mis-value">{data.slipNo || "-"}</span>
            </div>
            <div className="mis-info-row">
              <span className="mis-label">Purpose:</span>
              <span className="mis-value">{data.purpose || "-"}</span>
            </div>
            <div className="mis-info-row">
              <span className="mis-label">Additional Info:</span>
              <span className="mis-value">{data.additionalInfo || "-"}</span>
            </div>
          </div>
        </div>

        {/* Issued Items Table */}
        <div className="mis-section-title">Issued Items</div>
        <table className="mis-table">
          <thead>
            <tr>
              <th className="col-no">SR.</th>
              <th className="col-desc">DESCRIPTION</th>
              <th className="col-desc">WAREHOUSE</th>
              <th className="col-uom">UOM</th>
              <th className="col-qty">QUANTITY</th>
              <th className="col-qty">CLOSING STOCK</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item, index) => (
              <tr key={index}>
                <td className="col-no">{index + 1}</td>
                <td className="col-desc">{item.product.productName}</td>
                <td className="col-desc">{item.warehouse?.warehouseName || "-"}</td>
                <td className="col-uom">{item.product?.measurementUnit || "-"}</td>
                <td className="col-qty">{item.quantity}</td>
                <td className="col-qty">{item.closingStock}</td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* Returned Items Table */}
        {returnList.length > 0 && (
          <>
            <div className="mis-section-title mis-section-title--return">Returned Items</div>
            <table className="mis-table">
              <thead>
                <tr>
                  <th className="col-no">SR.</th>
                  <th className="col-date">DATE</th>
                  <th className="col-desc">DESCRIPTION</th>
                  <th className="col-uom">UOM</th>
                  <th className="col-qty">OLD QUANTITY</th>
                  <th className="col-qty">RETURNED QUANTITY</th>
                </tr>
              </thead>
              <tbody>
                {returnList.map((item, index) => (
                  <tr key={index}>
                    <td className="col-no">{index + 1}</td>
                    <td className="col-date">{item.returnDate || "-"}</td>
                    <td className="col-desc">{item.product.productName}</td>
                    <td className="col-uom">{item.product?.measurementUnit || "-"}</td>
                    <td className="col-qty">{item.oldQuantity}</td>
                    <td className="col-qty">{item.returnQuantity}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        )}

        {/* Rejected Items Table */}
        {rejectList.length > 0 && (
          <>
            <div className="mis-section-title mis-section-title--reject">Rejected Items</div>
            <table className="mis-table">
              <thead>
                <tr>
                  <th className="col-no">SR.</th>
                  <th className="col-date">DATE</th>
                  <th className="col-desc">DESCRIPTION</th>
                  <th className="col-uom">UOM</th>
                  <th className="col-qty">OLD QUANTITY</th>
                  <th className="col-qty">REJECTED QUANTITY</th>
                </tr>
              </thead>
              <tbody>
                {rejectList.map((item, index) => (
                  <tr key={index}>
                    <td className="col-no">{index + 1}</td>
                    <td className="col-date">{item.rejectDate || "-"}</td>
                    <td className="col-desc">{item.product.productName}</td>
                    <td className="col-uom">{item.product?.measurementUnit || "-"}</td>
                    <td className="col-qty">{item.oldQuantity}</td>
                    <td className="col-qty">{item.rejectQuantity}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </>
        )}
      </>
    );
  }

  render() {
    return (
      <div className="debit-note outward-print" id="outward-print">
        <div className="logo-container">
          <img
            src={`${process.env.PUBLIC_URL}/${process.env.REACT_APP_LOGIN_LOGO}.png`}
            alt="Logo"
            className="logo"
          />
        </div>
        <div className="data">{this.renderData()}</div>
        <div className="footer">
          <div>AUTHORISED SIGNATURE<div>{process.env.REACT_APP_NAME}</div></div>
          <div>STORE KEEPER</div>
          <div>CONTRACTOR</div>
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state) => ({
  tennant: state.tennant.tennant_id,
  allTenant: state.allTennant.tennants,
});

export default connect(mapStateToProps, null, null, { forwardRef: true })(Print);
