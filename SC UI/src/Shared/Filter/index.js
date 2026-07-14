import React, { Component } from "react";
import { messages, constants } from "./../../messages";
import Button from "./../../Shared/Button";
import CloseIcon from "@material-ui/icons/Close";
import IconButton from "@material-ui/core/IconButton";
import TextField from "./../../Shared/TextField/";
import Autocomplete from "@material-ui/lab/Autocomplete";
import Select from "./../../Shared/Select";
import { KeyboardDatePicker } from "@material-ui/pickers";
import moment from "moment";
import TF from "@material-ui/core/TextField";
import ClickAwayListener from "@material-ui/core/ClickAwayListener";
import Switch from "@material-ui/core/Switch";
import AutoCompleteWithSearch from "./../AutoCompleteWithSearch";
import { getRole, getUserName, getUserId } from "../../helper";

class CommonFilter extends Component {
  format = constants.dateFormat;
  filterData = {};
  state = { reset: true };
  constructor(props) {
    super(props);
    this.filterData = props.filterData;
  }
  render() {
    return (
      <ClickAwayListener onClickAway={this.props.close}>
        {this.renderFilter()}
      </ClickAwayListener>
    );
  }
  renderHeader() {
    return (
      <div className="filter-heading">
        <span className="filter-title">Filter</span>
        <IconButton
          aria-label="back"
          onClick={this.props.close}
          className="close-icon"
        >
          <CloseIcon />
        </IconButton>
      </div>
    );
  }
  renderTextField(name, fieldname, maxlength, validation) {
    const input = (
      <TextField
        name={fieldname}
        placeholder={this.labelsOutside ? "" : name}
        placeholderText={this.labelsOutside ? name : undefined}
        onChange={(e) => {
          this.filterData[fieldname] = e;
        }}
        defaultValue={this.filterData[fieldname]}
        validation={validation}
        lengthConstraint={maxlength}
      />
    );
    return (
      <div className="filter-item">
        {this.labelsOutside ? (
          <>
            <label className="filter-field-label">{name}</label>
            {input}
          </>
        ) : (
          input
        )}
      </div>
    );
  }
  renderFilterDate(label, name, seDisable = true, disabled = false) {
    const value = this.state[name] || this.filterData[name] || null;
    const picker = (
      <KeyboardDatePicker
        autoOk
        disabled={disabled}
        inputVariant="outlined"
        format={this.format}
        clearable={true}
        InputAdornmentProps={{ position: "start" }}
        InputLabelProps={{ shrink: true }}
        onBlur={(e) => {
          const value = e.target.value;
          let date = moment(value);
          if (!date._isValid) {
            this.setState({ [name]: undefined });
            this.filterData[name] = undefined;
          }
        }}
        onChange={(e) => {
          if (e) {
            if (e._isValid) {
              this.filterData[name] = moment(
                e._d,
                constants.USDateFormat
              ).format(this.format);
            } else {
              this.filterData[name] = undefined;
            }

            if (seDisable) {
              const startdate = this.filterData["startDate"];
              const enddate =
                this.filterData["endDate"] || this.filterData["EndDate"];
              const starttime = startdate && new Date(startdate).getTime();
              const endtime = enddate && new Date(enddate).getTime();

              if (startdate && enddate && starttime > endtime) {
                this.bdisable = true;
                this.disablemsg =
                  "Start Date should not be greater than End Date";
              } else {
                this.bdisable = false;
                this.disablemsg = undefined;
              }
            }
            this.setState({
              [name]: e._isValid
                ? moment(e._d, constants.USDateFormat).format(this.format)
                : undefined,
            });
          } else {
            this.setState({ [name]: undefined });
            this.filterData[name] = undefined;
          }
        }}
        //emptyLabel={messages.fields.date}
        label={this.labelsOutside ? "" : label}
        margin="normal"
        value={value}
        inputValue={value}
      />
    );
    if (this.labelsOutside) {
      return (
        <div className="filter-item">
          <label className="filter-field-label">{label}</label>
          {picker}
        </div>
      );
    }
    return picker;
  }
  renderAutoCompleteWithSearch(
    name,
    url,
    fieldname,
    getoption,
    multiple = true,
    disabled = false
  ) {
    return (
      <div className="filter-item">
        <AutoCompleteWithSearch
          url={url}
          onChange={(event, values) => (this.filterData[fieldname] = values)}
          defaultValue={this.filterData[fieldname]}
          name={name}
        />
      </div>
    );
  }
  renderAutoComplete(
    name,
    options = [],
    fieldname,
    getoption,
    multiple = true,
    disabled = false,
    onChangeCb = null
  ) {
    const optionsArray = Array.isArray(options) ? options : [];
    const rawValue = this.filterData[fieldname];
    const normalizedValue = multiple
      ? (rawValue == null ? [] : Array.isArray(rawValue) ? rawValue : [rawValue])
      : (Array.isArray(rawValue) ? rawValue[0] : rawValue);
    const autocomplete = (
      <Autocomplete
        multiple={multiple}
        id="tags-standard"
        options={optionsArray}
        disabled={disabled}
        onChange={(event, values) => {
          this.filterData[fieldname] = values;
          if (onChangeCb) {
            onChangeCb(values);
          }
          if (this.onFilterChange) {
            this.onFilterChange();
          }
        }}
        defaultValue={normalizedValue}
        getOptionLabel={(option) =>
          option == null ? "" : (typeof option === "object" ? getoption(option) : String(option))
        }
        getOptionSelected={(option, value) => {
          if (option == null || value == null) return false;
          const optVal = typeof option === "object" ? (option.id ?? option.name) : option;
          const valVal = typeof value === "object" ? (value.id ?? value.name) : value;
          return optVal === valVal;
        }}
        filterSelectedOptions={true}
        renderInput={(params) => (
          <TF
            {...params}
            name={name}
            id={fieldname}
            variant="outlined"
            margin="normal"
            label={this.labelsOutside ? "" : name}
            placeholder={this.labelsOutside ? "Select" : undefined}
            InputLabelProps={{ shrink: true }}
          />
        )}
      />
    );
    return (
      <div className="filter-item">
        {this.labelsOutside ? (
          <>
            <label className="filter-field-label">{name}</label>
            {autocomplete}
          </>
        ) : (
          autocomplete
        )}
      </div>
    );
  }
  renderSelect(label, options, fieldname, multiple = true) {
    return (
      <div className="filter-item filter-item-select">
        {label ? (
          <label className="filter-field-label" htmlFor={fieldname}>
            {label}
          </label>
        ) : null}
        <Select
          multiple={multiple}
          name={fieldname}
          label=""
          options={options}
          onChange={(event) => {
            this.filterData[fieldname] = event.target.value;
          }}
          defaultValue={this.filterData[fieldname]}
        />
      </div>
    );
  }

