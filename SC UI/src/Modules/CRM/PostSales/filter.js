import React from "react";
import CommonFilter from "./../../../Shared/Filter";
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

  renderFilter() {
    return (
      <div className="filter-container pipeline-filter-container">
        {this.renderHeader()}
        {this.state.reset ? (
          <div className="filter-content">
            <div class="pipeline-filter-row">
              {this.renderAutoComplete(
                "Customer Name",
                this.props?.typeahead,
                "name",
                (option) => {
                  return option;
                }
              )}
              {this.renderAutoComplete(
                "Phone Number",
                this.props?.typeahead,
                "mobile",
                (option) => {
                  return option;
                }
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
                "Assignee",
                this.assigneeDetails,
                "assignee",
                (option) => {
                  return option["name"];
                }
              )}
            </div>
            <div class="pipeline-filter-row">
            {this.renderAutoComplete(
                "Created By",
                this.assigneeDetails,
                "createdBy",
                (option) => {
                  return option["name"];
                }
              )}
              <div className="filter-item">
                {this.renderFilterDate("Creation Start Date", "createdStartDate")}
              </div>
              <div className="filter-item">  {this.renderFilterDate("Creation End Date", "createdEndDate")}
              </div>
              
            </div>
          </div>
        ) : null}
        {this.renderFooter()}
      </div>
    );
  }
}

export default filter;
