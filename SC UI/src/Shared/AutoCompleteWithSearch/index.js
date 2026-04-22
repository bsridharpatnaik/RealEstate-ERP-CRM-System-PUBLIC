import React, { Component } from "react";
import Autocomplete from "@material-ui/lab/Autocomplete";
import { API } from "./../../axios";
import TextField from "@material-ui/core/TextField";
import CircularProgress from "@material-ui/core/CircularProgress";
import { apiEndpoints } from "./../../endpoints";

class AutoCompleteWithSearch extends Component {
  state = {
    open: true,
    isLoading: false,
    options: [],
  };
  onChangeHandle = async (value) => {
    this.setState({ isLoading: true });
    const response = await API.GET(this.props.url + value);
    if (response.success) {
      this.setState({ options: response.data });
    }
    this.setState({ isLoading: false });
  };
  render() {
    return (
      <Autocomplete
        multiple
        id="tags-standard"
        options={this.state.options}
        onChange={this.props.onChange}
        getOptionLabel={this.props.getoption}
        loading={this.state.isLoading}
        defaultValue={this.props.defaultValue}
        // disabled={disabled}
        filterSelectedOptions={true}
        renderInput={(params) => (
          <TextField
            {...params}
            name={this.props.name}
            id={this.props.name}
            variant="outlined"
            margin="normal"
            label={this.props.name}
            InputLabelProps={{ shrink: true }}
            placeholder={this.props.placeholder}
            InputProps={{
              ...params.InputProps,
              endAdornment: (
                <React.Fragment>
                  {this.state.isLoading ? (
                    <CircularProgress color="inherit" size={20} />
                  ) : null}
                  {params.InputProps.endAdornment}
                </React.Fragment>
              ),
            }}
            onChange={(ev) => {
              if (ev.target.value !== "") {
                this.onChangeHandle(ev.target.value);
              }
            }}
          />
        )}
      />
    );
  }
}

export default AutoCompleteWithSearch;
