import React, { Component } from "react";
import "./upload.scss";
import Progress from "../Progress";
import { instance } from "./../../../../axios";
import { apiEndpoints } from "./../../../../endpoints";
import { SvgIcon } from "@material-ui/core";
import { ReactComponent as AttachSVG } from "./attach.svg";
function AttachIcon(props) {
  return (
    <SvgIcon {...props}>
      <AttachSVG />
    </SvgIcon>
  );
}
class Upload extends Component {
  constructor(props) {
    super(props);
    this.state = {
      files: [],
      uploading: false,
      uploadProgress: {},
      successfullUploaded: false,
    };

    this.onFilesAdded = this.onFilesAdded.bind(this);
    this.uploadFiles = this.uploadFiles.bind(this);
    this.sendRequest = this.sendRequest.bind(this);
    this.removeFile = this.removeFile.bind(this);
  }
  componentDidMount() {
    if (this.props.files) {
      const uploadProgress = {};
      if (this.props.files && this.props.files.length) {
        for (let i = 0; i < this.props.files.length; i++) {
          const file = this.props.files[i];
          uploadProgress[file.fileName] = {
            state: "done",
            percentage: 100,
            fileUUId: file.fileUUId,
          };
        }
      }
      this.setState({
        files: this.props.files,
        successfullUploaded: true,
        uploadProgress: uploadProgress,
      });
    }
  }
  componentDidUpdate(prevProps) {
    if (prevProps.files !== this.props.files) {
      const uploadProgress = {};
      if (this.props.files && this.props.files.length) {
        for (let i = 0; i < this.props.files.length; i++) {
          const file = this.props.files[i];
          uploadProgress[file.fileName] = {
            state: "done",
            percentage: 100,
            fileUUId: file.fileUUId,
          };
        }
      }
      this.setState({
        files: this.props.files,
        successfullUploaded: true,
        uploadProgress: uploadProgress,
      });
    }
  }

  onFilesAdded(e) {
    const files = e.target.files;
    this.setState(
      (prevState) => ({
        files: prevState.files.concat([...files]),
      }),
      () => this.uploadFiles()
    );
  }

  async uploadFiles() {
    this.setState({
      uploading: true,
      successfullUploaded: false,
    });
    const promises = [];
    this.state.files.forEach((file) => {

       const dotCount = (file.name.match(/\./g) || []).length;
      if (dotCount > 1) {
        alert(file.name + "in invalid file name");
      } else {
        promises.push(this.sendRequest(file));
      }
    });
    try {
      await Promise.all(promises);

      this.setState({ successfullUploaded: true, uploading: false });
    } catch (e) {
      // Not Production ready! Do some error handling here instead...
      this.setState({ successfullUploaded: true, uploading: false });
    }
  }
  sendRequest(file) {
    const config = {
      onUploadProgress: (progressEvent) => {
        if (progressEvent.lengthComputable) {
          const copy = { ...this.state.uploadProgress };
          copy[this.getFileName(file)] = {
            state: "pending",
            percentage: Math.round(
              (progressEvent.loaded * 100) / progressEvent.total
            ),
          };
          this.setState({ uploadProgress: copy });
        }
      },
    };

    let data = new FormData();
    data.append("file", file);

    instance
      .post(apiEndpoints.crmFileUpload, data, config)
      .then((res) => {
        const copy = { ...this.state.uploadProgress };
        copy[this.getFileName(file)] = {
          state: "done",
          percentage: 100,
          fileUUId: res.data.fileUUId,
        };
        this.setState({ uploadProgress: copy });
      })
      .catch((err) => {
        const copy = { ...this.state.uploadProgress };
        copy[this.getFileName(file)] = { state: "error", percentage: 0 };
        this.setState({ uploadProgress: copy });
      });
  }
  getFileName(file) {
    return file.name || file.fileName;
  }
  removeFile(file) {
    const filename = this.getFileName(file);
    let files = this.state.files.filter(
      (f) => filename !== this.getFileName(f)
    );
    const copy = { ...this.state.uploadProgress };
    delete copy[filename];
    this.setState({ files, uploadProgress: copy });
  }
  renderProgress(file) {
    const filename = this.getFileName(file);
    const uploadProgress = this.state.uploadProgress[filename];
    if (this.state.uploading || this.state.successfullUploaded) {
      return (
        <div className="ProgressWrapper">
          <Progress
            progress={uploadProgress ? uploadProgress.percentage : 0}
            file={file}
            removeFile={this.removeFile}
          />
        </div>
      );
    }
  }

  render() {
    return (
      <div className="Upload">
        <div className="upload-button">
          <label className="custom-file-input">
            <input type="file" onChange={this.onFilesAdded} multiple />
            <div className="icon-wrapper">
              <AttachIcon />
            </div>
            <span className="attach-text">Attach Files</span>
          </label>
        </div>
        <div className="Files">
          {this.state.files.map((file) => {
            return this.renderProgress(file);
          })}
        </div>
      </div>
    );
  }
}

export default Upload;
