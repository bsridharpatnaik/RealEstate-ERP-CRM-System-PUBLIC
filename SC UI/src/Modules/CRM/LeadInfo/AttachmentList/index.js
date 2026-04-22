import React, { Component } from "react";
import Button from "@material-ui/core/Button";
import Dialog from "@material-ui/core/Dialog";
import DialogActions from "@material-ui/core/DialogActions";
import DialogContent from "@material-ui/core/DialogContent";
import DialogTitle from "@material-ui/core/DialogTitle";
import { apiEndpoints } from "./../../../../endpoints";
import { API } from "./../../../../axios";
import GetAppIcon from "@material-ui/icons/GetApp";
import IconButton from "@material-ui/core/IconButton";
import "./style.scss";
class AttachmentList extends Component {
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
  render() {
    return (
      <div>
        <Dialog
          open={this.props.open}
          onClose={this.props.close}
          aria-labelledby="form-dialog-title"
        >
          <DialogTitle id="form-dialog-title">Attachments</DialogTitle>
          <DialogContent>
            <div className="detail-item">
              {this.props.list &&
                this.props.list.map((file) => (
                  <div className="value detail-file-item">
                    {file.fileName}
                    <IconButton
                      className="back-icon"
                      onClick={() => this.download(file)}
                    >
                      <GetAppIcon />
                    </IconButton>
                  </div>
                ))}
            </div>
          </DialogContent>
          <DialogActions>
            <Button onClick={this.props.close} color="primary">
              Close
            </Button>
          </DialogActions>
        </Dialog>
      </div>
    );
  }
}

export default AttachmentList;
