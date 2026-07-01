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
  searchDebounceTimer = null;
  state = {
    pageno: 0,
    data: [],
    globalSearchText: "",
    showDetails: false,
    selectedData: null,
    key: 1,
    showTotal: false,
    totals: [],
    currentIndex: 0,
    detailStack: [],
    tiles: {},
    activeTile: null,
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
    this.fetchTiles();
    this.filterRef = React.createRef();
  }

  fetchTiles = async () => {
    const response = await API.GET(apiEndpoints.getInwardInventoryTiles);
    if (response.success) {
      this.setState({ tiles: response.data || {} });
    }
  };

  fmtDate = (d) => {
    const dd = String(d.getDate()).padStart(2, "0");
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    return `${dd}-${mm}-${d.getFullYear()}`;
  };

  TILES = [
    { key: "missingChallan",   label: "Missing Challan/Bill",color: "#e67e22", bg: "#fdf2e9", countKey: "missingChallanBillCount", filterAttr: "missingChallanBill" },
    { key: "reject",           label: "Having Reject",       color: "#e74c3c", bg: "#fdedec", countKey: "rejectCount",           filterAttr: "showOnlyRejected" },
    { key: "fromPO",           label: "From PO",             color: "#8e44ad", bg: "#f4ecf7", countKey: "fromPOCount",           filterAttr: "inwardType", filterValue: "PO" },
    { key: "direct",           label: "Direct",              color: "#16a085", bg: "#e8f8f5", countKey: "directCount",           filterAttr: "inwardType", filterValue: "DIRECT" },
    { key: "sample",           label: "Sample",              color: "#d35400", bg: "#fef9e7", countKey: "sampleCount",           filterAttr: "inwardType", filterValue: "SAMPLE" },
    { key: "thisWeek",         label: "This Week",           color: "#2980b9", bg: "#eaf2f8", countKey: "thisWeekCount",         dateWindow: "week" },
    { key: "thisMonth",        label: "This Month",          color: "#27ae60", bg: "#eafaf1", countKey: "thisMonthCount",        dateWindow: "month" },
  ];

  handleTileClick = (tile) => {
    const isActive = this.state.activeTile === tile.key;

    delete this.filterData.missingChallanBill;
    delete this.filterData.showOnlyRejected;
    delete this.filterData.inwardType;
    if (this.state.activeTile && this.TILES.find((t) => t.key === this.state.activeTile)?.dateWindow) {
      delete this.filterData.startDate;
      delete this.filterData.endDate;
    }

    if (isActive) {
      this.setState({ activeTile: null }, () => this.search(0));
      return;
    }

    if (tile.filterAttr) {
      this.filterData[tile.filterAttr] = tile.filterValue || "true";
    } else if (tile.dateWindow) {
      const end = new Date();
      const start = new Date();
      if (tile.dateWindow === "week") {
        const day = start.getDay();
        const diffToMonday = day === 0 ? 6 : day - 1;
        start.setDate(start.getDate() - diffToMonday);
      } else {
        start.setDate(1);
      }
      start.setHours(0, 0, 0, 0);
      this.filterData.startDate = this.fmtDate(start);
      this.filterData.endDate = this.fmtDate(end);
    }
    this.setState({ activeTile: tile.key }, () => this.search(0));
  };

  renderTiles() {
    const { tiles, activeTile } = this.state;
    return (
      <div style={{ display: "flex", gap: 12, overflowX: "auto", padding: "12px 0" }}>
        {this.TILES.map((t) => {
          const count = tiles[t.countKey] || 0;
          const isActive = activeTile === t.key;
          return (
            <div
              key={t.key}
              onClick={() => this.handleTileClick(t)}
              style={{
                cursor: "pointer",
                minWidth: 130,
                padding: "10px 16px",
                borderRadius: 8,
                background: t.bg,
                border: isActive ? `2px solid ${t.color}` : "2px solid transparent",
                boxShadow: isActive ? "0 1px 4px rgba(0,0,0,0.15)" : "none",
              }}
            >
              <div style={{ fontSize: 22, fontWeight: 700, color: t.color }}>{count}</div>
              <div style={{ fontSize: 12, color: "#555", marginTop: 2 }}>{t.label}</div>
            </div>
          );
        })}
      </div>
    );
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
    if (this.state.globalSearchText.trim().length) {
      params.filterData.push({
        attrName: "globalSearch",
        attrValue: [this.state.globalSearchText.trim()],
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
          } else if (["startDate", "endDate", "showOnlyRejected", "textSearch", "missingChallanBill", "inwardType"].includes(field)) {
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
      this.props.setOptions(this.dropdowns);
      this.setState({
        data: response.data.inwardInventory.content,
        pages: response.data.inwardInventory.totalPages,
        totalRecords: response.data.inwardInventory.totalElements,
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
          {this.renderTiles()}
          <div className="filter-section">
            <form
              onSubmit={(e) => {
                e.preventDefault();
                clearTimeout(this.searchDebounceTimer);
                this.search(0);
              }}
            >
              <input
                className="global-search-input"
                type="text"
                placeholder="Search by ID, product, supplier, bill no, challan no..."
                value={this.state.globalSearchText}
                onChange={(e) => {
                  const val = e.target.value;
                  this.setState({ globalSearchText: val });
                  clearTimeout(this.searchDebounceTimer);
                  this.searchDebounceTimer = setTimeout(() => this.search(0), 3000);
                }}
              />
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
