//react
import React from "react";
//third party
import Button from "@material-ui/core/Button";
//style
//misc
import IconButton from "@material-ui/core/IconButton";
import { messages } from "./../../../../messages";
import CommonTable from "./../../../../Shared/Table";
import Switch from "@material-ui/core/Switch";
import TF from "@material-ui/core/TextField";
import {
  DownloadIcon,
  DownloadIconDisabled,
  CheckedIcon,
  UncheckIcon,
  SaveRowIcon,
  CancelIcon,
} from "./../../../../Shared/Icons/Index";
import { instance, API } from "./../../../../axios";
import { apiEndpoints } from "./../../../../endpoints";
import { withRouter } from "react-router-dom";
import { SvgIcon } from "@material-ui/core";
import { withSnackbar } from "notistack";

import { ReactComponent as AttachSVG } from "../UploadFile/attach.svg";
function AttachIcon(props) {
  return (
    <SvgIcon {...props}>
      <AttachSVG />
    </SvgIcon>
  );
}

class Table extends CommonTable {
  renderButton(value, row) {
    return (
      <Button
        color="primary"
        onClick={() => {
          this.onEdit(row.documentId);
        }}
      >
        <span>{`${value}`}</span>
      </Button>
    );
  }

  render() {
    return (
      <React.Fragment>
        {this.renderAdd()}
        <div className="list-section">
          <div className="table-wrapper">
            <table className="update-table">
              <thead>{this.renderHeader()}</thead>
              <tbody>{this.renderBody()}</tbody>
            </table>
          </div>
        </div>
      </React.Fragment>
    );
  }

  renderCheckBox(status, label, classname, fontsize) {
    return (
      <IconButton aria-label={label} className={classname}>
        {status
          ? CheckedIcon({ fontSize: fontsize })
          : UncheckIcon({ fontSize: fontsize })}
      </IconButton>
    );
  }

  renderCell(key, row) {
    const value = row[key] !== null ? row[key] : "";
    const { isEditMode, isAddMode } = row;
    if (isEditMode || isAddMode) {
      if (key === messages.formParams.documents.documentName) {
        return (
          <td>
            {this.renderTextField({
              value: row.tempdocumentName,
              id: row.documentId,
              fieldname: messages.formParams.documents.documentName,
              placeholder: "Document Name",
              required: true,
              lengthConstraint: 50,
              validation: "length",
            })}
            <span className="documentNameError">
              {row.showError ? "*Document Name is required" : ""}
            </span>
          </td>
        );
      } else if (key === messages.formParams.documents.receivedStatus) {
        return (
          <td>
            {this.renderCheckBoxClickable(
              row.documentId,
              row.tempreceivedStatus,
              row.tempreceivedStatus ? "Received" : "Not Received",
              "back-icon",
              "medium"
            )}
          </td>
        );
      } else if (key === messages.formParams.documents.upload) {
        return (
          <td>
            <div className="upload-button">
              <label className="custom-file-input">
                <input
                  type="file"
                  onChange={(e) => {
                    this.uploadFile(e, row);
                  }}
                />
                <div className="icon-wrapper">
                  <AttachIcon />
                </div>
                <span className="attach-text">Upload Document</span>
              </label>
            </div>
            <div className="uploaded-fileName">
              <label>
                {row.tempfileInformation
                  ? row.tempfileInformation.fileName
                  : "No file Chosen"}
              </label>
            </div>
          </td>
        );
      }
    } else {
      if (key === messages.formParams.documents.documentName) {
        return <td>{this.renderButton(value, row)}</td>;
      } else if (key === messages.formParams.documents.upload) {
        const value = row.fileInformation
          ? row.fileInformation.fileName
          : "No file Chosen";
        return <td>{`${value}`}</td>;
      } else if (key === messages.formParams.documents.receivedStatus) {
        return (
          <td>
            {row.receivedStatus
              ? this.renderCheckBox(
                  row.receivedStatus,
                  "Received",
                  "back-icon",
                  "medium"
                )
              : this.renderCheckBox(
                  row.receivedStatus,
                  "Not Received",
                  "back-icon",
                  "medium"
                )}
          </td>
        );
      } else {
        return super.renderCell(key, row);
      }
    }
  }

