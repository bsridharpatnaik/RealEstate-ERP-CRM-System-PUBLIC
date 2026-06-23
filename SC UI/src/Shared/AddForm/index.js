import React, { Component } from "react";
import TextField from "@material-ui/core/TextField";
import IconButton from "@material-ui/core/IconButton";
import KeyboardBackspaceIcon from "@material-ui/icons/KeyboardBackspace";
import { messages, constants } from "./../../messages";
import Button from "./../../Shared/Button";
import { API } from "./../../axios";
import Date from "./../../Shared/Date/";
import Autocomplete from "@material-ui/lab/Autocomplete";
import TF from "./../TextField";
import Radio from "./../Radio";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import Switch from "@material-ui/core/Switch";
import FileList from "./../../Shared/FileList";
import Fab from "@material-ui/core/Fab";
import AddIcon from "@material-ui/icons/Add";

class AddForm extends Component {
  formData = {};
  fieldRefs = {};
  formValidation = {};
  state = { isAdding: false };
  dateFormat = constants.dateFormat;
  async add(event) {
    this.setState({ isAdding: true });
    const params = { ...this.formData };
    event.preventDefault();
    const response = await API.POST(this.addurl, params);
    this.showToaster(response);
    this.setState({ isAdding: false });
  }
  showToaster(response) {
    if (response.success) {
      this.props.enqueueSnackbar(this.title + messages.common.addSuccess, {
        variant: "success",
      });
      this.props.back();
    } else {
      this.props.enqueueSnackbar(response.errorMessage, {
        variant: "error",
      });
    }
  }
  renderProductAddButton() {
    return (
      <Fab
        className="add-product"
        color="primary"
        aria-label="add"
        disabled={Object.keys(this.state.noproduct).length === 50}
        onClick={() => {
          const p = this.state.noproduct;
          p[this.key++] = {};
          this.setState({ noproduct: { ...p } });
        }}
      >
        <AddIcon />
      </Fab>
    );
  }

  renderFileArea() {
    if (!this.formData.fileInformations) {
      this.formData.fileInformations = [];
    }
    return (
      <FileList
        files={this.formData.fileInformations}
        remove={(removedFile) => {
          let fileList = [...this.formData.fileInformations];
          fileList = fileList.filter(
            (file) => file.fileUUId !== removedFile.fileUUId
          );
          this.formData.fileInformations = fileList;
          this.setState({ i: 1 });
        }}
      />
    );
  }
  renderHeading() {
    return (
      <div className="add-heading-wrapper">
        <IconButton
          aria-label="back"
          onClick={this.props.back}
          className="back-icon"
        >
          <KeyboardBackspaceIcon />
        </IconButton>
        <span className="add-heading">{messages.common.add + this.title}</span>
      </div>
    );
  }
  renderFooter() {
    return (
      <div className="form-footer">
        <Button
          onClick={this.props.back}
          buttonClass="grey"
          label={messages.common.cancel}
        />
        <Button
          type="submit"
          buttonClass="blue"
          label={messages.common.save}
          disabled={
            this.state.isAdding || Object.keys(this.formValidation).length
          }
        />
      </div>
    );
  }
  renderToggle(label, name) {
    if (this.formData[name] === undefined) {
      this.formData[name] = false;
    }
    return (
      <FormControlLabel
        control={
          <Switch
            checked={Boolean(this.formData[name])}
            onChange={(e) => {
              this.formData[name] = e.target.checked;
              this.forceUpdate(); // Force re-render after formData change
            }}
            name={name}
            color="primary"
          />
        }
        label={label}
      />
    );
  }
  renderDate({
    fieldname,
    disabled,
    label,
    minDate,
    maxDate,
    onChange,
    emptyDate,
    type,
    value,
  }) {
    return (
      <Date
        onChange={(e) => {
          this.formData[fieldname] = e;
          onChange && onChange();
        }}
        type={type}
        label={label}
        fieldname={fieldname}
        disabled={disabled}
        minDate={minDate}
        maxDate={maxDate}
        emptyDate={emptyDate}
        defaultValue={value}
      />
    );
  }
  renderDateTime({
    fieldname,
    disabled,
    label,
    minDate,
    maxDate,
    datetime = true,
    onChange,
  }) {
    return (
      <Date
        onChange={(e) => {
          this.formData[fieldname] = e;
          onChange && onChange();
        }}
        label={label}
        fieldname={fieldname}
        disabled={disabled}
        minDate={minDate}
        maxDate={maxDate}
        datetime={datetime}
      />
    );
  }

