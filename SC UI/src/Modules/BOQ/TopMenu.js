import React, { Component } from "react";
import "./style.scss";
import ScrollMenu from "react-horizontal-scrolling-menu";
import ChevronLeftIcon from "@material-ui/icons/ChevronLeft";
import ChevronRightIcon from "@material-ui/icons/ChevronRight";

const MenuItem = ({ text, selected }) => {
  return <div className={`menu-item ${selected ? "active" : ""}`}>{text}</div>;
};

export const Menu = (list, selected) =>
  list.map((el) => {
    const { name } = el;
    return <MenuItem text={name} key={el.id} selected={selected} />;
  });
class TopMenu extends Component {
  constructor(props) {
    super(props);
    this.menu = null;
    this.menuItems = Menu(this.props.data, this.props.activeItem);
  }
  renderItem(item, index) {
    let className = "menu-item";
    if (index === 0) {
      className += " first";
    }
    if (item.id === this.props.activeItem.id) {
      className += " active";
    }
    return (
      <div
        key={index}
        onClick={() => this.props.onSelect(item)}
        className={className}
      >
        {item.name}
      </div>
    );
  }

  render() {
    const items = this.props.data || [];
    const menu = this.menuItems;

    return (
      <div className="top-menu">
        {/* <div className="menu">
          {items.map((item, index) => this.renderItem(item, index))}
        </div> */}
        {items.length ? (
          <ScrollMenu
            arrowLeft={<ChevronLeftIcon />}
            arrowRight={<ChevronRightIcon />}
            data={menu}
            dragging={false}
            onFirstItemVisible={this.onFirstItemVisible}
            onLastItemVisible={this.onLastItemVisible}
            onSelect={this.props.onSelect}
            onUpdate={this.onUpdate}
            ref={(el) => (this.menu = el)}
            scrollToSelected={true}
            arr
            selected={this.props.activeItem.id}
            menuClass={"menu"}
          />
        ) : null}
      </div>
    );
  }
}

export default TopMenu;
