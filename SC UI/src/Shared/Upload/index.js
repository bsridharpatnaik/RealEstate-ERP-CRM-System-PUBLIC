import React, { Component } from "react";
import { messages } from "./../../messages";
import "./style.scss";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import Button from "./../Button";
import { withSnackbar } from "notistack";
import camera from "./camera.js";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import CloseIcon from "@material-ui/icons/Close";
import IconButton from "@material-ui/core/IconButton";

class Upload extends Component {
  state = { isLoaded: true };
  toBlob(dataURI) {
    var byteString = atob(dataURI.split(",")[1]);
    var mimeString = dataURI.split(",")[0].split(":")[1].split(";")[0];

    var buffer = new ArrayBuffer(byteString.length);
    var data = new DataView(buffer);

    for (var i = 0; i < byteString.length; i++) {
      data.setUint8(i, byteString.charCodeAt(i));
    }

    return new Blob([buffer], { type: mimeString });
  }
  async fileUpload(e, captured) {
    const file = captured ? this.toBlob(captured) : e.target.files[0];
    var FileSize = file.size / 1024 / 1024; // in MiB

    if (FileSize > 15) {
      this.props.enqueueSnackbar("File upload is restricted to 15MB", {
        variant: "error",
      });
      return false;
    }
    const formData = new FormData();
    formData.append("file", file, file.name || "file");
    this.setState({ isLoaded: false });
    const response = await API.POST(apiEndpoints.fileUpload, formData);
    this.setState({ isLoaded: true });

    if (response.success) {
      this.props.newFile(response.data);
      this.props.enqueueSnackbar("File uploaded successfully", {
        variant: "success",
      });
    } else {
      this.props.enqueueSnackbar(response.errorMessage, {
        variant: "error",
      });
    }
    return false;
  }
  closeCapture() {
    this.setState({ captureOpen: false, captured: false });
  }
  render() {
    return (
      <React.Fragment>
        {this.state.isLoaded && (
          <input
            id="contained-button-file"
            type="file"
            onChange={(e) => {
              this.fileUpload(e);
              return false;
            }}
          />
        )}
        <label htmlFor="contained-button-file">
          <Button
            component="span"
            buttonClass="upload"
            label={messages.common.upload}
            onClick={() => this.setState({ value: null })}
          />
        </label>
        <Button
          component="span"
          buttonClass="upload"
          label={"Capture"}
          onClick={() =>
            this.setState({ captureOpen: true }, () =>
              setTimeout(() => {
                camera.startCamera();
              }, 200)
            )
          }
        />
        <Dialog
          open={this.state.captureOpen}
          disableBackdropClick={false}
          maxWidth="xs"
          className="change-password-modal"
          onClose={() => this.closeCapture()}
        >
          <DialogTitle>
            <div className="filter-heading">
              <span className="filter-title">Capture Image</span>
              <IconButton
                aria-label="back"
                onClick={() => this.closeCapture()}
                className="close-icon"
              >
                <CloseIcon />
              </IconButton>
            </div>
          </DialogTitle>
          <DialogContent dividers>
            {!this.state.captured ? (
              <video
                id="video"
                autoplay={true}
                width="320"
                height="240"
              ></video>
            ) : (
              ""
            )}
            <canvas id="canvas" width="320" height="240"></canvas>
            <Button
              type="submit"
              fullWidth
              variant="contained"
              color="primary"
              buttonClass="blue"
              onClick={() => {
                if (this.state.captured) {
                  this.fileUpload(null, camera.getImage());
                  this.closeCapture();
                } else {
                  camera.takeSnapshot();
                  setTimeout(() => {
                    this.setState({ captured: true });
                    camera.stopCamera();
                  }, 200);
                }
              }}
              label={this.state.captured ? "Upload" : "Capture"}
            ></Button>
          </DialogContent>
        </Dialog>
      </React.Fragment>
    );
  }
}
export default withSnackbar(Upload);
