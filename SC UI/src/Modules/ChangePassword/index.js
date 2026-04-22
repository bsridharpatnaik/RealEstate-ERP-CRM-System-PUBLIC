import React, { Component } from "react";
import TextField from "@material-ui/core/TextField";
import InputAdornment from "@material-ui/core/InputAdornment";
import Button from "@material-ui/core/Button";
import { messages } from "./../../messages";
import { withSnackbar } from "notistack";
import { API } from "./../../axios";
import { apiEndpoints, appRoutes } from "./../../endpoints";
import { connect } from "react-redux";
import { removeToken } from "./../../helper";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import CloseIcon from "@material-ui/icons/Close";
import IconButton from "@material-ui/core/IconButton";

import "./style.scss";

class ChangePassword extends Component {
  state = { values: {}, disabled: true, sameError: false };
  constructor(props) {
    super(props);
    this.handleChange = this.handleChange.bind(this);
  }
  handleChange = (event) => {
    const values = this.state.values;
    values[event.target.name] = event.target.value;
    let sameError = false;
    const len = Object.keys(values).length;
    if (values["newPassword"] && values["confirmPassword"]) {
      sameError = values["newPassword"] !== values["confirmPassword"];
    } else {
      sameError = false;
    }
    const disabled =
      !!(
        values["newPassword"] &&
        values["confirmPassword"] &&
        values["oldPassword"]
      ) && values["newPassword"] === values["confirmPassword"];
    console.log(!disabled);
    this.setState({
      values: values,
      disabled: !disabled,
      sameError: sameError,
    });
  };
  changePwd = (event) => {
    event.preventDefault();
    const { values } = this.state;
    if (values.confirmPassword !== values.newPassword) {
      this.props.enqueueSnackbar(messages.common.passwordNotMatch, {
        variant: "error",
      });
      return;
    }
    const params = {
      oldPassword: values.oldPassword,
      newPassword: values.newPassword,
    };
    API.POST(apiEndpoints.changePassword, params).then((response) => {
      if (response.success) {
        this.props.enqueueSnackbar(messages.common.passwordSuccess, {
          variant: "success",
        });
        removeToken();
        window.location.reload();
      } else {
        this.props.enqueueSnackbar(response.message, {
          variant: "error",
        });
      }
    });
    return false;
  };
  render() {
    return (
      // <div className="page change-password">
      //   <div className="header-info">
      //     <span className="page-heading">{messages.common.changePassword}</span>
      //   </div>
      //   <div className="list-section">
      //     <form noValidate onSubmit={(e) => this.changePwd(e)}>
      //       <TextField
      //         variant="outlined"
      //         margin="normal"
      //         required
      //         fullWidth
      //         name="oldPassword"
      //         autoFocus
      //         type={"password"}
      //         InputLabelProps={{ shrink: true }}
      //         placeholder={messages.common.oldPassword}
      //         onChange={this.handleChange}
      //       />
      //       <TextField
      //         variant="outlined"
      //         margin="normal"
      //         required
      //         fullWidth
      //         name="newPassword"
      //         type={"password"}
      //         placeholder={messages.common.newPassword}
      //         onChange={this.handleChange}
      //       />
      //       <TextField
      //         variant="outlined"
      //         margin="normal"
      //         required
      //         fullWidth
      //         name="confirmPassword"
      //         type={"password"}
      //         placeholder={messages.common.confirmPassword}
      //         onChange={this.handleChange}
      //       />
      //       <Button type="submit" fullWidth variant="contained" color="primary">
      //         {messages.common.changePassword}
      //       </Button>
      //     </form>
      //   </div>
      // </div>
      <Dialog
        open={this.props.open}
        disableBackdropClick={false}
        maxWidth="xs"
        className="change-password-modal"
        onClose={this.props.onClose}
      >
        <DialogTitle>
          <div className="filter-heading">
            <span className="filter-title">Change Password</span>
            <IconButton
              aria-label="back"
              onClick={this.props.onClose}
              className="close-icon"
            >
              <CloseIcon />
            </IconButton>
          </div>
        </DialogTitle>
        <DialogContent dividers>
          <form noValidate onSubmit={(e) => this.changePwd(e)}>
            <TextField
              variant="outlined"
              margin="normal"
              required
              fullWidth
              name="oldPassword"
              autoFocus
              type={"password"}
              InputLabelProps={{ shrink: true }}
              placeholder={messages.common.oldPassword}
              onChange={this.handleChange}
            />
            <TextField
              variant="outlined"
              margin="normal"
              required
              fullWidth
              name="newPassword"
              type={"password"}
              placeholder={messages.common.newPassword}
              onChange={this.handleChange}
            />
            <TextField
              variant="outlined"
              margin="normal"
              required
              fullWidth
              name="confirmPassword"
              type={"password"}
              placeholder={messages.common.confirmPassword}
              onChange={this.handleChange}
            />
            {this.state.sameError && (
              <div className="pswd-error">
                Entered password and confirm password do not match.
              </div>
            )}
            <Button
              type="submit"
              fullWidth
              variant="contained"
              color="primary"
              buttonClass="blue"
              disabled={this.state.disabled}
            >
              {messages.common.changePassword}
            </Button>
          </form>
        </DialogContent>
      </Dialog>
    );
  }
}

export default connect(null, null, null, { forwardRef: true })(
  withSnackbar(ChangePassword)
);
