//react
import React from "react";
//Third Party
import ListCommon from "./../../Shared/List";
import { withSnackbar } from "notistack";
import AddIcon from "@material-ui/icons/Add";
import Popper from "@material-ui/core/Popper";
import { connect } from "react-redux";
import { Slide } from "@material-ui/core";

//component
import Table from "./table";
import Filter from "./filter";
import Button from "./../../Shared/Button";
import Details from "./details";
//misc
import { apiEndpoints, exportURL, noOfRecords } from "./../../endpoints";
import { messages } from "./../../messages";
import { API } from "./../../axios";
import { getTenantName } from "./../../helper";

class List extends ListCommon {
  title = messages.common.inventoryTransfer;
  state = {
    pageno: 0,
    data: [],
    options: [],
    isLoading: false,
    pages: 0,
    totalRecords: 0,
    filterOpen: false,
    showDetails: false,
    selectedRow: null,
  };
  filterData = {};
  filterRef = React.createRef();
  tableData = {
    headers: [
      "Transfer ID",
      "Transfer Date",
      "From Tenant",
      "To Tenant",
      "From Warehouse",
      "To Warehouse",
      "Products",
      "Remarks",
      "Created By",
    ],
    keys: [
      "transferId",
      "transferDate",
      "sourceTenant",
      "targetTenant",
      "sourceWarehouseName",
      "targetWarehouseName",
      "products",
      "remarks",
      "createdBy",
    ],
  };
  url = apiEndpoints.getInventoryTransfer;
  exportUrl = exportURL.getInventoryTransfer;
  exportFile = messages.exportFiles.inventoryTransfer;

  componentDidMount() {
    // Initialize page to 0 if not set
    if (this.page === undefined || isNaN(this.page)) {
      this.page = 0;
    }
    this.search();
    this.fetchDropdowns();
  }

  async fetchDropdowns() {
    // Dropdowns will be populated from the list response itDropdown
  }

  prepareRequestBody() {
    const params = {};
    params.filterData = [];

    if (!this.filterData) {
      return params;
    }

    if (this.filterData.globalSearch) {
      const globalSearchValue = Array.isArray(this.filterData.globalSearch)
        ? this.filterData.globalSearch
        : [this.filterData.globalSearch];
      params.filterData.push({
        attrName: "globalSearch",
        attrValue: globalSearchValue,
      });
    }

    if (this.filterData.startDate) {
      params.filterData.push({
        attrName: "startDate",
        attrValue: Array.isArray(this.filterData.startDate)
          ? this.filterData.startDate
          : [this.filterData.startDate],
      });
    }

    if (this.filterData.endDate) {
      params.filterData.push({
        attrName: "endDate",
        attrValue: Array.isArray(this.filterData.endDate)
          ? this.filterData.endDate
          : [this.filterData.endDate],
      });
    }

    if (this.filterData.categoryNames && this.filterData.categoryNames.length > 0) {
      const value = this.filterData.categoryNames.map((v) => v.name);
      params.filterData.push({ attrName: "categoryNames", attrValue: value });
    }

    if (this.filterData.productNames && this.filterData.productNames.length > 0) {
      const value = this.filterData.productNames.map((v) => v.id);
      params.filterData.push({ attrName: "productNames", attrValue: value });
    }

    if (this.filterData.productCodes && this.filterData.productCodes.length > 0) {
      const value = this.filterData.productCodes.map((v) => v.id);
      params.filterData.push({ attrName: "productCodes", attrValue: value });
    }

    if (this.filterData.sourceTenant) {
      const value = this.filterData.sourceTenant;
      // Options are now {tenantCode, name} objects — always send code to backend
      const codes = Array.isArray(value)
        ? value.map((v) => (typeof v === "object" ? v.tenantCode : v))
        : [typeof value === "object" ? value.tenantCode : value];
      params.filterData.push({ attrName: "sourceTenant", attrValue: codes });
    }

    if (this.filterData.targetTenant) {
      const value = this.filterData.targetTenant;
      const codes = Array.isArray(value)
        ? value.map((v) => (typeof v === "object" ? v.tenantCode : v))
        : [typeof value === "object" ? value.tenantCode : value];
      params.filterData.push({ attrName: "targetTenant", attrValue: codes });
    }

    return params;
  }

  getExportData(response) {
    return response.data?.inventoryTransfers?.content || response.data?.content || [];
  }

