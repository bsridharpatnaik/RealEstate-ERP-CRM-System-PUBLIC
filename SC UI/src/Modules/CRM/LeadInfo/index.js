import React, { Component } from "react";
import { connect } from "react-redux";

import Stepper from "@material-ui/core/Stepper";
import Step from "@material-ui/core/Step";
import StepLabel from "@material-ui/core/StepLabel";
import CheckCircleIcon from "@material-ui/icons/CheckCircle";
import RadioButtonUncheckedIcon from "@material-ui/icons/RadioButtonUnchecked";
import clsx from "clsx";
import StepConnector from "@material-ui/core/StepConnector";
import "./style.scss";
import { getLeadStatus } from "./../../../actions/leadStatus";
import { apiEndpoints } from "./../../../endpoints";
import { API } from "./../../../axios";
import BasicInfo from "./BasicInfo";
import Note from "./Note";
import Activity from "./Activity";
import Tabs from "@material-ui/core/Tabs";
import Tab from "@material-ui/core/Tab";
import Typography from "@material-ui/core/Typography";
import Box from "@material-ui/core/Box";
import NoteTab from "./NoteTab";
import ActivityTab from "./ActivityTab";
import AttachmentList from "./AttachmentList";
import Breadcrumbs from "@material-ui/core/Breadcrumbs";
import NavigateNextIcon from "@material-ui/icons/NavigateNext";
import { messages } from "./../../../messages";
import IconButton from "@material-ui/core/IconButton";
import KeyboardBackspaceIcon from "@material-ui/icons/KeyboardBackspace";
import AccordianCustom from "./../../../Shared/Accordian";
import { getSession, setSession, clearSession } from "../../../helper";
//images

