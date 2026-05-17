//react
import React from "react";
import Dialog from "@material-ui/core/Dialog";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import IconButton from "@material-ui/core/IconButton";
import CloseIcon from "@material-ui/icons/Close";
import TextField from "@material-ui/core/TextField";
import Button from "@material-ui/core/Button";

class AddNotesModal extends React.Component {
  state = {
    noteTitle: "",
    notes: "",
    errors: {},
  };

  handleInputChange = (field) => (event) => {
    this.setState({ [field]: event.target.value });
  };

  validate = () => {
    const errors = {};
    if (!this.state.noteTitle.trim()) {
      errors.noteTitle = "Required";
    }
    if (!this.state.notes.trim()) {
      errors.notes = "Required";
    }

    this.setState({ errors });
    return Object.keys(errors).length === 0;
  };

  handleSave = () => {
    if (this.validate()) {
      const note = {
        title: this.state.noteTitle,
        text: this.state.notes,
      };
      this.props.onSave(note);
    }
  };

  handleClose = () => {
    this.setState({
      noteTitle: "",
      notes: "",
      errors: {},
    });
    this.props.onClose();
  };

  render() {
    return (
      <Dialog
        open={this.props.open}
        onClose={this.handleClose}
        maxWidth="sm"
        fullWidth
        classes={{ paper: "notes-modal" }}
      >
        <DialogTitle>
          <div className="modal-header">
            Add Notes
            <IconButton
              aria-label="close"
              onClick={this.handleClose}
              className="close-button"
            >
              <CloseIcon />
            </IconButton>
          </div>
        </DialogTitle>
        <DialogContent>
          <div className="notes-form">
            <TextField
              label="Note Title"
              variant="outlined"
              fullWidth
              value={this.state.noteTitle}
              onChange={this.handleInputChange("noteTitle")}
              placeholder="Enter"
              error={!!this.state.errors.noteTitle}
              helperText={this.state.errors.noteTitle}
              size="small"
              style={{ marginBottom: "16px" }}
            />
            <TextField
              label="Notes"
              variant="outlined"
              fullWidth
              multiline
              rows={6}
              value={this.state.notes}
              onChange={this.handleInputChange("notes")}
              placeholder="Enter"
              error={!!this.state.errors.notes}
              helperText={this.state.errors.notes}
            />
          </div>
        </DialogContent>
        <DialogActions>
          <Button onClick={this.handleClose} color="default">
            Cancel
          </Button>
          <Button onClick={this.handleSave} color="primary" variant="contained">
            Save
          </Button>
        </DialogActions>
      </Dialog>
    );
  }
}

export default AddNotesModal;
