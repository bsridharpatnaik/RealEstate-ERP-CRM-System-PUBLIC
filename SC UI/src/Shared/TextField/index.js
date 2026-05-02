import React, { Component } from "react";
import TF from "@material-ui/core/TextField";

class TextField extends Component {
  state = { value: undefined, error: false };
  _defaultValueSyncTimeout = null;
  componentDidMount() {
    console.log(`[TextField componentDidMount] ${this.props.name}:`, {
      defaultValue: this.props.defaultValue,
      value: this.props.value,
      key: this.props.key || 'no-key'
    });
    
    if (this.props.compRef) {
      this.props.compRef(this);
    }
    if (this.props.defaultValue !== undefined) {
      console.log(`[TextField componentDidMount] ${this.props.name} - Setting state.value to:`, this.props.defaultValue);
      this.setState({ value: this.props.defaultValue }, () => {
        console.log(`[TextField componentDidMount] ${this.props.name} - State set, current state.value:`, this.state.value);
      });
    } else {
      console.log(`[TextField componentDidMount] ${this.props.name} - defaultValue is undefined, not setting state`);
    }
  }
  componentDidUpdate(prevProps) {
    const nextDefault = this.props.defaultValue;
    const prevDefault = prevProps.defaultValue;
    if (prevDefault === nextDefault) return;
    if (nextDefault === undefined) return;
    // Defer setState to next tick to avoid "Maximum update depth exceeded" (no setState during commit).
    if (this._defaultValueSyncTimeout) clearTimeout(this._defaultValueSyncTimeout);
    this._defaultValueSyncTimeout = setTimeout(() => {
      this._defaultValueSyncTimeout = null;
      if (this.state.value != nextDefault) {
        this.setState({ value: nextDefault });
      }
    }, 0);
  }
  componentWillUnmount() {
    if (this._defaultValueSyncTimeout) clearTimeout(this._defaultValueSyncTimeout);
  }
  onChange(e, val) {
    const value = val !== undefined ? val : e.target.value;

    if (this.props.validation) {
      if (this.props.validation === "length") {
        if (value.length && value.length !== this.props.lengthConstraint) {
          if (this.state.error !== true) {
            this.props.inValidateForm(true);
            this.setState({ error: true });
          }
        } else {
          this.props.inValidateForm(false);
          this.setState({ error: false });
        }
      }
      if (this.props.validation === "maxlength") {
        if (value.length && value.length > this.props.lengthConstraint) {
          this.setState({ error: true });
          return false;
        }
        this.setState({ error: false });
      }

      if (this.props.validation === "nonegative") {
        if (Number(value) < 0) {
          this.setState({ value: "" });
          return;
        }
      }
      if (this.props.validation === "positive") {
        if (Number(value) <= 0) {
          this.setState({ value: "" });
          return;
        }
      }
      if (this.props.validation === "regex" && this.props.regexConstraint) {
        if (value.length && !this.props.regexConstraint.test(value)) {
          if (this.state.error !== true) {
            this.props.inValidateForm(true);
            this.setState({ error: true });
          }
        } else {
          this.props.inValidateForm(false);
          this.setState({ error: false });
        }
      }
    }
    this.setState({ value });
    if (this.props.onChange) {
      this.props.onChange(value);
    }
  }
  keyPress(event) {
    var key = event.key ? event.key : "";
    if (
      this.props.validation === "nonegative" ||
      this.props.validation === "positive"
    ) {
      if (key === "e" || key === "-") {
        event.preventDefault();
        //event.stopPropagation();
        return false;
      }
    }
  }
  setValue(value) {
    this.onChange(null, value);
  }
  render() {
    const props = this.props;
    // If value prop is provided, use controlled mode
    // Otherwise, use uncontrolled mode with defaultValue
    const textFieldProps = {};
    if (props.value !== undefined) {
      textFieldProps.value = props.value;
    } else {
      // Parent passes defaultValue; show current value so updates (e.g. after async) are visible.
      // Using only defaultValue would not update the DOM after mount, so Measurement Unit / Closing Stock would stay empty.
      const displayValue = this.state.value !== undefined
        ? this.state.value
        : (props.defaultValue !== undefined ? props.defaultValue : '');
      textFieldProps.value = displayValue;
    }
    
    return (
      <TF
        className={props.className}
        variant={props.variant || "outlined"}
        margin="normal"
        required={props.required}
        fullWidth
        name={props.fieldname}
        InputProps={this.props.InputProps}
        InputLabelProps={{ shrink: true }}
        type={props.type}
        disabled={props.disabled}
        label={props.placeholder}
        onChange={(e) => this.onChange(e)}
        {...textFieldProps}
        multiline={this.props.multiline}
        onKeyPress={this.props.validation ? (e) => this.keyPress(e) : null}
        onWheel={props.type === "number" ? (e) => e.target.blur() : undefined}
        helperText={this.props.helperText !== undefined ? this.props.helperText : (this.state.error && this.props.errorMessage)}
        error={this.props.error !== undefined ? this.props.error : this.state.error}
        placeholder={this.props.placeholderText}
      />
    );
  }
}

export default TextField;
