import React from "react";
import {
  EditIcon,
  DeleteIcon,
  MoreIcon,
  CloseIcon,
} from "./../../Shared/Icons/Index.js";
import IconButton from "@material-ui/core/IconButton";
import Paper from "@material-ui/core/Paper";
import { messages } from "./../../messages";

import CommonDetails from "./../../Shared/Details";
import ReturnProduct from "./ReturnProduct";
import DeleteConfirm from "./../../Shared/DeleteConfirm";
import Tabs from "@material-ui/core/Tabs";
import Tab from "@material-ui/core/Tab";
import Typography from "@material-ui/core/Typography";
import Box from "@material-ui/core/Box";
import RejectProduct from "./rejectProduct";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import { checkifDateLessThan, getRoleEditConstraintDays, getRoleRejectReturnConstraintDays, canCreateInward } from "./../../helper";
import Tooltip from "@material-ui/core/Tooltip";
import WarningRoundedIcon from "@material-ui/icons/WarningRounded";
import DebitNotePrint from "./../../Shared/DebitNotePrint";
import ReactToPrint from "react-to-print";
import Menu from "@material-ui/core/Menu";
import MenuItem from "@material-ui/core/MenuItem";
import Table from "@material-ui/core/Table";
import TableBody from "@material-ui/core/TableBody";
import TableCell from "@material-ui/core/TableCell";
import TableHead from "@material-ui/core/TableHead";
import TableRow from "@material-ui/core/TableRow";
import Print from "./outwardPrint";
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
    batchConsumptions: [],
    batchConsumptionsLoaded: false,
  };
  deleteRow = null;

  componentDidMount() {
    this.addRef = React.createRef();
    this.addRejRef = React.createRef();
    this.componentRef = React.createRef();
    this.componentRef1 = React.createRef();
    this.detailTabRef = React.createRef();
    this.loadBatchConsumptions();
  }

  async loadHistory() {
    const { data } = this.props;
    if (!data || !data.outwardid) return;
    this.setState({ historyLoading: true });
    const r = await API.GET(apiEndpoints.activityLogByEntity('OUTWARD', data.outwardid));
    if (r.success) {
      this.setState({ historyLogs: r.data || [] });
    }
    this.setState({ historyLoading: false });
  }

  componentDidUpdate(prevProps) {
    if (prevProps.data?.outwardid !== this.props.data?.outwardid) {
      this.setState({ batchConsumptions: [], batchConsumptionsLoaded: false }, () =>
        this.loadBatchConsumptions()
      );
    }
  }

  async loadBatchConsumptions() {
    const { data } = this.props;
    if (!data || !data.outwardid || this.state.batchConsumptionsLoaded) return;
    const r = await API.GET(apiEndpoints.getOutwardBatchConsumptions(data.outwardid));
    if (r.success) {
      this.setState({ batchConsumptions: r.data || [], batchConsumptionsLoaded: true });
    }
  }

  handleCloseMenu = () => {
    this.setState({ anchorEl: null });
  };

  render() {
    const data = this.props.data;
    const days = getRoleEditConstraintDays();
    const isUpdateEnable = checkifDateLessThan(data.date, days);
    const rejectReturnDays = getRoleRejectReturnConstraintDays();
    const isRejectReturnDisabled = checkifDateLessThan(data.date, rejectReturnDays);
    const canWrite = canCreateInward();
    const hasReturn =
      data.returnOutwardList && data.returnOutwardList.length > 0;
    const hasReject =
      data.rejectOutwardList && data.rejectOutwardList.length > 0;
    return (
      <div className="list-section detail-section outward-detail-section">
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
        <div className="print-content">
          <Print ref={(el) => (this.detailTabRef = el)} data={data} />
        </div>
        <div className="debit-note-contetn">
          <DebitNotePrint
            data={data}
            ref={(el) => (this.componentRef = el)}
            ourwardReject={true}
          />
          <DebitNotePrint
            data={data}
            ref={(el) => (this.componentRef1 = el)}
            outwardReturn={true}
          />
        </div>

        <div className="details-header">
          {messages.common.details}
          <div>
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
              classes={{ paper: "detail-dropdown-menu" }}
            >
              <MenuItem onClick={() => this.handleCloseMenu()}>
                <ReactToPrint
                  trigger={() => {
                    return <div>Print</div>;
                  }}
                  content={() => {
                    return this.detailTabRef;
                  }}
                />
              </MenuItem>
              {hasReject ? (
                <MenuItem onClick={() => this.handleCloseMenu()}>
                  <ReactToPrint
                    trigger={() => {
                      return <div>Print Debit Note</div>;
                    }}
                    content={() => {
                      return this.componentRef;
                    }}
                  />
                </MenuItem>
              ) : null}
              {hasReturn ? (
                <MenuItem onClick={() => this.handleCloseMenu()}>
                  <ReactToPrint
                    trigger={() => {
                      return <div>Print Return Note</div>;
                    }}
                    content={() => {
                      return this.componentRef1;
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
              {canWrite && (
                <MenuItem
                  disabled={isRejectReturnDisabled}
                  onClick={() => {
                    this.handleCloseMenu();
                    this.setState({ filterOpen: true });
                  }}
                >
                  Add Return
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

            {this.state.filterOpen && (
              <ReturnProduct
                data={data}
                open={this.state.filterOpen}
                closeDetails={() => {
                  this.setState({ filterOpen: false });
                  this.props.close(data);
                }}
                goToDetails={() => this.props.goToDetails()}
                close={() => this.setState({ filterOpen: false })}
              />
            )}

            {/* <IconButton
              aria-label="add return"
              title="Return Inventory"
              onClick={() => {
                this.setState({ filterOpen: true });
              }}
              className="back-icon"
              innerRef={this.addRef}
              disabled={isUpdateEnable}
            >
              <AddCircle />
            </IconButton> */}
            {/* <IconButton
              aria-label="add reject"
              title="Reject Inventory"
              onClick={() => {
                this.setState({ rejectopen: true });
              }}
              className="back-icon"
              innerRef={this.addRejRef}
            >
              <ThumbDownIcon />
            </IconButton> */}
            {/* <Popper
              open={this.state.filterOpen}
              anchorEl={this.addRef && this.addRef.current}
              placement="bottom-start"
            >
              <ReturnProduct
                data={data}
                closeDetails={() => {
                  this.setState({ filterOpen: false });
                  this.props.close(data);
                }}
                goToDetails={() => this.props.goToDetails()}
                close={() => this.setState({ filterOpen: false })}
              />
            </Popper> */}
            {/* <Popper
              open={this.state.rejectopen}
              anchorEl={this.addRejRef && this.addRejRef.current}
              placement="bottom-start"
            >
              <RejectProduct
                data={data}
                closeDetails={() => {
                  this.setState({ rejectopen: false });
                  this.props.close(data);
                }}
                goToDetails={() => this.props.goToDetails()}
                close={() => this.setState({ rejectopen: false })}
              />
            </Popper> */}
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
            {/* {hasReject ? (
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
            ) : null}
            {hasReturn ? (
              <ReactToPrint
                trigger={() => {
                  return (
                    <IconButton aria-label="print" className="back-icon">
                      <GetAppRoundedIcon />
                    </IconButton>
                  );
                }}
                content={() => {
                  return this.componentRef1;
                }}
              />
            ) : null} */}
            {/* <IconButton
              aria-label="print"
              onClick={() => window.print()}
              className="back-icon"
            >
              <PrintIcon />
            </IconButton> */}
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
              <CloseIcon />
            </IconButton>
          </div>
        </div>
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
        <TabPanel value={this.state.value} index={0}>
          <div className="details-print-content">
            <Paper elevation={0}>
              {data.hasBOQ !== true && (
                <div style={{ padding: '10px 16px 0' }}>
                  <Tooltip title="BOQ Bypassed — outward created without BOQ configured" arrow>
                    <WarningRoundedIcon style={{ color: '#e65100', fontSize: '22px', cursor: 'default' }} />
                  </Tooltip>
                </div>
              )}
              <div className="details-wrapper">
                <div className="detail-item">
                  <div className="label">{messages.common.contractor}</div>
                  <div className="value">{data.contractor.name}</div>
                </div>
                <div className="detail-item">
                  <div className="label">{messages.common.warehouse}</div>
                  <div className="value">{data.warehouse.warehouseName}</div>
                </div>
                <div className="detail-item">
                  <div className="label">{messages.common.location}</div>
                  <div className="value">{data.usageLocation.locationName}</div>
                </div>
                <div className="detail-item">
                  <div className="label">{messages.common.finalLocation}</div>
                  <div className="value">{data.usageArea.usageAreaName}</div>
                </div>
                <div className="detail-item">
                  <div className="label">{messages.fields.date}</div>
                  <div className="value">{data.date}</div>
                </div>
                <div className="detail-item">
                  <div className="label">{messages.common.slipNo}</div>
                  <div className="value">{data.slipNo}</div>
                </div>
                <div className="detail-item">
                  <div className="label">{messages.common.purpose}</div>
                  <div className="value">{data.purpose}</div>
                </div>
              </div>
              <div className="detail-item">
                <div className="label">{messages.common.comment}</div>
                <div className="value">{data.additionalInfo}</div>
              </div>
              {this.renderFileList(data.fileInformations)}
            </Paper>
            <h4 className="reject-stock">Outward Stock</h4>
            <Paper elevation={0} className="table-wrapper">
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>{messages.common.inventory}</TableCell>
                    <TableCell>{messages.common.unit}</TableCell>
                    <TableCell>{messages.common.quantity}</TableCell>
                    <TableCell>{messages.common.closingStock}</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {data.inwardOutwardList.map((row) => (
                    <TableRow key={row.productName}>
                      <TableCell>{row.product.productName}</TableCell>
                      <TableCell>{row.product.measurementUnit}</TableCell>
                      <TableCell>{row.quantity}</TableCell>
                      <TableCell>{row.closingStock}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </Paper>

            {/* Batch consumption section — shown when data available */}
            {this.state.batchConsumptions.length > 0 && (() => {
              // Build productId → productName lookup from outward line items
              const productMap = {};
              (this.props.data?.inwardOutwardList || []).forEach((line) => {
                if (line.product) {
                  productMap[line.product.productId] = line.product.productName;
                }
              });
              return (
                <>
                  <h4 className="reject-stock">Batch Usage</h4>
                  <Paper elevation={0} className="table-wrapper">
                    <Table>
                      <TableHead>
                        <TableRow>
                          <TableCell>Product</TableCell>
                          <TableCell>Batch #</TableCell>
                          <TableCell>Expiry</TableCell>
                          <TableCell>Brand</TableCell>
                          <TableCell>Qty Consumed</TableCell>
                          <TableCell>FIFO Override</TableCell>
                          <TableCell>Override Reason</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {this.state.batchConsumptions.map((c) => (
                          <TableRow key={c.id} style={c.fifoOverridden ? { backgroundColor: '#fff8e1' } : {}}>
                            <TableCell>{productMap[c.productId] || '—'}</TableCell>
                            <TableCell>#{c.batch ? c.batch.batchId : '—'}</TableCell>
                            <TableCell>{c.batch && c.batch.expiryDate ? c.batch.expiryDate.replace(/-/g, '/') : '—'}</TableCell>
                            <TableCell>{c.batch && c.batch.brand ? c.batch.brand : '—'}</TableCell>
                            <TableCell>{c.qtyConsumed}</TableCell>
                            <TableCell>
                              {c.fifoOverridden
                                ? <span style={{ color: '#e65100', fontWeight: 600 }}>Yes</span>
                                : <span style={{ color: '#2e7d32' }}>No</span>}
                            </TableCell>
                            <TableCell style={{ color: '#e65100' }}>{c.overrideComment || '—'}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </Paper>
                </>
              );
            })()}

            {hasReturn ? (
              <>
                <h4 className="reject-stock">Outward Returned Stocks</h4>
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
                      {data.returnOutwardList.map((row) => (
                        <TableRow key={row.productName}>
                          <TableCell>{row.returnDate}</TableCell>
                          <TableCell>{row.product.productName}</TableCell>
                          <TableCell>{row.product.measurementUnit}</TableCell>
                          <TableCell>{row.oldQuantity}</TableCell>
                          <TableCell>{row.returnQuantity}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </Paper>
              </>
            ) : null}
            {hasReject ? (
              <>
                <h4 className="reject-stock">Outward Rejected Stocks</h4>
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
                      {data.rejectOutwardList.map((row) => (
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
      </div>
    );
  }
}

export default Details;