  renderAction(row) {
    if (row.isEditMode || row.isAddMode) {
      return (
        <React.Fragment>
          <td className="action-edit">
            <IconButton
              aria-label="Save"
              disabled={row.showError}
              onClick={() => {
                //call save api function here to save the row only.
                this.onSave(row.documentId);
              }}
              className="back-icon"
            >
              {SaveRowIcon({ fontSize: "medium" })}
            </IconButton>
          </td>
          <td className="action-delete">
            <IconButton
              aria-label="cancel"
              onClick={() => {
                this.onCancel(row.documentId);
              }}
              className="back-icon"
            >
              {CancelIcon({ fontSize: "medium" })}
            </IconButton>
          </td>
        </React.Fragment>
      );
    } else {
      if (row.fileInformation !== null && row.fileInformation.fileUUId) {
        return (
          <React.Fragment>
            <td className="action-edit">
              {/* <IconButton
                  aria-label="view"
                  className="back-icon"
                >
                  {ViewIcon({ fontSize: "medium" })}
                </IconButton> */}
            </td>
            <td className="action-delete">
              <IconButton
                aria-label="download"
                onClick={() => {
                  this.onDownload(row.fileInformation);
                }}
                className="back-icon"
              >
                {DownloadIcon({ fontSize: "medium" })}
              </IconButton>
            </td>
          </React.Fragment>
        );
      } else {
        return (
          <React.Fragment>
            <td className="action-edit">
              {/* <IconButton
                        aria-label="view"
                        
                        className="back-icon"
                      >
                        {ViewIconDisabled({ fontSize: "medium" })}
                      </IconButton> */}
            </td>
            <td className="action-delete">
              <IconButton aria-label="download" className="back-icon">
                {DownloadIconDisabled({ fontSize: "medium" })}
              </IconButton>
            </td>
          </React.Fragment>
        );
      }
    }
  }

  uploadFile(e, document) {
    if (e.target.files.length === 0) {
      const newData = this.state.rows.map((row) => {
        if (row.documentId === document.documentId) {
          const newRow = { ...row, tempfileInformation: null };
          return newRow;
        }
        return row;
      });
      this.setState({ rows: newData });
      return;
    }
    let data = new FormData();
    data.append("file", e.target.files[0]);

    instance
      .post(apiEndpoints.crmFileUpload, data)
      .then((res) => {
        const newData = this.state.rows.map((row) => {
          if (row.documentId === document.documentId) {
            const newRow = { ...row, tempfileInformation: res.data };
            return newRow;
          }
          return row;
        });
        this.setState({ rows: newData });
      })
      .catch((err) => {});
  }

  renderAdd() {
    return (
      <Button
        onClick={() => this.onAdd()}
        color="primary"
        variant="contained"
        classes={{
          root: "add-button, add-documentPostSales",
          label: "add-label",
        }}
      >
        {`${messages.common.add}`}
      </Button>
    );
  }

  async onSave(id) {
    if (this.validateRow(id)) {
      const filteredRow = this.state.rows.filter((row) => {
        return row.documentId === id;
      });
      let param = {
        leadId: this.props.leadId,
        documentName: filteredRow[0].tempdocumentName,
        receivedStatus: filteredRow[0].tempreceivedStatus,
        fileInformation: filteredRow[0].tempfileInformation,
      };
      if (id > 0) {
        const res = await API.PUT(
          apiEndpoints.updatePostSalesDocument.replace("{documentId}", id),
          param
        );
        if (res.success) {
          const newData = this.state.rows.map((row) => {
            if (row.documentId === id) {
              const newRow = {
                ...row,
                documentName: filteredRow[0].tempdocumentName,
                receivedStatus: filteredRow[0].tempreceivedStatus,
                fileInformation: filteredRow[0].tempfileInformation,
                isEditMode: !filteredRow[0].isEditMode,
              };
              return newRow;
            }
            return row;
          });
          this.setState({ rows: newData });
        } else {
          this.props.enqueueSnackbar(res.errorMessage, {
            variant: "error",
          });
        }
      } else {
        const res = await API.POST(apiEndpoints.createPostSalesDocument, param);
        if (res.success) {
          const newData = this.state.rows.map((row) => {
            if (row.documentId === id) {
              const newRow = {
                ...row,
                documentId: res.data.documentId,
                documentName: filteredRow[0].tempdocumentName,
                receivedStatus: filteredRow[0].tempreceivedStatus,
                fileInformation: filteredRow[0].tempfileInformation,
                isAddMode: !filteredRow[0].isAddMode,
              };
              return newRow;
            }
            return row;
          });
          this.setState({ rows: newData });
        } else {
          this.props.enqueueSnackbar(res.errorMessage, {
            variant: "error",
          });
        }
      }
    }
  }

