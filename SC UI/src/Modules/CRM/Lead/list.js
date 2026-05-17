//react
import React from "react";
//Third Party
import ListCommon from "./../../../Shared/List";
import { withSnackbar } from "notistack";
import Popper from "@material-ui/core/Popper";
import Filter from "./filter";
import IconButtons from "./../../../Shared/Button/IconButtons.js";
import { API } from "./../../../axios";
import Papa from 'papaparse';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faUpload } from '@fortawesome/free-solid-svg-icons';

//component
import Table from "./table";
//misc
import { apiEndpoints, exportURL } from "./../../../endpoints";
import {
  getSession,
  clearSession,
  getRole,
  getUserId,
  getUserName,
} from "./../../../helper";
import { messages } from "./../../../messages";
class List extends ListCommon {
  deleteKey = "leadId";
  deleteUrl = apiEndpoints.individualLead;
  importurl = apiEndpoints.importLead;
  title = messages.common.lead;
  showLoanStatus = getSession("showLoanStatus");
  state = { data: [], options: [] };
  tableData = {
    headers: [
      messages.common.id,
      "Name",
      messages.fields.mobileNo,
      this.showLoanStatus ? "Total Balance" : "Activity Status",
      this.showLoanStatus ? "Next Payment Due" : "Activity Type",
      this.showLoanStatus ? "Loan Status" : "Lead Status",
      messages.fields.owner,
    ],
    keys: [
      messages.formParams.lead.leadId,
      "name",
      "mobileNumber",
      this.showLoanStatus ? "totalPending" : "activityStatus",
      this.showLoanStatus ? "nextPaymentDate" : "activityType",
      this.showLoanStatus ? "loanStatus" : "leadStatus",
      "assigneeId",
    ],
  };
  url = apiEndpoints.getLead;
  exportUrl = exportURL.getLead;
  exportFile = messages.exportFiles.lead;
  filterData = {};
  disableAssignee = false;
  pageNumber = 0;
  componentDidMount() {
    const currentUserRole = getRole();
    const currentUserId = getUserId();
    const currentUserName = getUserName();
    const leadfilterData = getSession("leadfilterdata");
    const page = getSession("leadfilterPage");
    this.filterData = leadfilterData === null ? {} : leadfilterData;
    if (
      !(currentUserRole.toLowerCase().indexOf("admin") > -1) &&
      !(currentUserRole.toLowerCase().indexOf("crm-manager") > -1)
    ) {
      this.filterData = {
        ...this.filterData,
        assignee: [{ name: currentUserName, value: Number(currentUserId) }],
      };
      this.disableAssignee = true;
    }
    page === null || page === undefined ? this.search() : this.search(page);
    this.filterRef = React.createRef();
    this.fileInputRef = React.createRef(); 
    if (this.showLoanStatus) {
      let headers = this.tableData.headers;
      let keys = this.tableData.keys;
      headers.splice(3, 0, "Customer Status");
      keys.splice(3, 0, "customerStatus")
      this.tableData = {
        headers,
        keys
      }
    }
  }
  prepareRequestBody() {
    let params;
    params = {};
    params.filterData = [];
    if (this.filterData) {
      for (const field in this.filterData) {
        let value = this.filterData[field];
        if (value && (value.length || ["showOnlyLatest"].includes(field))) {
          if (
            ["source", "broker", "assignee", "stagnantStatus"].includes(field)
          ) {
            value = value.map((v) => "" + v.value);
          } else if (
            [
              "createdStartDate",
              "createdEndDate",
              "purpose",
              "textSearch",
              "address",
              "occupation",
              "activityType",
              "activityStartDate",
              "activityEndDate",
              
            ].includes(field)
          ) {
            value = [value];
          } else if (["showOnlyLatest"].includes(field)) {
            value = [value ? "true" : "false"];
          } //else if (["leadStatus"].includes(field)) {
            //value = [value];
          //}
          params.filterData.push({
            attrName: field,
            attrValue: value,
          });
        } else if (
          value &&
          ["activityStatus", "showRescheduled", "prospectLead"].includes(field)
        ) {
          value = value.value;
          params.filterData.push({
            attrName: field,
            attrValue: [value],
          });
        }
      }
    }
    params.filterData.push({
      attrName: "globalSearch",
      attrValue: this.searchValue,
    });

    return params;
  }
  getExportData(response) {
    return response.data.content;
  }

  replaceSortKey(sortKey) {
    if (["leadId", "customerStatus", "totalPending", "nextPaymentDate", "loanStatus"].includes(sortKey)) {
      return `lead.${sortKey}`
    } else if (sortKey === "name") {
      return 'lead.customerName';
    } else if (sortKey === "mobileNumber") {
      return 'lead.primaryMobile';
    } else if (sortKey === "activityStatus") {
      return 'isOpen';
    } else if (sortKey === "assigneeId") {
      return 'lead.asigneeId';
    }
    return `${sortKey}`;
  }

  async search(page = 0) {
    const params = this.prepareRequestBody();

    const response = await this.getData(page, params);
    this.pageNumber = page;
    clearSession("leadfilterdata");
    clearSession("leadfilterPage");
    if (response.success) {
      this.getOption();
      this.setState({
        data: response.data.leadPageDetails.content,
        pages: response.data.leadPageDetails.totalPages,
        totalRecords: response.data.leadPageDetails.totalElements,
      });
    }
  }

