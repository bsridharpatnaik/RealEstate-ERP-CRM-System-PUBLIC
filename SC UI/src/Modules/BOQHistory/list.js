import React from 'react';
import { withSnackbar } from 'notistack';
import Popper from '@material-ui/core/Popper';

import ListCommon from '../../Shared/List';
import Table from './table';
import Filter from './filter';
import IconButtons from '../../Shared/Button/IconButtons';
import { API } from '../../axios';
import { apiEndpoints } from '../../endpoints';
import { messages } from '../../messages';

class List extends ListCommon {
  filterData = {};
  sortkey = 'changeDateTime';
  sortby = 'desc';

  state = { data: [], pages: 0, totalRecords: 0, isLoading: false, filterOpen: false };

  tableData = {
    headers: [
      'Change Date Time', 'Structure Type', 'Structure',
      'Product', 'Category', 'Work Area',
      'Old Qty', 'New Qty', 'Changed By', 'Change Type', 'Remark',
    ],
    keys: [
      'changeDateTime', 'buildingTypeName', 'usageLocationName',
      'productName', 'categoryName', 'finalLocationName',
      'oldQuantity', 'newQuantity', 'changedBy', 'changeType', 'remark',
    ],
  };

  url = apiEndpoints.boqHistoryList;

  componentDidMount() {
    this.filterRef = React.createRef();
    this.search();
  }

  prepareRequestBody() {
    const params = { filterData: [] };
    if (this.filterData) {
      for (const field in this.filterData) {
        const value = this.filterData[field];
        if (value === undefined || value === null || value === '') continue;
        params.filterData.push({
          attrName: field,
          attrValue: Array.isArray(value) ? value.map(String) : [String(value)],
        });
      }
    }
    return params;
  }

  async search(page = 0) {
    const params = this.prepareRequestBody();
    const response = await this.getData(page, params);
    if (response.success) {
      const paged = response.data;
      this.setState({
        data: (paged.content || []).map(row => ({
          changeDateTime:    row.changeDateTime,
          buildingTypeName:  row.buildingType?.typeName    || '',
          usageLocationName: row.usageLocation?.locationName || '',
          productName:       row.product?.productName      || '',
          categoryName:      row.product?.category?.categoryName || '',
          finalLocationName: row.finalLocation?.usageAreaName   || '',
          oldQuantity:       row.oldQuantity,
          newQuantity:       row.newQuantity,
          changedBy:         row.changedBy  || '',
          changeType:        row.changeType || '',
          remark:            row.remark     || '',
        })),
        pages:        paged.totalPages    || 0,
        totalRecords: paged.totalElements || 0,
      });
    }
  }

  async exportExcel() {
    const r = await API.POSTBlob(apiEndpoints.boqHistoryExport, this.prepareRequestBody());
    if (r.success) {
      const url = window.URL.createObjectURL(new Blob([r.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', 'BOQ_History.xlsx');
      document.body.appendChild(link);
      link.click();
      link.remove();
    } else {
      this.props.enqueueSnackbar(r.errorMessage || 'Export failed', { variant: 'error' });
    }
  }

  render() {
    return (
      <div className="list-section">
        <div className="filter-section">
          <div />
          <div className="top-button-wrapper" style={{ display: 'flex', gap: '8px' }}>
            <IconButtons
              onClick={() => this.exportExcel()}
              buttonClass="filterIcon"
              label="Export Excel"
              icon="DownloadSVG"
            />
            <IconButtons
              onClick={() => this.setState({ filterOpen: true })}
              buttonClass="filterIcon"
              label={messages.common.filter}
              icon="FilterSVG"
              innerRef={this.filterRef}
            />
          </div>
        </div>

        <Popper
          open={this.state.filterOpen}
          anchorEl={this.filterRef && this.filterRef.current}
          placement="bottom-end"
          style={{ zIndex: 1300 }}
        >
          <Filter
            filterData={this.filterData}
            search={(data) => {
              this.filterData = data;
              this.setState({ filterOpen: false });
              this.search(0);
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
            hideedit
            hidedelete
            sortkey={this.sortkey}
            sortby={this.sortby}
            search={(sortkey, sortby) => {
              this.sortkey = sortkey;
              this.sortby = sortby;
              this.search();
            }}
          />
        )}

        {this.renderPagination()}
      </div>
    );
  }
}

export default withSnackbar(List);
