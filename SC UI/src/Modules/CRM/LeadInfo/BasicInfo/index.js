import React, { Component } from "react";
import Autocomplete from "@material-ui/lab/Autocomplete";
import TextField from "@material-ui/core/TextField";
import IconButton from "@material-ui/core/IconButton";
import CheckIcon from "@material-ui/icons/Check";
import "./style.scss";
import { stringNotNull } from "./../../../../helper";
import { apiEndpoints } from "./../../../../endpoints";
import { API } from "./../../../../axios";

class BasicInfo extends Component {
  state = { editProspectLead: false, isProspectLead: "" }
  updateProspectLead = async () => {
    this.setState({ basicLoading: true });
    const response = await API.PATCH(
      apiEndpoints.individualLead + this.props?.data?.leadId,
      { prospectLead: `${this.state.isProspectLead}` }
    );
    if (response.success) {
      this.setState({ editProspectLead: false, basicLoading: false });
      this.props.updateBasicInfo();
    }
  }
  render() {
    const { data } = this.props;
    return (
      <div className="basic-info">
        {(this.props.isLoading || this.state.basicLoading) ? (
          <div>Loading</div>
        ) : (
          <div>
            <div className="name">
              <div className="name-text">{data.customerName}</div>
              <div className="number-text">
                <a
                  style={{ textDecoration: "none", color: "#ffffff" }}
                  href={`tel:${data.primaryMobile}`}
                >
                  {data.primaryMobile}
                </a>
              </div>
            </div>
            <div className="info-wrapper">
              <div className="info-label">Lead Source</div>
              <div className="info-value"> {data.source}</div>
            </div>
            <div className="info-wrapper">
              <div className="info-label">Owner</div>
              <div className="info-value"> {data.asigneeId}</div>
            </div>
            <div className="info-wrapper">
              <div className="info-label">Date of Birth</div>
              <div className="info-value"> {data.dateOfBirth}</div>
            </div>
            <div className="info-wrapper">
              <div className="info-label">Email Id</div>
              <div className="info-value"> {data.emailId}</div>
            </div>
            <div className="info-wrapper">
              <div className="info-label">Address</div>
              {
                <div className="info-value">
                  {`${stringNotNull(data.addr_line1)} ${stringNotNull(
                    data.addr_line2
                  )}
            ${stringNotNull(data.city)} ${stringNotNull(data.pincode)}
            `}
                </div>
              }
            </div>
            <div className="info-wrapper">
              <div className="info-label">Secondary Mobile No</div>
              <div className="info-value"> {data.secondaryMobile}</div>
            </div>
            {!this.props.isCustomer && (
              <div className="info-wrapper">
                <div className="info-label">Purpose</div>
                <div className="info-value"> {data.purpose}</div>
              </div>
            )}
            <div className="info-wrapper">
              <div className="info-label">Occupation</div>
              <div className="info-value"> {data.occupation}</div>
            </div>
            {data.source === "broker" && (
              <div className="info-wrapper">
                <div className="info-label">Broker</div>
                <div className="info-value"> {data.broker}</div>
              </div>
            )}
            <div className="info-wrapper">
              <div className="info-label">Property Type</div>
              <div className="info-value"> {data.propertyType}</div>
            </div>
            {!this.props.isCustomer && (
              <div className="info-wrapper">
                <div className="info-label">Sentiment</div>
                <div className="info-value"> {data.sentiment}</div>
              </div>
            )}
            <div className="info-wrapper">
              <div className="info-label">Created By</div>
              <div className="info-value"> {data.creatorId}</div>
            </div>
            <div className="info-wrapper">
              <div className="info-label">Is Prospect Lead</div>
              {
                this.state.editProspectLead
                  ?
                  <div className="flex">
                    <Autocomplete
                      disabled={false}
                      id="tags-standard"
                      options={["Yes", "No"]}
                      onChange={(e, v) => {
                        this.setState({ isProspectLead: (v === "Yes") ? true : false });
                      }}
                      className="flex-grow"
                      disableClearable={true}
                      defaultValue={data.isProspectLead ? "Yes" : "No"}
                      renderInput={(params) => (
                        <TextField
                          {...params}
                          name="isProspectLead"
                          id="isProspectLead"
                          variant="outlined"
                          size="small"
                          required={true}
                          InputLabelProps={{ shrink: true }}
                        />
                      )}
                    />
                    <IconButton
                      aria-label="save"
                      onClick={this.updateProspectLead}
                      className="back-icon"
                    >
                      <CheckIcon className="icon active" />
                    </IconButton>
                  </div>
                  :
                  <div className="info-value" onClick={() => this.setState({ editProspectLead: true })}> {data.isProspectLead ? "Yes" : "No"}</div>
              }
            </div>
            {!this.props.isCustomer && (
              <div className="info-wrapper">
                <div className="info-label">Stagnant Days Count</div>
                <div className="info-value"> {data.stagnantDaysCount}</div>
              </div>
            )}
          </div>
        )}
      </div>
    );
  }
}

export default BasicInfo;
