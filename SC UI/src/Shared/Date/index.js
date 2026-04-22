import React, { Component } from "react";
import {
  KeyboardDatePicker,
  KeyboardDateTimePicker,
  KeyboardTimePicker,
} from "@material-ui/pickers";
import moment from "moment";
import { IconButton, InputAdornment } from "@material-ui/core";
import { ReactComponent as ClockSVG } from "./clock.svg";
import { constants } from "./../../messages";
class Date extends Component {
  dateFormat = constants.dateFormat;
  datetimeformat = constants.dateTimeFormat;
  timeFormat = "HH:mm";
  state = {
    value: null,
  };
  componentDidMount() {
    let date;
    if (this.props.type === "time") {
      this.format = this.timeFormat;
    } else if (this.props.datetime) {
      this.format = this.datetimeformat;
    } else {
      this.format = this.dateFormat;
    }
    if (this.props.emptyDate) {
      date = this.props.defaultValue || null;
      this.setState({ value: date });
    } else if (this.props.defaultValue) {
      date = this.props.defaultValue;
      this.setState({ value: date });
    } else {
      date = moment().format(this.format);
    }

    this.setState({ oldValidDate: date });
    if (!this.props.disabled) {
      this.props.onChange(date);
    }
  }
  componentDidUpdate(prevProps) {
    if (prevProps.disabled !== this.props.disabled) {
      if (this.props.disabled) {
        this.setState({ value: null });
        this.props.onChange(undefined);
      } else {
        const val = this.state.value || this.state.oldValidDate;
        this.props.onChange(val);
      }
    }
  }
  render() {
    if (this.props.emptyDate && this.props.type === "date") {
      return (
        <KeyboardDatePicker
          autoOk
          disabled={this.props.disabled}
          variant="inline"
          inputVariant="outlined"
          format={this.format}
          InputAdornmentProps={{ position: "start" }}
          emptyLabel={this.props.label}
          label={this.props.label}
          margin="normal"
          minDate={this.props.minDate}
          maxDate={this.props.maxDate}
          value={this.state.value}
          inputValue={this.state.value}
          KeyboardButtonProps={{ className: "date-button" }}
          onChange={(e) => {
            if (e) {
              let date = moment(e._d).format(this.format);
              date = e._isValid ? date : this.state.oldValidDate;
              this.setState({
                oldValidDate: date,
                value: e._isValid ? date : e._i,
              });
              this.props.onChange(date);
            } else if (e === null) {
              let date = null;
              this.setState({
                oldValidDate: date,
                value: date,
              });
              this.props.onChange(date);
            }
          }}
          onBlur={(e) => {
            const value = e.target.value;
            let date = moment(value);
            if (!date._isValid) {
              const date = this.state.oldValidDate;
              this.setState({ value: date });
            }
          }}
        />
      );
    } else if (this.props.datetime) {
      return (
        <KeyboardDateTimePicker
          autoOk
          disabled={this.props.disabled}
          variant="inline"
          inputVariant="outlined"
          InputAdornmentProps={{ position: "start" }}
          emptyLabel={this.props.label}
          label={this.props.label}
          margin="normal"
          inputValue={this.state.value || ""}
          minDate={this.props.minDate}
          maxDate={this.props.maxDate}
          KeyboardButtonProps={{ className: "date-button" }}
          onChange={(e) => {
            if (e) {
              let date = moment(e._d).format(this.datetimeformat);
              date = e._isValid ? date : this.state.oldValidDate;
              this.setState({
                oldValidDate: date,
                value: e._isValid ? date : e._i,
              });
              this.props.onChange(date);
            }
          }}
          onBlur={(e) => {
            const value = e.target.value;
            let date = moment(value);
            if (!date._isValid) {
              const date = this.state.oldValidDate;
              this.setState({ value: date });
            }
          }}
          onError={console.log}
          format={this.datetimeformat}
        />
      );
    } else if (this.props.type === "time") {
      return (
        <KeyboardTimePicker
          autoOk
          disabled={this.props.disabled}
          variant="inline"
          inputVariant="outlined"
          format={this.format}
          InputAdornmentProps={{ position: "start" }}
          emptyLabel={this.props.label}
          label={this.props.label}
          margin="normal"
          minDate={this.props.minDate}
          maxDate={this.props.maxDate}
          value={this.state.value === null ? null : undefined}
          inputValue={this.state.value || ""}
          KeyboardButtonProps={{ className: "date-button" }}
          keyboardIcon={<ClockSVG />}
          invalidDateMessage={this.state.value ? "Invalid Time Format" : ""}
          onChange={(e) => {
            if ((e && this.state.oldValidDate !== null) || (e && e._isValid)) {
              let date = moment(e._d).format(this.format);
              date = e._isValid ? date : this.state.oldValidDate;
              this.setState({
                oldValidDate: date,
                value: e._isValid ? date : e._i,
              });
              this.props.onChange(date);
            }
          }}
          onBlur={(e) => {
            const value = e.target.value;
            let date = moment(value);
            if (!date._isValid) {
              const date = this.state.oldValidDate;
              this.setState({ value: date });
            }
          }}
        />
      );
    } else {
      return (
        <KeyboardDatePicker
          autoOk
          disabled={this.props.disabled}
          variant="inline"
          inputVariant="outlined"
          format={this.format}
          InputAdornmentProps={{ position: "start" }}
          emptyLabel={this.props.label}
          label={this.props.label}
          margin="normal"
          minDate={this.props.minDate}
          maxDate={this.props.maxDate}
          inputValue={this.state.value}
          KeyboardButtonProps={{ className: "date-button" }}
          onChange={(e) => {
            if (e) {
              let date = moment(e._d).format(this.format);
              date = e._isValid ? date : this.state.oldValidDate;
              this.setState({
                oldValidDate: date,
                value: e._isValid ? date : e._i,
              });
              this.props.onChange(date);
            }
          }}
          onBlur={(e) => {
            const value = e.target.value;
            let date = moment(value);
            if (!date._isValid) {
              const date = this.state.oldValidDate;
              this.setState({ value: date });
            }
          }}
        />
      );
    }
  }
}

export default Date;
