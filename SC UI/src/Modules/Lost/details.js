import React from "react";
import CommonDetails from "./../../Shared/Details";
import IconButton from "@material-ui/core/IconButton";
import Clear from "@material-ui/icons/Clear";
import { messages } from "./../../messages";
import Tabs from "@material-ui/core/Tabs";
import Tab from "@material-ui/core/Tab";
import Typography from "@material-ui/core/Typography";
import Box from "@material-ui/core/Box";

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
      {value === index && <Box p={3}>{children}</Box>}
    </Typography>
  );
}

class Details extends CommonDetails {
  state = { value: 0 };

  render() {
    const data = this.props.data;
    return (
      <div className="list-section detail-section">
        <div className="details-header">
          {messages.common.details}
          <div>
            <IconButton
              aria-label="back"
              onClick={() => this.props.close(data)}
              className="back-icon"
            >
              <Clear />
            </IconButton>
          </div>
        </div>
        <Tabs
          indicatorColor="primary"
          textColor="primary"
          onChange={(e, value) => this.setState({ value: value })}
          value={this.state.value}
        >
          <Tab label="Details" id="simple-tabpanel-0" />
          <Tab label="History" id="simple-tabpanel-1" />
        </Tabs>
        <TabPanel value={this.state.value} index={1}>
          {data.createdBy && (
            <div className="detail-item">
              <div className="label">Created By</div>
              <div className="value">{data.createdBy}</div>
            </div>
          )}
          {data.creationDate && (
            <div className="detail-item">
              <div className="label">Creation Date</div>
              <div className="value">{data.creationDate}</div>
            </div>
          )}
          {data.lastModifiedBy && (
            <div className="detail-item">
              <div className="label">Last Modified By</div>
              <div className="value">{data.lastModifiedBy}</div>
            </div>
          )}
          {data.lastModifiedDate && (
            <div className="detail-item">
              <div className="label">Last Modified On</div>
              <div className="value">{data.lastModifiedDate}</div>
            </div>
          )}
        </TabPanel>
        <TabPanel value={this.state.value} index={0}>
          <div className="details-main-content">
            <div className="details-wrapper">
              <div className="detail-item">
                <div className="label">Type</div>
                <div className="value">{data.entryType === "EXCESS_FOUND" ? "Excess Found" : "Lost / Damaged"}</div>
              </div>
              <div className="detail-item">
                <div className="label">{data.entryType === "EXCESS_FOUND" ? "Remarks" : messages.fields.location}</div>
                <div className="value">{data.locationOfTheft}</div>
              </div>
              {data.additionalComment && (
                <div className="detail-item">
                  <div className="label">Additional Comments</div>
                  <div className="value">{data.additionalComment}</div>
                </div>
              )}
              <div className="detail-item">
                <div className="label">{messages.common.warehouse}</div>
                <div className="value">{data.warehouse.warehouseName}</div>
              </div>
              <div className="detail-item">
                <div className="label">{messages.fields.date}</div>
                <div className="value">{data.date}</div>
              </div>
            </div>
            {this.renderFileList(data.fileInformations)}

            <table>
              <thead>
                <tr>
                  <th>{messages.common.inventory}</th>
                  <th>{messages.common.unit}</th>
                  <th>{messages.common.quantity}</th>
                  <th>{messages.common.closingStock}</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td data-label={messages.common.inventory}>
                    {data.product.productName}
                  </td>
                  <td data-label={messages.common.unit}>
                    {data.product.measurementUnit}
                  </td>
                  <td data-label={messages.common.quantity}>{data.quantity}</td>
                  <td data-label={messages.common.closingStock}>
                    {data.closingStock}
                  </td>
                </tr>
              </tbody>
            </table>

            {(data.batchEntriesJson || data.batch) && (() => {
              let batchRows = [];
              if (data.batchEntriesJson) {
                try { batchRows = JSON.parse(data.batchEntriesJson); } catch(e) {}
              } else if (data.batch) {
                batchRows = [{
                  batchId: data.batch.batchId,
                  brand: data.batch.brand,
                  lotNumber: data.batch.lotNumber,
                  expiryDate: data.batch.expiryDate,
                  qty: data.quantity
                }];
              }
              if (!batchRows.length) return null;
              return (
                <>
                  <div style={{ marginTop: '16px', fontWeight: 600, fontSize: '14px' }}>
                    Batch Details
                  </div>
                  <table>
                    <thead>
                      <tr>
                        <th>Batch #</th>
                        <th>Brand</th>
                        <th>Lot No</th>
                        <th>Expiry Date</th>
                        <th>Qty</th>
                      </tr>
                    </thead>
                    <tbody>
                      {batchRows.map((row, i) => (
                        <tr key={i}>
                          <td>{row.batchId}</td>
                          <td>{row.brand || '-'}</td>
                          <td>{row.lotNumber || '-'}</td>
                          <td>{row.expiryDate || '-'}</td>
                          <td>{row.qty}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </>
              );
            })()}
          </div>
        </TabPanel>
      </div>
    );
  }
}

export default Details;
