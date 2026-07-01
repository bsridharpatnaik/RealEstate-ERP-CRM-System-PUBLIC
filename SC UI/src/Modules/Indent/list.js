//react
import React from "react";
//Third Party
import ListCommon from "./../../Shared/List";
import { withSnackbar } from "notistack";
import TextField from "@material-ui/core/TextField";
import InputAdornment from "@material-ui/core/InputAdornment";
import IconButton from "@material-ui/core/IconButton";
import SearchIcon from "@material-ui/icons/Search";
import Button from "@material-ui/core/Button";
import AddIcon from "@material-ui/icons/Add";

//component
import Table from "./table";
import Details from "./details";
import Filter from "./filter";
import PODetails from "./../PurchaseOrder/details";
import InwardDetails from "./../InwardInventory/details";
//misc
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import { fetchEntityByRelation } from "./../../relationDetailsFetch";
import { messages } from "./../../messages";
import Popper from "@material-ui/core/Popper";
import IconButtons from "./../../Shared/Button/IconButtons.js";
import DetailsPopup from "./../../Shared/DetailsPopup";
//style
import "./style.scss";
import {
  canEditIndentRecord,
  canCancelIndentRecord,
  canCreateIndent,
  getFilenameFromContentDisposition,
  triggerBlobDownload,
  getSession,
  clearSession,
  getTenantName,
} from "./../../helper";

class List extends ListCommon {
  deleteKey = "indentId";
  deleteUrl = apiEndpoints.deleteIndent;
  title = messages.common.indent;
  filterData = {};
  searchValue = "";
  searchTimeout = null;
  page = 0;
  state = {
    pageno: 0,
    data: [],
    options: [],
    filterOptions: {},
    filterOpen: false,
    filterAnchorEl: null,
    key: 1,
    sortkey: null,
    sortby: null,
    showDetails: false,
    selectedData: null,
    currentIndex: 0,
    detailStack: [],
    allExpanded: false,
    tiles: {},
    activeTile: null,
    activeStatusChip: null,
  };
  tableData = {
    headers: [
      "Indent. No.",
      "Project",
      "Indent Date",
      "Inventory Count",
      "PO Numbers",
      "Status",
      "Created By",
    ],
    keys: [
      "indentId",
      "projectName",
      "indentDate",
      "inventoryCount",
      "poNumbers",
      "status",
      "createdBy",
    ],
  };
  url = apiEndpoints.getIndent;

  transformDataForTable(rawData, allTenant) {
    if (!Array.isArray(rawData)) return [];

    return rawData.map(item => {
      return {
        indentId: item.indentId,
        indentDate: item.indentDate,
        inventoryCount: item.inventoryList?.length || 0,
        status: item.indentStatus,
        createdBy: item.createdBy,
        inventoryItems: item.inventoryList,
        inventoryList: item.inventoryList,
        fileInformations: item.fileInformations,
        poNumbers: item.poNumbers || [],
        approvalAllowed: item.approvalAllowed,
        projectName: getTenantName(item.tenant, allTenant),
        tenant: item.tenant, // keep raw code — used internally / sent to backend
      };
    });
  }

  /** Convert API preset filterData array to list filter object. */
  presetFilterArrayToObject(arr) {
    if (!Array.isArray(arr) || !arr.length) return {};
    const obj = {};
    arr.forEach(({ attrName, attrValue }) => {
      if (attrValue == null) return;
      obj[attrName] = attrValue.length === 1 ? attrValue[0] : attrValue;
    });
    return obj;
  }

  componentDidMount() {
    const presetFilter = getSession("indentPresetFilterData");
    if (presetFilter && Array.isArray(presetFilter)) {
      this.filterData = this.presetFilterArrayToObject(presetFilter);
      clearSession("indentPresetFilterData");
    }
    this.filterRef = React.createRef();
    if (this.props.isGlobal && Array.isArray(this.props.allTenant)) {
      this.projectOptions = this.props.allTenant
        .filter((t) => t.tenantCode && t.tenantName)
        .map((t) => ({ name: t.tenantName, id: t.tenantCode }));
    }
    this.search();
    this.fetchTiles();
  }

  fetchTiles = async () => {
    const config = this.props.isGlobal ? { skipTenantId: true } : {};
    const response = await API.GET(apiEndpoints.getIndentTiles, config);
    if (response.success) {
      this.setState({ tiles: response.data || {} });
    }
  };

