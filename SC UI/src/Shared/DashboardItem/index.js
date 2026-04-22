import React from "react";

import TreeItem from "@material-ui/lab/TreeItem";
import Typography from "@material-ui/core/Typography";
import "./style.scss";
import { NavLink } from "react-router-dom";
export default function DashboardItem(props) {
  const {
    labelText,
    labelIcon: LabelIcon,
    color,
    bgColor,
    linkurl,
    ...other
  } = props;

  return (
    <TreeItem
      className="dashboard-item"
      label={
        linkurl ? (
          <NavLink to={linkurl} activeClassName="active">
            <div className="menu-item">
              {LabelIcon && <LabelIcon color="inherit" className="icon" />}
              <Typography variant="body2" className="name">
                {labelText}
              </Typography>
            </div>
          </NavLink>
        ) : (
          <div className="menu-item">
            {LabelIcon && <LabelIcon color="inherit" className="icon" />}
            <Typography variant="body2" className="name">
              {labelText}
            </Typography>
          </div>
        )
      }
      {...other}
    />
  );
}
