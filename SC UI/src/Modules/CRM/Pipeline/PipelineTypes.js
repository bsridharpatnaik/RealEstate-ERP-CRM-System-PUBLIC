import React, { Component } from "react";
import { messages } from "../../../messages";

class PipelineTypes extends Component {
  render() {
    const { showDealClosure } = this.props;
    return (
      <div class="pipeline-types">
        {!showDealClosure && <div className="status-item">
          <div>
            {messages.common.pipelineLabel.leadGeneration}
          </div>
        </div>}
        <div className="status-item">
          <div>
            {messages.common.pipelineLabel.visitScheduled}
          </div>
        </div>
        <div className="status-item">
          <div>
            {messages.common.pipelineLabel.visitCompleted}
          </div>
        </div>
        <div className="status-item">
          <div>
            {messages.common.pipelineLabel.negotiation}
          </div>
        </div>
        {showDealClosure && <div className="status-item">
          <div>
            {messages.common.pipelineLabel.dealClosure}
          </div>
        </div>}
      </div>
    );
  }
}

export default PipelineTypes