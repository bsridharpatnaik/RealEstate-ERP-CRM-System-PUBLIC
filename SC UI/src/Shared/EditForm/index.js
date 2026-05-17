import React, { Component } from "react";
import TextField from "@material-ui/core/TextField";
import IconButton from "@material-ui/core/IconButton";
import KeyboardBackspaceIcon from "@material-ui/icons/KeyboardBackspace";
import { messages, constants } from "./../../messages";
import Button from "./../../Shared/Button";
import { API } from "./../../axios";
import Autocomplete from "@material-ui/lab/Autocomplete";
import Date from "./../Date";
import Radio from "@material-ui/core/Radio";
import RadioGroup from "@material-ui/core/RadioGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import FormControl from "@material-ui/core/FormControl";
import FormLabel from "@material-ui/core/FormLabel";
import TF from "./../TextField";
import Switch from "@material-ui/core/Switch";
import FileList from "./../../Shared/FileList";
import { isAdmin } from "./../../helper";
import Fab from "@material-ui/core/Fab";
import AddIcon from "@material-ui/icons/Add";

class EditForm extends Component {
  formData = {};
  formValidation = {};
  fieldRefs = {};
  dateFormat = constants.dateFormat;
  constructor(props) {
    super(props);
    this.isAdmin = isAdmin();
  }
  async update(event) {
    this.setState({ isUpdating: true });
    const params = this.formData;
    event.preventDefault();
    const response = await API.PUT(this.updateUrl, params);
    this.showToaster(response);
    this.setState({ isUpdating: false });
  }
  showToaster(response) {
    if (response.success) {
      this.props.enqueueSnackbar(this.title + messages.common.updateSuccess, {
        variant: "success",
      });
      this.props.back();
    } else {
      this.props.enqueueSnackbar(response.errorMessage, {
        variant: "error",
      });
    }
  }
  renderFileArea() {
    return (
      <FileList
        deleteAllowed={!this.isAdmin}
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
  renderProductAddButton(isDisabled = false) {
    return (
      <Fab
        className="add-product"
        color="primary"
        aria-label="add"
        disabled={
          !this.isAdmin ||
          Object.keys(this.state.noproduct).length === 50 ||
          isDisabled
        }
        onClick={() => {
          if (!this.formData.warehouseId) {
            this.props.enqueueSnackbar("Select Warehouse first", {
              variant: "error",
            });
            return;
          }
          const p = this.state.noproduct;
          p[this.key++] = {};
          this.setState({ noproduct: { ...p } });
        }}
      >
        <AddIcon />
      </Fab>
    );
  }
  renderToggle(label, name) {
    return (
      <FormControlLabel
        control={
          <Switch
            onChange={(e) => {
              this.formData[name] = e.target.checked;
              this.setState({ i: 1 });
            }}
            checked={this.formData[name]}
            name={name}
            color="primary"
          />
        }
        label={label}
      />
    );
  }
  renderTextArea({
    fieldname,
    placeholder,
    type,
    disabled,
    required,
    defaultKey,
    onChange,
  }) {
    defaultKey = defaultKey || fieldname;
    return (
      <TF
        defaultValue={this.formData[defaultKey]}
        required={required}
        name={fieldname}
        type={type}
        disabled={disabled}
        placeholder={placeholder}
        label={placeholder}
        onChange={(value) => (this.formData[fieldname] = value)}
        multiline={true}
      />
    );
  }
  renderDate({
    fieldname,
    disabled,
    label,
    minDate,
    maxDate,
    defaultKey,
    onChange,
    emptyDate,
    type,
  }) {
    const key = defaultKey || fieldname;
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
        defaultValue={this.formData[key]}
        emptyDate={emptyDate}
      />
    );
  }
  renderDateTime({
    defaultKey,
    fieldname,
    disabled,
    label,
    minDate,
    maxDate,
    datetime = true,
    onChange,
  }) {
    const key = defaultKey || fieldname;

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
        defaultValue={this.formData[key]}
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
        <span className="add-heading">
          {messages.common.update + this.title}
        </span>
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
            this.state.isUpdating || Object.keys(this.formValidation).length
          }
        />
      </div>
    );
  }
  renderRadio({
    label,
    options,
    fieldname,
    defaultValue,
    classname,
    onChange,
  }) {
    return (
      <FormControl component="fieldset" margin="normal" size="small">
        <FormLabel component="legend">{label}</FormLabel>
        <RadioGroup
          defaultValue={defaultValue}
          name={fieldname}
          className={classname}
          onChange={(e) => {
            this.formData[fieldname] = e.target.value;
            onChange && onChange(e.target.value);
          }}
        >
          {options.map((option) => (
            <FormControlLabel
              value={option.value}
              control={<Radio color="primary" />}
              label={option.label}
            />
          ))}
        </RadioGroup>
      </FormControl>
    );
  }
  renderAutoComplete({
    fieldname,
    placeholder,
    options,
    getOption,
    onChange,
    required,
    disableClearable,
    data,
    skipAdd,
    disabled,
    getDefaultValue,
  }) {
    let value = data || this.formData;
    let defaultValue = options.filter((op) => op.id === value[fieldname]);
    defaultValue = getDefaultValue
      ? getDefaultValue()
      : defaultValue
      ? defaultValue[0]
      : {};
    return (
      <Autocomplete
        disabled={disabled}
        id="tags-standard"
        options={options}
        getOptionLabel={getOption}
        filterSelectedOptions={true}
        onChange={(e, v) => {
          if (skipAdd) {
            onChange && onChange(e, v);
            return;
          }
          let value = Array.isArray(v) ? v : v && v.id ? v.id : v;
          this.formData[fieldname] = value;
        }}
        disableClearable={disableClearable}
        defaultValue={defaultValue}
        renderInput={(params) => (
          <TextField
            {...params}
            name="fieldname"
            id="fieldname"
            variant="outlined"
            margin="normal"
            label={placeholder}
            required={required}
            InputLabelProps={{ shrink: true }}
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
    defaultKey,
    value,
    data,
    onChange,
    skipAdd,
    validation,
    errorMessage,
    lengthConstraint,
    compRef,
    regexConstraint
  }) {
    let currentValue = data || this.formData;
    defaultKey = defaultKey || fieldname;
    let defaultValue = currentValue[defaultKey];

    return (
      <TF
        compRef={
          compRef
            ? (ref) => {
                this.fieldRefs[fieldname] = ref;
              }
            : null
        }
        defaultValue={defaultValue}
        required={required}
        name={fieldname}
        type={type}
        disabled={disabled}
        placeholder={placeholder}
        label={placeholder}
        validation={validation}
        errorMessage={errorMessage}
        lengthConstraint={lengthConstraint}
        regexConstraint={regexConstraint}
        onChange={(value) => {
          if (skipAdd) {
            onChange && onChange(value);
            return;
          }
          this.formData[fieldname] = value;
          onChange && onChange(value);
        }}
        value={value}
        inValidateForm={(bFlag) => {
          if (bFlag) {
            this.formValidation[fieldname] = bFlag;
          } else {
            delete this.formValidation[fieldname];
          }
          this.setState({ isLoaded: true });
        }}
      />
    );
  }
}

export default EditForm;
