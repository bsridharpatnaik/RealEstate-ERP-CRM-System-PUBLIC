import React, { useEffect } from "react";
import Button from "@material-ui/core/Button";
import CssBaseline from "@material-ui/core/CssBaseline";
import TextField from "@material-ui/core/TextField";
import Paper from "@material-ui/core/Paper";
import Grid from "@material-ui/core/Grid";
import Typography from "@material-ui/core/Typography";
import { makeStyles } from "@material-ui/core/styles";
import Visibility from "@material-ui/icons/Visibility";
import VisibilityOff from "@material-ui/icons/VisibilityOff";
import InputAdornment from "@material-ui/core/InputAdornment";
import IconButton from "@material-ui/core/IconButton";
import EmailIcon from "@material-ui/icons/Email";
import LockIcon from "@material-ui/icons/Lock";
import "./styles.scss";
import { messages } from "./../../messages";
import { API } from "./../../axios";
import { apiEndpoints, appRoutes } from "./../../endpoints";
import { useSnackbar } from "notistack";
import {
  setToken,
  setRole,
  setUserName,
  setUserId,
  getRole,
  clearCookies,
  clearSession
} from "./../../helper";
import { useHistory } from "react-router-dom";
import {
  setTenannt,
  getAllowedTennants,
  getAllTennants,
} from "./../../actions/tennant";
import { getDealLostReasons } from "./../../actions/dealLostReasons";
import { connect } from "react-redux";

const useStyles = makeStyles((theme) => ({
  root: {
    height: "100vh",
  },
  image: {
    backgroundColor:
      theme.palette.type === "light"
        ? theme.palette.grey[50]
        : theme.palette.grey[900],
    height: "100%",
    overflow: "hidden",
  },
  avatar: {
    margin: theme.spacing(1),
    backgroundColor: theme.palette.secondary.main,
  },
  form: {
    width: "100%", // Fix IE 11 issue.
    marginTop: theme.spacing(1),
  },
  submit: {
    margin: theme.spacing(3, 0, 2),
  },
}));

