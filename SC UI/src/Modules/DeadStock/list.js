//react
import React from "react";
//Third Party
import ListCommon from "./../../Shared/List";
import { withSnackbar } from "notistack";
import Popper from "@material-ui/core/Popper";
import moment from "moment";
import TextField from "@material-ui/core/TextField";
import InputAdornment from "@material-ui/core/InputAdornment";
import SearchIcon from "@material-ui/icons/Search";
import Button from "@material-ui/core/Button";
import CircularProgress from "@material-ui/core/CircularProgress";
import Menu from "@material-ui/core/Menu";
import MenuItem from "@material-ui/core/MenuItem";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import Typography from "@material-ui/core/Typography";
import MuiList from "@material-ui/core/List";
import MuiListItem from "@material-ui/core/ListItem";
import MuiListItemText from "@material-ui/core/ListItemText";
import InfoOutlinedIcon from "@material-ui/icons/InfoOutlined";
//component
import Table from "./table";
import Filter from "./filter";
import IconButtons from "./../../Shared/Button/IconButtons";
//misc
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";
import { API } from "./../../axios";
import { getTenantName, triggerBlobDownload } from "./../../helper";
import { connect } from "react-redux";
import "./style.scss";

class List extends ListCommon {
  title = messages.common.stockSummary;
  searchValue = "";
  searchTimeout = null;
  state = {
    pageno: 0,
    data: [],
    options: {},
    isLoading: false,
    pages: 0,
    totalRecords: 0,
    filterOpen: false,
    lastSyncDate: null,
    isExporting: false,
    isImporting: false,
    exportMenuAnchor: null,
    isDownloadingReport: false,
    importGuideOpen: false,
    importResultOpen: false,
    importResult: null,
    tiles: {},
    activeTile: null,
    activeProject: null,
  };
  filterData = {};
  filterRef = React.createRef();
  tableData = {
    headers: [
      "Product Name",
      messages.common.productCode,
      messages.common.project,
      messages.common.totalStock,
      "Dead Stock",
      "Reorder Level",
      "Measurement Unit",
      messages.common.lastUpdated,
    ],
    keys: [
      "productName",
      "productCode",
      "project",
      "quantityInHand",
      "deadStock",
      "reorderLevel",
      "measurementUnit",
      "syncedAt",
    ],
  };
  url = apiEndpoints.getStockSummary;

  TILES = [
    { key: "lowStock",      label: "Low Stock",  color: "#e67e22", bg: "#fdf2e9", countKey: "lowStockCount",  filterField: "lowStock" },
    { key: "deadStockPresent", label: "Dead Stock", color: "#b71c1c", bg: "#ffebee", countKey: "deadStockCount", filterField: "deadStockPresent" },
  ];

  componentDidMount() {
    if (this.page === undefined || isNaN(this.page)) {
      this.page = 0;
    }
    this.search();
    this.fetchTiles();
  }

  fetchTiles = async () => {
    const params = this.prepareRequestBody();
    const response = await API.POST(apiEndpoints.stockSummaryTiles, params);
    if (response.success) {
      this.setState({ tiles: response.data || {} });
    }
  };

  handleTileClick = (tile) => {
    const isActive = this.state.activeTile === tile.key;
    delete this.filterData[tile.filterField];
    delete this.filterData.tenants;
    if (isActive) {
      this.setState({ activeTile: null, activeProject: null }, () => { this.search(0); this.fetchTiles(); });
    } else {
      this.filterData[tile.filterField] = true;
      this.setState({ activeTile: tile.key, activeProject: null }, () => { this.search(0); this.fetchTiles(); });
    }
  };

  handleProjectChipClick = (tenantCode) => {
    const isActive = this.state.activeProject === tenantCode;
    if (isActive) {
      delete this.filterData.tenants;
      this.setState({ activeProject: null }, () => { this.search(0); this.fetchTiles(); });
    } else {
      this.filterData.tenants = [tenantCode];
      delete this.filterData.lowStock;
      delete this.filterData.deadStockPresent;
      this.setState({ activeProject: tenantCode, activeTile: null }, () => { this.search(0); this.fetchTiles(); });
    }
  };

