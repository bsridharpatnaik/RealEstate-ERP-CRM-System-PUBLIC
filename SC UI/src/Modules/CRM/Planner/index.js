import React from "react";
import { API } from "./../../../axios";
import { apiEndpoints } from "./../../../endpoints";
import Common from "../../../Shared/CommonIndex";
import { messages, constants } from "./../../../messages";
import { withRouter } from "react-router-dom";
import Button from "@material-ui/core/Button";
import IconButton from "@material-ui/core/IconButton";
import NavigateNextIcon from "@material-ui/icons/NavigateNext";
import NavigateBeforeIcon from "@material-ui/icons/NavigateBefore";
import Popper from "@material-ui/core/Popper";
import Filter from "./filter";
import IconButtons from "./../../../Shared/Button/IconButtons.js";
import moment from "moment";
import "./planner.scss";
import { withStyles } from "@material-ui/core/styles";
import Tooltip from "@material-ui/core/Tooltip";
import Typography from "@material-ui/core/Typography";
import {
  setSession,
  getSession,
  clearSession,
  getRole,
  getUserName,
  getUserId,
} from "../../../helper";
import Tabs from "@material-ui/core/Tabs";
import Tab from "@material-ui/core/Tab";
import Box from "@material-ui/core/Box";

const HtmlTooltip = withStyles((theme) => ({
  tooltip: {
    backgroundColor: "#f5f5f9",
    color: "rgba(0, 0, 0, 0.87)",
    maxWidth: 220,
    fontSize: theme.typography.pxToRem(12),
    border: "1px solid #dadde9",
  },
}))(Tooltip);

const a11yProps = (index) => {
  return {
    id: `vertical-tab-${index}`,
    "aria-controls": `vertical-tabpanel-${index}`,
  };
};

const AntTabs = withStyles({
  root: {
    borderRight: "1px solid #a8a0d5",
    width: "110px",
    // margin: 'auto',
  },
  indicator: {
    backgroundColor: "#6956cf",
  },
})(Tabs);

