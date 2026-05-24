//react
import React from "react";
//third party
import { Slide } from "@material-ui/core";
//components
import List from "./list";
import Add from "./add";
import AddLinesToPO from "./addLinesToPO";
import Common from "./../../Shared/CommonIndex";
import Button from "./../../Shared/Button";
//misc
import { messages } from "./../../messages";

class PurchaseOrder extends Common {
  constructor(props) {
    super(props);
    this.addRef = React.createRef();
  }
  state = {
    list: true,
    add: false,
    editData: null,
    draftId: null,
    draftData: null,
    addLinesToPo: false,
    addLinesToPoId: null,
  };
  title = messages.common.purchaseOrder;

  renderPurchaseOrderButtons() {
    if (this.state.add) {
      return null; // Buttons are handled in Add component
    }
    return null;
  }

  render() {
    return (
      <div className="page purchase-order-page">
        {this.state.list && (
          <div className="header-info">
            <div>
              {this.renderHeading()}
              {this.renderBreadcrums(messages.common.inventory)}
            </div>
            {this.renderPurchaseOrderButtons()}
          </div>
        )}
        <Slide
          direction="right"
          in={this.state.list}
          mountOnEnter
          unmountOnExit
          timeout={{ exit: 0 }}
          appear={false}
        >
          <div>
            <List
              isLoading={(bIsLoading) => this.setState({ isLoading: bIsLoading })}
              edit={(data) => {
                this.setState({ add: true, list: false, editData: data, draftId: null, draftData: null });
              }}
              onAdd={() => {
                this.setState({ add: true, list: false, editData: null, draftId: null, draftData: null });
              }}
              onAddFromDraft={(draftId, draftData) => {
                this.setState({ add: true, list: false, editData: null, draftId, draftData });
              }}
              onAddLineToPO={(poId) => {
                this.setState({ addLinesToPo: true, list: false, addLinesToPoId: poId });
              }}
            />
          </div>
        </Slide>
        <Slide
          direction="left"
          in={this.state.add}
          mountOnEnter
          unmountOnExit
          timeout={{ exit: 0 }}
          appear={false}
        >
          <div>
            <Add
              ref={this.addRef}
              editData={this.state.editData}
              draftId={this.state.draftId}
              draftData={this.state.draftData}
              back={() => {
                this.setState({ add: false, list: true, editData: null, draftId: null, draftData: null });
              }}
              dropdowns={{}}
            />
          </div>
        </Slide>
        <Slide
          direction="left"
          in={this.state.addLinesToPo}
          mountOnEnter
          unmountOnExit
          timeout={{ exit: 0 }}
          appear={false}
        >
          <div>
            <AddLinesToPO
              poId={this.state.addLinesToPoId}
              back={() => {
                this.setState({ addLinesToPo: false, list: true, addLinesToPoId: null });
              }}
            />
          </div>
        </Slide>
      </div>
    );
  }
}

export default PurchaseOrder;
