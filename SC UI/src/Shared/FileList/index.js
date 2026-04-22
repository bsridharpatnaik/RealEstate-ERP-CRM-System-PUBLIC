import React, { Component } from "react";
import DeleteIcon from "@material-ui/icons/Delete";
import IconButton from "@material-ui/core/IconButton";
import GetAppIcon from "@material-ui/icons/GetApp";
import { apiEndpoints } from "./../../endpoints";
import { API } from "./../../axios";
import Upload from "./../Upload";
import { messages } from "./../../messages";
import "./style.scss";
import DeleteConfirm from "./../DeleteConfirm";

class FileList extends Component {
  state = { deleteConfirmOpen: false };
  deleteRor = null;
  notifyChange(files) {
    if (typeof this.props.onChange === "function") {
      this.props.onChange(files);
    }
  }
  async download(file) {
    this.setState({ isLoading: true });
    const response = await API.GET(apiEndpoints.download + file.fileUUId, {
      responseType: "blob",
    });
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
  renderFile(file) {
    return (
      <div className="file-item">
        <div className="file-name">{file.fileName}</div>
        <div className="file-buttons">
          <IconButton className="back-icon" onClick={() => this.download(file)}>
            <GetAppIcon />
          </IconButton>
          <IconButton
            disabled={this.props.deleteAllowed}
            onClick={() => {
              this.deleteRow = file;
              this.setState({ deleteConfirmOpen: true });
            }}
            className="back-icon"
          >
            <DeleteIcon />
          </IconButton>
        </div>
      </div>
    );
  }
  renderUpload() {
    return (
      <div className="file-item">
        <Upload
          newFile={(e) => {
            this.props.files.push(e);
            this.setState({ i: 1 });
            this.notifyChange([...(this.props.files || [])]);
          }}
        />
      </div>
    );
  }
  render() {
    const files = this.props.files || [];
    return (
      <React.Fragment>
        <div className="product-heading">
          <div>{messages.common.uploadedDocuments}</div>
        </div>
        <div className="files">
          {files.map((file) => this.renderFile(file))}
          {this.renderUpload()}
        </div>
        <DeleteConfirm
          open={this.state.deleteConfirmOpen}
          onConfirm={() => {
            this.props.remove(this.deleteRow);
            this.setState({ deleteConfirmOpen: false });
            this.notifyChange([...(this.props.files || [])]);
          }}
          onCancel={() => {
            this.deleteRow = null;
            this.setState({ deleteConfirmOpen: false });
          }}
        />
      </React.Fragment>
    );
  }
}

export default FileList;
