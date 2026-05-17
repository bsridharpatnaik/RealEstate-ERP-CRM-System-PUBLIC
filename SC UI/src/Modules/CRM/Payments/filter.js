import React from "react";
import CommonFilter from "./../../../Shared/Filter";
class filter extends CommonFilter {
    assigneeDetails = this.props?.options?.assigneeDetails?.assigneeDetails?.map(
        (item) => {
          return {
            name: item.userName,
            value: item.userid,
          };
        }
      );

      paymentFrom = [
        {
          "name": 'Bank',
          "value": 'false',
        },
        {
          "name": 'Customer',
          "value": 'true',
        }
      ]

      paymentStatus = [
        {
            "name": "Yes",
            "value": "true"
           },
           {
            "name": "No",
            "value": "false"
           },
      ];

      renderFilter() {
        return (
          <div className="filter-container pipeline-filter-container">
            {this.renderHeader()}
            {this.state.reset ? (
              <div className="filter-content">
                  <div className="pipeline-filter-row">
                            <div className="filter-item">
                            {this.renderFilterDate("Start Date", "startDate")}
                            </div>
                            <div className="filter-item">
                            {this.renderFilterDate("End Date", "endDate")}
                            </div>
                                {this.renderAutoComplete(
                                "Payment Received",
                                this.paymentStatus,
                                "isReceived",
                                (option) => {
                                    return option["name"];
                                },
                                false
                            )}

                      </div>
                      <div className="pipeline-filter-row">
                            {this.renderAutoComplete(
                                "Property Type",
                                this.props?.options?.validPropertyTypes,
                                "propertyType",
                                (option) => {
                                    return option;
                                }
                            )}
                            {this.renderAutoComplete(
                                "Property",
                                this.props?.options?.validPropertyNames,
                                "propertyName",
                                (option) => {
                                    return option;
                                }
                            )}
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
                          </div>
                          <div className="pipeline-filter-row">
                              {this.renderAutoComplete(
                                    "Payment From",
                                    this.paymentFrom,
                                    "isCustomerPayment",
                                    (option) => {
                                      return option["name"];
                                    },
                                    false,
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