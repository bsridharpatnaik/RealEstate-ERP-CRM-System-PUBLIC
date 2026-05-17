//react
import React from "react";
//Third Party
import ListCommon from "./../../Shared/List";
import { withSnackbar } from "notistack";

//component
import Table from "./table";
//misc
import { apiEndpoints, exportURL } from "./../../endpoints";
import { messages } from "./../../messages";
import Popper from "@material-ui/core/Popper";
import Filter from "./filter";
import IconButtons from "./../../Shared/Button/IconButtons.js";
import {
  Dialog, DialogTitle, DialogContent, DialogContentText,
  DialogActions, Button, Table as MuiTable, TableHead, TableRow,
  TableCell, TableBody, CircularProgress,
} from "@material-ui/core";
import { API, instance } from "./../../axios";
import { param } from "jquery";
import { canEditBOQ } from "./../../helper";
import BOQEditModal from "./BOQEditModal";
// import OutwardInventory from "../OutwardInventory";


class List extends ListCommon {

  filterData = {};
  title = messages.common.boqStatus;
  state = {
    categoryArray: [], data: [], data2: [], options: [], options2: [],
    key: 1,
    summary: { total: 0, uniqueProducts: 0, onTrack: 0, atRisk: 0, exceeded: 0 },
    quickFilter: null,
    showBOQModal: false,
    boqModalData: null,
    showDeleteConfirm: false,
    deleteTarget: null,
    deleteSingleDetail: false,
    deleteInProgress: false,
    selectedRowIds: [],
    showBulkDeleteConfirm: false,
    bulkDeleteInProgress: false,
  };
  buildingTypeID = '';
  buildingUnitID = [];
  body = [];
  buildingTypeCount = 0;
  buildingUnitCount = 0;
  showDownload = false;
  dataExport = [];
  totalElements = 0;

  buildingType = [];
  buildingUnit = [];

  tableData = {
    headers: [
      messages.common.id,
      messages.common.category,
      messages.common.buildingType,
      messages.common.location,
      messages.common.inventory,
      "Work Areas",
      messages.common.boqQuantity,
      messages.common.outwardQuantity,
      messages.common.boqStatus,
    ],
    keys: [
      "id",
      "category",
      "buildingType",
      "buildingUnit",
      "product",
      "workAreaCount",
      "boqQuantity",
      "outwardQuantity",
      "status",
    ],
  };

  url = apiEndpoints.getBOQStatusDetails;
  boqStatus = true;

  componentDidMount() {
    this.filterRef = React.createRef();
    this.getOptions();
    this.getFiltersOptions();
    this.search();
  }

  // CategoryArray = new Array();
  // inventoryArray = new Array();
  // percentArray = new Array();


  async getFiltersOptions() {
    const response = await API.GET(apiEndpoints.stockDropdown);
    if (response.success) {
      this.dropdowns = response.data;
      this.props.setOptions(this.dropdowns);
    }
  }


  async getOptions() {
    const response = await API.GET(apiEndpoints.buildingType);
    if (response.success) {
      const options = [...response.data];
      this.buildingTypeCount = response.data.length;
      this.setState({ options: options });
    }
  }

  async getOptions2() {
    const response = await API.GET(apiEndpoints.getBuildingUnit + this.buildingTypeID.id);
    if (response.success) {
      const options = [...response.data.usageLocation];
      this.buildingUnitCount = response.data.usageLocationCount;
      this.setState({ options2: options });

    }
  }

  getExportData(response) {
    console.log('response at getExportData', response);
    let data = response.data.boqstatusDto.content
    if (data != null) {
      this.dataExport = [];
      for (let i = 0; i < data.length; i++) {
        const element = data[i];
        if (element.boqDetails != null) {
          for (let j = 0; j < element.boqDetails.length; j++) {
            const boqDetails = element.boqDetails[j];
            this.dataExport.push({
              id: element.id,
              category: element.category,
              buildingUnit: element.buildingUnit,
              inventory: element.product,
              totalBoqQuantity: element.boqQuantity,
              totalOutwardQuantity: element.outwardQuantity,
              finalLocation: boqDetails.finalLocation,
              boqQuantity: boqDetails.boqQuantity,
              outwardQuantity: boqDetails.outwardQuantity,
              status: element.status,
            })
          }
        }
      }
      console.log('this.dataExport', this.dataExport);
    }
    return this.dataExport;
  }

