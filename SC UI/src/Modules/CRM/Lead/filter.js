import React from "react";
import CommonFilter from "./../../../Shared/Filter";
import { getSession } from "./../../../helper";
class filter extends CommonFilter {
  sourceDetails = this.props?.options?.sourceDetails?.map((item) => {
    return {
      name: item.name,
      value: item.id,
    };
  });

  brokerDetails = this.props?.options?.brokerDetails?.map((item) => {
    return {
      name: item.name,
      value: item.id,
    };
  });

  assigneeDetails = this.props?.options?.assigneeDetails?.assigneeDetails?.map(
    (item) => {
      return {
        name: item.userName,
        value: item.userid,
      };
    }
  );

  stagnantDetails = [
    {
      name: "Green (10 - 20 Days)",
      value: "Green",
    },
    {
      name: "No Colour (0 - 10 Days)",
      value: "NoColour",
    },
    {
      name: "Orange (20 - 30 Days)",
      value: "Orange",
    },
    {
      name: "Red (> 30 Days)",
      value: "Red",
    },
  ];

  latestActivity = [
    {
      name: "Latest Activity Only",
      value: "true",
    },
  ];
  activityStatus = [
    {
      name: "Closed",
      value: "false",
    },
    {
      name: "Open",
      value: "true",
    },
  ];
  rescheduledActivity = [
    {
      name: "No",
      value: "false",
    },
    {
      name: "Yes",
      value: "true",
    },
  ];
  prospectLead = [
    {
      name: "Yes",
      value: "true",
    },
    {
      name: "No",
      value: "false",
    }
  ];
  dealLostReasons = getSession("dealLostReasons");
  componentDidMount() {
    this.onFilterChange();
  }
  onFilterChange() {
    const showDealLost = this.filterData.leadStatus
      ? this.filterData?.leadStatus.includes("Deal_Lost")
      : false;
    if (!showDealLost) {
      this.filterData.dealLostReason = undefined;
    }
    if (this.state.showDealLost !== showDealLost) {
      this.setState({ showDealLost });
    }
  }
  renderFilter() {
    return (
      <div className="filter-container pipeline-filter-container">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">
            <div class="pipeline-filter-row">
              {this.renderAutoComplete(
                "Name",
                this.props?.typeahead,
                "name",
                (option) => {
                  return option;
                }
              )}
              {this.renderAutoComplete(
                "Primary Mobile",
                this.props?.typeahead,
                "mobile",
                (option) => {
                  return option;
                }
              )}
              {this.renderTextField("Purpose", "purpose", 20, "maxlength")}
            </div>
            <div class="pipeline-filter-row">
              {this.renderTextField("Address", "address", 20, "maxlength")}
              {this.renderTextField(
                "Occupation",
                "occupation",
                20,
                "maxlength"
              )}
              {this.renderAutoComplete(
                "Source",
                this.sourceDetails,
                "source",
                (option) => {
                  return option["name"];
                }
              )}
            </div>
            <div class="pipeline-filter-row">
              {this.renderAutoComplete(
                "Broker",
                this.brokerDetails,
                "broker",
                (option) => {
                  return option["name"];
                }
              )}
              {this.renderAutoComplete(
                "Property Type",
                this.props?.options?.validPropertyType,
                "propertyType",
                (option) => {
                  return option;
                }
              )}
              {this.renderAutoComplete(
                "Sentiment",
                this.props?.options?.validSentiments,
                "sentiment",
                (option) => {
                  return option;
                }
              )}
            </div>
            <div class="pipeline-filter-row">
              {this.renderAutoComplete(
                "Assignee",
                this.assigneeDetails,
                "assignee",
                (option) => {
                  return option["name"];
                },
                true,
                this.props.disableAssignee
              )}
              <div className="filter-dates">
                {this.renderFilterDate("Lead Start Date", "createdStartDate")}
                {this.renderFilterDate("Lead End Date", "createdEndDate")}
              </div>
              {this.props.showLoanStatus
                ? this.renderAutoComplete(
                  "Loan Status",
                  [
                    "DocumentCollection",
                    "ApplicationSubmitted",
                    "ApplicationProcessing",
                    "Approved",
                    "Disbursed",
                  ],
                  "loanStatus",
                  (option) => {
                    return option;
                  },
                  true
                )
                : this.renderAutoComplete(
                  "Lead Status",
                  this.props?.options?.validStatusType,
                  "leadStatus",
                  (option) => {
                    return option;
                  },
                  true
                )}
            </div>
            {!this.props?.isPipeline && (
              <div class="pipeline-filter-row">
                {this.renderAutoComplete(
                  "Activity Type",
                  this.props?.options?.validActivityType,
                  "activityType",
                  (option) => {
                    return option;
                  },
                  false
                )}
                {this.renderAutoComplete(
                  "Activity Status",
                  this.activityStatus,
                  "activityStatus",
                  (option) => {
                    return option["name"];
                  },
                  false
                )}
                <div className="filter-dates">
                  {this.renderFilterDate(
                    "Activity Start Date",
                    "activityStartDate"
                  )}
                  {this.renderFilterDate(
                    "Activity End Date",
                    "activityEndDate"
                  )}
                  
                </div>
              </div>
            )}



            <div class="pipeline-filter-row">
              {this.props.showLoanStatus
                ? this.renderAutoComplete(
                  "Customer Status",
                  [
                    "Booked",
                    "DocumentCollection",
                    "Agreement",
                    "Finance",
                    "Registry",
                    "Handover"
                  ],
                  "customerStatus",
                  (option) => {
                    return option;
                  },
                  true
                )
                : this.renderAutoComplete(
                  "Stagnant Status",
                  this.stagnantDetails,
                  "stagnantStatus",
                  (option) => {
                    return option["name"];
                  }
                )}
              {this.renderAutoComplete(
                "Latest Activity",
                this.latestActivity,
                "showOnlyLatest",
                (option) => {
                  return option["name"];
                },
                false
              )}
              {this.renderAutoComplete(
                "Show Rescheduled Activities",
                this.rescheduledActivity,
                "showRescheduled",
                (option) => {
                  return option["name"];
                },
                false
              )}
            </div>
            <div class="pipeline-filter-row">
              {this.renderAutoComplete(
                "Prospect Lead",
                this.prospectLead,
                "prospectLead",
                (option) => {
                  return option["name"];
                },
                false
              )}
              {this.state.showDealLost &&
                this.renderAutoComplete(
                  "Deal Lost Reason",
                  [...this.dealLostReasons],
                  "dealLostReason",
                  (option) => {
                    return option;
                  }
                )}
                {this.renderTextField("Text/#tag Search", "textSearch", 20, "maxlength")}
            </div>
          </div>
        ) : null}
        {this.renderFooter(true)}
      </div>
    );
  }
}

export default filter;
