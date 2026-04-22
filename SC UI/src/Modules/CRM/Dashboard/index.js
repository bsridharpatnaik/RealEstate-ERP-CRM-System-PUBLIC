import React, { Component } from "react";
import { FormControl, FormControlLabel, Radio, RadioGroup } from "@material-ui/core";
// import TopPerformer from "./TopPerfomer";
import SalesFunnel from "./SalesFunnel";
import CustomerPipeline from "./CustomerPipeline";
import StagnatedLeads from "./StagnatedLeads";
import { API } from "./../../../axios";
import { apiEndpoints } from "./../../../endpoints";
import { setSession } from "./../../../helper";
import Chart from "../../../Shared/Chart";
import moment from "moment";
import "./dashboardstyle.scss";
import Skeleton from "@material-ui/lab/Skeleton";
import IconButton from "@material-ui/core/IconButton";
import NavigateNextIcon from "@material-ui/icons/NavigateNext";
import NavigateBeforeIcon from "@material-ui/icons/NavigateBefore";
import InfoIcon from "@material-ui/icons/Info";
import { constants } from "./../../../messages";

const height = 35;
class CRMDashboard extends Component {
  format = constants.dateFormat;
  state = {
    selectedDate: moment(),
    selected: "leadGenerated",
    selectedText: "Lead Generated",
    isLoaded: false,
    isChartLoaded: false,
    chartData: [],
    navigationType: "monthly"
  };
  data = {};
  getStartOfMonth(date = moment()) {
    return date.clone().startOf("month");
  }
  getEndOfMonth(date = moment()) {
    return date.clone().endOf("month");
  }

  getStartOfWeek(date = moment()) {
    return date.clone().startOf("week");
  }
  getEndOfWeek(date = moment()) {
    return date.clone().endOf("week");
  }

  async componentDidMount() {
    await this.getData(this.getStartOfMonth(moment()), this.getEndOfMonth(moment()));
    this.getActivityData();
  }

  async getData(startDate, endDate) {
    this.setState({ isLoaded: false, isChartLoaded: false });
    const params = {
      fromDate: startDate.format(this.format),
      toDate: endDate.format(this.format),
    };
    const response = await API.POST(apiEndpoints.getpipelineDB, params);
    if (response.success) {
      this.data = { ...this.data, ...response.data };
      this.setChartData(this.state.selected);
      this.setState({ isLoaded: true });
    }
  }

  async getActivityData() {
    this.setState({ isLoaded: false, isChartLoaded: false });
    const response = await API.GET(apiEndpoints.getActivitiesData);
    if (response.success) {
      this.data = { ...this.data, ...response.data };
      this.setChartData(this.state.selected);
      this.setState({ isLoaded: true });
    }
  }

  setChartData(selection) {
    if (selection === "leadGenerated") {
      this.setState({ chartData: this.data?.leadGenerated?.detailed });
    }
    if (selection === "activitiesCreated") {
      this.setState({ chartData: this.data?.activitiesCreated?.detailed });
    }
    if (selection === "totalPropertyVisit") {
      this.setState({ chartData: this.data?.totalPropertyVisit?.detailed });
    }
    if (selection === "dealClosed") {
      this.setState({ chartData: this.data?.dealClosed?.detailed });
    }
    if (selection === "dealLost") {
      this.setState({ chartData: this.data?.dealLost?.detailed });
    }
    if (selection === "liveLead") {
      this.setState({ chartData: this.data?.liveLeads?.detailed });
    }
    if (selection === "prospectiveLead") {
      this.setState({ chartData: this.data?.prospectiveLeads?.detailed });
    }
    if (selection === "todayactivity") {
      this.setState({ chartData: this.data?.todaysActivities?.detailed });
    }
    if (selection === "tomorrowsActivity") {
      this.setState({ chartData: this.data?.tomorrowsActivities?.detailed });
    }
    if (selection === "pendingactivity") {
      this.setState({ chartData: this.data?.pendingActivities?.detailed });
    }
    if (selection === "upcomingactivity") {
      this.setState({ chartData: this.data?.upcomingActivities?.detailed });
    }
    if (selection === "dealLostReasons") {
      this.setState({ chartData: this.data?.dealLostReasons?.detailed });
    }
    if (selection === "dealCancelled") {
      this.setState({ chartData: this.data?.dealCancelled?.detailed });
    }
    if (selection === "staleLeads") {
      this.setState({ chartData: this.data?.staleLeads?.detailed });
    }
    this.setState({ isChartLoaded: true });
  }

