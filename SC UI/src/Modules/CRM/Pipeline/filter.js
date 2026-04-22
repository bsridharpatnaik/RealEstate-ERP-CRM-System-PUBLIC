import React from "react";
import CommonFilter from "./../../../Shared/Filter";
class filter extends CommonFilter {
    sourceDetails = this.props?.options?.sourceDetails?.map((item) => {
        return {
            "name": item.name,
            "value": item.id
        }
    })

    brokerDetails = this.props?.options?.brokerDetails?.map((item) => {
        return {
            "name": item.name,
            "value": item.id
        }
    })

    assigneeDetails = this.props?.options?.assigneeDetails?.assigneeDetails?.map((item) => {
        return {
            "name": item.userName,
            "value": item.userid
        }
    })


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

    recentActivityStatus = [
        {
            name: "Today's Activity",
            value: "today",
        },
        {
            name: "Past Activity",
            value: "past",
        },
        {
            name: "Upcoming Activity",
            value: "upcoming",
        },
        {
            name: "Completed Activity",
            value: "completed",
        },
    ];

    activityStatus = [
        {
            "name": "Closed",
            "value": "false"
        },
        {
            "name": "Open",
            "value": "true"
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
                            {this.renderTextField(
                                "Purpose",
                                "purpose",
                                20,
                                "maxlength"
                            )}
                        </div>
                        <div class="pipeline-filter-row">
                            {this.renderTextField(
                                "Address",
                                "address",
                                20,
                                "maxlength"
                            )}
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
                            {this.renderAutoComplete(
                                "Lead Status",
                                this.props?.options?.validStatusType,
                                "leadStatus",
                                (option) => {
                                    return option;
                                },
                                false,
                                true

                            )}
                        </div>
                        {!this.props?.isPipeline &&
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
                                    {this.renderDate("Activity Start Date", "activityStartDate")}
                                    {this.renderDate("Activity End Date", "activityEndDate")}
                                </div>
                            </div>
                        }
                        <div class="pipeline-filter-row">
                            {this.renderAutoComplete(
                                "Stagnant Status",
                                this.stagnantDetails,
                                "stagnantStatus",
                                (option) => {
                                    return option["name"];
                                },
                            )}
                            {this.renderAutoComplete(
                                "Prospect Lead",
                                this.prospectLead,
                                "prospectLead",
                                (option) => {
                                    return option["name"];
                                },
                                false
                            )}
                            {this.renderAutoComplete(
                                "Recent Activity",
                                this.recentActivityStatus,
                                "recentActivityStatus",
                                (option) => {
                                    return option["name"];
                                },
                            )}
            
                        </div>

                    </div>
                ) : null}
                {this.renderFooter(true)}
            </div>
        );
    }
}

export default filter;