  prepareRequestBody() {
    let params;
    params = {};
    params.filterData = [];

    if (this.buildingType.length) {
      console.log(this.buildingType);
      params.filterData.push({
        attrName: "buildingType",
        attrValue: this.buildingType.map((v) => v.name),
      });
    }
    if (this.buildingUnit.length) {
      console.log(this.buildingUnit);
      params.filterData.push({
        attrName: "buildingUnit",
        attrValue: this.buildingUnit.map((v) => v.name),
      });
    }
    if (this.filterData) {
      for (const field in this.filterData) {
        let value = this.filterData[field];
        console.log(value);
        if (value && value.length) {
          if (["product", "category"].includes(field)) {
            value = value.map((v) => v.name);
          }
          else if (["consumedPercentage"].includes(field)) {
            console.log("%value",value);
            // let element=[];
            // for (let i = 0; i < value.length; i++) {
            //    element= value[i].split(" %").filter(v=>v!="");
            //   console.log(element);
            // }
            // let temp = value;
            // console.log(temp);
            // temp = temp.split("%");

            //  temp = temp.replace(/ %/g, '');
            // console.log(temp);
            value = value;
          }
          params.filterData.push({
            attrName: field,
            attrValue: value,
          });
        }
      }
    }
    if (this.state.quickFilter) {
      params.filterData.push({ attrName: 'statusGroup', attrValue: [this.state.quickFilter] });
    }
    return params;
  }

  async search(page = 0) {
    const params = this.prepareRequestBody();
    this.setState({ isLoading: true });
    const response = await this.getData(page, params);
    if (response.success) {
      const d = response.data;
      this.totalElements = d.boqstatusDto.totalElements;
      this.showDownload = this.totalElements !== 0;
      this.setState({
        data: d.boqstatusDto.content,
        pages: d.boqstatusDto.totalPages,
        totalRecords: d.boqstatusDto.totalElements,
        selectedRowIds: [],
        summary: {
          total:          d.totalCount          || 0,
          uniqueProducts: d.uniqueProductCount  || 0,
          onTrack:        d.onTrackCount        || 0,
          atRisk:         d.atRiskCount         || 0,
          exceeded:       d.exceededCount       || 0,
        },
      });
    }
  }

  setQuickFilter(filter) {
    const next = this.state.quickFilter === filter ? null : filter;
    this.setState({ quickFilter: next }, () => this.search(0));
  }

  onHandleBuildingType(value) {
    this.buildingType = value ? [value] : [];
    this.buildingTypeID = value;
    this.buildingUnit = [];
    this.buildingUnitID = [];
    this.setState({ options2: [] });
    if (value) {
      this.getOptions2();
    }
    this.search(0);
  }

  openEditModal = (row) => {
    // If the row has exactly one detail location, pre-fill it
    const detail = row.boqDetails && row.boqDetails.length === 1 ? row.boqDetails[0] : null;
    this.setState({
      showBOQModal: true,
      boqModalData: {
        buildingTypeId:   row.buildingTypeId,
        buildingTypeName: row.buildingType,
        buildingUnitId:   row.buildingUnitId,
        buildingUnitName: row.buildingUnit,
        productName:      row.product,
        finalLocation:    detail ? detail.finalLocation : undefined,
        quantity:         detail ? detail.boqQuantity : undefined,
      },
    });
  };

  openEditDetail = (parentRow, detail) => {
    this.setState({
      showBOQModal: true,
      boqModalData: {
        buildingTypeId:   parentRow.buildingTypeId,
        buildingTypeName: parentRow.buildingType,
        buildingUnitId:   parentRow.buildingUnitId,
        buildingUnitName: parentRow.buildingUnit,
        productName:      parentRow.product,
        finalLocation:    detail.finalLocation,
        quantity:         detail.boqQuantity,
      },
    });
  };

  handleDeleteDetail = (detail) => {
    // Wrap the single detail into the same confirm flow used for parent-row delete
    const syntheticRow = {
      product: detail.finalLocation,
      buildingUnit: '',
      boqDetails: [detail],
    };
    this.setState({ showDeleteConfirm: true, deleteTarget: syntheticRow, deleteSingleDetail: true });
  };

  handleDeleteBOQ = (row) => {
    this.setState({ showDeleteConfirm: true, deleteTarget: row });
  };