  goToLead(selection) {
    let filterData = [];
    switch (selection) {
      case "dealLostReasons":
        filterData = {
          activityStartDate: this.getStartOfMonth(
            this.state.selectedDate
          ).format(this.format),
          activityEndDate: this.getEndOfMonth(this.state.selectedDate).format(
            this.format
          ),
          activityType: "Deal_Lost",
          leadStatus: ["Deal_Lost"],
        };
        break;
      case "leadGenerated":
        filterData = {
          createdStartDate: this.getStartOfMonth(
            this.state.selectedDate
          ).format(this.format),
          createdEndDate: this.getEndOfMonth(this.state.selectedDate).format(
            this.format
          ),
          showOnlyLatest: { name: "Latest Activity Only", value: true },
        };
        break;
      case "activitiesCreated":
        filterData = {
          activityStartDate: this.getStartOfMonth(
            this.state.selectedDate
          ).format(this.format),
          activityEndDate: this.getEndOfMonth(this.state.selectedDate).format(
            this.format
          ),
        };
        break;
      case "totalPropertyVisit":
        filterData = {
          activityStartDate: this.getStartOfMonth(
            this.state.selectedDate
          ).format(this.format),
          activityEndDate: this.getEndOfMonth(this.state.selectedDate).format(
            this.format
          ),
          activityType: "Property_Visit",
          showRescheduled: { name: "No", value: false },
        };
        break;
      case "dealClosed":
        filterData = {
          activityStartDate: this.getStartOfMonth(
            this.state.selectedDate
          ).format(this.format),
          activityEndDate: this.getEndOfMonth(this.state.selectedDate).format(
            this.format
          ),
          activityType: "Deal_Close",
          leadStatus: ["Deal_Closed"]
        };
        break;
      case "dealLost":
        filterData = {
          activityStartDate: this.getStartOfMonth(
            this.state.selectedDate
          ).format(this.format),
          activityEndDate: this.getEndOfMonth(this.state.selectedDate).format(
            this.format
          ),
          activityType: "Deal_Lost",
        };
        break;
      case "dealCancelled":
        filterData = {
          activityStartDate: this.getStartOfMonth(
            this.state.selectedDate
          ).format(this.format),
          activityEndDate: this.getEndOfMonth(this.state.selectedDate).format(
            this.format
          ),
          activityType: "Deal_Cancelled",
          showOnlyLatest: { name: "Latest Activity Only", value: true },
        };
        break;
      case "liveLead":
        filterData = {
          leadStatus: [
            "New_Lead",
            "Visit_Scheduled",
            "Visit_Completed",
            "Negotiation"
          ],
          showOnlyLatest: { name: "Latest Activity Only", value: true }
        };
        break;
      case "prospectiveLead":
        filterData = {
          leadStatus: [
            "New_Lead",
            "Visit_Scheduled",
            "Visit_Completed",
            "Negotiation"
          ],
          showOnlyLatest: { name: "Latest Activity Only", value: true },
          prospectLead: { name: "Yes", value: true }
        };
        break;
      case "todayactivity":
        filterData = {
          activityStartDate: this.state.selectedDate.format(this.format),
          activityEndDate: this.state.selectedDate.format(this.format),
        };
        break;
      case "tomorrowsActivity":
        filterData = {
          activityStartDate: moment().add(1, "day").format(this.format),
          activityEndDate: moment().add(1, "day").format(this.format),
        };
        break;
        case "pendingactivity":
          if (this.state.selectedDate.isBefore(moment(), "day")) {
            filterData = {
              activityStatus: { name: "Open", value: true },
              activityEndDate: this.getEndOfMonth(this.state.selectedDate).format(
                this.format
              )
            };
          } else if (this.state.selectedDate.isAfter(moment(), "day")) {
            filterData = {
              activityStatus: { name: "Open", value: true },
              activityEndDate: this.state.selectedDate
                .add(-1, "days")
                .format(this.format)
              
            };
          } else if (this.state.selectedDate.isSame(moment(), "day")) {
            filterData = {
              activityStatus: { name: "Open", value: true },
              activityEndDate: this.state.selectedDate
                .add(-1, "days")
                .format(this.format)
            };
          }
          break;
      case "upcomingactivity":
        if (this.state.selectedDate.isBefore(moment(), "day")) {
          filterData = {
            activityStatus: { name: "Open", value: true },
            activityStartDate: this.state.selectedDate
              .add(1, "days")
              .format(this.format).format(
              this.format
            ),
          };
        } else if (this.state.selectedDate.isAfter(moment(), "day")) {
          filterData = {
            activityStatus: { name: "Open", value: true },
            activityStartDate: this.getStartOfMonth(
              this.state.selectedDate
            ).format(this.format).format(
              this.format
            ),
          };
        } 
        case "staleLeads":
          filterData = {
            leadStatus: [
              "New_Lead",
              "Visit_Scheduled",
              "Visit_Completed",
              "Negotiation"
            ],
            showOnlyLatest: { name: "Latest Activity Only", value: true },
            activityStatus: { name: "Closed", value: false }
          };
        
        break;
      default:
        break;
    }
    setSession("leadfilterdata", filterData);
    this.props.history.push("/crm/lead");
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

  renderPipeline() {
    return (
      <div className="leftPanel">
        <div className="pipeline">
          <span className="heading">Customer Pipeline</span>
          {this.renderpipelinedetail(
            "leadGenerated",
            "Lead Generated",
            this.data?.leadGenerated?.total
          )}
          {this.renderpipelinedetail(
            "activitiesCreated",
            "Activities Created",
            this.data?.activitiesCreated?.total
          )}
          {this.renderpipelinedetail(
            "dealClosed",
            "Deal Closed",
            this.data?.dealClosed?.total
          )}
          {this.renderpipelinedetail(
            "dealLost",
            "Deal Lost",
            this.data?.dealLost?.total
          )}
          {this.renderpipelinedetail(
            "dealLostReasons",
            "Deal Lost Reason",
            this.data?.dealLostReasons?.total
          )}
          {this.renderpipelinedetail(
            "dealCancelled",
            "Deal Cancelled",
            this.data?.dealCancelled?.total
          )}
        </div>
        <hr />
        <div className="activities">
          <span className="heading">Activities</span>
          {this.renderpipelinedetail(
            "liveLead",
            "Live Leads",
            this.data?.liveLeads?.total
          )}
          {this.renderpipelinedetail(
            "prospectiveLead",
            "Prospective Leads",
            this.data?.prospectiveLeads?.total
          )}
          {this.renderpipelinedetail(
            "todayactivity",
            "Todays Activities",
            this.data?.todaysActivities?.total
          )}
          {this.renderpipelinedetail(
            "tomorrowsActivity",
            "Tomorrow Activities",
            this.data?.tomorrowsActivities?.total
          )}
          {this.renderpipelinedetail(
            "pendingactivity",
            "Pending Activities",
            this.data?.pendingActivities?.total
          )}
          {this.renderpipelinedetail(
            "staleLeads",
            "Stale Leads",
            this.data?.staleLeads?.total
          )}
        </div>
      </div>
    );
  }
  renderactivities(title, count) {
    return (
      <div className="activity-detail">
        <span className="details">{`${title} (${count})`}</span>
      </div>
    );
  }

  renderpipelinedetail(name, title, count) {
    let classname =
      "pipeline-detail" + (this.state.selected === name ? " active" : "");
    return (
      <div className={classname}>
        <div
          className="header"
          onClick={() => {
            this.setState({
              selected: name,
              selectedText: title,
              isChartLoaded: false,
            });
            this.setChartData(name);
          }}
        >
          {title}
        </div>
        <div className="count">{count}</div>
        <div className="infoIcon">
          <IconButton
            disabled={!count}
            aria-label="info"
            onClick={() => {
              count && this.goToLead(name);
            }}
          >
            <InfoIcon fontSize="small" />
          </IconButton>
        </div>
        <div className="selected"></div>
      </div>
    );
  }

  renderChart() {
    return (
      <div className="chartwrapper">
        <div className="topchart">
          <div className="leftPanel heading">{this.state.selectedText}</div>
          <div className="rightPanel">
            <FormControl className="navigation-type" component="fieldset">
              <RadioGroup
                className="radio-group"
                name="navigationType"
                value={this.state.navigationType}
                onChange={(event) => {
                  let navigationType = event.target.value;
                  this.setState({ navigationType });
                  let date = this.state.selectedDate;
                  switch (navigationType) {
                    case "daily":
                      this.getData(date, date);
                      break;
                    case "weekly":
                      this.getData(this.getStartOfWeek(date), this.getEndOfWeek(date));
                      break;
                    case "monthly":
                      this.getData(this.getStartOfMonth(date), this.getEndOfMonth(date));
                      break;
                    default:
                      break;
                  }
                }}
              >
                <FormControlLabel className="fullform" value="daily" control={<Radio color="primary" size="small" />} label="Daily" />
                <FormControlLabel className="fullform" value="weekly" control={<Radio color="primary" size="small" />} label="Weekly" />
                <FormControlLabel className="fullform" value="monthly" control={<Radio color="primary" size="small" />} label="Monthly" />
                <FormControlLabel className="shortform" value="daily" control={<Radio color="primary" size="small" />} label="D" />
                <FormControlLabel className="shortform" value="weekly" control={<Radio color="primary" size="small" />} label="W" />
                <FormControlLabel className="shortform" value="monthly" control={<Radio color="primary" size="small" />} label="M" />
              </RadioGroup>
            </FormControl>
            <div className="navigation">
              {(this.state.navigationType === "daily") && <span className="month">
                {this.state.selectedDate.format("DD MMMM'YY")}
              </span>}
              {(this.state.navigationType === "weekly") && <span className="month">
                Week {this.state.selectedDate.week()}
              </span>}
              {(this.state.navigationType === "monthly") && <span className="month">
                {this.state.selectedDate.format("MMMM YYYY")}
              </span>}
              <IconButton
                aria-label="previous"
                onClick={() => {
                  let date = this.state.selectedDate;
                  switch (this.state.navigationType) {
                    case "daily":
                      date = date.add(-1, "d");
                      this.setState({ selectedDate: date });
                      this.getData(date, date);
                      break;
                    case "weekly":
                      date = date.add(-1, "w");
                      this.setState({ selectedDate: date });
                      this.getData(this.getStartOfWeek(date), this.getEndOfWeek(date));
                      break;
                    case "monthly":
                      date = date.add(-1, "M");
                      this.setState({ selectedDate: date });
                      this.getData(this.getStartOfMonth(date), this.getEndOfMonth(date));
                      break;
                    default:
                      break;
                  }
                }}
              >
                <NavigateBeforeIcon fontSize="medium" />
              </IconButton>

              <IconButton
                aria-label="next"
                onClick={() => {
                  let date = this.state.selectedDate;
                  switch (this.state.navigationType) {
                    case "daily":
                      date = date.add(1, "d");
                      this.setState({ selectedDate: date });
                      this.getData(date, date);
                      break;
                    case "weekly":
                      date = date.add(1, "w");
                      this.setState({ selectedDate: date });
                      this.getData(this.getStartOfWeek(date), this.getEndOfWeek(date));
                      break;
                    case "monthly":
                      date = date.add(1, "M");
                      this.setState({ selectedDate: date });
                      this.getData(this.getStartOfMonth(date), this.getEndOfMonth(date));
                      break;
                    default:
                      break;
                  }
                }}
              >
                <NavigateNextIcon fontSize="medium" />
              </IconButton>
            </div>
          </div>
        </div>
        <div className="chartDetail">
          <Chart
            key={this.state.selected}
            data={this.state.chartData}
            dashboardChart={false}
            dateX="userName"
            valueY="value"
          />
        </div>
      </div>
    );
  }

  render() {
    return (
      <div className="crmdashboard-wrapper">
        <div className="top">
          <div className="left box">
            {this.state.isLoaded ? this.renderPipeline() : this.renderLoader()}
          </div>
          <div className="right box">
            {this.state.isChartLoaded
              ? this.renderChart()
              : this.renderLoader()}
          </div>
        </div>
        <div className="bottom">
          <div className="left">
            <div className="topPerformer">
              <SalesFunnel />
            </div>
          </div>
          <div className="right">
            <div className="customerPipeline">
              <CustomerPipeline />
            </div>
            <div className="stagnatedleads">
              <StagnatedLeads />
            </div>
          </div>
        </div>
      </div>
    );
  }
}

export default CRMDashboard;
