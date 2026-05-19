import React from "react";
import CommonDetails from "./../../Shared/Details";
import IconButton from "@material-ui/core/IconButton";
import Paper from "@material-ui/core/Paper";
import { messages } from "./../../messages";
import {
  EditIcon,
  DeleteIcon,
  MoreIcon,
  CloseIcon,
} from "./../../Shared/Icons/Index.js";
import DeleteConfirm from "./../../Shared//DeleteConfirm";
import Tabs from "@material-ui/core/Tabs";
import Tab from "@material-ui/core/Tab";
import Typography from "@material-ui/core/Typography";
import Box from "@material-ui/core/Box";
import RejectProduct from "./rejectProduct";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import { checkifDateLessThan, getRoleEditConstraintDays, getRoleRejectReturnConstraintDays, canCreateInward } from "./../../helper";
import DebitNotePrint from "./../../Shared/DebitNotePrint";
import ReactToPrint from "react-to-print";
import Table from "@material-ui/core/Table";
import TableBody from "@material-ui/core/TableBody";
import TableCell from "@material-ui/core/TableCell";
import TableHead from "@material-ui/core/TableHead";
import TableRow from "@material-ui/core/TableRow";
import Menu from "@material-ui/core/Menu";
import MenuItem from "@material-ui/core/MenuItem";
import ArrowBackIosIcon from "@material-ui/icons/ArrowBackIos";
import ArrowForwardIosIcon from "@material-ui/icons/ArrowForwardIos";
import PrintIcon from "@material-ui/icons/Print";
import Print from "./inwardPrint";
import { renderActivityDescription } from '../Activity/renderActivityDescription';
const ACTION_BADGE_STYLES = {
  CREATED:     { color: '#2e7d32', background: '#e8f5e9' },
  UPDATED:     { color: '#e65100', background: '#fff3e0' },
  DELETED:     { color: '#c62828', background: '#ffebee' },
  REJECTED:    { color: '#6a1b9a', background: '#f3e5f5' },
  RETURNED:    { color: '#0d47a1', background: '#e3f2fd' },
};

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
  state = {
    value: 0,
    deleteConfirmOpen: false,
    rejectopen: false,
    anchorEl: null,
    historyLogs: [],
    historyLoading: false,
  };
  detailTabRef = React.createRef();
  deleteRow = null;

  componentDidMount() {
    this.addRejRef = React.createRef();
    this.componentRef = React.createRef();
  }

  async loadHistory() {
    const { data } = this.props;
    if (!data || !data.inwardId) return;
    this.setState({ historyLoading: true });
    const r = await API.GET(apiEndpoints.activityLogByEntity('INWARD', data.inwardId));
    if (r.success) {
      this.setState({ historyLogs: r.data || [] });
    }
    this.setState({ historyLoading: false });
  }

  handleCloseMenu = () => {
    this.setState({ anchorEl: null });
  };

  handlePrevious = () => {
    const { currentIndex, onNavigate } = this.props;
    if (currentIndex > 0 && onNavigate) {
      onNavigate(currentIndex - 1);
    }
  };

  handleNext = () => {
    const { currentIndex, allEntries, onNavigate } = this.props;
    if (allEntries && currentIndex < allEntries.length - 1 && onNavigate) {
      onNavigate(currentIndex + 1);
    }
  };

  render() {
    const data = this.props.data;
    if (!data) return null;

    const { currentIndex = 0, allEntries = [], fromRelation } = this.props;
    const canGoPrevious = currentIndex > 0;
    const canGoNext = currentIndex < allEntries.length - 1;
    const isFromRelation = fromRelation === true;
    const days = getRoleEditConstraintDays();
    const isUpdateEnable = checkifDateLessThan(data.date, days);
    const rejectReturnDays = getRoleRejectReturnConstraintDays();
    const isRejectReturnDisabled = checkifDateLessThan(data.date, rejectReturnDays);
    const canWrite = canCreateInward();
    const hasReject = data.rejectInwardList && data.rejectInwardList.length;
    return (
      <div className="list-section detail-section inward-detail-section">
        <div className="details-header">
          <div className="inward-header-left">
            Inward ID: {data.inwardId || ""}
            {!isFromRelation && (
              <div className="inward-navigation">
                <IconButton 
                  size="small" 
                  className="nav-arrow"
                  onClick={this.handlePrevious}
                  disabled={!canGoPrevious}
                >
                  <ArrowBackIosIcon fontSize="small" />
                </IconButton>
                <IconButton 
                  size="small" 
                  className="nav-arrow"
                  onClick={this.handleNext}
                  disabled={!canGoNext}
                >
                  <ArrowForwardIosIcon fontSize="small" />
                </IconButton>
              </div>
            )}
          </div>
          <div>
            {isFromRelation ? (
              <ReactToPrint
                content={() => this.detailTabRef?.current}
                trigger={() => (
                  <IconButton className="back-icon" aria-label="Print">
                    <PrintIcon fontSize="medium" />
                  </IconButton>
                )}
              />
            ) : (
              <>
                <IconButton
                  onClick={(event) =>
                    this.setState({ anchorEl: event.currentTarget })
                  }
                  className="back-icon"
                >
                  {MoreIcon({ fontSize: "medium" })}
                </IconButton>
                <Menu
                  anchorEl={this.state.anchorEl}
                  keepMounted
                  open={Boolean(this.state.anchorEl)}
                  onClose={this.handleCloseMenu}
                  anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
                  transformOrigin={{ vertical: "top", horizontal: "right" }}
                  getContentAnchorEl={null}
                  classes={{ paper: "detail-dropdown-menu" }}
                >
                  <MenuItem onClick={() => this.handleCloseMenu()}>
                    <ReactToPrint
                      trigger={() => {
                        return <div>Print</div>;
                      }}
content={() => this.detailTabRef.current}
                    />
                  </MenuItem>
                  {hasReject ? (
                    <MenuItem onClick={() => this.handleCloseMenu()}>
                      <ReactToPrint
                        trigger={() => {
                          return <div>Print Reject</div>;
                        }}
                        content={() => {
                          return this.componentRef;
                        }}
                      />
                    </MenuItem>
                  ) : null}
                  {canWrite && (
                    <MenuItem
                      disabled={isRejectReturnDisabled}
                      onClick={() => {
                        this.handleCloseMenu();
                        this.setState({ rejectopen: true });
                      }}
                    >
                      Add Reject
                    </MenuItem>
                  )}
                </Menu>
                {this.state.rejectopen && (
                  <RejectProduct
                    data={data}
                    open={this.state.rejectopen}
                    closeDetails={() => {
                      this.setState({ rejectopen: false });
                      this.props.close(data);
                    }}
                    goToDetails={() => this.props.goToDetails()}
                    close={() => this.setState({ rejectopen: false })}
                  />
                )}
                {canWrite && (
                  <IconButton
                    aria-label="edit"
                    onClick={() => this.props.edit(data)}
                    className="back-icon"
                    disabled={isUpdateEnable}
                  >
                    {EditIcon({ fontSize: "medium" })}
                  </IconButton>
                )}
                {canWrite && (
                  <IconButton
                    aria-label="delete"
                    onClick={() => {
                      this.deleteRow = data;
                      this.setState({ deleteConfirmOpen: true });
                    }}
                    className="back-icon"
                    disabled={isUpdateEnable}
                  >
                    {DeleteIcon({ fontSize: "medium" })}
                  </IconButton>
                )}
              </>
            )}
            <IconButton
              aria-label="close"
              onClick={() => this.props.close(data)}
              className="back-icon"
            >
              <CloseIcon />
            </IconButton>
          </div>
        </div>
        <div className="print-content">
          <Print ref={this.detailTabRef} data={data} tenantCode={this.props.tenantCode} />
        </div>
        <div className="debit-note-contetn">
          <DebitNotePrint
            data={data}
            ref={(el) => (this.componentRef = el)}
            inwardReject={true}
          />
        </div>
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
        <Tabs
          indicatorColor="primary"
          textColor="primary"
          onChange={(e, value) => {
            this.setState({ value });
            if (value === 1) this.loadHistory();
          }}
          value={this.state.value}
        >
          <Tab label="Details" id="simple-tabpanel-0" />
          <Tab label="History" id="simple-tabpanel-1" />
        </Tabs>
        <TabPanel value={this.state.value} index={0}>
          <div className="details-print-content">
            <Paper elevation={0}>
   <div className="details-wrapper">

     {/* Inward Type Tag — own row so it doesn't crowd the first field */}
     <div className="detail-item">
       <div className="label">Inward Type</div>
       <div className="value">
         {(() => {
           let label, color, background;
           if (data.isSampleInward) {
             label = "Sample Inward";
             color = "#6a1b9a";
             background = "#f3e5f5";
           } else if (data.createdFromPO) {
             label = "From PO";
             color = "#1565c0";
             background = "#e3f2fd";
           } else {
             label = "Direct Inward";
             color = "#2e7d32";
             background = "#e8f5e9";
           }
           return (
             <span style={{
               display: 'inline-block',
               padding: '3px 10px',
               borderRadius: '12px',
               fontSize: '12px',
               fontWeight: 600,
               color: color,
               backgroundColor: background,
               border: `1px solid ${color}`,
               letterSpacing: '0.3px',
             }}>
               {label}
             </span>
           );
         })()}
       </div>
     </div>

     <div className="detail-item">
       <div className="label">{messages.fields.name}</div>
       <div className="value">{data.supplier.name}</div>
     </div>
                <div className="detail-item">
                  <div className="label">{messages.common.warehouse}</div>
                  <div className="value">
                    {data.inwardOutwardList && data.inwardOutwardList.length > 0
                      ? data.inwardOutwardList[0].warehouse?.warehouseName || 'N/A'
                      : 'N/A'
                    }
                  </div>
                </div>
                <div className="detail-item">
                  <div className="label">{messages.common.receivingDate}</div>
                  <div className="value">{data.date}</div>
                </div>
                <div className="detail-item">
                  <div className="label">
                    {messages.common.purchaseOrderDate}
                  </div>
                  <div className="value">{data.purchaseOrderDate}</div>
                </div>
                <div className="detail-item">
                  <div className="label">{messages.common.purchaseOrderNo}</div>
                  <div className="value">
                    {data.createdFromPO && this.props.onOpenPO && (data.purchaseOrderId || data.purchaseOrderNo) ? (
                      <button
                        type="button"
                        className="relation-link"
                        onClick={() => this.props.onOpenPO(data.purchaseOrderId || data.purchaseOrderNo)}
                      >
                        {data.purchaseOrderNo || data.purchaseOrderId}
                      </button>
                    ) : (
                      data.purchaseOrderNo || "—"
                    )}
                  </div>
                </div>

                <div className="detail-item">
                  <div className="label">{messages.common.mrngrn}</div>
                  <div className="value">{data.ourSlipNo}</div>
                </div>
                <div className="detail-item">
                  <div className="label">{"Challan No"}</div>
                  <div className="value">{data.challanNo}</div>
                </div>
                <div className="detail-item">
                  <div className="label">{"Challan Date"}</div>
                  <div className="value">{data.challanDate}</div>
                </div>
                <div className="detail-item">
                  <div className="label">{"Bill No"}</div>
                  <div className="value">{data.billNo}</div>
                </div>
                {(!data.challanNo || !data.challanNo.trim()) && (!data.billNo || !data.billNo.trim()) && (
                  <div className="detail-item">
                    <div className="label">{"Doc Status"}</div>
                    <div className="value">
                      <span style={{
                        display: 'inline-block',
                        padding: '3px 10px',
                        borderRadius: '12px',
                        fontSize: '11px',
                        fontWeight: 600,
                        color: '#b71c1c',
                        backgroundColor: '#ffebee',
                        border: '1px solid #b71c1c',
                      }}>
                        No Challan / Bill
                      </span>
                      {data.noChallanBillReason && (
                        <div style={{ marginTop: '4px', fontSize: '12px', color: '#555' }}>
                          Reason: {data.noChallanBillReason}
                        </div>
                      )}
                    </div>
                  </div>
                )}
                <div className="detail-item">
                  <div className="label">{"Bill Date"}</div>
                  <div className="value">{data.billDate}</div>
                </div>
                <div className="detail-item">
                  <div className="label">{messages.common.vehicleNo}</div>
                  <div className="value">{data.vehicleNo}</div>
                </div>
                <div className="detail-item">
                  <div className="label">{messages.common.comment}</div>
                  <div className="value">{data.additionalInfo}</div>
                </div>
              </div>
              {this.renderFileList(data.fileInformations)}
            </Paper>
            <Paper elevation={0} className="table-wrapper">
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>{messages.common.warehouse}</TableCell>
                    {data.createdFromPO && <TableCell>Indent No</TableCell>}
                    <TableCell>{messages.common.inventory}</TableCell>
                    <TableCell>{messages.common.unit}</TableCell>
                    <TableCell>{messages.common.quantity}</TableCell>
                    <TableCell>{messages.common.closingStock}</TableCell>
                    <TableCell>Brand</TableCell>
                    <TableCell>Expiry Date</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {data.inwardOutwardList.map((row, idx) => (
                    <TableRow key={row.productName + (idx || "")}>
                      <TableCell>
                        {row.warehouse && row.warehouse.warehouseName
                          ? row.warehouse.warehouseName
                          : "N/A"}
                      </TableCell>
                      {data.createdFromPO && (
                        <TableCell>
                          {row.indentId && this.props.onOpenIndent ? (
                            <button
                              type="button"
                              className="relation-link"
                              onClick={() => this.props.onOpenIndent(row.indentId)}
                            >
                              {row.indentId}
                            </button>
                          ) : (
                            row.indentId || "—"
                          )}
                        </TableCell>
                      )}
                      <TableCell>
                        {row.product.productName}
                        {row.indentRemarks && (
                          <div style={{ fontSize: '11px', color: '#666', marginTop: '3px' }}>
                            <strong>Remarks:</strong> {row.indentRemarks}
                          </div>
                        )}
                        {row.indentSpecification && (
                          <div style={{ fontSize: '11px', color: '#666' }}>
                            <strong>Specification:</strong> {row.indentSpecification}
                          </div>
                        )}
                      </TableCell>
                      <TableCell>{row.product.measurementUnit}</TableCell>
                      <TableCell>{row.quantity}</TableCell>
                      <TableCell>{row.closingStock}</TableCell>
                      <TableCell>{row.brand || '—'}</TableCell>
                      <TableCell>{row.expiryDate ? new Date(row.expiryDate).toLocaleDateString('en-GB') : '—'}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Paper>
            {hasReject ? (
              <>
                <h4 className="reject-stock">Inward Rejected Stocks</h4>
                <Paper elevation={0} className="table-wrapper">
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>{messages.fields.date}</TableCell>
                        <TableCell>{messages.common.inventory}</TableCell>
                        <TableCell>{messages.common.unit}</TableCell>
                        <TableCell>{messages.common.oldquantity}</TableCell>
                        <TableCell>
                          {messages.common.returnedQauntity}
                        </TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {data.rejectInwardList.map((row) => (
                        <TableRow key={row.productName}>
                          <TableCell>{row.rejectDate}</TableCell>
                          <TableCell>{row.product.productName}</TableCell>
                          <TableCell>{row.product.measurementUnit}</TableCell>
                          <TableCell>{row.oldQuantity}</TableCell>
                          <TableCell>{row.rejectQuantity}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </Paper>
              </>
            ) : null}
          </div>
        </TabPanel>
        <TabPanel value={this.state.value} index={1}>
          <Paper elevation={0}>
            {this.state.historyLoading ? (
              <div style={{ padding: '16px', color: '#666' }}>Loading history...</div>
            ) : this.state.historyLogs.length === 0 ? (
              <div style={{ padding: '16px', color: '#999' }}>No history available.</div>
            ) : (
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Time</TableCell>
                    <TableCell>Action</TableCell>
                    <TableCell>Description</TableCell>
                    <TableCell>By</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {this.state.historyLogs.map((log, i) => (
                    <TableRow key={i}>
                      <TableCell style={{ whiteSpace: 'nowrap', fontSize: '12px' }}>
                        {log.activityTime ? new Date(log.activityTime).toLocaleString() : ''}
                      </TableCell>
                      <TableCell>
                        <span style={{
                          padding: '2px 8px', borderRadius: '4px', fontWeight: 600, fontSize: '12px',
                          ...ACTION_BADGE_STYLES[log.action]
                        }}>
                          {log.action}
                        </span>
                      </TableCell>
                      <TableCell style={{ fontSize: '13px', maxWidth: '360px' }}>
                        {renderActivityDescription(log.description)}
                      </TableCell>
                      <TableCell style={{ fontSize: '12px' }}>{log.performedBy}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </Paper>
        </TabPanel>
      </div>
    );
  }
}

export default Details;