  renderSwitch(label, fieldname, disabled = false) {
    return (
      <div className="filter-item filter-item-switch">
        <label className="filter-field-label" htmlFor={fieldname}>
          {label}
        </label>
        <Switch
          name={fieldname}
          disabled={disabled}
          checked={!!this.filterData[fieldname]}
          onChange={(event) => {
            this.filterData[fieldname] = event.target.checked;
            this.setState({});
          }}
          color="primary"
          inputProps={{ "aria-label": label }}
        />
      </div>
    );
  }
  renderFooter(isAssigneeDefault = false, buttonLabel = messages.common.apply, isDisabled = false, onReset = () => {}) {
    return (
      <div className="filter-bottom">
        <Button
          onClick={() => {
            this.filterData = {};
            if (isAssigneeDefault) {
              const currentUserRole = getRole();
              const currentUserId = getUserId();
              const currentUserName = getUserName();
              if (
                !(currentUserRole.toLowerCase().indexOf("admin") > -1) &&
                !(currentUserRole.toLowerCase().indexOf("crm-manager") > -1)
              ) {
                this.filterData = {
                  ...this.filterData,
                  assignee: [
                    { name: currentUserName, value: Number(currentUserId) },
                  ],
                };
              }
            }
            this.setState({ reset: false }, () =>
              this.setState({ reset: true })
            );
            onReset();
          }}
          buttonClass="grey"
          label={messages.common.reset}
        />
        <div title={this.disablemsg}>
          <Button
            onClick={() => {
              this.props.search(this.filterData);
              this.props.close();
            }}
            disabled={this.bdisable || isDisabled}
            buttonClass="blue"
            label={buttonLabel}
          />
        </div>
      </div>
    );
  }
}

export default CommonFilter;
