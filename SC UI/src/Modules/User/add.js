//react
import React from "react";
import { connect } from "react-redux";
//third party
import { withSnackbar } from "notistack";
import AddForm from "./../../Shared/AddForm";

//misc
import { apiEndpoints } from "./../../endpoints";
import { messages } from "./../../messages";
//style
import "./style.scss";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import {Radio} from "@material-ui/core";

class Add extends AddForm {
  title = messages.common.user;
  addurl = apiEndpoints.createUser;

  state = {permissions: null}

  render() {
    return (
      <div className="list-section add">
        {this.renderHeading()}
        <form onSubmit={(e) => {
          this.formData.tenants = this.state.permissions.map(permission => ({tenantName: permission.tenant.name, authorization: permission.authorization}));
          this.add(e);
        }}>
          {this.renderTextField({
            fieldname: "username",
            placeholder: messages.fields.username,
            required: true,
          })}
          {this.renderTextField({
            fieldname: "password",
            placeholder: messages.fields.password,
            required: true,
            type: "password",
          })}
          {this.renderTextField({
            fieldname: "email",
            placeholder: "Email (s) - Seperated by ;",
            required: false,
          })}
          {this.renderAutoComplete({
            fieldname: "roles",
            placeholder: messages.fields.role,
            options: this.props.roles,
            multiple: true,
            onChange: (e, value) => {
              this.formData.roles = value;
            },
          })}
          {this.renderAutoComplete({
            fieldname: "tenants",
            placeholder: "Projects",
            options: this.props.tennants,
            disableClearable: true,
            multiple: true,
            getOption: (option) => {
              return option["tenantName"];
            },
            onChange: (e, values = []) => {
              this.formData.tenants = values.map((v) => v.tenantCode);
              this.setState(
                  {
                    permissions: values.map(value => {
                      if (value.authorization) {
                        return value;
                      } else {
                        return (
                            {
                              authorization: 'FullAccess',
                              tenant: {...value, name: value.tenantCode, tenantLongName: value.tenantName}
                            }
                        );
                      }
                    })
                  });
            },
          })}
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
  withSnackbar(Add)
);