  executeDelete = async () => {
    const { deleteTarget } = this.state;
    if (!deleteTarget || !deleteTarget.boqDetails) return;
    const idsToDelete = deleteTarget.boqDetails.filter(d => d.boqUploadId);
    if (idsToDelete.length === 0) {
      this.props.enqueueSnackbar('Delete not available — please restart the backend service.', { variant: 'error' });
      return;
    }
    this.setState({ deleteInProgress: true });
    let allOk = true;
    for (const detail of idsToDelete) {
      const res = await API.DELETE(apiEndpoints.deleteBOQEntry + detail.boqUploadId);
      if (!res.success) {
        allOk = false;
        this.props.enqueueSnackbar('Failed to delete entry: ' + detail.finalLocation, { variant: 'error' });
      }
    }
    this.setState({ showDeleteConfirm: false, deleteTarget: null, deleteSingleDetail: false, deleteInProgress: false });
    if (allOk) {
      this.props.enqueueSnackbar('BOQ entry deleted successfully', { variant: 'success' });
    }
    this.search(0);
  };

  handleSelectRow = (rowId) => {
    this.setState(prev => {
      const ids = prev.selectedRowIds.includes(rowId)
        ? prev.selectedRowIds.filter(id => id !== rowId)
        : [...prev.selectedRowIds, rowId];
      return { selectedRowIds: ids };
    });
  };

  handleSelectAll = (checked) => {
    const ids = checked ? (this.state.data || []).map(r => r.id) : [];
    this.setState({ selectedRowIds: ids });
  };

  executeBulkDelete = async () => {
    const { selectedRowIds, data } = this.state;
    const selectedRows = (data || []).filter(r => selectedRowIds.includes(r.id));
    const allDetails = selectedRows.flatMap(r => r.boqDetails || []).filter(d => d.boqUploadId);
    if (allDetails.length === 0) {
      this.props.enqueueSnackbar('No deletable entries found — restart backend if issue persists.', { variant: 'error' });
      return;
    }
    this.setState({ bulkDeleteInProgress: true });
    let allOk = true;
    for (const detail of allDetails) {
      const res = await API.DELETE(apiEndpoints.deleteBOQEntry + detail.boqUploadId);
      if (!res.success) {
        allOk = false;
        this.props.enqueueSnackbar('Failed to delete: ' + detail.finalLocation, { variant: 'error' });
      }
    }
    this.setState({ showBulkDeleteConfirm: false, bulkDeleteInProgress: false, selectedRowIds: [] });
    if (allOk) {
      this.props.enqueueSnackbar(`${selectedRows.length} BOQ entry(s) deleted successfully`, { variant: 'success' });
    }
    this.search(0);
  };

  async downloadStatusExcel() {
    const params = new URLSearchParams();
    this.buildingType.forEach(bt => params.append('buildingType', bt.name));
    this.buildingUnit.forEach(bu => params.append('buildingUnit', bu.name));
    if (this.filterData) {
      const product = this.filterData.product;
      const category = this.filterData.category;
      const consumedPercentage = this.filterData.consumedPercentage;
      if (product && product.length) product.forEach(p => params.append('product', p.name || p));
      if (category && category.length) category.forEach(c => params.append('category', c.name || c));
      if (consumedPercentage && consumedPercentage.length) consumedPercentage.forEach(s => params.append('consumedPercentage', s));
    }
    const url = apiEndpoints.exportBOQStatus + (params.toString() ? '?' + params.toString() : '');
    try {
      const response = await instance.get(url, { responseType: 'blob' });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(new Blob([response.data]));
      link.download = 'BOQ_Status.xlsx';
      link.click();
    } catch (e) {
      console.error('BOQ Status download failed', e);
    }
  }

  onHandleBuildingUnit(value) {
    this.buildingUnit = value;
    this.buildingUnitID = value.map(v => v.id);
    this.search(0);
  }

