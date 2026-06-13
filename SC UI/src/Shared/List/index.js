//react
import React, { Component } from "react";
import Pagination from "@material-ui/lab/Pagination";
import { API } from "./../../axios";
import TextField from "@material-ui/core/TextField";
import Autocomplete from "@material-ui/lab/Autocomplete";
import Skeleton from "@material-ui/lab/Skeleton";
import { messages } from "./../../messages";
import { getJson2csvCallback, isAdmin } from "./../../helper";
import IconButtons from "./../../Shared/Button/IconButtons.js";
import converter from "json-2-csv";
import AutoCompleteWithSearch from "./../AutoCompleteWithSearch";
const height = 35;

class ListCommon extends Component {
  searchValue = [];
  constructor(props) {
    super(props);
    this.isAdmin = isAdmin();
    this.inputRef = React.createRef();
  }
  renderPagination() {
    return (
      <div className="pagination-main">
        {this.state.totalRecords ? (
          <div className="totalRecords">
            {<span>Total: {this.state.totalRecords}</span>}
          </div>
        ) : (
          <div className="totalRecords"></div>
        )}
        <div className="pagination">
          <form
            className="go-to-form"
            onSubmit={(e) => {
              e.preventDefault();
              e.stopPropagation();
              const value = Number(this.inputRef.current.value);
              if (value >= 1 && value <= this.state.pages) {
                this.search(value - 1);
              }
            }}
          >
            <div className="goto-page-label">
              <span>Go to page:</span>
              <TextField
                defaultValue={this.page + 1}
                variant="outlined"
                type="number"
                onBlur={() => {
                  this.inputRef.current.value = Number(this.page) + 1;
                }}
                InputProps={{ inputProps: { min: 1, max: this.state.pages } }}
                max={this.state.pages}
                inputRef={this.inputRef}
              />
            </div>
          </form>
          <Pagination
            count={this.state.pages}
            variant="outlined"
            shape="rounded"
            onChange={(e, page) => this.search(page - 1)}
            page={this.page + 1}
          />
        </div>
      </div>
    );
  }
  replaceSortKey(sortKey) {
    return sortKey;
  }
  async getData(page, params) {
    console.log('params at getData: ',params);
    this.page = page;
    if (this.inputRef && this.inputRef.current) {
      this.inputRef.current.value = page + 1;
    }
    this.setState({ isLoading: true });
    if (this.props.isLoading) {
      this.props.isLoading(true);
    }
    let sortParam = "";
    if (this.sortkey) {
      let sortkey = this.replaceSortKey(this.sortkey);
      sortParam = "&sort=" + sortkey;
      if (this.sortby) {
        sortParam += "," + this.sortby;
      }
    }
    const response = await API.POST(
      this.url + "&page=" + page + sortParam,
      params
    );

    if (this.props.isLoading) {
      this.props.isLoading(false);
    }
    this.setState({ isLoading: false });
    this.showToaster(response);
    return response;
  }
  async getExportAPIData() {
    const page = 0;
    const params = this.prepareRequestBody();
    // console.log('params at getExportAPIData: ', params);
    let sortParam = "";
    if (this.sortkey) {
      let sortkey = this.replaceSortKey(this.sortkey);
      sortParam = "&sort=" + sortkey;
      if (this.sortby) {
        sortParam += "," + this.sortby;
      }
    }
    let url = this.exportUrl;
    if (!this.ignoreQueryParamsExport) {
      url = this.exportUrl + "&page=" + page + sortParam;
    }
    if(this.boqStatus){
      console.log('BOQ Status');
      console.log(this.exportUrl);
      url = this.exportUrl+ "?size=" +this.totalElements + "&page=" + page + sortParam;
      console.log(url);
    }
    const response = await API.POST(url, params);
    return response;
  }
  async exportToCSV() {
    const response = await this.getExportAPIData();
    if (!response.success) {
      this.props.enqueueSnackbar(response.errorMessage, {
        variant: "error",
      });
      return;
    }
    let json2csvCallback = getJson2csvCallback(this.exportFile + ".csv");
    converter.json2csv(this.getExportData(response), json2csvCallback);
  }
  showToaster(response) {
    if (!response.success) {
      const message = response.errorMessage || "Something went wrong";
      this.props.enqueueSnackbar(message, {
        variant: "error",
      });
    }
  }
  renderAutoComplete(options, placeholder, getoption) {
    return (
      <Autocomplete
        multiple
        id="tags-standard"
        options={options}
        onChange={(event, values) => (this.searchValue = values)}
        getOptionLabel={getoption}
        filterSelectedOptions={true}
        renderInput={(params) => (
          <TextField {...params} placeholder={placeholder} variant="outlined" />
        )}
      />
    );
  }

  renderAutoCompleteBT(options, placeholder, getoption) {
    return (
      <Autocomplete
        id="tags-standard"
        options={options}
        onChange={(event, values) => { this.onHandleBuildingType(values); }}
        getOptionLabel={getoption}
        filterSelectedOptions={true}
        renderInput={(params) => (
          <TextField {...params} placeholder={placeholder} variant="outlined" />
        )}
      />
    );
  }
  renderAutoCompleteBU(options, placeholder, getoption) {
    return (
      <Autocomplete
        multiple
        id="tags-standard"
        options={options}
        onChange={(event, values) => { this.onHandleBuildingUnit(values); }}
        getOptionLabel={getoption}
        filterSelectedOptions={true}
        renderInput={(params) => (
          <TextField {...params} placeholder={placeholder} variant="outlined" />
        )}
      />
    );
  }
  renderAutoSearch(url, placeholder) {
    return (
      <AutoCompleteWithSearch
        url={url}
        placeholder={placeholder}
        onChange={(event, values) => (this.searchValue = values)}
      />
    );
  }
  renderExport() {
    return (
      <IconButtons
        onClick={() => {
          this.exportToCSV();
        }}
        buttonClass="download"
        label={messages.common.export}
        icon={"DownloadSVG"}
      />
    );
  }
  renderLoader() {
    return (
      <React.Fragment>
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
        <Skeleton variant="rect" height={height} />
      </React.Fragment>
    );
  }

  async delete(data) {
    const id = data[this.deleteKey];
    const response = await API.DELETE(this.deleteUrl + id);
    if (response.success) {
      this.search();

      this.props.enqueueSnackbar(this.title + messages.common.deleteSuccess, {
        variant: "success",
      });
      this.setState({ showDetails: false });
    } else {
      this.props.enqueueSnackbar(response.errorMessage, {
        variant: "error",
      });
    }
  }
}

export default ListCommon;