  validateRow(id) {
    let isValid = true;
    const newData = this.state.rows.map((row) => {
      if (row.documentId === id) {
        if (
          row.tempdocumentName === undefined ||
          row.tempdocumentName === null ||
          row.tempdocumentName === ""
        ) {
          isValid = false;
          const newRow = { ...row, showError: true };
          return newRow;
        }
        return row;
      }
      return row;
    });
    this.setState({ rows: newData });
    return isValid;
  }

  onCancel(id) {
    if (id > 0) {
      const newData = this.state.rows.map((row) => {
        if (row.documentId === id) {
          const newRow = { ...row, isEditMode: !row.isEditMode };
          return newRow;
        }
        return row;
      });
      this.setState({ rows: newData });
    } else {
      const newData = this.state.rows.filter(function (row) {
        return row.documentId !== id;
      });
      this.setState({ rows: newData });
    }
  }

  onEdit(id) {
    const newData = this.state.rows.map((row) => {
      if (row.documentId === id) {
        const newRow = {
          ...row,
          isEditMode: !row.isEditMode,
          tempdocumentName: row.documentName,
          tempreceivedStatus: row.receivedStatus,
          tempfileInformation: row.fileInformation,
        };
        return newRow;
      }
      return row;
    });
    this.setState({ rows: newData });
  }

  onAdd() {
    const fakeId = this.state.fakeId === undefined ? 0 : this.state.fakeId - 1;
    const newData = this.state.rows;
    newData[newData.length++] = {
      documentId: fakeId,
      tempdocumentName: null,
      tempreceivedStatus: false,
      tempfileInformation: null,
      isAddMode: true,
    };
    this.setState({ rows: newData, fakeId: fakeId });
  }

  async onDownload(file) {
    this.setState({ isLoading: true });
    const response = await instance.get(
      apiEndpoints.fileDownload.replace("{fileid}", file.fileUUId),
      {
        responseType: "blob",
      }
    );
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

  renderCheckBoxClickable(id, status, label, classname, fontsize) {
    return (
      <IconButton
        aria-label={label}
        className={classname}
        onClick={() => {
          const newData = this.state.rows.map((row) => {
            if (row.documentId === id) {
              const newRow = {
                ...row,
                tempreceivedStatus: !row.tempreceivedStatus,
              };
              return newRow;
            }
            return row;
          });
          this.setState({ rows: newData });
        }}
      >
        {status
          ? CheckedIcon({ fontSize: fontsize })
          : UncheckIcon({ fontSize: fontsize })}
      </IconButton>
    );
  }

  renderToggle(value, name, id) {
    return (
      <Switch
        onChange={(e) => {
          const newData = this.state.rows.map((row) => {
            if (row.documentId === id) {
              const newRow = { ...row, tempreceivedStatus: e.target.checked };
              return newRow;
            }
            return row;
          });
          this.setState({ rows: newData });
        }}
        checked={value}
        name={name}
        color="primary"
      />
    );
  }

  renderTextField({
    value,
    id,
    fieldname,
    placeholder,
    type,
    disabled,
    required,
  }) {
    return (
      <TF
        variant="outlined"
        margin="normal"
        required={required}
        fullWidth
        name={fieldname}
        InputLabelProps={{ shrink: true }}
        type={type}
        disabled={disabled}
        label={placeholder}
        onChange={(e) => {
          const newData = this.state.rows.map((row) => {
            if (row.documentId === id) {
              const newRow = {
                ...row,
                tempdocumentName: e.target.value,
                showError: e.target.value === "" ? true : false,
              };
              return newRow;
            }
            return row;
          });
          this.setState({ rows: newData });
        }}
        value={value}
      />
    );
  }
}
export default withRouter(withSnackbar(Table));
