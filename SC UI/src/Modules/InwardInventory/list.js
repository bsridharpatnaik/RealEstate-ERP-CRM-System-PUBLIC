//react
import React from "react";
//Third Party
import ListCommon from "./../../Shared/List";
import { withSnackbar } from "notistack";

//component
import Table from "./table";
import IndentDetails from "./../Indent/details";
import PODetails from "./../PurchaseOrder/details";
import Details from "./details";
//misc
import { apiEndpoints, exportURL } from "./../../endpoints";
import { messages } from "./../../messages";
import Popper from "@material-ui/core/Popper";
import Filter from "./filter";
import Button from "./../../Shared/Button";
import IconButtons from "./../../Shared/Button/IconButtons.js";
import DetailsPopup from "./../../Shared/DetailsPopup";
import { Slide } from "@material-ui/core";
import Total from "./../../Shared/TotalSidePanel";
import { API } from "../../axios";
import { fetchEntityByRelation } from "./../../relationDetailsFetch";

class List extends ListCommon {
  deleteKey = "inwardId";
  deleteUrl = apiEndpoints.deleteInwardInventory;
  title = messages.common.inwardInventory;
  filterData = {};
  state = {
    pageno: 0,
    data: [],
    options: [],
    showDetails: false,
    selectedData: null,
    key: 1,
    showTotal: false,
    totals: [],
    currentIndex: 0,
    detailStack: [],
  };
  tableData = {
    headers: [
      messages.common.id,
      messages.fields.date,
      messages.fields.supplier,
      "PO No",
      "Inventory Count",
      "Challan No",
      "Challan Date",
      "Bill No",
      "Bill Date",
      "Doc Status",
      "Inward Type",
      "Created By",
    ],
    keys: [
      "inwardId",
      "date",
      "supplier",
      "purchaseOrderNo",
      "inventoryCount",
      "challanNo",
      "challanDate",
      "billNo",
      "billDate",
      "missingChallanBillFlag",
      "inwardType",
      "createdBy",
    ],
  };
  url = apiEndpoints.getInwardInventory;
  exportUrl = exportURL.getInwardInventory;
  exportFile = messages.exportFiles.inwardInventory;
  ignoreQueryParamsExport = true;

  componentDidMount() {
    this.search();
    this.filterRef = React.createRef();
  }
  replaceSortKey(sortKey) {
    if (sortKey === "warehouse") {
      return "warehouse.warehouseName";
    } else if (sortKey === "supplier") {
      return "supplier.name";
    }
    return sortKey;
  }
  prepareRequestBody() {
    let params;
    params = {};
    params.filterData = [];
    if (this.searchValue.length) {
      const searchVal = this.searchValue.map((v) => v.name);
      params.filterData.push({
        attrName: "globalSearch",
        attrValue: Array.isArray(searchVal) ? searchVal : [searchVal],
      });
    }
    if (this.filterData) {
      for (const field in this.filterData) {
        let value = this.filterData[field];

        if (value && value.length) {
          if (
            ["productNames", "supplierNames", "warehouseNames", "categoryNames"].includes(field)
          ) {
            value = value.map((v) => v.name);
          } else if (["startDate", "endDate", "showOnlyRejected", "textSearch", "missingChallanBill"].includes(field)) {
            value = [value];
          }
          params.filterData.push({
            attrName: field,
            attrValue: Array.isArray(value) ? value : [value],
          });
        }
      }
    }
    return params;
  }
  async goToDetails() {
    if (this.state && this.state.pageno !== undefined) {
      await this.search(this.state.pageno);

      this.setState({
        showDetails: true,
        showTotal: false,
        selectedData: this.state.data.filter(
          (x) => x.inwardId === this.state.selectedData.inwardId
        )[0],
      });
    }
  }
  getExportData(response) {
    return response.data;
  }
  async search(page = 0, sortkey = null, sortby = null) {
    if (sortkey !== null) this.sortkey = sortkey;
    if (sortby !== null) this.sortby = sortby;
    this.setState({ pageno: page });
    const params = this.prepareRequestBody();
    const response = await this.getData(page, params);
    this.getTotals();
    if (response.success) {
      this.dropdowns = response.data.iiDropdown;
      const options = [...this.dropdowns.product, ...this.dropdowns.supplier];
      this.props.setOptions(this.dropdowns);
      this.setState({
        data: response.data.inwardInventory.content,
        pages: response.data.inwardInventory.totalPages,
        totalRecords: response.data.inwardInventory.totalElements,
        options: options,
        // totals: response.data.totals,
      });
    }
  }

  
  scrollBottom(){
    window.scrollTo(0, document.body.scrollHeight);
  }
  
  showDetail = (row) => {
    const currentIndex = this.state.data.findIndex(
      (item) => item.inwardId === row.inwardId
    );
    this.setState({
      showDetails: true,
      showTotal: false,
      selectedData: row,
      currentIndex: currentIndex,
      detailStack: [],
    });
    this.scrollBottom();
  };

