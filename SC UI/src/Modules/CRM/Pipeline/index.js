import React from "react";
import IconButton from "@material-ui/core/IconButton";
import NavigateNextIcon from "@material-ui/icons/NavigateNext";
import NavigateBeforeIcon from "@material-ui/icons/NavigateBefore";
import { API } from "./../../../axios";
import { apiEndpoints } from "./../../../endpoints";
import Common from "../../../Shared/CommonIndex";
import { messages } from "./../../../messages";
import "./pipelines.scss";
import PipelineTypes from "./PipelineTypes";
import PipelineDetails from "./PipelineDetails";
import Popper from "@material-ui/core/Popper";
import Filter from "./filter";
import IconButtons from "./../../../Shared/Button/IconButtons.js";
import { withRouter } from "react-router-dom";
import {
  getSession,
  clearSession,
  getRole,
  getUserId,
  getUserName,
} from "../../../helper";
import { withSnackbar } from "notistack";

class Pipeline extends Common {
  title = messages.common.pipelines;
  url = apiEndpoints.getPipeLineDetails;
  filterData = {};
  disableAssignee = false;
  dropdowns = {};
  typeahead = [];
  leadgenerationTotal = 0;
  visitScheduledTotal = 0;
  visitCompletedTotal = 0;
  negotiationTotal = 0;
  dealclosureTotal = 0;
  state = { isLoading: true, pipelineData: [], showDealClosure: false };
  async componentDidMount() {
    const currentUserRole = getRole();
    const currentUserId = getUserId();
    const currentUserName = getUserName();
    const pipelinefilterData = getSession("pipelinefilterData");
    this.filterData = pipelinefilterData === null ? {} : pipelinefilterData;
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
    this.search();
    this.filterRef = React.createRef();
  }

  async search() {
    const params = this.prepareRequestBody();
    const response = await this.getData(params);
    clearSession("pipelinefilterData");
    if (response.success) {
      this.getOption();
      const pipelineData = {
        leadgeneration: response.data.leadGeneration.leads,
        propertyvisitScheduled: response.data.propertyVisitScheduled.leads,
        propertyvisitCompleted: response.data.propertyVisitCompleted.leads,
        negotiation: response.data.negotiation.leads,
        dealclosure: response.data.deal_close.leads,
      };
      this.leadgenerationTotal = response.data.leadGeneration.totalCount;
      this.visitScheduledTotal = response.data.propertyVisitScheduled.totalCount;
      this.visitCompletedTotal = response.data.propertyVisitCompleted.totalCount;
      this.negotiationTotal = response.data.negotiation.totalCount;
      this.dealclosureTotal = response.data.deal_close.totalCount;
      this.setState({ pipelineData: pipelineData });
      this.setState({ isLoading: false });
    } else {
      this.setState({ isLoading: false });
    }
  }

  async getOption() {
    const response = await API.GET(apiEndpoints.getLeadDropdowns);
    if (response.success) {
      this.typeahead = response.data.typeAheadDataForGlobalSearch;
      this.dropdowns = response.data.dropdownData;
      this.setState({
        options: response.data.typeAheadDataForGlobalSearch,
        dropdownData: response.data.dropdownData,
      });
    }
  }

  async getData(params) {
    this.setState({ isLoading: true });
    const response = await API.POST(this.url, params);
    this.showToaster(response);
    return response;
  }

