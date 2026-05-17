import React, { Component } from "react";
import FormLabel from "@material-ui/core/FormLabel";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import FormControl from "@material-ui/core/FormControl";
import Radio from "@material-ui/core/Radio";
import RadioGroup from "@material-ui/core/RadioGroup";

class CustomRadio extends Component {
  render() {
    const { options, fieldname, label, defaultValue, classname } = this.props;
    return (
      <FormControl component="fieldset" margin="normal" size="small">
        <FormLabel component="legend">{label}</FormLabel>
        <RadioGroup
          defaultValue={defaultValue}
          name={fieldname}
          className={classname}
          onChange={(e) => {
            this.props.onChange(e.target.value);
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
}

export default CustomRadio;
