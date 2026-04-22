import React, { Component } from "react";
import TextField from "./../../../../Shared/TextField";
import Upload from "./../UploadFile";
import Button from "./../../../../Shared/Button";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import Checkbox from "@material-ui/core/Checkbox";
import { API } from "./../../../../axios";
import { apiEndpoints } from "./../../../../endpoints";
import { withSnackbar } from "notistack";

import "./style.scss";
class NoteTab extends Component {
  state = { editData: undefined };
  data = {
    pinned: false,
    content: "",
  };
  constructor(props) {
    super(props);
    this.uploadRef = React.createRef();
  }
  componentDidMount() {
    if (this.props.editData !== this.state.editData) {
      this.setFormData();
    }
  }
  componentDidUpdate(prevProps) {
    if (
      prevProps.editData !== this.props.editData &&
      this.props.editData !== this.state.editData
    ) {
      this.setFormData();
    }
  }
  setFormData() {
    this.data.pinned = this.props.editData ? this.props.editData.pinned : "";

    this.data.content = this.props.editData ? this.props.editData.content : "";
    this.setState({ editData: this.props.editData });
  }
  prepareRequest() {
    const fileInformations = [];
    for (const [key, value] of Object.entries(
      this.uploadRef.current.state.uploadProgress
    )) {
      if (value.fileUUId) {
        fileInformations.push({
          fileUUId: value.fileUUId,
          fileName: key,
        });
      }
    }
    const params = {
      ...this.data,
      fileInformations,
      leadId: this.props.leadId,
    };
    return params;
  }
  update() {}
  submit = async () => {
    const params = this.prepareRequest();
    if (this.props.editData) {
      const response = await API.PUT(
        apiEndpoints.updateNote + this.props.editData.noteId,
        params
      );
      if (response.success) {
        this.props.refresh();

        this.props.enqueueSnackbar("Note Updated Successfully", {
          variant: "success",
        });
      } else {
        this.props.enqueueSnackbar(response.errorMessage, {
          variant: "error",
        });
      }
    } else {
      const response = await API.POST(apiEndpoints.createNote, params);

      if (response.success) {
        this.props.refresh();

        this.props.enqueueSnackbar("Note added Successfully", {
          variant: "success",
        });
      } else {
        this.props.enqueueSnackbar(response.errorMessage, {
          variant: "error",
        });
      }
    }
  };
  render() {
    return (
      <div className="note-tab">
        <TextField
          key={this.state.editData ? this.state.editData.noteId : 0}
          label="Description"
          multiline={true}
          fieldname="description"
          onChange={(e) => {
            this.data.content = e;
            this.setState({ formValid: true });
          }}
          placeholderText="Write your note.."
          defaultValue={
            this.state.editData ? this.state.editData.content : undefined
          }
        />
        <Upload
          ref={this.uploadRef}
          files={
            this.state.editData
              ? this.state.editData.fileInformations
              : undefined
          }
        />
        <div className="button-section">
          <FormControlLabel
            className="password-checkbox"
            control={
              <Checkbox
                name="checkedB"
                color="primary"
                onChange={(event) => {
                  this.data.pinned = event.target.checked;
                }}
                disabled={!this.data.content.length}
              />
            }
            label="Pinned Note"
          />
          <Button
            type="submit"
            buttonClass="blue"
            label={this.state.editData ? "Update" : "ADD"}
            onClick={() => this.submit()}
            disabled={!this.data.content.length}
          />
        </div>
      </div>
    );
  }
}

export default withSnackbar(NoteTab);