  renderTilesAndProjects() {
    const { tiles, activeTile, activeProject, options } = this.state;
    const tenants = options?.tenants || [];
    return (
      <div style={{ marginBottom: 8 }}>
        {/* Stat tiles */}
        <div style={{ display: "flex", gap: 12, overflowX: "auto", padding: "12px 0 8px" }}>
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
                  flexShrink: 0,
                }}
              >
                <div style={{ fontSize: 22, fontWeight: 700, color: t.color }}>{count}</div>
                <div style={{ fontSize: 12, color: "#555", marginTop: 2 }}>{t.label}</div>
              </div>
            );
          })}
        </div>
        {/* Project chips — only resolved display names, exclude masterschema */}
        {(() => {
          const resolved = tenants.filter(t => {
            const code = t.tenantCode || t.id;
            const name = t.name || code;
            return code && code !== "masterschema" && name !== code;
          });
          if (!resolved.length) return null;
          return (
            <div style={{ display: "flex", gap: 8, flexWrap: "wrap", padding: "4px 0 8px" }}>
              <span style={{ fontSize: 11, color: "#888", alignSelf: "center", marginRight: 4 }}>Project:</span>
              {resolved.map((t) => {
                const code = t.tenantCode || t.id;
                const name = t.name || code;
                const isActive = activeProject === code;
                return (
                  <div
                    key={code}
                    onClick={() => this.handleProjectChipClick(code)}
                    style={{
                      cursor: "pointer",
                      padding: "3px 10px",
                      borderRadius: 12,
                      fontSize: 12,
                      background: isActive ? "#1976d2" : "#f0f4fa",
                      color: isActive ? "#fff" : "#334",
                      border: isActive ? "1px solid #1976d2" : "1px solid #d0d8e8",
                      fontWeight: isActive ? 600 : 400,
                    }}
                  >
                    {name}
                  </div>
                );
              })}
            </div>
          );
        })()}
      </div>
    );
  }

  componentDidUpdate(prevProps) {
    if (
      this.props.syncTriggerAt &&
      this.props.syncTriggerAt !== prevProps.syncTriggerAt
    ) {
      this.syncNow();
    }
  }

  componentWillUnmount() {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
  }

  handleSearchChange = (value) => {
    this.searchValue = value;
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
    this.searchTimeout = setTimeout(() => {
      this.search(0);
    }, 500);
  };

  prepareRequestBody() {
    const params = {};
    params.filterData = [];
    if (this.searchValue && this.searchValue.trim().length) {
      params.filterData.push({
        attrName: "globalSearch",
        attrValue: [this.searchValue.trim()],
      });
    }
    if (this.filterData) {
      for (const field in this.filterData) {
        let value = this.filterData[field];
        if (field === "deadStockPresent") {
          if (value === true) {
            params.filterData.push({
              attrName: "deadStockPresent",
              attrValue: ["true"],
            });
          }
          continue;
        }
        if (field === "lowStock") {
          if (value === true) {
            params.filterData.push({
              attrName: "lowStock",
              attrValue: ["true"],
            });
          }
          continue;
        }
        if (value && value.length) {
          if (["productNames", "productCodes"].includes(field)) {
            value = value.map((v) => (typeof v === "object" && v?.name != null ? v.name : v));
          } else if (field === "tenants") {
            // Options are {tenantCode, name} objects — always send the code to backend
            value = value.map((v) => (typeof v === "object" && v?.tenantCode != null ? v.tenantCode : v));
          } else if (field === "globalSearch") {
            value = [value];
          }
          if (Array.isArray(value) && value.length) {
            params.filterData.push({
              attrName: field,
              attrValue: value,
            });
          }
        }
      }
    }
    return params;
  }

  async syncNow() {
    if (this.state.isLoading) return;
    this.setState({ isLoading: true });
    if (this.props.isLoading) this.props.isLoading(true);
    const response = await API.POST(apiEndpoints.syncDeadStock, {});
    if (this.props.isLoading) this.props.isLoading(false);
    this.setState({ isLoading: false });
    if (response.success) {
      this.props.enqueueSnackbar("Stock sync started successfully", { variant: "success" });
      this.search(0);
    } else {
      this.props.enqueueSnackbar(response.errorMessage || "Sync failed", { variant: "error" });
    }
  }

  async search(page = 0, sortkey = null, sortby = null) {
    const params = this.prepareRequestBody();
    params.page = page;
    params.size = this.pageSize;
    let sortParam = "";
    if (sortkey) {
      sortParam = "&sort=" + sortkey;
      if (sortby) sortParam += "," + sortby;
    }
    const pageNum = Number(page) || 0;
    this.page = pageNum;
    if (this.inputRef && this.inputRef.current) {
      const pageValue = pageNum + 1;
      this.inputRef.current.value = pageValue > 0 ? pageValue : 1;
    }
    this.setState({ isLoading: true, pageno: pageNum });
    if (this.props.isLoading) this.props.isLoading(true);

    const url = this.url + "?size=" + this.pageSize + "&page=" + page + sortParam;
    const response = await API.POST(url, params);

    if (this.props.isLoading) this.props.isLoading(false);
    this.setState({ isLoading: false });
    this.showToaster(response);

    if (response.success) {
      const data = response.data || {};
      const lastSync = data.lastSyncDate || null;
      const summaries = data.stockSummaries || {};
      const content = summaries.content || [];
      const totalPages = summaries.totalPages ?? 0;
      const totalElements = summaries.totalElements ?? content.length;
      const dropdown = data.stockDropdown || {};
      const allTenant = this.props.allTenant || [];
      // Convert plain tenant-code strings to {tenantCode, name} objects so the
      // filter shows the human-readable name while the payload sends the code.
      const tenantOptions = (dropdown.tenants || []).map((code) => {
        const found = allTenant.find((t) => t.tenantCode === code);
        return { tenantCode: code, name: found ? found.tenantName : code };
      });
      const options = {
        productNames: dropdown.product || [],
        productCodes: dropdown.productCodes || [],
        tenants: tenantOptions,
      };
      this.setState({ options });

      const transformedData = content.map((item) => ({
        productName: item.productName ?? "",
        productCode: item.productCode ?? "",
        project: getTenantName(item.tenantSchema ?? item.project, allTenant) || item.tenantSchema || item.project || "",
        quantityInHand: item.quantityInHand ?? "",
        deadStock: item.deadStock ?? "",
        reorderLevel: item.reorderLevel ?? "-",
        measurementUnit: item.measurementUnit ?? "-",
        syncedAt: item.syncedAt
          ? moment(item.syncedAt).format("DD MMM YYYY, hh:mm A")
          : "",
        // keep raw numeric values for low-stock badge logic in table
        _quantityInHand: item.quantityInHand,
        _reorderLevel: item.reorderLevel,
        ...item,
      }));

      this.setState({
        data: transformedData,
        pages: totalPages || 0,
        totalRecords: totalElements || 0,
        lastSyncDate: lastSync,
      });
    }
  }

  formatLastSync() {
    const d = this.state.lastSyncDate;
    if (!d) return null;
    const m = moment(d);
    return m.isValid() ? m.format("DD MMM YYYY, hh:mm A") : null;
  }

  handleExport = async () => {
    this.setState({ isExporting: true });
    const params = this.prepareRequestBody();
    const response = await API.POSTBlob(apiEndpoints.stockSummaryExport, params);
    this.setState({ isExporting: false });
    if (response.success) {
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement("a");
      link.href = url;
      link.setAttribute("download", "stock-summary.xlsx");
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } else {
      this.props.enqueueSnackbar("Export failed. Please try again.", { variant: "error" });
    }
  };

  handleDownloadReport = async () => {
    this.setState({ isDownloadingReport: true, exportMenuAnchor: null });
    try {
      const response = await API.GETBlob(apiEndpoints.downloadStockReport);
      if (response.success) {
        const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, "");
        triggerBlobDownload(response.data, `Stock_Report_${dateStr}.xlsx`);
      } else {
        this.props.enqueueSnackbar("Report download failed. Please try again.", { variant: "error" });
      }
    } finally {
      this.setState({ isDownloadingReport: false });
    }
  };

  handleImportClick = () => {
    this.setState({ importGuideOpen: true });
  };

  handleImportProceed = () => {
    this.setState({ importGuideOpen: false });
    this.importInputRef.click();
  };

  handleImportFileChange = async (e) => {
    const file = e.target.files && e.target.files[0];
    // Reset input so same file can be re-selected if needed
    e.target.value = "";
    if (!file) return;

    this.setState({ isImporting: true });
    const formData = new FormData();
    formData.append("file", file);

    const response = await API.POSTMultipart(apiEndpoints.stockSummaryImport, formData);
    this.setState({ isImporting: false });

    if (response.success) {
      this.setState({ importResult: response.data, importResultOpen: true });
      // Refresh the list so the new reorder levels are visible after next sync
      this.search(this.page || 0);
    } else {
      this.props.enqueueSnackbar(
        response.errorMessage || "Import failed. Please check the file and try again.",
        { variant: "error" }
      );
    }
  };

  render() {
    const formatted = this.formatLastSync();
    const { isExporting, isImporting, importGuideOpen, importResultOpen, importResult, exportMenuAnchor, isDownloadingReport } = this.state;
    return (
      <div className="dead-stock-list-wrapper">
        {/* Hidden file input for import */}
        <input
          type="file"
          accept=".xlsx"
          style={{ display: "none" }}
          ref={(ref) => { this.importInputRef = ref; }}
          onChange={this.handleImportFileChange}
        />

        <div className="list-section dead-stock-list">
          {this.renderTilesAndProjects()}
          <div className="filter-section">
            <TextField
            variant="outlined"
            placeholder={messages.common.searchByName}
            defaultValue={this.searchValue}
            onChange={(e) => this.handleSearchChange(e.target.value)}
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
              <Button
                variant="outlined"
                size="small"
                onClick={(e) => this.setState({ exportMenuAnchor: e.currentTarget })}
                disabled={isExporting || isDownloadingReport}
                style={{ marginRight: 8 }}
                startIcon={(isExporting || isDownloadingReport) ? <CircularProgress size={14} /> : null}
              >
                {isExporting ? "Exporting…" : isDownloadingReport ? "Downloading…" : "Export ▾"}
              </Button>
              <Menu
                anchorEl={exportMenuAnchor}
                open={Boolean(exportMenuAnchor)}
                onClose={() => this.setState({ exportMenuAnchor: null })}
              >
                <MenuItem onClick={() => { this.setState({ exportMenuAnchor: null }); this.handleExport(); }}>
                  List
                </MenuItem>
                <MenuItem onClick={this.handleDownloadReport}>
                  Report
                </MenuItem>
              </Menu>
              <Button
                variant="outlined"
                size="small"
                onClick={this.handleImportClick}
                disabled={isImporting}
                style={{ marginRight: 8 }}
                startIcon={isImporting ? <CircularProgress size={14} /> : null}
              >
                {isImporting ? "Importing…" : "Import"}
              </Button>
              <IconButtons
              onClick={() => this.setState({ filterOpen: true })}
              buttonClass="filterIcon"
              innerRef={this.filterRef}
              label={messages.common.filter}
              icon={"MenuSVG"}
              />
            </div>
          </div>
          <Popper
            open={this.state.filterOpen}
            anchorEl={this.filterRef && this.filterRef.current}
            placement="bottom-end"
          >
            <Filter
              filterData={this.filterData}
              options={this.state.options}
              search={(data) => {
                this.filterData = data;
                if (data.globalSearch != null) this.searchValue = data.globalSearch;
                this.setState({ filterOpen: false, activeTile: null, activeProject: null });
                this.search(0);
                this.fetchTiles();
              }}
              close={() => this.setState({ filterOpen: false })}
            />
          </Popper>
          {formatted && (
            <p className="dead-stock-sync-message">
              {messages.common.deadStockSyncMessage} {formatted}
            </p>
          )}
          {this.state.isLoading ? (
            this.renderLoader()
          ) : (
            <Table
              tableData={this.tableData}
              rows={this.state.data}
              hidedelete
              hideedit
              sortby={this.sortby}
              sortkey={this.sortkey}
              search={(sortkey, sortby) => {
                this.sortby = sortby;
                this.sortkey = sortkey;
                this.search(0, sortkey, sortby);
              }}
            />
          )}
          {this.renderPagination()}
        </div>

        {/* Import guide dialog */}
        <Dialog
          open={importGuideOpen}
          onClose={() => this.setState({ importGuideOpen: false })}
          maxWidth="sm"
          fullWidth
        >
          <DialogTitle>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <InfoOutlinedIcon style={{ color: "#1976d2" }} />
              How to Import Reorder Levels
            </div>
          </DialogTitle>
          <DialogContent>
            <Typography variant="body2" color="textSecondary" gutterBottom>
              Follow these steps to update reorder levels in bulk:
            </Typography>
            <MuiList dense disablePadding>
              <MuiListItem alignItems="flex-start" style={{ paddingLeft: 0 }}>
                <MuiListItemText
                  primary={
                    <Typography variant="body2">
                      <strong>Step 1 — Export</strong>
                    </Typography>
                  }
                  secondary="Click the Export button to download the current stock summary as an Excel file. Apply any filters first if you want to export a specific subset."
                />
              </MuiListItem>
              <MuiListItem alignItems="flex-start" style={{ paddingLeft: 0 }}>
                <MuiListItemText
                  primary={
                    <Typography variant="body2">
                      <strong>Step 2 — Edit</strong>
                    </Typography>
                  }
                  secondary="Open the downloaded file and update the Reorder Level column (highlighted in yellow). Do not modify Tenant or Product Code — they are used to match records. All other columns are ignored during import."
                />
              </MuiListItem>
              <MuiListItem alignItems="flex-start" style={{ paddingLeft: 0 }}>
                <MuiListItemText
                  primary={
                    <Typography variant="body2">
                      <strong>Step 3 — Upload</strong>
                    </Typography>
                  }
                  secondary="Click Choose File below and select the edited file. The reorder levels will be saved and will reflect in the table after the next stock sync."
                />
              </MuiListItem>
            </MuiList>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => this.setState({ importGuideOpen: false })} color="default">
              Cancel
            </Button>
            <Button
              onClick={this.handleImportProceed}
              color="primary"
              variant="contained"
              disabled={isImporting}
              startIcon={isImporting ? <CircularProgress size={14} /> : null}
            >
              Choose File
            </Button>
          </DialogActions>
        </Dialog>

        {/* Import result dialog */}
        <Dialog
          open={importResultOpen}
          onClose={() => this.setState({ importResultOpen: false })}
          maxWidth="sm"
          fullWidth
        >
          <DialogTitle>Import Complete</DialogTitle>
          <DialogContent>
            {importResult && (
              <div>
                <Typography variant="body1" gutterBottom>
                  <strong>Updated:</strong> {importResult.updated} record(s)
                </Typography>
                <Typography variant="body1" gutterBottom>
                  <strong>Skipped:</strong> {importResult.skipped} row(s)
                </Typography>
                {importResult.errors && importResult.errors.length > 0 && (
                  <div style={{ marginTop: 12 }}>
                    <Typography variant="body2" color="error" gutterBottom>
                      <strong>Errors ({importResult.errors.length}):</strong>
                    </Typography>
                    <ul style={{ paddingLeft: 18, margin: 0 }}>
                      {importResult.errors.map((err, i) => (
                        <li key={i}>
                          <Typography variant="body2" color="error">{err}</Typography>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
                {importResult.updated > 0 && (
                  <Typography
                    variant="body2"
                    style={{ marginTop: 12, color: "#666" }}
                  >
                    Note: Reorder level changes will reflect in the table after the next stock sync.
                  </Typography>
                )}
              </div>
            )}
          </DialogContent>
          <DialogActions>
            <Button
              onClick={() => this.setState({ importResultOpen: false })}
              color="primary"
              variant="contained"
            >
              OK
            </Button>
          </DialogActions>
        </Dialog>
      </div>
    );
  }
}

const mapStateToProps = (state) => ({
  allTenant: state.allTennant.tennants,
});

export default connect(mapStateToProps)(withSnackbar(List));
