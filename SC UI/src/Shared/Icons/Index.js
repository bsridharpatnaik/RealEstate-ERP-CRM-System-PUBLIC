//react
import React from "react";

import { SvgIcon } from "@material-ui/core";
import { ReactComponent as InventorySVG } from "../Icons/InventoryIcon.svg";
import { ReactComponent as CRMSVG } from "../Icons/CRMIcon.svg";
import { ReactComponent as FilterSVG } from "../Icons/FilterIcon.svg";
import { ReactComponent as EditSVG } from "../Icons/EditIcon.svg";
import { ReactComponent as DownloadSVG } from "../Icons/DownloadIcon.svg";
import { ReactComponent as DeleteSVG } from "../Icons/DeleteIcon.svg";
import { ReactComponent as RefreshSVG} from "../Icons/refresh.svg";
import VisibilityIcon from "@material-ui/icons/Visibility";
import SaveIcon from "@material-ui/icons/Save";
import BackspaceIcon from "@material-ui/icons/Backspace";
import CheckBoxIcon from "@material-ui/icons/CheckBox";
import CheckBoxOutlineBlankIcon from "@material-ui/icons/CheckBoxOutlineBlank";
import { ReactComponent as DeleteCircleSVG } from "../Icons/DeleteCircleIcon.svg";
import ellipsisVerticalImg from "../Icons/ellipsis-vertical.png";
import xImg from "../Icons/x.png";

export function InventoryIcon(props) {
  return (
    <SvgIcon {...props}>
      <InventorySVG />
    </SvgIcon>
  );
}

export function CRMIcon(props) {
  return (
    <SvgIcon {...props}>
      <CRMSVG />
    </SvgIcon>
  );
}

export function FilterIcon(props) {
  return (
    <SvgIcon {...props}>
      <FilterSVG />
    </SvgIcon>
  );
}

export function EditIcon(props) {
  return (
    <SvgIcon {...props}>
      <EditSVG />
    </SvgIcon>
  );
}

export function DownloadIcon(props) {
  return (
    <SvgIcon {...props}>
      <DownloadSVG />
    </SvgIcon>
  );
}

export function DownloadIconDisabled(props) {
  return (
    <SvgIcon color={"disabled"} {...props}>
      <DownloadSVG />
    </SvgIcon>
  );
}

export function DeleteIcon(props) {
  return (
    <SvgIcon {...props}>
      <DeleteSVG />
    </SvgIcon>
  );
}
export function RefreshIcon(props) {
  return (
    <SvgIcon {...props}>
      <RefreshSVG />
    </SvgIcon>
  );
}
function iconSizeFromProps(props) {
  const { style, fontSize } = props;
  const size = style?.width ?? fontSize ?? 24;
  if (typeof size === "number") return size;
  if (size === "small") return 20;
  if (size === "medium" || size === "default") return 24;
  if (size === "large") return 28;
  return 24;
}

export function CloseIcon(props) {
  const { style, ...rest } = props;
  const px = iconSizeFromProps(props);
  return (
    <img
      src={xImg}
      alt="Close"
      {...rest}
      style={{ width: px, height: px, display: "block", ...style }}
    />
  );
}

export function MoreIcon(props) {
  const { style, ...rest } = props;
  const px = iconSizeFromProps(props);
  return (
    <img
      src={ellipsisVerticalImg}
      alt="More"
      {...rest}
      style={{ width: px, height: px, display: "block", ...style }}
    />
  );
}

export function ViewIcon(props) {
  return (
    <SvgIcon htmlColor={"#000000"} {...props}>
      <VisibilityIcon />
    </SvgIcon>
  );
}

export function ViewIconDisabled(props) {
  return (
    <SvgIcon color={"disabled"} {...props}>
      <VisibilityIcon />
    </SvgIcon>
  );
}

export function SaveRowIcon(props) {
  return (
    <SvgIcon htmlColor={"#f9b571"} {...props}>
      <SaveIcon />
    </SvgIcon>
  );
}

export function CancelIcon(props) {
  return (
    <SvgIcon htmlColor={"#a2a2c3"} {...props}>
      <BackspaceIcon />
    </SvgIcon>
  );
}

export function CheckedIcon(props) {
  return (
    <SvgIcon htmlColor={"#44C4A1"} {...props}>
      <CheckBoxIcon />
    </SvgIcon>
  );
}

export function UncheckIcon(props) {
  return (
    <SvgIcon htmlColor={"#44C4A1"} {...props}>
      <CheckBoxOutlineBlankIcon />
    </SvgIcon>
  );
}
export function DeleteCircleIcon(props) {
  return (
    <SvgIcon {...props}>
      <DeleteCircleSVG />
    </SvgIcon>
  );
}
