import React from "react";
import { KeyboardDatePicker } from "@material-ui/pickers";

const KeyDatePickerContainer = (disabled, format, label, value, handleDateChange) => {
    
    this.setState({isOpen: false})
    
    return (
      <KeyboardDatePicker
        autoOk
        disabled={disabled}
        variant="inline"
        value={value}
        format={format}
        inputVariant="outlined"
        clearable={true}
        label={label}
        margin="normal"
        InputAdornmentProps={{ position: "start" }}
        onChange={newDate => {
          handleDateChange(newDate);
        }}
        KeyboardButtonProps={{
          onFocus: e => {
            this.setState({isOpen:true});
          }
        }}
        PopoverProps={{
          disableRestoreFocus: true,
          onClose: () => {
            this.setState({isOpen:false});
          }
        }}
        InputProps={{
          onFocus: () => {
            this.setState({isOpen:true});
          }
        }}
        open={this.state.isOpen}
      />
    );
  };

  export default KeyDatePickerContainer;