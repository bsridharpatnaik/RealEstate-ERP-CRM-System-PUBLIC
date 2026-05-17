import React, { Component } from "react";
import "./style.scss";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import Checkbox from "@material-ui/core/Checkbox";
import TF from "./../../../../Shared/TextField";
import DateComp from "./../../../../Shared/Date";
import Button from "./../../../../Shared/Button";
import { API } from "./../../../../axios";
import { apiEndpoints } from "./../../../../endpoints";
import { withSnackbar } from "notistack";
import moment from "moment";
import { ReactComponent as UserSvg } from "./user.svg";
import { SvgIcon } from "@material-ui/core";
import ActivityImg from "./telecommuting.svg";
import { constants } from "./../../../../messages";
import HistoryIcon from "@material-ui/icons/History";
function UserIcon(props) {
  return (
    <SvgIcon {...props}>
      <UserSvg />
    </SvgIcon>
  );
}
function FolloUpIcon(props) {
  return (
    <SvgIcon {...props}>
      <HistoryIcon />
    </SvgIcon>
  );
}
class Activity extends Component {
  state = { isPaymentsActivity: false, isDisplayDateTime: false };
  formData = { closingComment: "", isRescheduled: false };
  componentDidMount() {
    if (this.props.data.activityType.toLowerCase() === "payment") {
      this.setState({ reschedule: true, isPaymentsActivity: true });
      this.formData.isRescheduled = true;
    }
  }
  submit() {
    if (this.state.reschedule) {
      this.reschedule();
    } else {
      this.close();
    }
  }
  async revert() {
    const params = {};
    const response = await API.PUT(
      apiEndpoints.revertActivity + this.props.data.leadActivityId,
      params
    );
    if (response.success) {
      this.props.refresh();
      this.props.enqueueSnackbar("Activity Reverted Successfully", {
        variant: "success",
      });
    } else {
      this.props.enqueueSnackbar(response.errorMessage, {
        variant: "error",
      });
    }
  }
  showDateTime() {
    if (
      this.formData.closingComment.length > 0 &&
      this.state.isPaymentsActivity
    ) {
      this.setState({ isDisplayDateTime: true });
    } else if (
      this.state.isPaymentsActivity &&
      this.formData.closingComment.length === 0
    ) {
      this.setState({ isDisplayDateTime: false });
    }
  }
  async close() {
    const params = {
      ...this.formData,
      leadId: this.props.leadId,
      moveToNegotiation: !!this.formData.moveToNegotiation
    };
    const response = await API.PATCH(
      apiEndpoints.closeActivity + this.props.data.leadActivityId,
      params
    );
    if (response.success) {
      this.props.refresh();
      this.props.enqueueSnackbar("Activity closed Successfully", {
        variant: "success",
      });
    } else {
      this.props.enqueueSnackbar(response.errorMessage, {
        variant: "error",
      });
    }
  }
  async reschedule() {
    const rescheduleDateTime =
      this.formData.rescheduleDateTime + " " + this.time;
    const params = {
      ...this.formData,
      rescheduleDateTime: rescheduleDateTime,
      leadId: this.props.leadId,
    };
    const response = await API.PUT(
      apiEndpoints.rescheduleActivity + this.props.data.leadActivityId,
      params
    );
    if (response.success) {
      this.props.refresh();
      this.props.enqueueSnackbar("Activity Rescheduled Successfully", {
        variant: "success",
      });
    } else {
      this.props.enqueueSnackbar(response.errorMessage, {
        variant: "error",
      });
    }
  }
  checkIfDisabled() {
    if (this.state.reschedule) {
      const { closingComment, rescheduleDateTime } = this.formData;
      return !(closingComment.length && rescheduleDateTime && this.time);
    } else {
      return !this.formData.closingComment.length;
    }
  }
  render() {
    const m = moment(
      this.props.data.activityDateTime,
      constants.USDate24Format
    );
    const date = m.locale("en").format("lll");
    return (
      <div className={"activity"}>
        <div>
          <img className="activity-icon" alt="icon" src={ActivityImg} />
        </div>
        <div className="activity-content">
          {this.props.data.isRevertable && (
            <div className="revert-button" onClick={() => this.revert()}>
              Revert
            </div>
          )}
          <div className="date-user-section">
            <span className="date">{date}</span>
            <UserIcon className="user-icon" />
            <span className="user">{`Created By: ${this.props.data.creatorId}`}</span>
            {this.props.data.followUpCount && (
              <>
                <FolloUpIcon className="follow-up-icon" />
                <span className="follow-up">{`Follow up count:  ${this.props.data.followUpCount}`}</span>
              </>
            )}
          </div>
          <div className="content-activitytype">
            <span className="title-heading">
              {this.props.data.activityType} {" : "}
            </span>
            <span className="title">{this.props.data.title}</span>
          </div>
          <div className="activity-content-description">
            {this.props.data.description}
          </div>
          <div className="tags">
            {this.props.data.tags.map((tag) => (
              <span className="tag">{tag}</span>
            ))}
          </div>
          {console.log(this.props.data)}
          {!this.props.data.isOpen ? (
            <div style={{ display: 'flex' }}>
              <div className="closing-comment">
                Comments: {this.props.data.closingComment}
              </div>
              {(this.props.data.activityType === 'Deal_Lost') && this.props.data.dealLostReason && <div className="closing-comment">
                <span>&nbsp;|&nbsp;Deal Lost Reason: {this.props.data.dealLostReason} </span>
              </div>}
            </div>
          ) : (
            <React.Fragment>
              <div className="flex">
                <TF
                  placeholderText="Add comment to this activity"
                  multiline={true}
                  fieldname="description"
                  onChange={(event) => {
                    this.formData.closingComment = event;
                    this.setState({ formValid: event.length });
                    this.showDateTime();
                  }}
                  validation={"maxlength"}
                  lengthConstraint={150}
                />
                <FormControlLabel
                  className="checkbox"
                  control={
                    <Checkbox
                      name="checkedB"
                      color="primary"
                      checked={this.formData.isRescheduled}
                      onChange={(event) => {
                        if (this.state.isPaymentsActivity) {
                          this.props.enqueueSnackbar(
                            'Activity of type "Payment" Can only be re-scheduled.',
                            {
                              variant: "error",
                            }
                          );
                        } else {
                          this.formData.isRescheduled = event.target.checked;
                          if (event.target.checked) {
                            this.setState({
                              reschedule: event.target.checked,
                              isDisplayDateTime: event.target.checked,
                              moveToNegotiation: false
                            });
                          } else {
                            this.setState({
                              reschedule: event.target.checked,
                              isDisplayDateTime: event.target.checked,
                            });
                          }
                        }
                      }}
                    />
                  }
                  label="Rescheduled"
                />
              </div>
              <div
                className={"flex reschedule"}
              >
                {
                  this.state.isDisplayDateTime
                    ?
                    (
                      <div>
                        <DateComp
                          onChange={(e) => {
                            this.formData.rescheduleDateTime = e;
                            this.setState({
                              formValid: true,
                            });
                          }}
                          fieldname={"Date"}
                          emptyDate={true}
                          minDate={moment()}
                          type="date"
                        />
                        <span className="time">
                          <DateComp
                            onChange={(e) => {
                              this.time = e;
                              this.setState({
                                formValid: true,
                              });
                            }}
                            fieldname={"Time"}
                            type="time"
                            emptyDate={true}
                          />
                        </span>
                      </div>
                    )
                    :
                    <div>
                      {
                      this.props.data.showMoveToNegotiation 
                      &&
                      this.formData.closingComment
                      &&
                        <FormControlLabel
                          control={
                            <Checkbox
                              name="moveToNegotiation"
                              color="primary"
                              checked={this.formData.moveToNegotiation}
                              onChange={(event) => {
                                this.formData.moveToNegotiation = event.target.checked;
                                this.setState({
                                  moveToNegotiation: event.target.checked
                                });
                              }}
                            />
                          }
                          label="Move lead to Negotiation"
                        />}
                    </div>
                }
                <Button
                  type="submit"
                  buttonClass="blue"
                  label="SAVE"
                  onClick={() => this.submit()}
                  disabled={this.checkIfDisabled()}
                />
              </div>
            </React.Fragment>
          )}
        </div>
      </div>
    );
  }
}
export default withSnackbar(Activity);
