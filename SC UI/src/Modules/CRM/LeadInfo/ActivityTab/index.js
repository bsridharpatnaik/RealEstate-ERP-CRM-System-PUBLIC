import React, { Component } from "react";
import { connect } from "react-redux";
import TF from "./../../../../Shared/TextField";
import TextField from "@material-ui/core/TextField";
import Select from "./../../../../Shared/Select";
import Date from "./../../../../Shared/Date/";
import Autocomplete from "@material-ui/lab/Autocomplete";
import Chip from "@material-ui/core/Chip";
import Button from "./../../../../Shared/Button";
import { API } from "./../../../../axios";
import { apiEndpoints } from "./../../../../endpoints";
import { withSnackbar } from "notistack";
import "./style.scss";
import { getDealLostReasons } from "./../../../../actions/dealLostReasons";

class ActivityTab extends Component {
  data = {
    title: "",
    activityDateTime: "",
    activityType: "",
    tags: [], // Ensure tags is initialized here
  };

  state = {
    formValid: false,
    options: [],
    tags: [], // Also initialize tags in the state for better control
  };

  async componentDidMount() {
    if (!this.props.dealLostReasons) {
      await this.props.getDealLostReasons();
    }
    const response = await API.GET(
      apiEndpoints.getAllowedActivityStatus + this.props.leadId
    );
    if (response.success) {
      this.setState({ options: response.data });
    }
  }

  submit = async () => {
    const activityDateTime =
      this.data.activityDateTime + " " + this.activityTime;
    const params = {
      ...this.data,
      activityDateTime: activityDateTime,
      leadId: this.props.leadId,
    };

    const response = await API.POST(apiEndpoints.createActivity, params);
    if (response.success) {
      if (
        (params.activityType.toLowerCase() === "meeting" ||
          params.activityType.toLowerCase() === "deal_cancelled") &&
        this.props.isCustomerInfo
      ) {
        this.props.refresh(true);
      } else {
        this.props.refresh();
      }
      this.props.enqueueSnackbar("Activity added Successfully", {
        variant: "success",
      });
    } else {
      this.props.enqueueSnackbar(response.errorMessage, {
        variant: "error",
      });
    }
  };

  formValid = () => {
    let formValid =
      this.data.title.length &&
      this.data.activityDateTime.length &&
      this.data.activityType.length;
    if (formValid && this.data.activityType === "Deal_Lost") {
      formValid = formValid && this.data.dealLostReason;
    }
    this.setState({ formValid });
  };

  handleTagsChange = (event, values) => {
    const tags = values.map(tag => tag.trim());
    this.data.tags = tags; // Update the data object
    this.setState({ tags }); // Update the state
    this.formValid();
  };

  render() {
    return (
      <div className="activity-tab">
        <div className="flex width30">
          <div className="MuiFormControl-root MuiTextField-root MuiFormControl-marginNormal">
            <label className="MuiFormLabel-root MuiInputLabel-root MuiInputLabel-formControl MuiInputLabel-animated MuiInputLabel-shrink MuiInputLabel-outlined MuiFormLabel-filled">
              Activity Type
            </label>
            <Select
              label="Activity Type"
              name={"activityType"}
              options={this.state.options || []}
              onChange={(event) => {
                this.data.activityType = event.target.value;
                this.formValid();
                this.showReasonsDropdown =
                  this.data.activityType === "Deal_Lost";
              }}
            />
          </div>
          <Date
            onChange={(e) => {
              this.data.activityDateTime = e;
              this.formValid();
            }}
            label="Date"
            fieldname={"Date"}
            dontSetDefault={true}
          />
          <Date
            onChange={(e) => {
              this.activityTime = e;
              this.formValid();
            }}
            label="Time"
            defaultValue="11:00"
            fieldname={"Time"}
            type={"time"}
            dontSetDefault={true}
          />
        </div>
        <div className="flex">
          <div className="left">
            <TF
              placeholder="Title"
              fieldname="title"
              onChange={(event) => {
                this.data.title = event;
                this.formValid();
              }}
              validation={"maxlength"}
              lengthConstraint={25}
            />
            {this.showReasonsDropdown ? (
              <div className="width100 MuiFormControl-root MuiTextField-root MuiFormControl-marginNormal">
                <label className="MuiFormLabel-root MuiInputLabel-root MuiInputLabel-formControl MuiInputLabel-animated MuiInputLabel-shrink MuiInputLabel-outlined MuiFormLabel-filled">
                  Deal Lost Reason
                </label>
                <Select
                  label="Deal Lost Reason"
                  name={"dealLostReason"}
                  options={this.props.dealLostReasons || []}
                  onChange={(event) => {
                    this.data.dealLostReason = event.target.value;
                    this.formValid();
                  }}
                />
              </div>
            ) : (
              <Autocomplete
                multiple
                id="tags-filled"
                options={[]}
                freeSolo
                variant="outlined"
                name={"tags"}
                limitTags={3}
                onChange={this.handleTagsChange}
                renderTags={(value, getTagProps) =>
                  value.map((option, index) => (
                    <Chip
                      variant="outlined"
                      label={option}
                      {...getTagProps({ index })}
                    />
                  ))
                }
                renderInput={(params) => (
                  <TextField
                    variant="outlined"
                    fullWidth
                    {...params}
                    inputProps={{ ...params.inputProps, maxLength: 10 }}
                    label="Tags"
                    placeholder="Tags"
                    InputLabelProps={{ shrink: true }}
                    validation={"maxlength"}
                    lengthConstraint={10}
                  />
                )}
              />
            )}
          </div>
          <div className="right">
            <TF
              placeholder="Description"
              multiline={true}
              fieldname="description"
              onChange={(event) => {
                this.data.description = event;
              }}
              validation={"maxlength"}
              lengthConstraint={150}
            />
          </div>
        </div>
        <div className="button-section">
          <Button
            type="submit"
            buttonClass="blue"
            label="SAVE"
            onClick={() => this.submit()}
            disabled={!this.state.formValid}
          />
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state) => {
  return {
    dealLostReasons: state.dealLostReasons.dealLostReasons,
  };
};

export default connect(mapStateToProps, { getDealLostReasons }, null, {
  forwardRef: true,
})(withSnackbar(ActivityTab));
