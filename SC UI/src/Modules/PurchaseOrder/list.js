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
import IndentDetails from "./../Indent/details";
import InwardDetails from "./../InwardInventory/details";
//misc
import { messages } from "./../../messages";
import Popper from "@material-ui/core/Popper";
import IconButtons from "./../../Shared/Button/IconButtons.js";
import DetailsPopup from "./../../Shared/DetailsPopup";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import CircularProgress from "@material-ui/core/CircularProgress";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import {
  getFilenameFromContentDisposition,
  triggerBlobDownload,
  getSession,
  clearSession,
} from "./../../helper";
import { fetchEntityByRelation } from "./../../relationDetailsFetch";
//style
import "./style.scss";
import moment from "moment";
import { canEditInventoryModules } from "./../../helper";

class List extends ListCommon {
  deleteKey = "purchaseOrderId";
  deleteUrl = apiEndpoints.deletePurchaseOrder;
  title = messages.common.purchaseOrder;
  filterData = {};
  searchValue = "";
  searchTimeout = null;
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
    draftsDialogOpen: false,
    drafts: [],
    isDraftsLoading: false,
  };
  tableData = {
    headers: [
      "PO Number",
      "Date Creation",
      "Inventory Count",
      "Project",
      "Supplier Name",
      "Created By",
      "PO Status",
      "SPL",
    ],
    keys: [
      "poNumber",
      "dateCreation",
      "inventoryCount",
      "projectName",
      "supplierName",
      "createdBy",
      "poStatus",
      "specialPo",
    ],
  };

  transformDataForTable(rawData) {
    if (!Array.isArray(rawData)) return [];

    return rawData.map(item => ({
      // Keep original API data for details view
      ...item,
      // Override with table display fields
      poNumber: item.purchaseOrderId || "",
      dateCreation: item.poDate || "",
      inventoryCount: item.lines ? item.lines.length : 0,
      supplierName: item.supplier ? item.supplier.name : "",
      createdBy: item.createdBy || "",
      poStatus: item.status || "",
      projectName: item.projectName || null,
      specialPo: item.specialPo || false,
    }));
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
    const presetFilter = getSession("poPresetFilterData");
    if (presetFilter && Array.isArray(presetFilter)) {
      this.filterData = this.presetFilterArrayToObject(presetFilter);
      clearSession("poPresetFilterData");
    }
    this.filterRef = React.createRef();
    this.fetchDropdownOptions();
    this.search();
  }

  componentWillUnmount() {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
  }

  async fetchDropdownOptions() {
    try {
      const [poRes, tenantsRes] = await Promise.all([
        API.POST(apiEndpoints.getPurchaseOrder, { filterData: [] }),
        API.GET(apiEndpoints.getTenants),
      ]);

      let filterOptions = {};
      if (poRes.success && poRes.data && poRes.data.poDropdown) {
        filterOptions = { ...poRes.data.poDropdown };
      }

      if (tenantsRes.success && Array.isArray(tenantsRes.data)) {
        const projectOptions = tenantsRes.data
          .filter((t) => t.inventory === true)
          .map((t) => { const name = t.name || t.tenantName || ""; return { name, id: name }; })
          .filter((p) => p.name);
        filterOptions.projects = [{ name: "No Project", id: "EMPTY" }, ...projectOptions];
      }

      this.setState({ filterOptions });
    } catch (error) {
      console.error("Error fetching dropdown options:", error);
      this.setState({
        filterOptions: {
          category: [],
          product: [],
          supplier: [],
          productCodes: [],
          purchaseOrderStatus: [],
          projects: [],
        }
      });
    }
  }


  scrollBottom() {
    window.scrollTo(0, document.body.scrollHeight);
  }

  showDetail = async (row) => {
    const currentIndex = this.state.data.findIndex(
      (item) => item.purchaseOrderId === row.purchaseOrderId
    );
    // Open the panel immediately with list data so it feels instant
    this.setState({
      showDetails: true,
      selectedData: row,
      currentIndex: currentIndex,
      detailStack: [],
    });
    this.scrollBottom();

    // Fetch full PO via GET /{id} to populate @Transient needByDate on each line
    try {
      const response = await API.GET(
        apiEndpoints.getPurchaseOrderDetail(row.purchaseOrderId)
      );
      if (response.success && response.data) {
        this.setState((prev) => {
          // Only update if the same PO is still open (user hasn't navigated away)
          if (
            prev.selectedData &&
            prev.selectedData.purchaseOrderId === row.purchaseOrderId &&
            prev.detailStack.length === 0
          ) {
            return { selectedData: { ...prev.selectedData, ...response.data } };
          }
          return null;
        });
      }
    } catch (e) {
      // non-fatal — panel already shows with partial data
    }
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
      detailStack: [...prev.detailStack, { type, data: result.data, tenant: relation.tenant }],
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
        : { type: "PO", data: selectedData };
    if (!currentView.data) return null;

    const commonClose = this.handleCloseDetails;
    const commonRelation = this.handleOpenRelation;

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
          tenantCode={currentView.tenant}
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
        onRefresh={() => this.search()}
        close={commonClose}
        onOpenRelation={commonRelation}
        fromRelation={fromRelation}
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
    if (sortKey === "poNumber") {
      return "purchaseOrderId";
    } else if (sortKey === "dateCreation") {
      return "poDate";
    } else if (sortKey === "supplierName") {
      return "supplier.name";
    } else if (sortKey === "poStatus") {
      return "status";
    }
    return sortKey;
  }

  prepareRequestBody() {
    let params = {};
    params.filterData = [];

    // Handle global search from the search input
    if (this.searchValue && this.searchValue.trim().length) {
      params.filterData.push({
        attrName: "globalSearch",
        attrValue: [this.searchValue.trim()],
      });
    }

    // Handle filter data from the filter component
    if (this.filterData) {
      // Handle startDate and endDate
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

        if (this.filterData.isSpecialPo) {
          params.filterData.push({
            attrName: "isSpecialPo",
            attrValue: [this.filterData.isSpecialPo],
          });
        }

      // Handle multi-select filters - convert objects to IDs (normalize to array for preset/single values)
      const productNamesArr = Array.isArray(this.filterData.productNames)
        ? this.filterData.productNames
        : this.filterData.productNames != null ? [this.filterData.productNames] : [];
      if (productNamesArr.length > 0) {
        const productNames = productNamesArr.map(product =>
          product && typeof product === "object" ? product.name : product
        ).filter(Boolean);
        params.filterData.push({ attrName: "productNames", attrValue: productNames });
      }

      const productCodesArr = Array.isArray(this.filterData.productCodes)
        ? this.filterData.productCodes
        : this.filterData.productCodes != null ? [this.filterData.productCodes] : [];
      if (productCodesArr.length > 0) {
        const productCodeNames = productCodesArr.map(code =>
          code && typeof code === "object" ? code.name : code
        ).filter(Boolean);
        params.filterData.push({ attrName: "productCodes", attrValue: productCodeNames });
      }

      const statusArr = Array.isArray(this.filterData.status)
        ? this.filterData.status
        : this.filterData.status != null ? [this.filterData.status] : [];
      if (statusArr.length > 0) {
        const statusValues = statusArr.map(status =>
          status && typeof status === "object" ? (status.id || status.name) : status
        );
        params.filterData.push({
          attrName: "status",
          attrValue: statusValues,
        });
      }

      const categoryNamesArr = Array.isArray(this.filterData.categoryNames)
        ? this.filterData.categoryNames
        : this.filterData.categoryNames != null ? [this.filterData.categoryNames] : [];
      if (categoryNamesArr.length > 0) {
        const categoryIds = categoryNamesArr.map(cat =>
          cat && typeof cat === "object" ? cat.id : cat
        ).filter(Boolean);
        params.filterData.push({ attrName: "categoryNames", attrValue: categoryIds });
      }

      const suppliersArr = Array.isArray(this.filterData.suppliers)
        ? this.filterData.suppliers
        : this.filterData.suppliers != null ? [this.filterData.suppliers] : [];
      if (suppliersArr.length > 0) {
        const supplierNames = suppliersArr.map(supplier =>
          supplier && typeof supplier === "object" ? supplier.name : supplier
        ).filter(Boolean);
        params.filterData.push({ attrName: "suppliers", attrValue: supplierNames });
      }

      if (this.filterData.staleBuckets && this.filterData.staleBuckets.id) {
        params.filterData.push({
          attrName: "staleBuckets",
          attrValue: [this.filterData.staleBuckets.id],
        });
      }

      if (this.filterData.statusChangedTo) {
        const statusVal = this.filterData.statusChangedTo;
        const value = statusVal && typeof statusVal === "object"
          ? (statusVal.name ?? statusVal.id)
          : statusVal;
        if (value) {
          params.filterData.push({
            attrName: "statusChangedTo",
            attrValue: [value],
          });
        }
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

      // Priority filter
      const priorityArr = Array.isArray(this.filterData.priority)
        ? this.filterData.priority
        : this.filterData.priority != null ? [this.filterData.priority] : [];
      if (priorityArr.length > 0) {
        const priorityValues = priorityArr.map(p =>
          p && typeof p === "object" ? (p.id || p.name) : p
        ).filter(Boolean);
        params.filterData.push({ attrName: "priority", attrValue: priorityValues });
      }

      // Project filter
      const projectNamesArr = Array.isArray(this.filterData.projectNames)
        ? this.filterData.projectNames
        : this.filterData.projectNames != null ? [this.filterData.projectNames] : [];
      if (projectNamesArr.length > 0) {
        const projectValues = projectNamesArr.map(p =>
          p && typeof p === "object" ? p.id : p
        ).filter(Boolean);
        params.filterData.push({ attrName: "projectNames", attrValue: projectValues });
      }
    }

    return params;
  }

  fetchDrafts = async () => {
    this.setState({ isDraftsLoading: true });
    const response = await API.GET(apiEndpoints.listDrafts("PO"));
    this.setState({ isDraftsLoading: false });
    if (response.success) {
      this.setState({ drafts: response.data || [] });
    } else {
      this.props.enqueueSnackbar("Failed to load drafts", { variant: "error" });
    }
  };

  openDraftsDialog = () => {
    this.setState({ draftsDialogOpen: true, drafts: [] });
    this.fetchDrafts();
  };

  loadDraft = async (draft) => {
    const response = await API.GET(apiEndpoints.getDraftById(draft.draftId));
    if (!response.success) {
      this.props.enqueueSnackbar("Failed to load draft", { variant: "error" });
      return;
    }
    let parsed;
    try {
      parsed = typeof response.data === "string" ? JSON.parse(response.data) : response.data;
    } catch (e) {
      this.props.enqueueSnackbar("Draft data is invalid", { variant: "error" });
      return;
    }
    this.setState({ draftsDialogOpen: false });
    this.props.onAddFromDraft(draft.draftId, parsed);
  };

  deleteDraft = async (draftId) => {
    const response = await API.DELETE(apiEndpoints.deleteDraftById(draftId));
    if (response.success) {
      this.props.enqueueSnackbar("Draft deleted", { variant: "success" });
      this.setState((prev) => ({ drafts: prev.drafts.filter((d) => d.draftId !== draftId) }));
    } else {
      this.props.enqueueSnackbar("Failed to delete draft", { variant: "error" });
    }
  };

  renderDraftsDialog() {
    const { drafts, isDraftsLoading } = this.state;
    return (
      <Dialog
        open={this.state.draftsDialogOpen}
        onClose={() => this.setState({ draftsDialogOpen: false })}
        maxWidth="sm"
        fullWidth
        aria-labelledby="drafts-dialog-title"
      >
        <DialogTitle id="drafts-dialog-title">Saved Drafts</DialogTitle>
        <DialogContent dividers style={{ minHeight: 120 }}>
          {isDraftsLoading ? (
            <div style={{ display: "flex", justifyContent: "center", padding: 24 }}>
              <CircularProgress size={28} />
            </div>
          ) : drafts.length === 0 ? (
            <div style={{ color: "#888", textAlign: "center", padding: 24 }}>No saved drafts found.</div>
          ) : (
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr style={{ borderBottom: "1px solid #e0e0e0" }}>
                  <th style={{ textAlign: "left", padding: "6px 8px", fontWeight: 600 }}>Draft Name</th>
                  <th style={{ textAlign: "left", padding: "6px 8px", fontWeight: 600 }}>Saved On</th>
                  <th style={{ textAlign: "right", padding: "6px 8px" }}></th>
                </tr>
              </thead>
              <tbody>
                {drafts.map((draft) => (
                  <tr key={draft.draftId} style={{ borderBottom: "1px solid #f0f0f0" }}>
                    <td style={{ padding: "8px 8px" }}>{draft.draftName || `Draft #${draft.draftId}`}</td>
                    <td style={{ padding: "8px 8px", color: "#666", fontSize: 13 }}>
                      {draft.createdDate ? new Date(draft.createdDate).toLocaleString("en-IN") : "—"}
                    </td>
                    <td style={{ padding: "8px 8px", textAlign: "right", whiteSpace: "nowrap" }}>
                      <Button
                        size="small"
                        color="primary"
                        variant="outlined"
                        onClick={() => this.loadDraft(draft)}
                        style={{ marginRight: 8, textTransform: "none" }}
                      >
                        Load
                      </Button>
                      <Button
                        size="small"
                        style={{ color: "#c62828", borderColor: "#c62828", textTransform: "none" }}
                        variant="outlined"
                        onClick={() => this.deleteDraft(draft.draftId)}
                      >
                        Delete
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => this.setState({ draftsDialogOpen: false })} style={{ textTransform: "none" }}>
            Close
          </Button>
        </DialogActions>
      </Dialog>
    );
  }

  /** Download PO list as Excel using current UI filters. tenant-id is sent automatically when inside a project. */
  async exportToCSV() {
    const payload = this.prepareRequestBody();
    const response = await API.POSTBlob(
      apiEndpoints.purchaseOrderExportExcel,
      payload
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
      "purchase_order_export.xlsx";
    triggerBlobDownload(blob, filename);
    this.props.enqueueSnackbar("Purchase order export downloaded", {
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

    const response = await API.POST(
      apiEndpoints.getPurchaseOrder + "?page=" + page + sortParam,
      params
    );

    this.setState({ isLoading: false, showDetails: false });
    if (this.props.isLoading) {
      this.props.isLoading(false);
    }

    this.showToaster(response);

    if (response.success) {
      let rawData = response.data.puchaseOrders?.content || response.data.content || response.data || [];
      let transformedData = this.transformDataForTable(rawData);
      this.setState({
        data: transformedData,
        pages: response.data.puchaseOrders?.totalPages || response.data.totalPages || 1,
        totalRecords: response.data.puchaseOrders?.totalElements || response.data.totalElements || 0,
        sortkey: sortkey !== null ? sortkey : this.state.sortkey,
        sortby: sortby !== null ? sortby : this.state.sortby,
      });
    }
  }


  render() {
    return (
      <div className="purchase-order-list-wrapper">
        {this.renderDraftsDialog()}
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
            <div className="top-button-wrapper">
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
              {canEditInventoryModules() && (
                <Button
                  onClick={this.openDraftsDialog}
                  color="default"
                  variant="outlined"
                  style={{ marginRight: 8, textTransform: "none" }}
                >
                  Drafts {this.state.drafts.length > 0 ? `(${this.state.drafts.length})` : ""}
                </Button>
              )}
              {canEditInventoryModules() && (
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
                  {messages.common.add} {messages.common.purchaseOrder}
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
              hideedit={!canEditInventoryModules()}
              hidedelete={!canEditInventoryModules()}
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
