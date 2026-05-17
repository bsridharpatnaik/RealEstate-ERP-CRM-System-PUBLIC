import React, { Component } from "react";
import Tabs from "@material-ui/core/Tabs";
import Tab from "@material-ui/core/Tab";
import Typography from "@material-ui/core/Typography";
import Box from "@material-ui/core/Box";
import IconButton from "@material-ui/core/IconButton";
import Clear from "@material-ui/icons/Clear";
import { stringNotNull, canEditContactModules } from "./../../helper";
import { EditIcon, DeleteIcon } from "./../../Shared/Icons/Index.js";
import DeleteConfirm from "./../../Shared//DeleteConfirm";
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

class Details extends Component {
  state = { value: 0, deleteConfirmOpen: false };
  deleteRow = null;
  history = [
    "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, ",
    "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, ",
    "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, ",
    "Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, ",
  ];
  render() {
    const contact = this.props.contact;
    return (
      <div className="list-section detail-section">
        <div className="contact-name">
          {contact.name}
          <div>
            {canEditContactModules() && (
              <IconButton
                aria-label="back"
                onClick={() => this.props.edit(this.props.contact)}
                className="back-icon"
              >
                {EditIcon({ fontSize: "medium" })}
              </IconButton>
            )}
            {canEditContactModules() && (
              <IconButton
                aria-label="back"
                onClick={() => {
                  this.deleteRow = this.props.contact;
                  this.setState({ deleteConfirmOpen: true });
                }}
                className="back-icon"
              >
                {DeleteIcon({ fontSize: "medium" })}
              </IconButton>
            )}
            <IconButton
              aria-label="back"
              onClick={() => this.props.close(this.props.contact)}
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
          <Tab label="Account" id="simple-tabpanel-0" />
          {/* <Tab label="History" id="simple-tabpanel-1" /> */}
        </Tabs>
        <TabPanel value={this.state.value} index={0}>
          <div className="detail-item">
            <div className="label">Contact Type</div>
            <div className="value">
              <span className={contact.contactType.toLowerCase()}>
                {contact.contactType}
              </span>
            </div>
          </div>
          <div className="detail-item">
            <div className="label">Mobile Number</div>
            <div className="value">{contact.mobileNo}</div>
          </div>
          <div className="detail-item">
            <div className="label">Email Id</div>
            <div className="value">{contact.emailId}</div>
          </div>
          {contact.contactPerson && (
            <div className="detail-item">
              <div className="label">Contact Person</div>
              <div className="value">{contact.contactPerson}</div>
            </div>
          )}
          {contact.contactPersonMobileNo && (
            <div className="detail-item">
              <div className="label">Contact Person Number</div>
              <div className="value">{contact.contactPersonMobileNo}</div>
            </div>
          )}
          {contact.gstNumber && (
            <div className="detail-item">
              <div className="label">GST</div>
              <div className="value">{contact.gstNumber}</div>
            </div>
          )}

          <div className="detail-item">
            <div className="label">Address</div>
            <div className="value">{`${stringNotNull(
              contact.addr_line1
            )} ${stringNotNull(contact.addr_line2)}
            ${stringNotNull(contact.city)} ${stringNotNull(
              contact.state
            )} ${stringNotNull(contact.zip)}
            `}</div>
          </div>
          {contact.createdBy && (
            <div className="detail-item">
              <div className="label">Created By</div>
              <div className="value">{contact.createdBy}</div>
            </div>
          )}
{contact.creationDate && (
            <div className="detail-item">
              <div className="label">Creation Date</div>
              <div className="value">{contact.creationDate}</div>
            </div>
          )}

          {/* Bank Details */}
          {(contact.accountName || contact.accountNumber || contact.bankName || contact.branchName || contact.ifscCode) && (
            <>
              <div className="detail-item">
                <div className="label" style={{ fontWeight: "bold", marginTop: "8px" }}>Bank Details</div>
                <div className="value"></div>
              </div>
              {contact.accountName && (
                <div className="detail-item">
                  <div className="label">Account Holder Name</div>
                  <div className="value">{contact.accountName}</div>
                </div>
              )}
              {contact.accountNumber && (
                <div className="detail-item">
                  <div className="label">Account Number</div>
                  <div className="value">{contact.accountNumber}</div>
                </div>
              )}
              {contact.bankName && (
                <div className="detail-item">
                  <div className="label">Bank Name</div>
                  <div className="value">{contact.bankName}</div>
                </div>
              )}
              {contact.branchName && (
                <div className="detail-item">
                  <div className="label">Branch Name</div>
                  <div className="value">{contact.branchName}</div>
                </div>
              )}
              {contact.ifscCode && (
                <div className="detail-item">
                  <div className="label">IFSC Code</div>
                  <div className="value">{contact.ifscCode}</div>
                </div>
              )}
            </>
          )}
        </TabPanel>
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
        {/* <TabPanel value={this.state.value} index={1}>
          <div className="history">
            {this.history.map((his) => (
              <div className="history-details">{his}</div>
            ))}
          </div>
        </TabPanel> */}
      </div>
    );
  }
}

export default Details;