  prepareRequestBody() {
    let params;
    params = {};
    params.filterData = [];
    if (this.filterData) {
      for (const field in this.filterData) {
        let value = this.filterData[field];

        if (value && value.length) {
          if (
            ["source", "broker", "assignee", "stagnantStatus","recentActivityStatus"].includes(field)
          ) {
            value = value.map((v) => "" + v.value);
          } else if (
            [
              "createdStartDate",
              "leadStatus",
              "createdEndDate",
              "purpose",
              "address",
              "occupation",
            ].includes(field)
          ) {
            value = [value];
          }
          params.filterData.push({
            attrName: field,
            attrValue: value,
          });
        } else if (value && ["activityStatus", "prospectLead"].includes(field)) {
          value = value.value;
          params.filterData.push({
            attrName: field,
            attrValue: [value],
          });
        }
      }
    }
    return params;
  }
  showToaster(response) {
    if (!response.success) {
      const message = response.errorMessage || "Something went wrong";
      this.props.enqueueSnackbar(message, {
        variant: "error",
      });
    }
  }
  render() {
    return (
      <div className="page">
        <div className="header-info">
          <div>
            {this.renderHeading()}
            {this.renderBreadcrums(messages.common.crm)}
          </div>
        </div>
        <div class="pipeline-container">
          <div class="pipeline-header">
            <div class="pipeline-stagnated">
              <div class="text-header">
                {messages.common.pipelineLabel.stagnated}
              </div>
              <div class="stagnated-items">
                <div class="stagnated-item">
                  <div class="triangle staginated-image"></div>
                  <div class="staginated-text">
                    {messages.common.pipelineLabel.days1020}
                  </div>
                </div>
                <div class="stagnated-item">
                  <div class="square staginated-image"></div>
                  <div class="staginated-text">
                    {messages.common.pipelineLabel.days2030}
                  </div>
                </div>
                <div class="stagnated-item">
                  <div class="circle staginated-image"></div>
                  <div class="staginated-text">
                    {messages.common.pipelineLabel.days30more}
                  </div>
                </div>
              </div>
            </div>
            <div class="sentiment-customer">
              <div class="text-header">
                {messages.common.pipelineLabel.sentimentOfCustomer}
              </div>
              <div class="sentiment-items">
                <div class="sentiment-item">
                  <div class="sentiment-itemimage green"></div>
                  <div class="sentiment-text">
                    {messages.common.pipelineLabel.high}
                  </div>
                </div>
                <div class="sentiment-item">
                  <div class="sentiment-itemimage orange"></div>
                  <div class="sentiment-text">
                    {messages.common.pipelineLabel.medium}
                  </div>
                </div>
                <div class="sentiment-item">
                  <div class="sentiment-itemimage red"></div>
                  <div class="sentiment-text">
                    {messages.common.pipelineLabel.low}
                  </div>
                </div>
              </div>
            </div>
            {this.renderFilter()}
          </div>
          <p style={{ fontSize: '12px' }}>
          Note: Activity information on this page is updated every 20 minutes. Please wait after updating an activity.
    </p><div className="pipeline-wrapper">
            <div className="status-navigation">
              <IconButton
                aria-label="next"
                disabled={!this.state.showDealClosure}
                onClick={() => {
                  this.setState({ showDealClosure: false });
                }}
              >
                <NavigateBeforeIcon fontSize="large" />
              </IconButton>
            </div>
            <div style={{ flexGrow: 1 }}>
              <PipelineTypes showDealClosure={this.state.showDealClosure} />
              <div class="pipeline-total">
                {!this.state.showDealClosure && <div class="pipeline-total-item">
                  {messages.common.total}: {this.leadgenerationTotal}
                </div>}
                <div class="pipeline-total-item">
                  {messages.common.total}: {this.visitScheduledTotal}
                </div>
                <div class="pipeline-total-item">
                  {messages.common.total}: {this.visitCompletedTotal}
                </div>
                <div class="pipeline-total-item">
                  {messages.common.total}: {this.negotiationTotal}
                </div>
                {this.state.showDealClosure && <div class="pipeline-total-item">
                  {messages.common.total}: {this.dealclosureTotal}
                </div>}
              </div>
              {this.state.isLoading ? (
                this.renderLoader()
              ) : (
                <PipelineDetails
                  pipelineData={this.state.pipelineData}
                  filterData={this.filterData}
                  history={this.props.history}
                  showDealClosure={this.state.showDealClosure}
                />
              )}
            </div>
            <div className="status-navigation">
              <IconButton
                aria-label="next"
                disabled={this.state.showDealClosure}
                onClick={() => {
                  this.setState({ showDealClosure: true });
                }}
              >
                <NavigateNextIcon fontSize="large" />
              </IconButton>
            </div>
          </div>
        </div>
      </div>
    );
  }

  renderFilter() {
    return (
      <div class="pipeline-filter">
        <div className="top-button-wrapper">
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
          placement="bottom-start"
        >
          <Filter
            filterData={this.filterData}
            options={this.dropdowns || {}}
            typeahead={this.typeahead || []}
            disableAssignee={this.disableAssignee}
            isPipeline={true}
            search={(data) => {
              this.filterData = data;
              this.search();
            }}
            close={() => this.setState({ filterOpen: false })}
          />
        </Popper>
      </div>
    );
  }
}

export default withRouter(withSnackbar(Pipeline));