function TabPanel(props) {
  const { children, value, index, ...other } = props;

  return (
    <Typography
      component="div"
      role="tabpanel"
      hidden={value !== index}
      id={`simple-tabpanel-${index}`}
      aria-labelledby={`simple-tab-${index}`}
      {...other}
    >
      {value === index && <Box p={3}>{children}</Box>}
    </Typography>
  );
}
function StepIcon(props) {
  const { active, completed } = props;

  return (
    <div
      className={clsx("icon", {
        active: active,
      })}
    >
      {completed || active ? (
        <CheckCircleIcon className="active" />
      ) : (
        <RadioButtonUncheckedIcon />
      )}
    </div>
  );
}
class LeadInfo extends Component {
  state = {
    basicLoading: true,
    data: {},
    filter: "all",
    value: 0,
    allLoading: true,
    pastData: [],
    pipelinefilterData: {},
    plannerfilterData: {},
    leadfilterData: {},
    leadfilterPage: 0,
  };
  async componentDidMount() {
    this.leadId = this.props.match.params.id;
    if (!this.props.leadStatus) {
      await this.props.getLeadStatus();
    }
    this.setState({
      leadfilterPage: getSession("leadfilterPage"),
      pipelinefilterData: getSession("pipelinefilterData"),
      plannerfilterData: getSession("plannerfilterData"),
      leadfilterData: getSession("leadfilterdata"),
    });
    this.getLeadBasicInfo();
    this.getAllInfo();
  }
  refresh() {
    //this.getAllInfo();
    this.getLeadBasicInfo();
    this.getAllInfo();
    //window.location.reload(false);
  }
  async getLeadBasicInfo() {
    this.setState({ basicLoading: true });
    const response = await API.GET(
      apiEndpoints.individualLead + this.props.match.params.id
    );

    if (response.success) {
      this.activeStatus =
        this.props.leadStatus &&
        this.props.leadStatus.indexOf(response.data.status);
      this.basicInfo = response.data;
    }
    this.setState({ basicLoading: false });
  }
  async getAllInfo() {
    this.setState({ allLoading: true });
    const response = await API.GET(
      apiEndpoints.getAllLeadInfo + this.props.match.params.id
    );

    if (response.success) {
      const data = response.data;

      this.setState(
        {
          data: data,
          allPast: data.allNotedAndActivities.pastNotesAndActivities,
          editNote: null,
        },
        () => this.filterData(this.state.filter)
      );
    }
    this.setState({ allLoading: false });
    clearSession("pipelinefilterData");
    clearSession("plannerfilterData");
    clearSession("leadfilterdata");
    clearSession("leadfilterPage");
  }
  filterData(filterBy) {
    this.setState({ filter: filterBy });
    let data;
    if (filterBy === "all") {
      data = this.state.allPast;
    } else if (filterBy === "note") {
      data = this.state.data.allNotes.unPinnedNotes;
    } else {
      data = this.state.data.allActivities.pastActivities;
    }
    this.setState({ pastData: data });
  }
  editNote(note) {
    this.setState({ value: 0, editNote: note });
    window.scroll({
      top: 0,
      left: 0,
      behavior: "smooth",
    });
  }
  render() {
    const pinnedNoteslen =
      this.state.data.allNotes && this.state.data.allNotes.pinnedNotes.length;
    // const pendingActLen =
    //   this.state.data.allActivities &&
    //   this.state.data.allActivities.pendingActivities.length;
    const showPinnedNotes =
      this.state.filter === "all" || this.state.filter === "note";
    const showPendingActivities =
      this.state.filter === "all" || this.state.filter === "activity";

    return (
      <div className="lead-info page">
        <div className="left-info">
          {this.state.allLoading ? (
            <div>Loading</div>
          ) : (
            <React.Fragment>
              <div>
                <div className="heading-wrapper">
                  <IconButton
                    aria-label="back"
                    onClick={() => {
                      setSession(
                        "pipelinefilterData",
                        this.state.pipelinefilterData
                      );
                      setSession(
                        "plannerfilterData",
                        this.state.plannerfilterData
                      );
                      setSession("leadfilterdata", this.state.leadfilterData);
                      setSession("leadfilterPage", this.state.leadfilterPage);
                      this.props.history.goBack();
                      //this.props.history.push(appRoutes.lead);
                    }}
                    className="back-icon"
                  >
                    <KeyboardBackspaceIcon />
                  </IconButton>
                  <span className="page-heading">Add Action</span>
                </div>
                <Breadcrumbs
                  separator={<NavigateNextIcon />}
                  aria-label="breadcrumb"
                  className="breadcrumbs"
                >
                  <span>{messages.common.crm}</span>
                  <span>Leads</span>
                  <span>Add Action</span>
                </Breadcrumbs>
              </div>
              <div className="stepper">
                <Stepper
                  nonLinear={this.activeStatus === 5}
                  alternativeLabel
                  activeStep={this.activeStatus}
                  connector={<StepConnector />}
                >
                  {this.props.leadStatus &&
                    this.props.leadStatus.map((label, index) => (
                      <Step
                        key={label}
                        completed={
                          this.activeStatus === 5
                            ? index === 4
                              ? false
                              : true
                            : index <= this.activeStatus
                        }
                      >
                        <StepLabel StepIconComponent={StepIcon}>
                          {label}
                        </StepLabel>
                      </Step>
                    ))}
                </Stepper>
              </div>
              <div className="top-right-info">
                <BasicInfo
                  data={this.basicInfo}
                  isLoading={this.state.basicLoading}
                  updateBasicInfo={() => {this.getLeadBasicInfo();}}
                />
              </div>
              <div className="tabs">
                <Tabs
                  indicatorColor="primary"
                  textColor="primary"
                  onChange={(e, value) => this.setState({ value: value })}
                  value={this.state.value}
                >
                  <Tab label="Add Activity" id="simple-tabpanel-0" />
                  <Tab label="Add Note" id="simple-tabpanel-1" />
                  <Tab label="Add SMS" id="simple-tabpanel-2" disabled />
                  <Tab label="Add Whatsapp" id="simple-tabpanel-3" disabled />
                  <Tab label="Add Email" id="simple-tabpanel-4" disabled />
                </Tabs>
                <TabPanel value={this.state.value} index={0}>
                  <ActivityTab
                    leadId={this.leadId}
                    leadOptions={this.props.leadStatus}
                    isCustomerInfo={false}
                    refresh={() => this.refresh()}
                  />
                </TabPanel>
                <TabPanel value={this.state.value} index={1}>
                  <NoteTab
                    leadId={this.leadId}
                    refresh={() => this.refresh()}
                    editData={this.state.editNote}
                  />
                </TabPanel>
                <TabPanel value={this.state.value} index={2}></TabPanel>
                <TabPanel value={this.state.value} index={3}></TabPanel>
                <TabPanel value={this.state.value} index={4}></TabPanel>
              </div>
              <div>
                <div className="filter-badge">
                  <span
                    className={`badge ${
                      this.state.filter === "all" ? "active" : ""
                    }`}
                    onClick={() => this.filterData("all")}
                  >
                    All
                  </span>
                  <span
                    className={`badge ${
                      this.state.filter === "note" ? "active" : ""
                    }`}
                    onClick={() => this.filterData("note")}
                  >
                    Note
                  </span>
                  <span
                    className={`badge ${
                      this.state.filter === "activity" ? "active" : ""
                    }`}
                    onClick={() => this.filterData("activity")}
                  >
                    Activity
                  </span>
                  <span className="badge disabled">SMS</span>
                  <span className="badge disabled">Whatsapp</span>
                  <span className="badge disabled">Email</span>
                </div>
              </div>
              {showPinnedNotes ? (
                <div>
                  <AccordianCustom
                    title={() => (
                      <h2 className="lead-section-heading">{`Pinned Notes (${this.state.data.allNotes.pinnedNotes.length})`}</h2>
                    )}
                    content={() => {
                      return pinnedNoteslen ? (
                        <div className="pinned-note-wrapper">
                          {this.state.data.allNotes.pinnedNotes.map((note) => (
                            <Note
                              data={note}
                              refresh={() => this.refresh()}
                              edit={() => this.editNote(note)}
                              viewAttachments={() => {
                                this.attachments = note.fileInformations;
                                this.setState({ listopen: true });
                              }}
                            />
                          ))}
                        </div>
                      ) : (
                        ""
                      );
                    }}
                  />
                </div>
              ) : (
                ""
              )}
              {showPendingActivities ? (
                <div>
                  <AccordianCustom
                    title={() => (
                      <h2 className="lead-section-heading">{`Pending Activities (${this.state.data.allActivities.pendingActivities.length})`}</h2>
                    )}
                    content={() =>
                      this.state.data.allActivities.pendingActivities.map(
                        (activity) => (
                          <Activity
                            data={activity}
                            refresh={() => this.refresh()}
                          />
                        )
                      )
                    }
                  />
                </div>
              ) : (
                ""
              )}

              <div>
                <AccordianCustom
                  title={() => (
                    <h2 className="lead-section-heading">{`Past (${this.state.pastData.length})`}</h2>
                  )}
                  content={() => {
                    return (
                      this.state.pastData &&
                      this.state.pastData.map((item) =>
                        item.type === "pastActivity" || item.activityType ? (
                          <Activity
                            data={item.leadActivity ? item.leadActivity : item}
                            refresh={() => this.refresh()}
                          />
                        ) : (
                          <Note
                            data={item.note ? item.note : item}
                            viewAttachments={() => {
                              this.attachments = item.fileInformations
                                ? item.fileInformations
                                : item.note.fileInformations;
                              this.setState({ listopen: true });
                            }}
                            refresh={() => this.refresh()}
                            edit={() => this.editNote(item)}
                          />
                        )
                      )
                    );
                  }}
                />
              </div>
            </React.Fragment>
          )}
        </div>
        <div className="right-info">
          <BasicInfo
            data={this.basicInfo}
            isLoading={this.state.basicLoading}
            updateBasicInfo={() => {this.getLeadBasicInfo();}}
          />
        </div>
        <AttachmentList
          open={this.state.listopen}
          list={this.attachments}
          close={() => this.setState({ listopen: false })}
        />
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    leadStatus: state.leadStatus.leadStatus,
  };
};
export default connect(mapStateToProps, { getLeadStatus }, null, {
  forwardRef: true,
})(LeadInfo);