function Login(props) {
  const { enqueueSnackbar } = useSnackbar();
  let history = useHistory();
  let projects = [];
  const classes = useStyles();
  const [values, setValues] = React.useState({
    username: "",
    password: "",
    showPassword: false,
  });

  useEffect(() => {
    clearCookies();
  }, [])

  const handleChange = (event) => {
    setValues({
      ...values,
      [event.target.name]: event.target.value,
    });
  };
  const handleClickShowPassword = () => {
    setValues({ ...values, showPassword: !values.showPassword });
  };

  const handleMouseDownPassword = (event) => {
    event.preventDefault();
  };
  const login = (event) => {
    event.preventDefault();
    const params = {
      userName: values.username,
      password: values.password,
    };
    API.POST(apiEndpoints.login, params).then(async (response) => {
      console.log(response);
      if (response.success) {
        setUserName(response.data.name);
        enqueueSnackbar(messages.common.loginSuccess, {
          variant: "success",
        });
        setRole(response.data.roles[0]);
        setToken(response.data.token);
        setUserId(response.data.userid);
        clearSession("dealLostReasons");
        props.getDealLostReasons();
        API.GET(apiEndpoints.getAllTennants).then((response) => {
          props.getAllTennants(response.data);
        });
        API.GET(apiEndpoints.getTenants).then(async (response) => {
          projects = response.data;
          props.getAllowedTennants(projects);

          if (projects.length === 1) {
            // Auto-select single tenant (project)
            await props.setTenannt(projects[0].tenantCode);

            // After tenant selection, fetch backend-driven inventory
            // edit constraints and persist them in cookie.
            const projectConstantsResponse = await API.GET(
              apiEndpoints.inventoryProjectConstants
            );
            if (
              projectConstantsResponse &&
              projectConstantsResponse.success &&
              Array.isArray(projectConstantsResponse.data)
            ) {
              const configList = projectConstantsResponse.data;

              const getValueByKey = (key) => {
                const item = configList.find((c) => c.key === key);
                if (!item || item.value == null) return undefined;
                const num = Number(item.value);
                return Number.isFinite(num) ? num : undefined;
              };

              const adminDays = getValueByKey("INVENTORY_ALLOWED_DAYS_ADMIN");
              const managerDays = getValueByKey("INVENTORY_ALLOWED_DAYS_MANAGER");
              const generalDays = getValueByKey("INVENTORY_ALLOWED_DAYS_EXECUTIVE");

              const rejectReturnAdminDays = getValueByKey("INVENTORY_REJECT_RETURN_DAYS_ADMIN");
              const rejectReturnManagerDays = getValueByKey("INVENTORY_REJECT_RETURN_DAYS_MANAGER");
              const rejectReturnGeneralDays = getValueByKey("INVENTORY_REJECT_RETURN_DAYS_EXECUTIVE");

              // Lazy import to avoid circular deps at top-level
              const { setInventoryEditDays, setRejectReturnDays } = await import("./../../helper");
              setInventoryEditDays({ adminDays, managerDays, generalDays });
              setRejectReturnDays({
                adminDays: rejectReturnAdminDays,
                managerDays: rejectReturnManagerDays,
                generalDays: rejectReturnGeneralDays,
              });
            }

            history.push(appRoutes.globalDashboard);
          } else {
            history.push(appRoutes.globalDashboard);
          }
        });
      } else {
        const status = response.status;
        const errorMsg = response.errorMessage;
        let msg;
        if (status === 401 && errorMsg === "INVALID_CREDENTIALS") {
          msg = messages.common.loginFailure;
        } else if (!status) {
          msg = "Unable to reach server. Please check your connection.";
        } else {
          msg = "Something went wrong. Please contact administrator.";
        }
        enqueueSnackbar(msg, { variant: "error" });
      }
    });
    return false;
  };
  return (
    <Grid container component="main" className={`${classes.root} login`}>
      <CssBaseline />
      <Grid
        item
        xs={12}
        sm={12}
        md={6}
        component={Paper}
        elevation={6}
        square
        className="login-form-wrapper"
      >
        <div className="login-form">
          <img
            className="logo"
            src={`${process.env.PUBLIC_URL}/${process.env.REACT_APP_LOGIN_LOGO}.png`}
            alt="company logo"
          />
          {/* <Typography component="h1" variant="h5">
            {messages.common.login}
          </Typography> */}
          {/* <Typography component="h1" variant="subtitle1">
            {messages.common.loginSubText}
          </Typography> */}
          <form className={classes.form} noValidate onSubmit={login}>
            <TextField
              variant="outlined"
              margin="normal"
              required
              fullWidth
              name="username"
              autoFocus
              InputLabelProps={{ shrink: true }}
              placeholder={"Username"}
              onChange={handleChange}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <EmailIcon style={{ color: "rgba(108,108,108,0.6)" }} />
                  </InputAdornment>
                ),
              }}
            />
            <TextField
              variant="outlined"
              margin="normal"
              required
              fullWidth
              name="password"
              type={values.showPassword ? "text" : "password"}
              id="password"
              placeholder={messages.fields.password}
              onChange={handleChange}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <LockIcon style={{ color: "rgba(108,108,108,0.6)" }} />
                  </InputAdornment>
                ),
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      onClick={handleClickShowPassword}
                      onMouseDown={handleMouseDownPassword}
                    >
                      {values.showPassword ? (
                        <Visibility style={{ color: "rgba(255,128,139,1)" }} />
                      ) : (
                        <VisibilityOff
                          style={{ color: "rgba(255,128,139,1)" }}
                        />
                      )}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />
            <Button
              type="submit"
              fullWidth
              variant="contained"
              color="primary"
              className={classes.submit}
            >
              {messages.common.login}
            </Button>
          </form>
        </div>
      </Grid>
      <Grid item xs={12} sm={12} md={6} className={classes.image}>
        <div className="img-wrapper">
          <img
            className="login-img"
            src={process.env.PUBLIC_URL + "/login.cms"}
            alt="company logo"
          />
        </div>
      </Grid>
    </Grid>
  );
}
const mapStateToProps = (state) => {
  return {};
};
export default connect(
  mapStateToProps,
  { setTenannt, getAllowedTennants, getAllTennants, getDealLostReasons },
  null,
  {
    forwardRef: true,
  }
)(Login);
