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
import { apiEndpoints, noOfRecords } from "./../../endpoints";
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
  };
  tableData = {
    headers: [
      "Indent. No.",
      "Project",
      "Indent Date",
      "Expected Date",
      "Days Left",
      "Inventory Count",
      "PO Numbers",
      "Status",
      "Created By",
    ],
    keys: [
      "indentId",
      "projectName",
      "indentDate",
      "needByDate",
      "daysRemaining",
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
      // Helper: parse dd-MM-yyyy string to Date
      const parseDate = (s) => {
        if (!s) return null;
        const p = s.split("-");
        return p.length === 3 ? new Date(p[2], p[1] - 1, p[0]) : new Date(s);
      };

      // Effective expected date: header needByDate OR minimum of line-item dates
      let effectiveNeedByDate = item.needByDate || null;
      if (!effectiveNeedByDate && item.inventoryList && item.inventoryList.length > 0) {
        const itemDates = item.inventoryList.map((li) => li.needByDate).filter(Boolean);
        if (itemDates.length > 0) {
          effectiveNeedByDate = itemDates.reduce((min, d) => {
            const dMs = parseDate(d);
            const mMs = parseDate(min);
            return dMs && mMs && dMs < mMs ? d : min;
          });
        }
      }

      // Compute daysRemaining from effective date
      let daysRemaining = null;
      if (effectiveNeedByDate) {
        const deadline = parseDate(effectiveNeedByDate);
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        deadline.setHours(0, 0, 0, 0);
        daysRemaining = Math.round((deadline - today) / (1000 * 60 * 60 * 24));
      }

      return {
        indentId: item.indentId,
        indentDate: item.indentDate,
        needByDate: effectiveNeedByDate,
        daysRemaining,
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
        const value = this.filterData.productCodes.map((v) => v.id);
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

      if (this.filterData.staleBuckets && this.filterData.staleBuckets.id) {
        params.filterData.push({
          attrName: "staleBuckets",
          attrValue: [this.filterData.staleBuckets.id],
        });
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
    if (this.inputRef && this.inputRef.current) {
      this.inputRef.current.value = page + 1;
    }

    const params = this.prepareRequestBody();
    params.page = page;
    params.size = noOfRecords;

    let sortParam = "";
    if (sortkey || this.sortkey) {
      let sortkeyValue = sortkey || this.sortkey;
      sortkeyValue = this.replaceSortKey(sortkeyValue);
      sortParam = "&sort=" + sortkeyValue;
      if (sortby || this.sortby) {
        sortParam += "," + (sortby || this.sortby);
      }
    }

    this.setState({ isLoading: true });
    if (this.props.isLoading) {
      this.props.isLoading(true);
    }

    const config = this.props.isGlobal ? { skipTenantId: true } : {};
    const response = await API.POST(
      this.url + "&page=" + page + sortParam,
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