  render() {
    const { summary, quickFilter, showBOQModal, boqModalData,
            showDeleteConfirm, deleteTarget, deleteInProgress,
            selectedRowIds, showBulkDeleteConfirm, bulkDeleteInProgress } = this.state;
    const hasSelection = selectedRowIds.length > 0;
    return (
      <div>
        <div className="list-section">

          {/* Summary cards */}
          <div className="boq-summary-cards">
            <div className={`boq-summary-card total${!quickFilter ? ' active' : ''}`} onClick={() => this.setQuickFilter(null)}>
              <div className="card-count">{summary.total}</div>
              <div className="card-label">Total Items</div>
            </div>
            <div className="boq-summary-card" style={{ cursor: 'default' }}>
              <div className="card-count">{summary.uniqueProducts}</div>
              <div className="card-label">Unique Products</div>
            </div>
            <div className={`boq-summary-card on-track${quickFilter === 'onTrack' ? ' active' : ''}`} onClick={() => this.setQuickFilter('onTrack')}>
              <div className="card-count">{summary.onTrack}</div>
              <div className="card-label">On Track (&lt;80%)</div>
            </div>
            <div className={`boq-summary-card at-risk${quickFilter === 'atRisk' ? ' active' : ''}`} onClick={() => this.setQuickFilter('atRisk')}>
              <div className="card-count">{summary.atRisk}</div>
              <div className="card-label">At Risk (80–100%)</div>
            </div>
            <div className={`boq-summary-card exceeded${quickFilter === 'exceeded' ? ' active' : ''}`} onClick={() => this.setQuickFilter('exceeded')}>
              <div className="card-count">{summary.exceeded}</div>
              <div className="card-label">Exceeded (&gt;100%)</div>
            </div>
          </div>

          {/* Quick filter chips */}
          <div className="boq-quick-filters">
            <span style={{ fontSize: 12, color: '#888', marginRight: 4 }}>Filter:</span>
            <button className={`boq-quick-filter-btn on-track${quickFilter === 'onTrack' ? ' active' : ''}`} onClick={() => this.setQuickFilter('onTrack')}>On Track</button>
            <button className={`boq-quick-filter-btn at-risk${quickFilter === 'atRisk' ? ' active' : ''}`}  onClick={() => this.setQuickFilter('atRisk')}>At Risk</button>
            <button className={`boq-quick-filter-btn exceeded${quickFilter === 'exceeded' ? ' active' : ''}`} onClick={() => this.setQuickFilter('exceeded')}>Exceeded</button>
            {quickFilter && <button className="boq-quick-filter-btn clear" onClick={() => this.setQuickFilter(null)}>✕ Clear</button>}
          </div>

          <div className="filter-section">
            {this.renderAutoCompleteBT(
              this.state.options,
              'Filter by Structure Type',
              (option) => option.name
            )}
            {this.renderAutoCompleteBU(
              this.state.options2,
              'Filter by Structure',
              (option) => option.name
            )}

            <div className="top-button-wrapper">
              {canEditBOQ() && (
                <IconButtons
                  onClick={() => this.setState({ showBOQModal: true, boqModalData: null })}
                  buttonClass="filterIcon"
                  label={"Add BOQ Entry"}
                  icon={"AddSVG"}
                />
              )}
              {canEditBOQ() && (
                <Button
                  variant="contained"
                  color="secondary"
                  disabled={!hasSelection}
                  onClick={() => this.setState({ showBulkDeleteConfirm: true })}
                  style={{ marginLeft: 8, textTransform: 'none', height: 36 }}
                >
                  {`Delete Selected${hasSelection ? ` (${selectedRowIds.length})` : ''}`}
                </Button>
              )}
              <IconButtons
                onClick={() => this.downloadStatusExcel()}
                buttonClass="filterIcon"
                label={"Download Excel"}
                icon={"DownloadSVG"}
              />
              <IconButtons
                onClick={() => {
                  this.setState({ filterOpen: true });
                }}
                buttonClass="filterIcon"
                innerRef={this.filterRef}
                label={messages.common.filter}
                icon={"FilterSVG"}
              />
            </div>
            <Popper
              open={this.state.filterOpen}
              anchorEl={this.filterRef && this.filterRef.current}
              placement="bottom-end"
            >
              <Filter
                filterData={this.filterData}
                options={this.dropdowns}
                search={(data) => {
                  this.filterData = data;
                  this.search();
                }}
                close={() => this.setState({ filterOpen: false })}
              />
            </Popper>
          </div>
          {this.state.isLoading ? (
            this.renderLoader()
          ) : (
            <Table
              key={this.state.key}
              tableData={this.tableData}
              rows={this.state.data}
              headerName={this.tableData.headers}
              hidedelete={true}
              hideedit={true}
              sortby={this.sortby}
              sortkey={this.sortkey}
              search={(sortkey, sortby) => {
                this.sortby = sortby;
                this.sortkey = sortkey;
                this.search();
              }}
              canEditBOQ={canEditBOQ()}
              onDeleteBOQ={(row) => this.handleDeleteBOQ(row)}
              onEditDetail={(parentRow, detail) => this.openEditDetail(parentRow, detail)}
              onDeleteDetail={(detail) => this.handleDeleteDetail(detail)}
              selectedRowIds={selectedRowIds}
              onSelectRow={(id) => this.handleSelectRow(id)}
              onSelectAll={(checked) => this.handleSelectAll(checked)}
            />
          )}
          {this.renderPagination()}
        </div>

        {/* Delete confirmation dialog */}
        <Dialog open={showDeleteConfirm} onClose={() => !deleteInProgress && this.setState({ showDeleteConfirm: false, deleteTarget: null, deleteSingleDetail: false })}>
          <DialogTitle>{this.state.deleteSingleDetail ? 'Delete Work Area Entry' : 'Delete BOQ Entry'}</DialogTitle>
          <DialogContent>
            <DialogContentText>
              {this.state.deleteSingleDetail
                ? <>This will permanently delete the BOQ entry for work area <strong>{deleteTarget && deleteTarget.product}</strong>. This cannot be undone.</>
                : <>This will permanently delete the following BOQ record(s) for{' '}
                    <strong>{deleteTarget && deleteTarget.product}</strong>
                    {' '}({deleteTarget && deleteTarget.buildingUnit}):</>
              }
            </DialogContentText>
            {deleteTarget && deleteTarget.boqDetails && !this.state.deleteSingleDetail && (
              <MuiTable size="small">
                <TableHead>
                  <TableRow>
                    <TableCell><strong>Work Area</strong></TableCell>
                    <TableCell align="right"><strong>BOQ Qty</strong></TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {deleteTarget.boqDetails.map((d, i) => (
                    <TableRow key={i}>
                      <TableCell>{d.finalLocation || '—'}</TableCell>
                      <TableCell align="right">{d.boqQuantity}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </MuiTable>
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => this.setState({ showDeleteConfirm: false, deleteTarget: null })} disabled={deleteInProgress}>
              Cancel
            </Button>
            <Button onClick={this.executeDelete} color="secondary" variant="contained" disabled={deleteInProgress}>
              {deleteInProgress ? <CircularProgress size={18} /> : 'Delete'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Bulk delete confirmation dialog */}
        <Dialog open={showBulkDeleteConfirm} onClose={() => !bulkDeleteInProgress && this.setState({ showBulkDeleteConfirm: false })}>
          <DialogTitle>Delete Selected BOQ Entries</DialogTitle>
          <DialogContent>
            <DialogContentText>
              This will permanently delete all BOQ records for the{' '}
              <strong>{selectedRowIds.length}</strong> selected item(s).
              This action cannot be undone.
            </DialogContentText>
            <MuiTable size="small">
              <TableHead>
                <TableRow>
                  <TableCell><strong>Product</strong></TableCell>
                  <TableCell><strong>Structure</strong></TableCell>
                  <TableCell align="right"><strong>BOQ Qty</strong></TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {(this.state.data || [])
                  .filter(r => selectedRowIds.includes(r.id))
                  .map((r, i) => (
                    <TableRow key={i}>
                      <TableCell>{r.product}</TableCell>
                      <TableCell>{r.buildingUnit}</TableCell>
                      <TableCell align="right">{r.boqQuantity}</TableCell>
                    </TableRow>
                  ))}
              </TableBody>
            </MuiTable>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => this.setState({ showBulkDeleteConfirm: false })} disabled={bulkDeleteInProgress}>
              Cancel
            </Button>
            <Button onClick={this.executeBulkDelete} color="secondary" variant="contained" disabled={bulkDeleteInProgress}>
              {bulkDeleteInProgress ? <CircularProgress size={18} /> : `Delete ${selectedRowIds.length} Item(s)`}
            </Button>
          </DialogActions>
        </Dialog>

        <BOQEditModal
          open={showBOQModal}
          onClose={() => this.setState({ showBOQModal: false, boqModalData: null })}
          initialData={boqModalData}
          stockDropdowns={this.dropdowns}
          onSaved={() => this.search(0)}
        />

      </div>
    );
  }
}

export default withSnackbar(List);
