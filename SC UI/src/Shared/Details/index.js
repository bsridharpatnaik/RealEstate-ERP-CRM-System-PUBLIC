import React, { Component } from "react";
import { apiEndpoints } from "./../../endpoints";
import { API } from "./../../axios";
import { messages } from "./../../messages";
import IconButton from "@material-ui/core/IconButton";
import { DownloadIcon } from "./../../Shared/Icons/Index.js";
import AttachmentThumbnail from "./../../Shared/AttachmentThumbnail";

class Details extends Component {
  renderFileList(list) {
    return (
      <div className="detail-item">
        <div className="label">{messages.common.uploadedDocuments}</div>
        {list.map((file) => (
          <div key={file.id || file.fileUUId || file.fileName} className="value detail-file-item">
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <AttachmentThumbnail file={file} />
              {file.fileName}
            </div>
            <IconButton
              className="back-icon"
              onClick={() => this.download(file)}
            >
              {DownloadIcon({ fontSize: "medium" })}
            </IconButton>
          </div>
        ))}
      </div>
    );
  }
  async download(file) {
    this.setState({ isLoading: true });
    // When this detail view was opened cross-tenant (e.g. an inward reached via a
    // PO's status history from the global menu), the Redux tenant is master/wrong.
    // The opener passes the owning project's code as `tenantCode` — send it as an
    // explicit tenant-id header so the file is fetched from the correct schema.
    // Falls back to the Redux tenant (axios interceptor) when not provided.
    const config = { responseType: "blob" };
    if (this.props.tenantCode) {
      config.headers = { "tenant-id": this.props.tenantCode };
    }
    const response = await API.GET(apiEndpoints.download + file.fileUUId, config);
    this.setState({ isLoading: false });
    if (response.status === 200) {
      var a = document.createElement("a");
      var url = window.URL.createObjectURL(response.data);
      a.href = url;
      a.download = file.fileName;
      document.body.append(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    }
  }
}

export default Details;
