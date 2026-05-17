import React, { Component } from "react";
import MenuItem from "@material-ui/core/MenuItem";
import SelectBox from "@material-ui/core/Select";
class Select extends Component {
  constructor(props) {
    super(props);
    this.valueKey = this.props.valueKey || "value";
    this.labelKey = this.props.labelKey || "label";
    
    let defaultValue = this.props.defaultValue
      ? this.props.options.filter(
          (val) => val[this.valueKey] === this.props.defaultValue
        )
      : null;
    this.multiDefault = this.props.defaultValue ? this.props.defaultValue : [];
    this.defaultValue = this.props.defaultValue;
  }
  render() {
    return (
      <SelectBox
        className="custom-select"
        label={this.props.label}
        name={this.props.name}
        variant="outlined"
        fullWidth
        defaultValue={
          this.props.multiple ? this.multiDefault : this.defaultValue
        }
        onChange={this.props.onChange}
        multiple={this.props.multiple ? true : false}
        disabled={this.props.disabled}
        MenuProps={{ disablePortal: true }}
      >
        {this.props.options.map((option, index) => (
          <MenuItem
            value={typeof option === "string" ? option : option[this.valueKey]}
            key={index}
          >
            {typeof option === "string" ? option : option[this.labelKey]}
          </MenuItem>
        ))}
      </SelectBox>
    );
  }
}

export default Select;