  fmtDate = (d) => {
    const dd = String(d.getDate()).padStart(2, "0");
    const mm = String(d.getMonth() + 1).padStart(2, "0");
    return `${dd}-${mm}-${d.getFullYear()}`;
  };

  INDENT_STATUS_CHIPS = [
    { status: "NEW",            label: "New",            color: "#1565c0", bg: "#e3f2fd" },
    { status: "APPROVED",       label: "Approved",       color: "#2e7d32", bg: "#e8f5e9" },
    { status: "PO PARTIAL",     label: "PO Partial",     color: "#e65100", bg: "#fff3e0" },
    { status: "PO COMPLETED",   label: "PO Completed",   color: "#4527a0", bg: "#ede7f6" },
    { status: "INWARD PARTIAL", label: "Inward Partial", color: "#00695c", bg: "#e0f2f1" },
    { status: "CLOSED",         label: "Closed",         color: "#37474f", bg: "#eceff1" },
    { status: "CANCELLED",      label: "Cancelled",      color: "#b71c1c", bg: "#ffebee" },
    { status: "REJECTED",       label: "Rejected",       color: "#880e4f", bg: "#fce4ec" },
  ];

  handleStatusChipClick = (status) => {
    const isActive = this.state.activeStatusChip === status;
    delete this.filterData.indentStatus;
    if (isActive) {
      this.setState({ activeStatusChip: null }, () => this.search(0));
    } else {
      this.filterData.indentStatus = [status];
      this.setState({ activeStatusChip: status }, () => this.search(0));
    }
  };

  renderStatusChips() {
    const { activeStatusChip, tiles } = this.state;
    const statusCounts = tiles.statusCounts || {};
    return (
      <div style={{ display: "flex", gap: 6, flexWrap: "wrap", padding: "4px 0 10px" }}>
        {this.INDENT_STATUS_CHIPS.map((c) => {
          const isActive = activeStatusChip === c.status;
          const count = statusCounts[c.status];
          return (
            <span
              key={c.status}
              onClick={() => this.handleStatusChipClick(c.status)}
              style={{
                cursor: "pointer",
                display: "inline-block",
                padding: "4px 12px",
                borderRadius: 16,
                fontSize: 12,
                fontWeight: isActive ? 700 : 500,
                background: isActive ? c.color : c.bg,
                color: isActive ? "#fff" : c.color,
                border: `1px solid ${c.color}`,
                userSelect: "none",
                transition: "all 0.15s",
              }}
            >
              {c.label}{count != null ? ` · ${count}` : ""}
            </span>
          );
        })}
      </div>
    );
  }

  INDENT_TILES = [
    { key: "thisWeek", label: "This Week", color: "#2980b9", bg: "#eaf2f8", countKey: "thisWeekCount", dateWindow: "week" },
    { key: "thisMonth", label: "This Month", color: "#27ae60", bg: "#eafaf1", countKey: "thisMonthCount", dateWindow: "month" },
    { key: "open", label: "Open", color: "#e67e22", bg: "#fdf2e9", countKey: "openCount", filterAttr: "indentStatus", filterValue: ["NEW", "APPROVED"] },
    { key: "quoteRequested", label: "Quote Requested", color: "#8e44ad", bg: "#f4ecf7", countKey: "quoteRequestedCount", filterAttr: "hasQuoteRequested", filterValue: "Yes" },
    { key: "stale", label: "Stale (3+ days)", color: "#795548", bg: "#efebe9", countKey: "staleCount", filterAttr: "staleBuckets", filterValue: "GT_3_DAYS" },
  ];