  async getOption() {
    const response = await API.GET(apiEndpoints.getLeadDropdowns);
    if (response.success) {
      this.props.setOptions(response.data.dropdownData);
      this.typeahead = response.data.typeAheadDataForGlobalSearch;
      this.dropdowns = response.data.dropdownData;
      const assigneeDetails = response.data.dropdownData.assigneeDetails.assigneeDetails || []; 
      this.setState({
        options: response.data.typeAheadDataForGlobalSearch,
        dropdownData: response.data.dropdownData,
        assigneeDetails: assigneeDetails,
      });
    }
  }

  convertCSVToJson = (data) => {
    const assigneeDetailsMap = this.state.assigneeDetails.reduce((map, assignee) => {
      map[assignee.userName] = assignee.userid;
      return map;
    }, {});
  
    return data.map(row => ({
      Name: row.Name,
      MobileNo: row.MobileNo,
      Assignee: row.Assignee,
      AssigneeId: assigneeDetailsMap[row.Assignee] || null, // Get assigneeId from the map
    }));
  };
  
  handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      Papa.parse(file, {
        complete: (result) => {
          const data = result.data;
          const headersValid = this.validateCSVHeaders(data);
          const { isValid: mobileNumbersValid, invalidRow } = headersValid ? this.validateMobileNumbers(data) : { isValid: false, invalidRow: null };
          const invalidAssignees = headersValid ? this.validateAssignees(data) : [];
  
          if (headersValid && mobileNumbersValid && invalidAssignees.length === 0) {
            const jsonData = this.convertCSVToJson(data);
            this.sendToBackend(jsonData);
          } else {
            let errorMessage = '';
            if (!headersValid) {
              errorMessage = 'Invalid CSV format. Please ensure it has exactly 3 columns: Name, MobileNo, Assignee.';
            } else if (!mobileNumbersValid) {
              errorMessage = `Invalid mobile number format at row ${invalidRow}. Please ensure all mobile numbers are exactly 10 digits.`;
            } else if (invalidAssignees.length > 0) {
              errorMessage = `Invalid assignee names at the following rows: ${invalidAssignees.map(item => `Row ${item.row}: ${item.assignee}`).join(', ')}.`;
            }
            alert(errorMessage);
          }
          // Reset the file input value
          if (this.fileInputRef.current) {
            this.fileInputRef.current.value = '';
          }
        },
        header: true,
        skipEmptyLines: true,
      });
    }
  };
  
  validateCSVHeaders = (data) => {
    const requiredHeaders = ['name', 'mobileno', 'assignee'];
    if (data.length === 0) {
      return false;
    }
    const headers = Object.keys(data[0]).map(header => header.toLowerCase());
    return (
      headers.length === 3 &&
      requiredHeaders.every(requiredHeader => headers.includes(requiredHeader))
    );
  };
  
  validateMobileNumbers = (data) => {
    for (let i = 0; i < data.length; i++) {
      const row = data[i];
      const mobileNo = row.MobileNo || row.mobileno; // case-insensitive access
      if (!/^[0-9]{10}$/.test(mobileNo)) {
        return { isValid: false, invalidRow: i + 1 }; // +1 to account for header row
      }
    }
    return { isValid: true, invalidRow: null };
  };
  
  validateAssignees = (data) => {
    const validAssignees = this.state.assigneeDetails.map(assignee => assignee.userName);
    const invalidAssignees = [];
  
    for (let i = 0; i < data.length; i++) {
      const row = data[i];
      if (!validAssignees.includes(row.Assignee)) {
        invalidAssignees.push({ assignee: row.Assignee, row: i + 1 });
      }
    }
  
    return invalidAssignees;
  };

  sendToBackend = async (data) => {
    try {
      const response = await API.POST(apiEndpoints.importLead, data);
      if (response.success) {
        alert('Data successfully uploaded. Please refresh the page');
        window.location.reload();  // Reload the page after the alert is closed
      } else {
        throw new Error(response.errorMessage || 'Error uploading data');
      }
    } catch (error) {
      let errorMessage = 'Unknown error occurred';
      if (error.message) {
        errorMessage = error.message;
      }
      alert(`Backend error: ${errorMessage}`);
    }
  };
  
  
  

  render() {
    return (
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
              messages.common.searchByName
            )}
          </form>
          <div className="top-button-wrapper">
            {this.renderExport()}
            <button onClick={() => this.fileInputRef.current.click()}>
              <FontAwesomeIcon icon={faUpload} /> Import CSV
            </button>
            <input
              ref={this.fileInputRef} // Attach the ref to the file input
              type="file"
              accept=".csv"
              style={{ display: 'none' }}
              onChange={this.handleFileChange}
            />
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
              options={this.dropdowns || {}}
              typeahead={this.typeahead || []}
              showLoanStatus={this.showLoanStatus}
              disableAssignee={this.disableAssignee}
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
            hidedelete={true}
            tableData={this.tableData}
            rows={this.state.data}
            edit={this.props.edit}
            delete={(row) => this.delete(row)}
            sortby={this.sortby}
            sortkey={this.sortkey}
            filterData={this.filterData}
            pageNumber={this.pageNumber}
            search={(sortkey, sortby) => {
              this.sortby = sortby;
              this.sortkey = sortkey;
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
