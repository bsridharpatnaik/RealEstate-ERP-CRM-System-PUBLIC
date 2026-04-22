import React from "react";
import moment from "moment";
import { messages } from "./../../../messages";
import { withStyles } from "@material-ui/core/styles";
import Tooltip from "@material-ui/core/Tooltip";
import Typography from "@material-ui/core/Typography";
import {
  setSession,
  getDiffInDays,
  getDiffInDaysv2,
  getFormatedDateTime,
} from "../../../helper";

const HtmlTooltip = withStyles((theme) => ({
  tooltip: {
    backgroundColor: "#f5f5f9",
    color: "rgba(0, 0, 0, 0.87)",
    maxWidth: 220,
    fontSize: theme.typography.pxToRem(12),
    border: "1px solid #dadde9",
  },
}))(Tooltip);

const PipelineItems = ({
  pipelineData = [],
  filterData,
  history,
  isDealClosure = false,
}) => {
  return pipelineData.map((item, index) => {
    const diff = getDiffInDaysv2(
      moment(item.activityDateTime, "DD-MM-YYYY hh:mm:ss")
    );
    const status =
      diff > 0 && item.isOpen
        ? messages.common.pipelineLabel.pastActivity
        : diff === 0 && item.isOpen
        ? messages.common.pipelineLabel.todayActivity
        : diff < 0 && item.isOpen
        ? messages.common.pipelineLabel.upcomingActivity
        : !item.isOpen
        ? messages.common.pipelineLabel.completed
        : "";
    const statusClass =
      "pipeline-item " + status.replace(" ", "").replace("'", "");
    var sentiment = item.sentiment === null ? "" : item.sentiment;
    
    switch(sentiment){
      case "COLD":
        sentiment="RED";
        break;
      case "HOT":
          sentiment="GREEN";
          break;
      case "WARM":
          sentiment="ORANGE";
          break; 
    }
    const sentimentClass = "pipeline-item-right " + sentiment;
    const statusTextClass =
      "pipeline-item-status " +
      status.replace(" ", "").replace("'", "") +
      "Text";
    const stagnateClass =
      "pipeline-item-image " +
      (item.stagnantStatus === "GREEN"
        ? "triangle"
        : item.stagnantStatus === "ORANGE"
        ? "square"
        : item.stagnantStatus === "RED"
        ? "circle"
        : "");

    const MyComponent = React.forwardRef(function MyComponent(props, ref) {
      //  Spread the props to the underlying DOM element.
      return (
        <div
          {...props}
          ref={ref}
          class={statusClass}
          onClick={() => {
            setSession("pipelinefilterData", filterData);
            if (isDealClosure) {
              history.push("/crm/customerinfo/" + item.leadId);
            } else history.push("/crm/leadinfo/" + item.leadId);
          }}
        >
          <div class={sentimentClass}></div>
          <div class="pipeline-item-left">
            <div class="pipeline-item-first">
              <div class="pipeline-item-name">{item.name}</div>
              <div class={stagnateClass}></div>
            </div>
            <div class="pipeline-item-second">
              <div class="pipeline-item-mobile">{item.mobileNumber}</div>
              <div class={statusTextClass}>
                <span>{status} </span>
              </div>
            </div>
          </div>
        </div>
      );
    });
    const date = getFormatedDateTime(item.activityDateTime);
    return (
      <HtmlTooltip
        title={
          <React.Fragment>
            <Typography color="inherit">{item.assignee}</Typography>
            <Typography color="inherit">
              {getFormatedDateTime(item.activityDateTime)}
            </Typography>
          </React.Fragment>
        }
      >
        <MyComponent></MyComponent>
      </HtmlTooltip>
    );
  });
};

export default PipelineItems;