  handleOpenPO = async (poId) => {
    const result = await fetchEntityByRelation({
      relationType: "PO",
      referenceId: String(poId),
    });
    if (!result.success) {
      this.props.enqueueSnackbar(result.errorMessage || "Failed to load PO", {
        variant: "error",
      });
      return;
    }
    this.setState((prev) => ({
      detailStack: [...prev.detailStack, { type: "PO", data: result.data }],
    }));
  };

  handleOpenIndent = async (indentId) => {
    const result = await fetchEntityByRelation({
      relationType: "INDENT",
      referenceId: String(indentId),
    });
    if (!result.success) {
      this.props.enqueueSnackbar(result.errorMessage || "Failed to load Indent", {
        variant: "error",
      });
      return;
    }
    this.setState((prev) => ({
      detailStack: [...prev.detailStack, { type: "INDENT", data: result.data }],
    }));
  };

  handleOpenRelation = async (relation) => {
    const result = await fetchEntityByRelation(relation);
    if (!result.success) {
      this.props.enqueueSnackbar(result.errorMessage || "Failed to load", {
        variant: "error",
      });
      return;
    }
    const type =
      relation.relationType === "INDENT"
        ? "INDENT"
        : relation.relationType === "PO"
        ? "PO"
        : "INWARD";
    this.setState((prev) => ({
      detailStack: [...prev.detailStack, { type, data: result.data, tenantCode: relation.tenant || null }],
    }));
  };

  handleCloseDetails = () => {
    this.setState({
      showDetails: false,
      detailStack: [],
      key: this.state.key + 1,
    });
  };

  handleDetailBack = () => {
    this.setState((prev) => ({
      detailStack: prev.detailStack.slice(0, -1),
    }));
  };

  renderDetailsContent = () => {
    const { detailStack, selectedData, data, currentIndex } = this.state;
    const currentView =
      detailStack.length > 0
        ? detailStack[detailStack.length - 1]
        : { type: "INWARD", data: selectedData };
    if (!currentView.data) return null;

    const commonClose = this.handleCloseDetails;
    const fromRelation = detailStack.length > 0;

    if (currentView.type === "INDENT") {
      return (
        <IndentDetails
          data={currentView.data}
          allEntries={[]}
          currentIndex={0}
          onNavigate={() => {}}
          edit={this.props.edit}
          delete={(row) => this.delete(row)}
          goToDetails={() => this.search()}
          onRefresh={() => this.search()}
          close={commonClose}
          onOpenRelation={this.handleOpenRelation}
          fromRelation={fromRelation}
        />
      );
    }
    if (currentView.type === "PO") {
      return (
        <PODetails
          data={currentView.data}
          allEntries={[]}
          currentIndex={0}
          onNavigate={() => {}}
          edit={this.props.edit}
          delete={(row) => this.delete(row)}
          onRefresh={() => this.search()}
          close={commonClose}
          onOpenRelation={this.handleOpenRelation}
          fromRelation={fromRelation}
        />
      );
    }
    return (
      <Details
        data={currentView.data}
        allEntries={data}
        currentIndex={currentIndex}
        onNavigate={(newIndex) => {
          if (newIndex >= 0 && newIndex < data.length) {
            this.setState({
              selectedData: data[newIndex],
              currentIndex: newIndex,
            });
          }
        }}
        edit={this.props.edit}
        delete={(row) => this.delete(row)}
        goToDetails={() => this.goToDetails()}
        close={commonClose}
        onOpenPO={this.handleOpenPO}
        onOpenIndent={this.handleOpenIndent}
        fromRelation={fromRelation}
      />
    );
  };

  getTotals = async () => {
    const params = this.prepareRequestBody();
    const response = await API.POST(apiEndpoints.getInwardInventoryTotals, params);
    if (response.success) {
      this.setState({ totals: response?.data ?? [] });
    }
  }

  render() {
    return (
      <div className={this.state.showTotal ? "split" : ""}>
        <div className="list-section">
          <div className="filter-section">
            <form
              onSubmit={(e) => {
                e.preventDefault();
                this.search(0);
              }}
            >
              {this.renderAutoComplete(
                this.state.options,
                messages.common.searchByName,
                (option) => {
                  return option.name;
                }
              )}
            </form>
            <div className="top-button-wrapper">
              <Button
                onClick={() => {
                  this.setState({ showTotal: true, showDetail: false });
                }}
                buttonClass="filter"
                innerRef={this.filterRef}
                label={messages.common.total}
              />
              {this.renderExport()}
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
              edit={this.props.edit}
              delete={(row) => this.delete(row)}
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
        <DetailsPopup
          open={this.state.showDetails}
          onClose={this.handleCloseDetails}
          onBack={this.handleDetailBack}
          showBackButton={this.state.detailStack.length > 0}
        >
          {this.renderDetailsContent()}
        </DetailsPopup>
        <Slide
          direction="right"
          in={this.state.showTotal}
          mountOnEnter
          unmountOnExit
          timeout={{ exit: 0 }}
        >
          <Total
            totals={this.state.totals}
            close={() => this.setState({ showTotal: false })}
          />
        </Slide>
      </div>
    );
  }
}

export default withSnackbar(List);
