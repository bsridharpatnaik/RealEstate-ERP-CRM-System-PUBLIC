import React, { Component } from "react";
import { connect } from "react-redux";
import "./printstyle.scss";

class Print extends Component {
  renderInwardTypeBadge(data) {
    let label, color, background;
    if (data.isSampleInward) {
      label = "Sample Inward"; color = "#6a1b9a"; background = "#f3e5f5";
    } else if (data.createdFromPO) {
      label = "From PO"; color = "#1565c0"; background = "#e3f2fd";
    } else {
      label = "Direct Inward"; color = "#2e7d32"; background = "#e8f5e9";
    }
    return (
      <span className="mrn-badge" style={{ color, backgroundColor: background, border: `1px solid ${color}` }}>
        {label}
      </span>
    );
  }

  renderData() {
    const data = this.props.data;
    const items = data.inwardOutwardList || [];
    const rejectList = data.rejectInwardList || [];

    const tenantCode = this.props.tenantCode || this.props.tennant;
    let selectenant = this.props.allTenant.filter(t => t.tenantCode === tenantCode);
    selectenant = selectenant[0] ? selectenant[0] : this.props.tennant;

    const showIndentCol = !!data.createdFromPO;

    return (
      <>
        {/* Title Block */}
        <div className="mrn-title-block">
          <div className="mrn-app-name">{process.env.REACT_APP_NAME}</div>
          <div className="mrn-tenant-name">{selectenant.tenantName}</div>
          <div className="mrn-doc-title">MATERIAL RECEIPT NOTE</div>
        </div>

        {/* Info Panel */}
        <div className="mrn-info-panel">
          <div className="mrn-info-col">
            <div className="mrn-info-row">
              <span className="mrn-label">Inward ID:</span>
              <span className="mrn-value">{data.inwardId || "-"}</span>
            </div>
            <div className="mrn-info-row">
              <span className="mrn-label">Inward Type:</span>
              <span className="mrn-value">{this.renderInwardTypeBadge(data)}</span>
            </div>
            <div className="mrn-info-row">
              <span className="mrn-label">Supplier:</span>
              <span className="mrn-value">{data.supplier?.name || "-"}</span>
            </div>
            <div className="mrn-info-row">
              <span className="mrn-label">Receiving Date:</span>
              <span className="mrn-value">{data.date || "-"}</span>
            </div>
            <div className="mrn-info-row">
              <span className="mrn-label">MRN/GRN No:</span>
              <span className="mrn-value">{data.ourSlipNo || "-"}</span>
            </div>
            <div className="mrn-info-row">
              <span className="mrn-label">Additional Info:</span>
              <span className="mrn-value">{data.additionalInfo || "-"}</span>
            </div>
          </div>
          <div className="mrn-info-col">
            <div className="mrn-info-row">
              <span className="mrn-label">PO Number:</span>
              <span className="mrn-value">{data.purchaseOrderNo || "-"}</span>
            </div>
            <div className="mrn-info-row">
              <span className="mrn-label">PO Date:</span>
              <span className="mrn-value">{data.purchaseOrderDate || "-"}</span>
            </div>
            <div className="mrn-info-row">
              <span className="mrn-label">Challan No:</span>
              <span className="mrn-value">{data.challanNo || "-"}</span>
            </div>
            <div className="mrn-info-row">
              <span className="mrn-label">Challan Date:</span>
              <span className="mrn-value">{data.challanDate || "-"}</span>
            </div>
            <div className="mrn-info-row">
              <span className="mrn-label">Bill No:</span>
              <span className="mrn-value">{data.billNo || "-"}</span>
            </div>
            <div className="mrn-info-row">
              <span className="mrn-label">Bill Date:</span>
              <span className="mrn-value">{data.billDate || "-"}</span>
            </div>
            <div className="mrn-info-row">
              <span className="mrn-label">Vehicle No:</span>
              <span className="mrn-value">{data.vehicleNo || "-"}</span>
            </div>
          </div>
        </div>

        {/* Items Table */}
        <div className="mrn-section-title">Received Items</div>
        <table className="mrn-table">
          <thead>
            <tr>
              <th className="col-no">SR.</th>
              <th className="col-warehouse">WAREHOUSE</th>
              {showIndentCol && <th className="col-indent">INDENT NO</th>}
              <th className="col-desc">DESCRIPTION</th>
              <th className="col-uom">UOM</th>
              <th className="col-qty">QUANTITY</th>
              <th className="col-brand">BRAND</th>
              <th className="col-expiry">EXPIRY DATE</th>
            </tr>
          </thead>
          <tbody>
            {items.map((item, index) => (
              <tr key={index}>
                <td className="col-no">{index + 1}</td>
                <td className="col-warehouse">{item.warehouse?.warehouseName || "-"}</td>
                {showIndentCol && <td className="col-indent">{item.indentId || "-"}</td>}
                <td className="col-desc">
                  {item.product.productName}
                  {item.indentRemarks && (
                    <div className="mrn-sub-text"><strong>Remarks:</strong> {item.indentRemarks}</div>
                  )}
                  {item.indentSpecification && (
                    <div className="mrn-sub-text"><strong>Spec:</strong> {item.indentSpecification}</div>
                  )}
                </td>
                <td className="col-uom">{item.product.measurementUnit}</td>
                <td className="col-qty">{item.quantity}</td>
                <td className="col-brand">{item.brand || '-'}</td>
                <td className="col-expiry">{item.expiryDate ? new Date(item.expiryDate).toLocaleDateString('en-GB') : '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* Reject Table */}
        {rejectList.length > 0 && (
          <>
            <div className="mrn-section-title mrn-section-title--reject">Rejected Items</div>
            <table className="mrn-table">
              <thead>
                <tr>
                  <th className="col-no">SR.</th>
                  <th className="col-date">DATE</th>
                  <th className="col-desc">DESCRIPTION</th>
                  <th className="col-uom">UOM</th>
                  <th className="col-qty">OLD QUANTITY</th>
                  <th className="col-qty">REJECT QUANTITY</th>
                </tr>
              </thead>
              <tbody>
                {rejectList.map((item, index) => (
                  <tr key={index}>
                    <td className="col-no">{index + 1}</td>
                    <td className="col-date">{item.rejectDate || "-"}</td>
                    <td className="col-desc">{item.product.productName}</td>
                    <td className="col-uom">{item.product.measurementUnit}</td>
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
      <div className="debit-note inward-print" id="inward-print">
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
          <div>STORE KEEPER<div>{this.props.data.createdBy || ""}</div></div>
          <div>SUPPLIER</div>
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
