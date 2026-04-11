import React from "react";
import BOQStatus from "../BOQStatus";
import { messages } from "../../messages";

const ProjectMaterialStatus = (props) => (
  <BOQStatus
    {...props}
    pageTitle={messages.common.projectMaterialStatus}
    breadcrumbTitle={messages.common.inventory}
  />
);

export default ProjectMaterialStatus;
