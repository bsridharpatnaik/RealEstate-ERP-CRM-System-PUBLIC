import React, { Component } from "react";
import { connect } from "react-redux";

import "./printstyle.scss";
import { canViewMoneyFields } from "./../../helper";

class Print extends Component {
  renderData() {
    const data = this.props.data;
    const items = data.lines || [];

    // Permission: project-manager and store-incharge should NOT see any money related fields
    const showMoneyFields = canViewMoneyFields();

    return (
      <>
        <table className="top-table">{this.renderMahavirRow()}</table>
        <table>{this.renderOrderDetails()}</table>

        {items && items.length > 0 && (
          <table className="items-table">
            <tr>
              <th className="no">SR. NO.</th>
              <th className="product">PRODUCT</th>
              <th className="details">DETAILS</th>
              <th className="quantity">QUANTITY</th>
              {showMoneyFields && <th className="rate">RATE</th>}
              {showMoneyFields && <th className="gst">GST %</th>}
              {showMoneyFields && <th className="net-rate">NET RATE</th>}
              {showMoneyFields && <th className="total">TOTAL AMOUNT</th>}
            </tr>
            {items.map((item, index) => {
              const productName = item.product?.name || item.product?.productName || "-";
              const quantity = item.quantity || 0;
              const rate = parseFloat(item.rate || 0);
              const gstPercent = parseFloat(item.gstPercent || 0);
              const netRate = parseFloat(item.netRate || 0);
              const totalAmount = parseFloat(item.totalAmount || 0);

              // Build product details string
              const details = [
                item.brand && `Brand: ${item.brand}`,
                item.grade && `Grade: ${item.grade}`,
                item.diameter && `Diameter: ${item.diameter}`,
                item.specification && `Spec: ${item.specification}`
              ].filter(Boolean).join(", ");

              return (
                <tr key={index}>
                  <td className="no">{index + 1}</td>
                  <td className="product">{productName}</td>
                  <td className="details">{details || "-"}</td>
                  <td className="quantity">{quantity}</td>
                  {showMoneyFields && (
                    <>
                      <td className="rate">Rs. {rate.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                      <td className="gst">{gstPercent}%</td>
                      <td className="net-rate">Rs. {netRate.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                      <td className="total">Rs. {totalAmount.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}</td>
                    </>
                  )}
                </tr>
              );
            })}
            {showMoneyFields && data.freightCharges > 0 && (
              <>
                <tr>
                  <td colSpan="7" className="align-right">Freight Charges:</td>
                  <td className="total-amount">
                    Rs. {(data.freightCharges || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </td>
                </tr>
                <tr>
                  <td colSpan="7" className="align-right">Freight GST ({(data.freightGstPercent || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}%):</td>
                  <td className="total-amount">
                    Rs. {((data.freightCharges || 0) * (data.freightGstPercent || 0) / 100).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </td>
                </tr>
                <tr>
                  <td colSpan="7" className="align-right">Total Freight:</td>
                  <td className="total-amount">
                    Rs. {(data.totalFreightCharges || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </td>
                </tr>
              </>
            )}
            {showMoneyFields && (data.customCharges || []).filter(c => c.chargeAmount > 0).map((cc, idx) => (
              <React.Fragment key={idx}>
                <tr>
                  <td colSpan="7" className="align-right">{cc.chargeName || "Additional Charges"}:</td>
                  <td className="total-amount">
                    Rs. {(cc.chargeAmount || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </td>
                </tr>
                <tr>
                  <td colSpan="7" className="align-right">{cc.chargeName || "Additional Charges"} GST ({(cc.chargeGstPercent || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}%):</td>
                  <td className="total-amount">
                    Rs. {((cc.chargeAmount || 0) * (cc.chargeGstPercent || 0) / 100).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </td>
                </tr>
                <tr>
                  <td colSpan="7" className="align-right">Total {cc.chargeName || "Additional Charges"}:</td>
                  <td className="total-amount">
                    Rs. {(cc.totalChargeAmount || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </td>
                </tr>
              </React.Fragment>
            ))}
            {showMoneyFields && (
              <tr className="total-row">
                <td colSpan="7" className="align-right total-label">
                  GRAND TOTAL:
                </td>
                <td className="total-amount">
                  Rs. {(data.grandTotal || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </td>
              </tr>
            )}
          </table>
        )}

        {data.notes && data.notes.trim().length > 0 && (
          <div className="notes-section">
            <h4>Notes:</h4>
            <p>{data.notes}</p>
          </div>
        )}
      </>
    );
  }

  renderMahavirRow() {
    let selectenant = this.props.allTenant.filter(
      (t) => t.tenantCode === this.props.tennant
    );
    selectenant = selectenant[0] ? selectenant[0] : this.props.tennant;

    return (
      <>
        <tr>
          <td colSpan="8" className="middle">
            {process.env.REACT_APP_NAME}
          </td>
        </tr>
        <tr>
          <td colSpan="8" className="middle">
            {selectenant.tenantName}
          </td>
        </tr>
        <tr>
          <td colSpan="8" className="middle">
            PURCHASE ORDER
          </td>
        </tr>
      </>
    );
  }

  renderOrderDetails() {
    const data = this.props.data;

    return (
      <>
        <tr>
          <td>PO Number:</td>
          <td>{data.purchaseOrderId || "-"}</td>
          <td>Date:</td>
          <td>{data.poDate || "-"}</td>
        </tr>
        <tr>
          <td>Subject:</td>
          <td colSpan="3">{data.subject || "-"}</td>
        </tr>
        <tr>
          <td>Status:</td>
          <td>{data.status || "-"}</td>
          <td></td>
          <td></td>
        </tr>

        {/* Order To Section */}
        <tr>
          <td colSpan="4" className="section-header">ORDER TO</td>
        </tr>
        <tr>
          <td>Supplier Name:</td>
          <td>{data.supplier?.name || "-"}</td>
          <td>Contact Type:</td>
          <td>{data.supplier?.contactType || "-"}</td>
        </tr>
        <tr>
          <td>GST Number:</td>
          <td>{data.supplier?.gstNumber || "-"}</td>
          <td>Contact Person:</td>
          <td>{data.supplier?.contactPerson || "-"}</td>
        </tr>
        <tr>
          <td>Mobile Number:</td>
          <td>{data.supplier?.mobileNo || data.supplier?.contactPersonMobileNo || "-"}</td>
          <td>Email:</td>
          <td>{data.supplier?.emailId || "-"}</td>
        </tr>
        <tr>
          <td>Address:</td>
          <td colSpan="3">
            {[data.supplier?.addr_line1, data.supplier?.addr_line2, data.supplier?.city, data.supplier?.state, data.supplier?.zip]
              .filter(Boolean)
              .join(", ") || "-"}
          </td>
        </tr>

        {/* Order From Section */}
        <tr>
          <td colSpan="4" className="section-header">ORDER FROM</td>
        </tr>
        <tr>
          <td>Firm Name:</td>
          <td>{data.firm?.firmName || "-"}</td>
          <td>Description:</td>
          <td>{data.firm?.firmDescription || "-"}</td>
        </tr>
        <tr>
          <td>GST Number:</td>
          <td>{data.firm?.firmGstNumber || "-"}</td>
          <td>PAN Number:</td>
          <td>{data.firm?.firmPanNumber || "-"}</td>
        </tr>
        <tr>
          <td>Contact Number:</td>
          <td>{data.firm?.firmContactNumber || "-"}</td>
          <td>Address:</td>
          <td>{data.firm?.firmAddress || "-"}</td>
        </tr>
      </>
    );
  }

  render() {
    return (
      <div className="debit-note purchase-order-print" id="purchase-order-print">
        <div className="logo-container">
          <img src={`${process.env.PUBLIC_URL}/${process.env.REACT_APP_LOGIN_LOGO}.png`} alt="Logo" className="logo" />
        </div>
        <div className="data">{this.renderData()}</div>
        <div className="footer">
          <div>
            AUTHORISED SIGNATURE <div>{process.env.REACT_APP_NAME}</div>
          </div>
          <div>PURCHASE MANAGER</div>
          <div>Supplier</div>
          <div></div>
          <div className="footer-note4"></div>
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    tennant: state.tennant.tennant_id,
    allTenant: state.allTennant.tennants,
  };
};

export default connect(mapStateToProps, null, null, {
  forwardRef: true,
})(Print);