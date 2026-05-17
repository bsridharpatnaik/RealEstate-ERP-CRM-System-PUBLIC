import React, { Component } from "react";
import "./style.scss";
import { SvgIcon } from "@material-ui/core";
import { ReactComponent as CloseSVG } from "./../Progress/close.svg";
import { ReactComponent as EditSVG } from "./edit (1).svg";
import { API } from "./../../../../axios";
import { apiEndpoints } from "./../../../../endpoints";
import { withSnackbar } from "notistack";
import { ReactComponent as AttachSVG } from "./../UploadFile/attach.svg";
import moment from "moment";
import PinImg from "./pin.svg";
import RegImg from "./register.svg";
import { constants } from "./../../../../messages";

function EditIcon(props) {
  return (
    <SvgIcon {...props}>
      <EditSVG />
    </SvgIcon>
  );
}
function CrossIcon(props) {
  return (
    <SvgIcon {...props}>
      <CloseSVG />
    </SvgIcon>
  );
}
class Note extends Component {
  async unpinnote() {
    const params = {
      ...this.formData,
      leadId: this.props.leadId,
    };
    const response = await API.PATCH(
      apiEndpoints.unpinNote + this.props.data.noteId,
      params
    );
    if (response.success) {
      this.props.refresh();
      this.props.enqueueSnackbar("Note Unpinned Successfully", {
        variant: "success",
      });
    } else {
      this.props.enqueueSnackbar(response.errorMessage, {
        variant: "error",
      });
    }
  }
  render() {
    const m = moment(this.props.data.updated, constants.dateFormat);
    const date = m.locale("en").format("lll");
    const data = this.props.data;
    return (
      <div className={this.props.data.pinned ? "note pinned" : "note"}>
        {this.props.data.pinned && (
          <div className="actions">
            <EditIcon onClick={() => this.props.edit()} />
            <CrossIcon onClick={() => this.unpinnote()} />
          </div>
        )}
        <div>
          <img
            className="pin-icon"
            alt=""
            src={data.pinned ? PinImg : RegImg}
          />
        </div>
        <div>
          <span className="date">{date}</span>
          <div className="note-content">{data.content}</div>
          {data.fileInformations && data.fileInformations.length ? (
            <div
              className="view-attachment-btn"
              onClick={() => this.props.viewAttachments(data)}
            >
              <AttachSVG className="attach-icon" />
              View Attachments
            </div>
          ) : (
            ""
          )}
        </div>
      </div>
    );
  }
}
export default withSnackbar(Note);
