//react
import React from "react";
import { connect } from "react-redux";
//third party
import TextField from "@material-ui/core/TextField";
import { withSnackbar } from "notistack";
import Autocomplete from "@material-ui/lab/Autocomplete";
import Checkbox from "@material-ui/core/Checkbox";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import {Radio} from "@material-ui/core";

//misc
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";
//style
import "./style.scss";
import EditForm from "./../../Shared/EditForm";

class Edit extends EditForm {
  updateUrl = apiEndpoints.updateUser;
  title = messages.common.user;
  state = { isLoaded: false, changePassword: false, permissions: null };

  componentDidMount() {
    this.updateUrl = this.updateUrl + this.props.id;
    this.search();
  }
  async search() {
    const response = await API.GET(apiEndpoints.getUserDetail + this.props.id);
    if (response.success) {
      const data = response.data;
      let roles = data.roles;
      roles = roles.map((r) => r.name);
      this.formData.username = data.userName;
      this.formData.email = data.email;
      this.formData.roles = roles;
      this.formData.tenants = data.tenantList.map(item => ({...item , tenantCode: item.tenant.name, tenantName: item.tenant.tenantLongName}));
      this.setState({
        permissions: data.tenantList,
        isLoaded: true,
      });
    }
  }

  render() {
    return (
      <div className="list-section add">
        {this.renderHeading()}
        {this.state.isLoaded && (
          <form onSubmit={(e) => {
            this.formData.tenants = this.state.permissions.map(permission => ({tenantName: permission.tenant.name, authorization: permission.authorization}));
            this.update(e)
          }}>
            {this.renderTextField({
              fieldname: "username",
              placeholder: messages.fields.username,
              required: true,
            })}
            <div className="password-edit">
              <FormControlLabel
                className="password-checkbox"
                control={
                  <Checkbox
                    name="checkedB"
                    color="primary"
                    onChange={(event) => {
                      this.setState({
                        changePassword: event.target.checked,
                      });
                    }}
                  />
                }
                label={messages.common.changePassword}
              />
              {this.renderTextField({
                fieldname: "password",
                placeholder: messages.fields.password,
                required: true,
                type: "password",
                disabled: !this.state.changePassword,
              })}
              
            </div>
            {this.renderTextField({
              fieldname: "email",
              placeholder: "Email (s) - Seperated by ;",
              required: false,
            })}
            <div className="flex">
              <Autocomplete
                multiple
                id="tags-standard"
                options={this.props.roles}
                onChange={(event, values) => (this.formData.roles = values)}
                defaultValue={this.formData.roles}
                filterSelectedOptions={true}
                renderInput={(params) => (
                  <TextField
                    {...params}
                    name="role"
                    variant="outlined"
                    placeholder={messages.fields.role}
                    label="Role"
                    InputLabelProps={{ shrink: true }}
                    margin="normal"
                  />
                )}
              />
            </div>
            <div className="flex">
              <Autocomplete
  multiple
  options={this.props.tennants}
  defaultValue={this.formData.tenants}
  filterSelectedOptions
  getOptionLabel={(option) => option.tenantName}
  getOptionSelected={(option, value) =>
    option.tenantCode === value.tenantCode
  }
  onChange={(e, values = []) => {
    this.formData.tenants = values.map(v => v.tenantCode);

    this.setState({
      permissions: values.map(value =>
        value.authorization
          ? value
          : {
              authorization: "FullAccess",
              tenant: {
                ...value,
                name: value.tenantCode,
                tenantLongName: value.tenantName,
              },
            }
      ),
    });
  }}
  renderInput={(params) => (
    <TextField
      {...params}
      variant="outlined"
      label="Projects"
      placeholder="Projects"
      InputLabelProps={{ shrink: true }}
      margin="normal"
    />
  )}
/>
            </div>
            {
              this.state.permissions
              &&
              this.state.permissions.length > 0
              &&
              <div className="list-section">
                  <div className="add-heading">Permissions</div>
                  <table>
                    {console.log(this.state.permissions)}
                    <tbody>
                    {
                      this.state.permissions.map((tenant) => (
                              <tr>
                                <td>{tenant?.tenant?.tenantLongName}</td>
                                <td>
                                  <FormControlLabel
                                      value="ReadOnly"
                                      checked={tenant?.authorization === 'ReadOnly'}
                                      onClick={event => {
                                        const {permissions} = this.state;
                                        const index = permissions.indexOf(tenant);
                                        permissions[index].authorization = event.target.value;
                                        this.setState({permissions: [...permissions]});
                                      }}
                                      control={<Radio color="primary" />}
                                      label="View"
                                  />
                                </td>
                                <td>
                                  <FormControlLabel
                                      value="FullAccess"
                                      checked={tenant?.authorization === 'FullAccess'}
                                      onClick={event => {
                                        const {permissions} = this.state;
                                        const index = permissions.indexOf(tenant);
                                        permissions[index].authorization = event.target.value;
                                        this.setState({permissions: [...permissions]});
                                      }}
                                      control={<Radio color="primary" />}
                                      label="Edit"
                                  />
                                </td>
                              </tr>
                          )
                      )
                    }
                    </tbody>
                  </table>
                </div>
            }
            {this.renderFooter()}
          </form>
        )}
      </div>
    );
  }
}
const mapStateToProps = (state) => {
  return {
    roles: state.roles.roles,
    tennants: state.allTennant.tennants,
  };
};
export default connect(mapStateToProps, null, null, { forwardRef: true })(
  withSnackbar(Edit)
);
