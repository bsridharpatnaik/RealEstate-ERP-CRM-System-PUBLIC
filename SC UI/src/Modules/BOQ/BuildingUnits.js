import React, { Component } from "react";
import "./style.scss";
import Skeleton from "@material-ui/lab/Skeleton";

class BuildingUnits extends Component {
  renderItem(item, index) {
    const isActive = this.props.selected
      ? item.id === this.props.selected.id
      : false;
    return (
      <div
        key={index}
        className={isActive ? "unit-item active" : "unit-item"}
        onClick={() => this.props.onSelect(item)}
      >
        {item.name}
      </div>
    );
  }
  render() {
    const items = this.props.data || [];
    return (
      <div className="boq-units">
        <div className="unit-name">Flat No</div>
        <div className="units-wrapper">
          {this.props.isLoading ? (
            <Skeleton variant="rect" height={50} />
          ) : (
            <>
              {items.length ? (
                items.map((item, index) => this.renderItem(item, index))
              ) : (
                <div> No flats found</div>
              )}
            </>
          )}
        </div>
      </div>
    );
  }
}

export default BuildingUnits;
