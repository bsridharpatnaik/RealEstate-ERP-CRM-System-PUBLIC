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
import { API } from "./../../axios";
import { param } from "jquery";
// import OutwardInventory from "../OutwardInventory";


class List extends ListCommon {

  filterData = {};
  title = messages.common.boqStatus;
  state = { categoryArray: [], data: [], data2: [], options: [], options2: [], showDetails: false, key: 1 };
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
  exportUrl = exportURL.getBOQStatus;
  exportFile = messages.exportFiles.BOQStatus;
  ignoreQueryParamsExport = true;
  boqStatus = true;

  componentDidMount() {
    // this.search();
    this.filterRef = React.createRef();
    this.getOptions();
    this.getFiltersOptions();
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
    return params;
  }

  async search(page = 0) {
    const params = this.prepareRequestBody();
    this.setState({ isLoading: true });
    const response = await this.getData(page, params);
    console.log(response);
    this.totalElements = response.data.boqstatusDto.totalElements;
    console.log('totalElements: ', this.totalElements);
    if (this.totalElements !== 0) {
      this.showDownload = true;
    }
    else {
      this.showDownload = false;
    }

    if (response.success) {
      this.setState({
        data: response.data.boqstatusDto.content,
        pages: response.data.boqstatusDto.totalPages,
        totalRecords: response.data.boqstatusDto.totalElements,
      });
    }
  }

  onHandleBuildingType(value) {
    this.buildingType = [value];
    if (value === null) {
      this.buildingType = [];
    }
    console.log('Building Type: ', this.buildingType);
    this.buildingTypeID = value;
    this.getOptions()
    if (value) {
      this.getOptions2()
    }
    // this.getBOQStatusAPI(value);
  }

  onHandleBuildingUnit(value) {
    this.buildingUnit = value;
    console.log('Building Unit: ', this.buildingUnit);
    let buildingUnitArray = new Array();
    for (let i = 0; i < value.length; i++) {
      buildingUnitArray.push(value[i].id);
    }
    this.buildingUnitID = buildingUnitArray;

    // this.buildingUnitID = value;
    // console.log(value);
    // if(this.buildingUnitID.length===0){
    //   this.showDownload = false;
    //   console.log(this.showDownload);
    // }

    // console.log('this.buildingUnitID: ', this.buildingUnitID);
  }

  render() {
    return (
      <div className={this.state.showDetails ? "split" : ""}>
        <div className="list-section">
          <div className="filter-section">
            <form
              onSubmit={(e) => {
                e.preventDefault();
                // this.getOptions2(0);
              }}
            >
              {this.renderAutoCompleteBT(
                this.state.options,
                'Select Building Type',
                (option) => {
                  return option.name;
                }
              )}
              total: {this.buildingTypeCount}
            </form>

            <form
              onSubmit={(e) => {
                e.preventDefault();
                this.search(0);
              }}
            >
              {this.renderAutoCompleteBU(
                this.state.options2,
                'Select Building Unit',
                (option) => {
                  return option.name;
                }
              )}
              total: {this.buildingUnitCount}
            </form>

            <div className="top-button-wrapper">
              {
                this.showDownload &&
                this.renderExport()}
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
            />
          )}
          {this.renderPagination()}
        </div>
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