function TabPanel(props) {
  const { children, value, index, ...other } = props;

  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`vertical-tabpanel-${index}`}
      aria-labelledby={`vertical-tab-${index}`}
      {...other}
    >
      {value === index && (
        <Box pl={1}>
          <Typography>{children}</Typography>
        </Box>
      )}
    </div>
  );
}
class Planner extends Common {
  title = messages.common.planner;
  url = apiEndpoints.getPlannerDetails;
  dropdowns = {};
  typeahead = [];
  callTotal = 0;
  propertyvisitTotal = 0;
  meetingTotal = 0;
  dealclosureTotal = 0;
  reminderTotal = 0;
  taskTotal = 0;
  messageTotal = 0;
  emailTotal = 0;
  deallostTotal = 0;
  format = constants.dateFormat;
  state = {
    isLoading: true,
    value: 0,
    plannerData: {},
    selectedDate: moment().format(this.format),
    selectedWeek: this.calcCurrentWeek(moment()),
    totalWeek: this.calcAllWeek(moment()),
  };
  filterData = {};
  disableAssignee = false;
  async componentDidMount() {
    const currentUserRole = getRole();
    const currentUserId = getUserId();
    const currentUserName = getUserName();
    const plannerfilterData = getSession("plannerfilterData");
    this.filterData =
      plannerfilterData === null
        ? {
            activityStartDate: this.state.selectedDate,
            activityEndDate: this.state.selectedDate,
            ...this.filterData,
          }
        : plannerfilterData;
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

  async search() {
    const params = this.prepareRequestBody();
    const response = await this.getData(params);
    clearSession("plannerfilterData");
    if (response.success) {
      this.getOption();
      const plannerData = {
        call: response.data.call.activities,
        meeting: response.data.meeting.activities,
        propertyvisit: response.data.property_visit.activities,
        dealclosure: response.data.deal_close.activities,
        reminder: response.data.reminder.activities,
        task: response.data.payment?.activities,
        meessage: response.data.message.activities,
        email: response.data.email.activities,
        deallost: response.data.deal_lost.activities,
      };
      this.callTotal = response.data.call.totalActivities;
      this.meetingTotal = response.data.meeting.totalActivities;
      this.propertyvisitTotal = response.data.property_visit.totalActivities;
      this.dealclosureTotal = response.data.deal_close.totalActivities;
      this.reminderTotal = response.data.reminder.totalActivities;
      this.taskTotal = response.data.payment?.totalActivities;
      this.messageTotal = response.data.message.totalActivities;
      this.emailTotal = response.data.email.totalActivities;
      this.deallostTotal = response.data.deal_lost.totalActivities;
      this.setState({ plannerData: plannerData });
      this.setState({ isLoading: false });
    } else {
      this.setState({ isLoading: false });
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
            ["source", "broker", "assignee", "stagnantStatus"].includes(field)
          ) {
            value = value.map((v) => "" + v.value);
          } else if (
            [
              "createdStartDate",
              "activityStartDate",
              "activityEndDate",
              //"leadStatus",
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

  handleChange = (event, newValue) => {
    this.setState({ value: newValue });
  };

  render() {
    const { value } = this.state;
    return (
      <div className="page pageWrapper">
        <div className="header-info">
          <div>
            {this.renderHeading()}
            {this.renderBreadcrums(messages.common.crm)}
          </div>
          {this.renderFilter()}
        </div>

        <div className="planner-container">
          <div className="planner-months">
            <IconButton
              aria-label="previous"
              onClick={() => {
                const searchDate = moment(this.state.selectedDate, this.format)
                  .startOf("year")
                  .add(-1, "years");
                this.setState({
                  selectedDate: searchDate.format(this.format),
                  selectedWeek: 1,
                });
                this.filterData["activityStartDate"] = searchDate.format(
                  this.format
                );
                this.filterData["activityEndDate"] = searchDate.format(
                  this.format
                );
                this.search();
              }}
            >
              <NavigateBeforeIcon fontSize="large" />
            </IconButton>
            {this.rendermonth(
              "Jan ",
              moment(this.state.selectedDate, this.format)
                .startOf("year")
                .format(this.format)
            )}
            {this.rendermonth(
              "Feb ",
              moment(this.state.selectedDate, this.format)
                .startOf("year")
                .add(1, "M")
                .format(this.format)
            )}
            {this.rendermonth(
              "Mar ",
              moment(this.state.selectedDate, this.format)
                .startOf("year")
                .add(2, "M")
                .format(this.format)
            )}
            {this.rendermonth(
              "Apr ",
              moment(this.state.selectedDate, this.format)
                .startOf("year")
                .add(3, "M")
                .format(this.format)
            )}
            {this.rendermonth(
              "May ",
              moment(this.state.selectedDate, this.format)
                .startOf("year")
                .add(4, "M")
                .format(this.format)
            )}
            {this.rendermonth(
              "Jun ",
              moment(this.state.selectedDate, this.format)
                .startOf("year")
                .add(5, "M")
                .format(this.format)
            )}
            {this.rendermonth(
              "Jul ",
              moment(this.state.selectedDate, this.format)
                .startOf("year")
                .add(6, "M")
                .format(this.format)
            )}
            {this.rendermonth(
              "Aug ",
              moment(this.state.selectedDate, this.format)
                .startOf("year")
                .add(7, "M")
                .format(this.format)
            )}
            {this.rendermonth(
              "Sep ",
              moment(this.state.selectedDate, this.format)
                .startOf("year")
                .add(8, "M")
                .format(this.format)
            )}
            {this.rendermonth(
              "Oct ",
              moment(this.state.selectedDate, this.format)
                .startOf("year")
                .add(9, "M")
                .format(this.format)
            )}
            {this.rendermonth(
              "Nov ",
              moment(this.state.selectedDate, this.format)
                .startOf("year")
                .add(10, "M")
                .format(this.format)
            )}
            {this.rendermonth(
              "Dec ",
              moment(this.state.selectedDate, this.format)
                .startOf("year")
                .add(11, "M")
                .format(this.format)
            )}

            <IconButton
              aria-label="next"
              onClick={() => {
                const searchDate = moment(this.state.selectedDate, this.format)
                  .startOf("year")
                  .add(1, "years");
                this.setState({
                  selectedDate: searchDate.format(this.format),
                  selectedWeek: 1,
                });
                this.filterData["activityStartDate"] = searchDate.format(
                  this.format
                );
                this.filterData["activityEndDate"] = searchDate.format(
                  this.format
                );
                this.search();
              }}
            >
              <NavigateNextIcon fontSize="large" />
            </IconButton>
          </div>

          <div className="planner-week-header">
            <div className="planner-week-number">
              <div className="planner-week-name">
                <span className="planner-week-name">
                  {messages.common.plannerLabel.week} {this.state.selectedWeek}
                </span>
              </div>
              <div className="planner-week-button">
                <IconButton
                  aria-label="previous"
                  disabled={this.state.selectedWeek === 1}
                  onClick={() => {
                    let previousWeek = moment(
                      this.state.selectedDate,
                      this.format
                    )
                      .startOf("isoWeek")
                      .add(-1, "week");
                    const searchDate =
                      moment(this.state.selectedDate, this.format).month() ===
                      previousWeek.month()
                        ? previousWeek
                        : moment(this.state.selectedDate, this.format).startOf(
                            "month"
                          );
                    this.setState({
                      selectedDate: searchDate.format(this.format),
                      selectedWeek:
                        1 === this.state.selectedWeek
                          ? 1
                          : this.state.selectedWeek - 1,
                    });
                    this.filterData["activityStartDate"] = searchDate.format(
                      this.format
                    );
                    this.filterData["activityEndDate"] = searchDate.format(
                      this.format
                    );
                    this.search();
                  }}
                >
                  <NavigateBeforeIcon fontSize="large" />
                </IconButton>

                <IconButton
                  aria-label="next"
                  disabled={this.state.selectedWeek === this.state.totalWeek}
                  onClick={() => {
                    let nextWeek = moment(this.state.selectedDate, this.format)
                      .startOf("isoWeek")
                      .add(1, "week");
                    const searchDate =
                      moment(this.state.selectedDate, this.format).month() ===
                      nextWeek.month()
                        ? nextWeek
                        : moment(this.state.selectedDate, this.format).endOf(
                            "month"
                          );
                    this.setState({
                      selectedDate: searchDate.format(this.format),
                      selectedWeek:
                        this.state.totalWeek === this.state.selectedWeek
                          ? 1
                          : this.state.selectedWeek + 1,
                    });
                    this.filterData["activityStartDate"] = searchDate.format(
                      this.format
                    );
                    this.filterData["activityEndDate"] = searchDate.format(
                      this.format
                    );
                    this.search();
                  }}
                >
                  <NavigateNextIcon fontSize="large" />
                </IconButton>
              </div>
            </div>
            <div className="planner-header-items">
              {this.renderHeaderItems(
                messages.common.plannerLabel.call,
                this.callTotal
              )}
              {this.renderHeaderItems(
                messages.common.plannerLabel.meeting,
                this.meetingTotal
              )}
              {this.renderHeaderItems(
                messages.common.plannerLabel.propertyvisit,
                this.propertyvisitTotal
              )}
              {this.renderHeaderItems(
                messages.common.plannerLabel.dealclose,
                this.dealclosureTotal
              )}
              {this.renderHeaderItems(
                messages.common.plannerLabel.reminder,
                this.reminderTotal
              )}
              {this.renderHeaderItems(
                messages.common.plannerLabel.task,
                this.taskTotal
              )}
              {this.renderHeaderItems(
                messages.common.plannerLabel.message,
                this.messageTotal
              )}
              {this.renderHeaderItems(
                messages.common.plannerLabel.email,
                this.emailTotal
              )}
              {this.renderHeaderItems(
                messages.common.plannerLabel.deallost,
                this.deallostTotal
              )}
            </div>
          </div>
          <div className="planner-date-Data">
            {this.renderDates()}
            {/* <div className="planner-header-itemss">
              {this.renderHeaderItems(
                messages.common.plannerLabel.call,
                this.callTotal
              )}
              {this.renderHeaderItems(
                messages.common.plannerLabel.meeting,
                this.meetingTotal
              )}
              {this.renderHeaderItems(
                messages.common.plannerLabel.propertyvisit,
                this.propertyvisitTotal
              )}
              {this.renderHeaderItems(
                messages.common.plannerLabel.dealclose,
                this.dealclosureTotal
              )}
              {this.renderHeaderItems(
                messages.common.plannerLabel.reminder,
                this.reminderTotal
              )}
              {this.renderHeaderItems(
                messages.common.plannerLabel.task,
                this.taskTotal
              )}
              {this.renderHeaderItems(
                messages.common.plannerLabel.message,
                this.messageTotal
              )}
              {this.renderHeaderItems(
                messages.common.plannerLabel.email,
                this.emailTotal
              )}
              {this.renderHeaderItems(
                messages.common.plannerLabel.deallost,
                this.deallostTotal
              )}
            </div> */}
            {this.state.isLoading ? (
              this.renderLoader()
            ) : (
              <>
                <div className={"root"}>
                  <AntTabs
                    orientation="vertical"
                    variant="scrollable"
                    value={value}
                    onChange={this.handleChange}
                    aria-label="Vertical tabs example"
                    // className={classes.tabs}
                  >
                    <Tab
                      label={messages.common.plannerLabel.call}
                      {...a11yProps(0)}
                    />
                    <Tab
                      label={messages.common.plannerLabel.meeting}
                      {...a11yProps(1)}
                    />
                    <Tab
                      label={messages.common.plannerLabel.propertyvisit}
                      {...a11yProps(2)}
                    />
                    <Tab
                      label={messages.common.plannerLabel.dealclose}
                      {...a11yProps(3)}
                    />
                    <Tab
                      label={messages.common.plannerLabel.reminder}
                      {...a11yProps(5)}
                    />
                    <Tab
                      label={messages.common.plannerLabel.task}
                      {...a11yProps(6)}
                    />
                    <Tab
                      label={messages.common.plannerLabel.message}
                      {...a11yProps(7)}
                    />
                    <Tab
                      label={messages.common.plannerLabel.email}
                      {...a11yProps(8)}
                    />
                    <Tab
                      label={messages.common.plannerLabel.deallost}
                      {...a11yProps(9)}
                    />
                  </AntTabs>
                  <div className="planner-data">
                    <TabPanel value={value} index={0}>
                      {this.renderdata(this.state.plannerData.call)}
                    </TabPanel>
                    <TabPanel value={value} index={1}>
                      {this.renderdata(this.state.plannerData.meeting)}
                    </TabPanel>
                    <TabPanel value={value} index={2}>
                      {this.renderdata(this.state.plannerData.propertyvisit)}
                    </TabPanel>
                    <TabPanel value={value} index={3}>
                      {this.renderdata(this.state.plannerData.dealclosure)}
                    </TabPanel>
                    <TabPanel value={value} index={5}>
                      {this.renderdata(this.state.plannerData.reminder)}
                    </TabPanel>
                    <TabPanel value={value} index={6}>
                      {this.renderdata(this.state.plannerData.task)}
                    </TabPanel>
                    <TabPanel value={value} index={7}>
                      {this.renderdata(this.state.plannerData.message)}
                    </TabPanel>
                    <TabPanel value={value} index={8}>
                      {this.renderdata(this.state.plannerData.email)}
                    </TabPanel>
                    <TabPanel value={value} index={9}>
                      {this.renderdata(this.state.plannerData.deallost)}
                    </TabPanel>
                  </div>
                </div>
                <div className="planner-data planner-data-wrapper">
                  {this.renderdata(this.state.plannerData.call)}
                  {this.renderdata(this.state.plannerData.meeting)}
                  {this.renderdata(this.state.plannerData.propertyvisit)}
                  {this.renderdata(this.state.plannerData.dealclosure)}
                  {this.renderdata(this.state.plannerData.reminder)}
                  {this.renderdata(this.state.plannerData.task)}
                  {this.renderdata(this.state.plannerData.message)}
                  {this.renderdata(this.state.plannerData.email)}
                  {this.renderdata(this.state.plannerData.deallost)}
                </div>
              </>
            )}
          </div>
        </div>
      </div>
    );
  }

  renderdata(data) {
    return (
      <div class="planner-data-container">{this.renderdataitem(data)}</div>
    );
  }

  renderdataitem(data) {
    return (
      data &&
      data.map((item, index) => {
        let classname =
          "planner-data-item" + (!item.activityStatus ? " green" : " red");
        let history = this.props.history;
        let setSesion = setSession;
        let filterData = this.filterData;
        const MyComponent = React.forwardRef(function MyComponent(props, ref) {
          //  Spread the props to the underlying DOM element.
          return (
            <div
              {...props}
              ref={ref}
              class={classname}
              onClick={() => {
                setSesion("plannerfilterData", filterData);
                if (item.leadStatus === "Deal_Closed") {
                  history.push("/crm/customerinfo/" + item.leadId);
                } else history.push("/crm/leadinfo/" + item.leadId);
              }}
            >
              <div class="planner-data-item-name">{item.name}</div>

              <div class="planner-data-item-mobile">{item.mobileNumber}</div>
            </div>
          );
        });
        return (
          <HtmlTooltip
            title={
              <React.Fragment>
                <Typography color="inherit">{item.assignee}</Typography>
                <Typography color="inherit">
                  {moment(item.activityDateTime, "DD-MM-YYYY hh:mm:ss A").format("hh:mm:ss A")}
                </Typography>
              </React.Fragment>
            }
          >
            <MyComponent></MyComponent>
          </HtmlTooltip>
        );
      })
    );
  }

  renderHeaderItems(text, total) {
    return (
      <div className="planner-header-item">
        <div className="planner-header-item-name">{text}</div>
        <div className="planner-header-item-total">Total: {total}</div>
      </div>
    );
  }

  renderDates() {
    let startDate = moment(this.state.selectedDate, this.format).startOf(
      "isoWeek"
    );
    return (
      <div className="planner-dates">
        {this.renderDate(startDate, "Mon")}
        {this.renderDate(startDate.add(1, "days"), "Tue")}
        {this.renderDate(startDate.add(1, "days"), "Wed")}
        {this.renderDate(startDate.add(1, "days"), "Thu")}
        {this.renderDate(startDate.add(1, "days"), "Fri")}
        {this.renderDate(startDate.add(1, "days"), "Sat")}
        {this.renderDate(startDate.add(1, "days"), "Sun")}
      </div>
    );
  }

  renderDate(date, day) {
    let classname =
      "planner-date-item-container" +
      (moment(this.state.selectedDate, this.format).date() === date.date()
        ? "-selected"
        : "");
    return (
      <div
        id={date.format(this.format)}
        className={classname}
        onClick={(event) => {
          const searchDate = event.currentTarget.id;
          this.setState({ selectedDate: searchDate });
          this.filterData["activityStartDate"] = searchDate;
          this.filterData["activityEndDate"] = searchDate;
          this.search();
        }}
      >
        <div className="planner-date-item">
          <span className="planner-date-day">{day}</span>
          <br />
          <span className="planner-date-number">{date.date()}</span>
        </div>
        <div className="planner-date-pipe">
          <span className="pipe">|</span>
        </div>
        <div className="planner-side-triangle"></div>
      </div>
    );
  }

  rendermonth(month, searchDate) {
    const selectedYear = moment(this.state.selectedDate, this.format).format(
      "YY"
    );
    let className = "planner-month-item " + month + selectedYear;
    className =
      className +
      (moment(this.state.selectedDate, this.format).month() ===
      moment(searchDate, this.format).month()
        ? " selected"
        : "");
    return (
      <div class={className}>
        <Button
          onClick={() => {
            if (
              moment(this.state.selectedDate, this.format).month() !==
              moment(searchDate, this.format).month()
            ) {
              this.setState({
                selectedDate: searchDate,
                selectedWeek: this.calcCurrentWeek(
                  moment(searchDate, this.format)
                ),
                totalWeek: this.calcAllWeek(moment(searchDate)),
              });
              this.filterData["activityStartDate"] = searchDate;
              this.filterData["activityEndDate"] = searchDate;
              this.search();
            }
          }}
          variant="outlined"
        >
          {`${month + selectedYear}`}
        </Button>
      </div>
    );
  }

  calcCurrentWeek(date = moment()) {
    const firstDayOfMonth = date.clone().startOf("month");
    const firstDayOfWeek = firstDayOfMonth.clone().startOf("isoweek");
    const offset = firstDayOfMonth.diff(firstDayOfWeek, "days");
    return Math.ceil((date.date() + offset) / 7);
  }

  calcAllWeek(date) {
    const dateFirst = moment(date).date(1);
    const dateLast = moment(date).date(moment(date).daysInMonth());
    const startWeek = dateFirst.isoWeek();
    const endWeek = dateLast.isoWeek();

    if (endWeek < startWeek) {
      // Yearly overlaps, month is either DEC or JAN
      if (dateFirst.month() === 0) {
        // January
        return endWeek + 1;
      } else {
        // December
        if (dateLast.isoWeekday() === 7) {
          // Sunday is last day of year
          return endWeek - startWeek + 1;
        } else {
          // Sunday is NOT last day of year
          return dateFirst.isoWeeksInYear() - startWeek + 1;
        }
      }
    } else {
      return endWeek - startWeek + 1;
    }
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

export default withRouter(Planner);