  handleTileClick = (tile) => {
    const isActive = this.state.activeTile === tile.key;

    // Clear all tile-driven filters
    if (this.state.activeTile) {
      const prev = this.INDENT_TILES.find((t) => t.key === this.state.activeTile);
      if (prev?.dateWindow) {
        delete this.filterData.startDate;
        delete this.filterData.endDate;
      }
      if (prev?.filterAttr === "indentStatus") {
        delete this.filterData.indentStatus;
      }
      if (prev?.filterAttr === "hasQuoteRequested") {
        delete this.filterData.hasQuoteRequested;
      }
      if (prev?.filterAttr === "staleBuckets") {
        delete this.filterData.staleBuckets;
      }
    }

    if (isActive) {
      this.setState({ activeTile: null }, () => this.search(0));
      return;
    }

    if (tile.filterAttr === "indentStatus") {
      this.filterData.indentStatus = tile.filterValue;
    } else if (tile.filterAttr) {
      this.filterData[tile.filterAttr] = tile.filterValue;
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
      <div style={{ display: "flex", gap: 12, flexWrap: "wrap", padding: "12px 0" }}>
        {this.INDENT_TILES.map((t) => {
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

  componentWillUnmount() {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
  }

  scrollBottom() {
    window.scrollTo(0, document.body.scrollHeight);
  }

  showDetail = (row) => {
    const currentIndex = this.state.data.findIndex(
      (item) => item.indentId === row.indentId
    );

    this.setState({
      showDetails: true,
      selectedData: row,
      currentIndex: currentIndex,
      detailStack: [],
    });
    this.scrollBottom();
  };

  handleOpenRelation = async (relation) => {
    const result = await fetchEntityByRelation(relation);
    if (!result.success) {
      this.props.enqueueSnackbar(result.errorMessage || "Failed to load", {
        variant: "error",
      });
      return;
    }
    const type = relation.relationType === "INDENT" ? "INDENT" : relation.relationType === "PO" ? "PO" : "INWARD";
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
        : { type: "INDENT", data: selectedData };
    if (!currentView.data) return null;

    const commonClose = this.handleCloseDetails;
    const commonRelation = this.handleOpenRelation;

    const fromRelation = detailStack.length > 0;
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
          onOpenRelation={commonRelation}
          fromRelation={fromRelation}
        />
      );
    }
    if (currentView.type === "INWARD") {
      return (
        <InwardDetails
          data={currentView.data}
          allEntries={[]}
          currentIndex={0}
          onNavigate={() => {}}
          edit={this.props.edit}
          delete={(row) => this.delete(row)}
          goToDetails={() => this.search()}
          close={commonClose}
          onOpenRelation={commonRelation}
          fromRelation={fromRelation}
          tenantCode={currentView.tenantCode}
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
        onRefresh={() => this.search()}
        close={commonClose}
        onOpenRelation={commonRelation}
        fromRelation={fromRelation}
        onResubmit={this.props.onResubmit}
      />
    );
  };

  handleSearchChange = (value) => {
    this.searchValue = value;

    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }

    this.searchTimeout = setTimeout(() => {
      this.search(0);
    }, 500);
  };

  replaceSortKey(sortKey) {
    return sortKey;
  }

  prepareRequestBody() {
    let params = {};
    params.filterData = [];

    if (this.searchValue && this.searchValue.trim().length) {
      params.filterData.push({
        attrName: "globalSearch",
        attrValue: [this.searchValue.trim()],
      });
    }

    if (this.filterData) {
      if (this.filterData.startDate) {
        params.filterData.push({
          attrName: "startDate",
          attrValue: Array.isArray(this.filterData.startDate) ? this.filterData.startDate : [this.filterData.startDate],
        });
      }

      if (this.filterData.endDate) {
        params.filterData.push({
          attrName: "endDate",
          attrValue: Array.isArray(this.filterData.endDate) ? this.filterData.endDate : [this.filterData.endDate],
        });
      }

      if (this.filterData.productNames && this.filterData.productNames.length > 0) {
        const value = this.filterData.productNames.map((v) => v.name);
        params.filterData.push({ attrName: "productNames", attrValue: value });
      }

      if (this.filterData.categoryNames && this.filterData.categoryNames.length > 0) {
        const value = this.filterData.categoryNames.map((v) => v.name);
        params.filterData.push({ attrName: "categoryNames", attrValue: value });
      }

      if (this.filterData.productCodes && this.filterData.productCodes.length > 0) {
        const value = this.filterData.productCodes.map((v) => v.name);
        params.filterData.push({ attrName: "productCodes", attrValue: value });
      }

      if (this.filterData.indentStatus && this.filterData.indentStatus.length > 0) {
        const value = this.filterData.indentStatus;
        params.filterData.push({
          attrName: "indentStatus",
          attrValue: Array.isArray(value) ? value : [value],
        });
      }

      if (this.filterData.lineItemStatus && this.filterData.lineItemStatus.length > 0) {
        const value = this.filterData.lineItemStatus;
        params.filterData.push({
          attrName: "lineItemStatus",
          attrValue: Array.isArray(value) ? value : [value],
        });
      }

      if (this.filterData.hasQuoteRequested) {
        params.filterData.push({
          attrName: "hasQuoteRequested",
          attrValue: [this.filterData.hasQuoteRequested === "Yes" ? "true" : "false"],
        });
      }

      if (this.filterData.staleBuckets) {
        const staleBucketVal = typeof this.filterData.staleBuckets === "object"
          ? this.filterData.staleBuckets.id
          : this.filterData.staleBuckets;
        if (staleBucketVal) {
          params.filterData.push({ attrName: "staleBuckets", attrValue: [staleBucketVal] });
        }
      }

      if (this.filterData.statusChangedTo) {
        const value = Array.isArray(this.filterData.statusChangedTo)
          ? this.filterData.statusChangedTo
          : [this.filterData.statusChangedTo];
        params.filterData.push({
          attrName: "statusChangedTo",
          attrValue: value,
        });
      }
      if (this.filterData.statusChangedAfterDate) {
        params.filterData.push({
          attrName: "statusChangedAfterDate",
          attrValue: [this.filterData.statusChangedAfterDate],
        });
      }
      if (this.filterData.statusChangedBeforeDate) {
        params.filterData.push({
          attrName: "statusChangedBeforeDate",
          attrValue: [this.filterData.statusChangedBeforeDate],
        });
      }

      const tenantsArr = Array.isArray(this.filterData.tenants)
        ? this.filterData.tenants
        : this.filterData.tenants != null ? [this.filterData.tenants] : [];
      if (tenantsArr.length > 0) {
        const tenantValues = tenantsArr.map(t =>
          t && typeof t === "object" ? t.id : t
        ).filter(Boolean);
        params.filterData.push({ attrName: "tenants", attrValue: tenantValues });
      }
    }

    return params;
  }

  async goToDetails() {
    if (this.state && this.state.pageno !== undefined) {
      const prevSelected = this.state.selectedData;
      const indentId = prevSelected?.indentId;
      await this.search(this.state.pageno);
      const nextSelected =
        (indentId
          ? (this.state.data || []).filter((x) => x.indentId === indentId)[0]
          : null) || prevSelected;
      this.setState({
        showDetails: true,
        selectedData: nextSelected,
      });
    }
  }

  /** Download indent list as Excel using current UI filters. When inside a project, tenant-id is sent automatically. */
  async exportToCSV() {
    const payload = this.prepareRequestBody();
    const config = this.props.isGlobal ? { skipTenantId: true } : {};
    const response = await API.POSTBlob(
      apiEndpoints.indentExportExcel,
      payload,
      config
    );
    if (!response.success) {
      this.props.enqueueSnackbar(
        response.errorMessage || "Download failed",
        { variant: "error" }
      );
      return;
    }
    const blob = response.data;
    const filename =
      getFilenameFromContentDisposition(response.headers) ||
      "indent_export.xlsx";
    triggerBlobDownload(blob, filename);
    this.props.enqueueSnackbar("Indent export downloaded", {
      variant: "success",
    });
  }

  async search(page = 0, sortkey = null, sortby = null) {
    this.page = page;
    if (sortkey !== null) this.sortkey = sortkey;
    if (sortby !== null) this.sortby = sortby;
    if (this.inputRef && this.inputRef.current) {
      this.inputRef.current.value = page + 1;
    }

    const params = this.prepareRequestBody();
    params.page = page;
    params.size = this.pageSize;

    let sortParam = "";
    if (this.sortkey) {
      let sortkeyValue = this.replaceSortKey(this.sortkey);
      sortParam = "&sort=" + sortkeyValue;
      if (this.sortby) {
        sortParam += "," + this.sortby;
      }
    }

    this.setState({ isLoading: true });
    if (this.props.isLoading) {
      this.props.isLoading(true);
    }

    const config = this.props.isGlobal ? { skipTenantId: true } : {};
    const baseUrl = this.url.replace(/([?&])size=\d+/, `$1size=${this.pageSize}`);
    const response = await API.POST(
      baseUrl + "&page=" + page + sortParam,
      params,
      config
    );

    // Don't forcibly close the details panel on refresh/search.
    // (Used by approve/reject refresh, and helps prevent blank details.)
    this.setState({ isLoading: false });
    if (this.props.isLoading) {
      this.props.isLoading(false);
    }

    this.showToaster(response);

    if (response.success) {
      const data = response.data || {};
      const dropdowns = data.iiDropdown || {};
      const options = [...(dropdowns.product || []), ...(dropdowns.supplier || [])];
      if (this.props.setOptions) {
        this.props.setOptions(dropdowns);
      }

      const rawData = data.indentInventories?.content || [];
      const transformedData = this.transformDataForTable(
        rawData,
        this.props.allTenant
      );

      this.setState({
        data: transformedData,
        pages: data.indentInventories?.totalPages || 0,
        totalRecords: data.indentInventories?.totalElements || 0,
        options: options,
        filterOptions: { ...dropdowns, ...(this.projectOptions ? { projects: this.projectOptions } : {}) },
        pageno: page,
        sortkey: sortkey !== null ? sortkey : this.state.sortkey,
        sortby: sortby !== null ? sortby : this.state.sortby,
      });
    }
  }

  render() {
    return (
      <div className="indent-list-wrapper">
        <div className="list-section">
          {this.renderTiles()}
          {this.renderStatusChips()}
          <div className="filter-section">
            <TextField
              variant="outlined"
              placeholder={messages.common.searchByName}
              defaultValue={this.searchValue}
              onChange={(e) => {
                this.handleSearchChange(e.target.value);
              }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon style={{ color: "rgba(108,108,108,0.6)" }} />
                  </InputAdornment>
                ),
              }}
              style={{ marginBottom: 0 }}
            />
            <div className={`top-button-wrapper${this.props.isGlobal ? " global-indent-no-add" : ""}`}>
              <IconButtons
                onClick={(e) => {
                  this.setState({ filterOpen: true, filterAnchorEl: e.currentTarget });
                }}
                buttonClass="filterIcon"
                innerRef={this.filterRef}
                label={messages.common.filter}
                icon={"MenuSVG"}
              />
              <Button
                onClick={() => this.setState(prev => ({ allExpanded: !prev.allExpanded }))}
                variant="outlined"
                size="small"
                style={{ textTransform: 'none', height: 34, minHeight: 34, marginRight: 4 }}
              >
                {this.state.allExpanded ? "⊖ Collapse All" : "⊕ Expand All"}
              </Button>
              {this.renderExport()}
              {!this.props.isGlobal && canCreateIndent() && (
                <Button
                  onClick={this.props.onAdd}
                  color="primary"
                  variant="contained"
                  startIcon={<AddIcon />}
                  classes={{
                    root: "add-button",
                    label: "add-label",
                  }}
                >
                  {messages.common.add} {messages.common.indent}
                </Button>
              )}
            </div>
            <Popper
              open={this.state.filterOpen}
              anchorEl={this.state.filterAnchorEl}
              placement="bottom-end"
            >
              <Filter
                filterData={this.filterData}
                options={this.state.filterOptions}
                isGlobal={this.props.isGlobal}
                search={(data) => {
                  this.filterData = data;
                  this.setState({ activeStatusChip: null });
                  this.search();
                }}
                close={() => this.setState({ filterOpen: false, filterAnchorEl: null })}
              />
            </Popper>
          </div>
          {this.state.isLoading ? (
            this.renderLoader()
          ) : (
            <Table
              key={this.state.key}
              tableData={this.tableData}
              rows={Array.isArray(this.state.data) ? this.state.data : []}
              edit={this.props.edit}
              delete={(row) => this.delete(row)}
              sortby={this.state.sortby}
              sortkey={this.state.sortkey}
              search={(sortkey, sortby) => {
                this.setState({ sortkey, sortby });
                this.search(0, sortkey, sortby);
              }}
              showDetail={(row) => {
                this.showDetail(row);
              }}
              refreshList={() => this.search(this.state.pageno)}
              enqueueSnackbar={this.props.enqueueSnackbar}
              isGlobal={this.props.isGlobal}
              allExpanded={this.state.allExpanded}
              canEditIndentRow={(row) => canEditIndentRecord(row?.status)}
              canDeleteIndentRow={(row) => canCancelIndentRecord(row?.status)}
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
      </div>
    );
  }
}

export default withSnackbar(List);