  async search(page = 0, sortkey = null, sortby = null) {
    const params = this.prepareRequestBody();

    // Add pagination to request body for POST request
    params.page = page;
    params.size = noOfRecords;

    // Build sort parameter for URL
    let sortParam = "";
    if (sortkey) {
      sortParam = "&sort=" + sortkey;
      if (sortby) {
        sortParam += "," + sortby;
      }
    }

    // Ensure page is a valid number
    const pageNum = Number(page) || 0;
    this.page = pageNum;
    if (this.inputRef && this.inputRef.current) {
      const pageValue = pageNum + 1;
      this.inputRef.current.value = pageValue > 0 ? pageValue : 1;
    }
    this.setState({ isLoading: true, pageno: pageNum });
    if (this.props.isLoading) {
      this.props.isLoading(true);
    }

    const response = await API.POST(this.url + "&page=" + page + sortParam, params);

    if (this.props.isLoading) {
      this.props.isLoading(false);
    }
    this.setState({ isLoading: false });
    this.showToaster(response);

    if (response.success) {
      const data = response.data || {};
      const inventoryTransfers = data.inventoryTransfers || {};
      const content = inventoryTransfers.content || [];
      const totalPages = inventoryTransfers.totalPages || 0;
      const totalElements = inventoryTransfers.totalElements || 0;

      // Extract dropdown data
      const itDropdown = data.itDropdown || {};
      const allTenant = this.props.allTenant || [];
      // Convert plain tenant-code strings to {tenantCode, name} objects so the
      // filter dropdown shows the human-readable name while the payload sends the code.
      const tenantOptions = (itDropdown.tenants || []).map((code) => {
        const found = allTenant.find((t) => t.tenantCode === code);
        return { tenantCode: code, name: found ? found.tenantName : code };
      });
      const dropdowns = {
        category: itDropdown.category || [],
        product: itDropdown.product || [],
        productCodes: itDropdown.productCodes || [],
        tenants: tenantOptions,
        warehouse: [],
      };

      // Set dropdown options if available
      if (this.props.setOptions) {
        this.props.setOptions(dropdowns);
      }

      // Store options in state for filter component
      this.setState({ options: dropdowns });

      // Transform the data to match expected table structure.
      // Spread first so our explicit fields override the raw API values.
      const transformedData = content.map(item => ({
        ...item,
        transferId: item.transferId || "",
        transferDate: item.transferDate || "",
        creationDate: item.creationDate || "",
        // Resolve codes → display names (spread above has raw codes, these win)
        sourceTenant: getTenantName(item.sourceTenant, allTenant) || item.sourceTenant || "",
        targetTenant: getTenantName(item.targetTenant, allTenant) || item.targetTenant || "",
        sourceWarehouseName: item.sourceWarehouseName || "",
        targetWarehouseName: item.targetWarehouseName || "",
        products: item.items ? item.items.map(prod => `${prod.productName} (${prod.quantity})`).join(", ") : "",
        remarks: item.remarks || "-",
        createdBy: item.createdBy || "",
      }));

      this.setState({
        data: transformedData,
        pages: totalPages || 0,
        totalRecords: totalElements || 0,
      });

      // Ensure page is initialized
      if (this.page === undefined || isNaN(this.page)) {
        this.page = 0;
      }
    }
  }

  showDetail = (row) => {
    this.setState({ showDetails: true, selectedRow: row });
  };

  render() {
    const { showDetails, selectedRow } = this.state;
    return (
      <div className={showDetails ? "split" : "inventory-transfer-list-wrapper"}>
        <div className="list-section">
          <div className="filter-section" style={{ justifyContent: "flex-end" }}>
            <div className="top-button-wrapper">
            {this.renderExport()}
            <Button
              onClick={() => {
                this.setState({ filterOpen: true });
              }}
              buttonClass="filterIcon"
              innerRef={this.filterRef}
              label={messages.common.filter}
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
                this.search(0);
                this.setState({ filterOpen: false });
              }}
              close={() => this.setState({ filterOpen: false })}
            />
          </Popper>
          {this.state.isLoading ? (
            this.renderLoader()
          ) : (
            <Table
              tableData={this.tableData}
              rows={this.state.data}
              hidedelete={true}
              hideedit={true}
              showDetail={(row) => this.showDetail(row)}
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
        <Slide
          direction="left"
          in={showDetails}
          mountOnEnter
          unmountOnExit
          timeout={{ exit: 0 }}
        >
          <div>
            {showDetails && (
              <Details
                row={selectedRow}
                back={() => this.setState({ showDetails: false, selectedRow: null })}
              />
            )}
          </div>
        </Slide>
      </div>
    );
  }
}

const mapStateToProps = (state) => ({
  allTenant: state.allTennant.tennants,
});

export default connect(mapStateToProps)(withSnackbar(List));
