import React from "react";
import { EditIcon, DeleteIcon } from "./../../Shared/Icons/Index.js";
import CommonDetails from "./../../Shared/Details";
import IconButton from "@material-ui/core/IconButton";
import Clear from "@material-ui/icons/Clear";
import { messages } from "./../../messages";
import DeleteConfirm from "./../../Shared/DeleteConfirm";
import Tabs from "@material-ui/core/Tabs";
import Tab from "@material-ui/core/Tab";
import Typography from "@material-ui/core/Typography";
import Box from "@material-ui/core/Box";
import { checkifDateLessThan, getRoleEditConstraintDays, canCreateInward } from "./../../helper";
import DebitNotePrint from "./../../Shared/DebitNotePrint";
import ReactToPrint from "react-to-print";
import GetAppRoundedIcon from "@material-ui/icons/GetAppRounded";

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
  componentDidMount() {
    this.componentRef = React.createRef();
  }
  render() {
    const data = this.props.data;
    const days = getRoleEditConstraintDays();
    const isUpdateEnable = checkifDateLessThan(data.date, days);
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
        <div className="debit-note-contetn">
          <DebitNotePrint
            data={data}
            ref={(el) => (this.componentRef = el)}
            mor={true}
          />
        </div>

        <div className="details-header">
          {messages.common.details}
          <div>
            <ReactToPrint
              trigger={() => {
                return (
                  <IconButton aria-label="print" className="back-icon">
                    <GetAppRoundedIcon />
                  </IconButton>
                );
              }}
              content={() => {
                return this.componentRef;
              }}
            />
            {canWrite && (
              <IconButton
                aria-label="back"
                onClick={() => this.props.edit(data)}
                className="back-icon"
                disabled={isUpdateEnable}
              >
                {EditIcon({ fontSize: "medium" })}
              </IconButton>
            )}
            {canWrite && (
              <IconButton
                aria-label="back"
                disabled={isUpdateEnable}
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
                <div className="label">{messages.fields.date}</div>
                <div className="value">{data.date}</div>
              </div>
              <div className="detail-item">
                <div className="label">{messages.common.machinery}</div>
                <div className="value">{data.machinery.machineryName}</div>
              </div>
              {data.supplier && (
                <div className="detail-item">
                  <div className="label">{messages.fields.supplier}</div>
                  <div className="value">{data.supplier.name}</div>
                </div>
              )}
              {data.usageLocation && (
                <div className="detail-item">
                  <div className="label">{messages.fields.location}</div>
                  <div className="value">{data.usageLocation.locationName}</div>
                </div>
              )}
              <div className="detail-item">
                <div className="label">{messages.fields.mode}</div>
                <div className="value">{data.mode}</div>
              </div>
              {data.startDate && (
                <div className="detail-item">
                  <div className="label">{messages.common.startDate}</div>
                  <div className="value">{data.startDate}</div>
                </div>
              )}
              {data.endDate && (
                <div className="detail-item">
                  <div className="label">{messages.common.endDate}</div>
                  <div className="value">{data.endDate}</div>
                </div>
              )}
              {data.startDateTime && (
                <div className="detail-item">
                  <div className="label">{messages.common.startDate}</div>
                  <div className="value">{data.startDateTime}</div>
                </div>
              )}
              {data.endDateTime && (
                <div className="detail-item">
                  <div className="label">{messages.common.endDate}</div>
                  <div className="value">{data.endDateTime}</div>
                </div>
              )}
              {data.initialMeterReading && (
                <div className="detail-item">
                  <div className="label">
                    {messages.common.initialMeterReading}
                  </div>
                  <div className="value">{data.initialMeterReading}</div>
                </div>
              )}
              {data.endMeterReading && (
                <div className="detail-item">
                  <div className="label">{messages.common.endMeterReading}</div>
                  <div className="value">{data.endMeterReading}</div>
                </div>
              )}
              {data.noOfTrips && (
                <div className="detail-item">
                  <div className="label">{messages.common.noOfTrips}</div>
                  <div className="value">{data.noOfTrips}</div>
                </div>
              )}
              {data.amountCharged && (
                <div className="detail-item">
                  <div className="label">{messages.common.amountCharged}</div>
                  <div className="value">{data.amountCharged}</div>
                </div>
              )}
              {data.vehicleNo && (
                <div className="detail-item">
                  <div className="label">{messages.common.vehicleNo}</div>
                  <div className="value">{data.vehicleNo}</div>
                </div>
              )}
              {data.mrnGrn && (
                <div className="detail-item">
                  <div className="label">{messages.common.mrngrn}</div>
                  <div className="value">{data.mrnGrn}</div>
                </div>
              )}
            </div>
            <div className="detail-item">
              <div className="label">{messages.common.comment}</div>
              <div className="value">{data.additionalNotes}</div>
            </div>
            {this.renderFileList(data.fileInformations)}
          </div>
        </TabPanel>
      </div>
    );
  }
}

export default Details;