  renderRadio({
    label,
    options = [],
    fieldname,
    defaultValue,
    classname,
    onChange,
  }) {
    return (
      <Radio
        options={options}
        label={label}
        defaultValue={defaultValue}
        classname={classname}
        onChange={(value) => {
          this.formData[fieldname] = value;
          onChange && onChange(value);
        }}
      />
    );
  }
  renderAutoComplete({
    fieldname,
    placeholder,
    options = [],
    getOption,
    onChange,
    required,
    disableClearable,
    multiple,
    disabled,
    value,
    freeSolo,
    helperText,
  }) {
    return (
      <Autocomplete
        id="tags-standard"
        disabled={disabled}
        options={options}
        value={value}
        freeSolo={freeSolo}
        getOptionLabel={getOption}
        onChange={onChange}
        disableClearable={disableClearable}
        multiple={multiple}
        filterSelectedOptions={!freeSolo}
        renderInput={(params) => (
          <TextField
            {...params}
            name={fieldname}
            variant="outlined"
            margin="normal"
            label={placeholder}
            required={required}
            helperText={helperText}
            InputLabelProps={{ shrink: true }}
            onBlur={(e) => {
              params.inputProps.onBlur && params.inputProps.onBlur(e);
              if (freeSolo && !multiple) {
                const typed = e.target.value;
                if (typed) onChange && onChange(e, typed);
              }
            }}
          />
        )}
      />
    );
  }
  renderTextField({
    fieldname,
    placeholder,
    type,
    disabled,
    required,
    onChange,
    value,
    skipAdd,
    validation,
    errorMessage,
    error,
    helperText,
    lengthConstraint,
    compRef,
    key,
  }) {
    // Use defaultValue instead of value to allow uncontrolled behavior
    // This allows users to type in the fields without re-render issues
    // However, if value is explicitly provided and skipAdd is true, use controlled mode
    const defaultValue = value !== undefined ? value : (this.formData[fieldname] || '');
    const useControlledMode = skipAdd && value !== undefined;
    
    // Debug log for specific fields to verify defaultValue
    if (fieldname === 'ourSlipNo' || fieldname === 'vehicleNo' || fieldname === 'challanNo' || fieldname === 'billNo' || fieldname === 'supplierSlipNo') {
      console.log(`[renderTextField] ${fieldname}:`, {
        valueProp: value,
        defaultValueCalculated: defaultValue,
        formDataValue: this.formData[fieldname],
        useControlledMode: useControlledMode,
        skipAdd: skipAdd,
        key: key
      });
    }
    
    return (
      <TF
        key={key}
        compRef={
          compRef
            ? (ref) => {
                this.fieldRefs[fieldname] = ref;
              }
            : null
        }
        required={required}
        name={fieldname}
        type={type}
        disabled={disabled}
        placeholder={placeholder}
        defaultValue={useControlledMode ? undefined : defaultValue}
        value={useControlledMode ? value : undefined}
        validation={validation}
        errorMessage={errorMessage}
        error={error}
        helperText={helperText}
        lengthConstraint={lengthConstraint}
        inValidateForm={(bFlag) => {
          if (bFlag) {
            this.formValidation[fieldname] = bFlag;
          } else {
            delete this.formValidation[fieldname];
          }
          this.setState({ isLoaded: true });
        }}
        onChange={(value) => {
          if (skipAdd) {
            onChange && onChange(value);
            return;
          }
          this.formData[fieldname] = value;
          onChange && onChange(value);
        }}
      />
    );
  }
  renderTextArea({
    fieldname,
    placeholder,
    type,
    disabled,
    required,
    onChange,
    value,
    key,
  }) {
    // Use defaultValue from value prop or formData
    const defaultValue = value !== undefined ? value : (this.formData[fieldname] || '');
    
    return (
      <TF
        key={key}
        required={required}
        name={fieldname}
        type={type}
        disabled={disabled}
        placeholder={placeholder}
        label={placeholder}
        defaultValue={defaultValue}
        onChange={(value) => {
          this.formData[fieldname] = value;
          onChange && onChange(value);
        }}
        multiline={true}
      />
    );
  }
}

export default AddForm;
