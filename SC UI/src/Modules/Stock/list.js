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
import Details from "./details";
import DetailsPopup from "./../../Shared/DetailsPopup";
import { API } from "./../../axios";
import { triggerBlobDownload } from "./../../helper";

class List extends ListCommon {
  filterData = {};
  title = messages.common.stock;
  state = { data: [], options: [], showDetails: false, key: 1, expiryTiles: null, expiryFilter: null };
  tableData = {
    headers: [
      messages.common.id,
      messages.common.inventory,
      messages.common.inventoryCode,
      messages.common.category,
      messages.common.totalStock,
      "Warehouses",
      "Last Inward Date",
      messages.common.reorderQuantity,
      messages.common.stockStatus,
    ],
    keys: [
      "productId",
      "productName",
      "productCode",
      "categoryName",
      "totalQuantityInHand",
      "warehouseCount",
      "lastInwardDate",
      "reorderQuantity",
      "stockStatus",
    ],
  };
  url = apiEndpoints.getStock;
  exportUrl = exportURL.getStock;
  exportFile = messages.exportFiles.stock;
  ignoreQueryParamsExport = true;
  componentDidMount() {
    this.search();
    this.filterRef = React.createRef();
    this.getOptions();
    this.loadExpiryTiles();
  }

  async loadExpiryTiles() {
    const body = this.prepareRequestBody();
    // Strip expiryFilter so tile counts reflect regular filters only (not circular)
    const tileBody = { filterData: (body.filterData || []).filter(f => f.attrName !== 'expiryFilter') };
    const response = await API.POST(apiEndpoints.getExpiryTiles, tileBody);
    if (response.success) {
      this.setState({ expiryTiles: response.data });
    }
  }
  async getOptions() {
    const response = await API.GET(apiEndpoints.stockDropdown);
    if (response.success) {
      this.dropdowns = response.data;
      const options = [...this.dropdowns.product, ...this.dropdowns.warehouse, ...(this.dropdowns.productCodes || [])];
      this.props.setOptions(this.dropdowns);
      this.setState({ options: options });
    }
  }
  getExportData(response) {
    return response.data;
  }

  async exportToCSV() {
    const params = this.prepareRequestBody();
    const response = await API.POSTBlob(exportURL.stockExportExcel, params);
    if (!response.success) {
      this.props.enqueueSnackbar(response.errorMessage || "Export failed", { variant: "error" });
      return;
    }
    triggerBlobDownload(response.data, "stock-export.xlsx");
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
    if (this.searchValue.length) {
      params.filterData.push({
        attrName: "globalSearch",
        attrValue: this.searchValue.map((v) => v.name),
      });
    }
    if (this.filterData) {
      for (const field in this.filterData) {
        let value = this.filterData[field];

        if (value && value.length) {
          if (["products", "categories", "warehouses"].includes(field)) {
            value = value.map((v) => v.name);
          } else if (field === "productCodes") {
            value = value.map((v) => v.productCode != null ? v.productCode : v.name);
          } else if (["closingDate"].includes(field)) {
            value = [value];
          }
          params.filterData.push({
            attrName: field,
            attrValue: value,
          });
        }
      }
    }
    if (this.state.expiryFilter) {
      params.filterData.push({
        attrName: "expiryFilter",
        attrValue: [this.state.expiryFilter],
      });
    }
    return params;
  }
  async search(page = 0, sortkey = null, sortby = null) {
    if (sortkey !== null) this.sortkey = sortkey;
    if (sortby !== null) this.sortby = sortby;
    const params = this.prepareRequestBody();
    this.setState({ isLoading: true });

    const response = await this.getData(page, params);

    if (response.success) {
      this.setState({
        data: response.data.stockInformation.content,
        pages: response.data.stockInformation.totalPages,
        totalRecords: response.data.stockInformation.totalElements,
      });
      this.loadExpiryTiles();
    }
  }

  render() {
    return (
      <div>
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
          {this.state.expiryTiles && (() => {
            const t = this.state.expiryTiles;
            const tiles = [
              { key: 'expiring30', label: 'Expiring ≤30d',   count: t.expiring30Days, color: '#e65100', bg: '#fff3e0', border: '#ffcc80' },
              { key: 'expiring60', label: 'Expiring 31–60d', count: t.expiring60Days, color: '#f57f17', bg: '#fff8e1', border: '#ffe082' },
              { key: 'expired',    label: 'Expired',          count: t.expiredCount,   color: '#c62828', bg: '#ffebee', border: '#ef9a9a' },
              { key: 'lowStock',   label: 'Low Stock',        count: t.lowStockCount,  color: '#1565c0', bg: '#e3f2fd', border: '#90caf9' },
              { key: 'highStock',  label: 'High Stock',       count: t.highStockCount, color: '#2e7d32', bg: '#e8f5e9', border: '#a5d6a7' },
              { key: 'aging30',    label: 'Aging 30d+',       count: t.aging30Days,    color: '#6a1b9a', bg: '#f3e5f5', border: '#ce93d8' },
              { key: 'aging60',    label: 'Aging 60d+',       count: t.aging60Days,    color: '#4a148c', bg: '#ede7f6', border: '#b39ddb' },
              { key: 'aging90',    label: 'Aging 90d+',       count: t.aging90Days,    color: '#311b92', bg: '#e8eaf6', border: '#9fa8da' },
              { key: 'untracked',  label: 'Untracked',        count: t.untrackedCount, color: '#37474f', bg: '#eceff1', border: '#b0bec5' },
            ];
            return (
              <div style={{ display: 'flex', gap: '8px', margin: '10px 0', flexWrap: 'wrap' }}>
                {tiles.map(({ key, label, count, color, bg, border }) => {
                  const active = this.state.expiryFilter === key;
                  return (
                    <div
                      key={key}
                      onClick={() => {
                        const next = active ? null : key;
                        this.setState({ expiryFilter: next }, () => this.search(0));
                      }}
                      style={{
                        flex: '1 1 100px', minWidth: '90px', maxWidth: '160px',
                        padding: '6px 10px', borderRadius: '5px',
                        backgroundColor: bg,
                        border: `${active ? '2px' : '1px'} solid ${active ? color : border}`,
                        cursor: 'pointer',
                        boxShadow: active ? `0 0 0 2px ${color}30` : 'none',
                        transition: 'border 0.15s, box-shadow 0.15s',
                        position: 'relative',
                      }}
                    >
                      <div style={{ fontSize: '10px', color, fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.3px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{label}</div>
                      <div style={{ fontSize: '20px', fontWeight: 'bold', color, lineHeight: 1.2 }}>{count}</div>
                      {active && <div style={{ position: 'absolute', top: '4px', right: '6px', fontSize: '10px', color, fontWeight: 700 }}>✕</div>}
                    </div>
                  );
                })}
              </div>
            );
          })()}
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
        <DetailsPopup
          open={this.state.showDetails}
          onClose={() => this.setState({ showDetails: false, key: this.state.key + 1 })}
        >
          {this.state.showDetails && (
            <Details
              data={this.state.selectedData}
              edit={this.props.edit}
              delete={(row) => this.delete(row)}
              close={() =>
                this.setState({ showDetails: false, key: this.state.key + 1 })
              }
            />
          )}
        </DetailsPopup>
      </div>
    );
  }
}

export default withSnackbar(List);
