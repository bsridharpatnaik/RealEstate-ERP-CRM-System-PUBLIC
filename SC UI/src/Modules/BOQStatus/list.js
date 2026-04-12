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
import { Dialog, Slide } from "@material-ui/core";
import Details from "./details";
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
    showDetails: false, key: 1,
    summary: { total: 0, onTrack: 0, atRisk: 0, exceeded: 0 },
    quickFilter: null,
    showBOQModal: false,
    boqModalData: null,
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
      messages.common.location,
      messages.common.inventory,
      messages.common.boqQuantity,
      messages.common.outwardQuantity,
      messages.common.boqStatus,
    ],
    keys: [
      "id",
      "category",
      "buildingUnit",
      "product",
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

  scrollBottom() {
    window.scrollTo(0, document.body.scrollHeight);
  }

  showDetail = (row) => {
    this.setState({ showDetails: true, selectedData: row });
    this.scrollBottom()
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
        summary: {
          total:    d.totalCount    || 0,
          onTrack:  d.onTrackCount  || 0,
          atRisk:   d.atRiskCount   || 0,
          exceeded: d.exceededCount || 0,
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
        buildingTypeId: row.buildingTypeId,
        buildingUnitId: row.buildingUnitId,
        productName: row.product,
        finalLocation: detail ? detail.finalLocation : undefined,
        quantity: detail ? detail.boqQuantity : undefined,
      },
    });
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
    const { summary, quickFilter, showBOQModal, boqModalData } = this.state;
    return (
      <div className={this.state.showDetails ? "split" : ""}>
        <div className="list-section">

          {/* Summary cards */}
          <div className="boq-summary-cards">
            <div className={`boq-summary-card total${!quickFilter ? ' active' : ''}`} onClick={() => this.setQuickFilter(null)}>
              <div className="card-count">{summary.total}</div>
              <div className="card-label">Total Items</div>
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
              'Filter by Building Type',
              (option) => option.name
            )}
            {this.renderAutoCompleteBU(
              this.state.options2,
              'Filter by Building Unit',
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
              showDetail={(row) => {
                this.showDetail(row)
              }}
              canEditBOQ={canEditBOQ()}
              onEditBOQ={(row) => this.openEditModal(row)}
            />
          )}
          {this.renderPagination()}
        </div>

        <BOQEditModal
          open={showBOQModal}
          onClose={() => this.setState({ showBOQModal: false, boqModalData: null })}
          initialData={boqModalData}
          stockDropdowns={this.dropdowns}
          onSaved={() => this.search(0)}
        />

        <Slide
          direction="right"
          in={this.state.showDetails}
          mountOnEnter
          unmountOnExit
          timeout={{ exit: 0 }}
        >
          <Details
            data={this.state.selectedData}
            edit={this.props.edit}
            delete={(row) => this.delete(row)}
            close={() =>
              this.setState({ showDetails: false, key: this.state.key + 1 })
            }
          />
        </Slide>
      </div>
    );
  }
}

export default withSnackbar(List);
