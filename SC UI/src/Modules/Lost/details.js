import React from "react";
import { EditIcon, DeleteIcon } from "./../../Shared/Icons/Index.js";
import CommonDetails from "./../../Shared/Details";
import IconButton from "@material-ui/core/IconButton";
import Clear from "@material-ui/icons/Clear";
import { messages } from "./../../messages";
import { canCreateInward } from "./../../helper";
import DeleteConfirm from "./../../Shared/DeleteConfirm";
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
  state = { value: 0, deleteConfirmOpen: false };
  deleteRow = null;

  render() {
    const data = this.props.data;
    const canWrite = canCreateInward();
    return (
      <div className="list-section detail-section">
        <DeleteConfirm
          open={this.state.deleteConfirmOpen}
          onConfirm={() => {
            this.props.delete(this.deleteRow);
            this.setState({ deleteConfirmOpen: false });
          }}
          onCancel={() => {
            this.deleteRow = null;
            this.setState({ deleteConfirmOpen: false });
          }}
        />

        <div className="details-header">
          {messages.common.details}
          <div>
            {canWrite && (
              <IconButton
                aria-label="back"
                onClick={() => this.props.edit(data)}
                className="back-icon"
              >
                {EditIcon({ fontSize: "medium" })}
              </IconButton>
            )}
            {canWrite && (
              <IconButton
                aria-label="back"
                onClick={() => {
                  this.deleteRow = data;
                  this.setState({ deleteConfirmOpen: true });
                }}
                className="back-icon"
              >
                {DeleteIcon({ fontSize: "medium" })}
              </IconButton>
            )}
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
                <div className="label">{messages.fields.location}</div>
                <div className="value">{data.locationOfTheft}</div>
              </div>
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
          </div>
        </TabPanel>
      </div>
    );
  }
}

export default Details;
