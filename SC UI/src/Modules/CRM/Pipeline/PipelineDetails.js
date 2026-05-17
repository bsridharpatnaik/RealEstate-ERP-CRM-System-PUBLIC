//react
import React from "react";
import { messages } from "../../../messages";
import PipelineItems from "./PipelineItems";

const PipelineDetails = ({ pipelineData, filterData, history, showDealClosure }) => {
  return (
    <div class="pipeline-details">
      {!showDealClosure && <div class="pipeline-itemlist">
        <div class="pipeline-types-menu">
          <div className="status-item">
            <div>{messages.common.pipelineLabel.leadGeneration}</div>
          </div>
        </div>
        <PipelineItems
          pipelineData={pipelineData.leadgeneration}
          filterData={filterData}
          history={history}
        />
      </div>}

      <div class="pipeline-itemlist">
        <div class="pipeline-types-menu">
          <div className="status-item">
            <div>{messages.common.pipelineLabel.visitScheduled}</div>
          </div>
        </div>
        <PipelineItems
          pipelineData={pipelineData.propertyvisitScheduled}
          filterData={filterData}
          history={history}
        />
      </div>
      <div class="pipeline-itemlist">
        <div class="pipeline-types-menu">
          <div className="status-item">
            <div>{messages.common.pipelineLabel.visitCompleted}</div>
          </div>
        </div>
        <PipelineItems
          pipelineData={pipelineData.propertyvisitCompleted}
          filterData={filterData}
          history={history}
        />
      </div>
      <div class="pipeline-itemlist">
        <div class="pipeline-types-menu">
          <div className="status-item">
            <div>{messages.common.pipelineLabel.negotiation}</div>
          </div>
        </div>
        <PipelineItems
          pipelineData={pipelineData.negotiation}
          filterData={filterData}
          history={history}
        />
      </div>
      {showDealClosure && <div class="pipeline-itemlist">
        <div class="pipeline-types-menu">
          <div className="status-item">
            <div>{messages.common.pipelineLabel.dealClosure}</div>
          </div>
        </div>
        <PipelineItems
          pipelineData={pipelineData.dealclosure}
          filterData={filterData}
          history={history}
          isDealClosure={true}
        />
      </div>}
    </div>
  );
};

export default PipelineDetails;
