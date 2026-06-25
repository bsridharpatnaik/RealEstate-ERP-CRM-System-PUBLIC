import React, { Component } from "react";
import Dialog from "@material-ui/core/Dialog";
import { apiEndpoints } from "./../../endpoints";
import { API } from "./../../axios";

const IMAGE_EXTENSIONS = ["jpg", "jpeg", "png", "gif", "webp", "bmp"];

export function isImageFile(fileName) {
  if (!fileName) return false;
  const ext = fileName.split(".").pop().toLowerCase();
  return IMAGE_EXTENSIONS.includes(ext);
}

// Small clickable thumbnail for image attachments — fetches the file as a blob (the
// download endpoint requires the Authorization header, so a plain <img src> can't be
// used directly) and opens full-size in a lightbox on click. Renders nothing for
// non-image files; callers keep showing the filename/download-icon row as before.
class AttachmentThumbnail extends Component {
  state = { objectUrl: null, lightboxOpen: false, failed: false };

  componentDidMount() {
    this.loadPreview();
  }

  componentWillUnmount() {
    if (this.state.objectUrl) {
      window.URL.revokeObjectURL(this.state.objectUrl);
    }
  }

  async loadPreview() {
    const { file, downloadUrl = apiEndpoints.download, buildUrl } = this.props;
    if (!isImageFile(file.fileName) || !file.fileUUId) return;
    const url = buildUrl ? buildUrl(file) : downloadUrl + file.fileUUId;
    try {
      const response = await API.GET(url, {
        responseType: "blob",
      });
      if (response.status === 200) {
        const objectUrl = window.URL.createObjectURL(response.data);
        this.setState({ objectUrl });
      } else {
        this.setState({ failed: true });
      }
    } catch (e) {
      // Preview is a nice-to-have — fall back to filename-only display rather than
      // leaving a perpetual loading placeholder when the file can't be fetched.
      this.setState({ failed: true });
    }
  }

  render() {
    const { file, size = 36 } = this.props;
    if (!isImageFile(file.fileName)) return null;
    const { objectUrl, lightboxOpen, failed } = this.state;

    if (failed) return null;

    if (!objectUrl) {
      return (
        <div
          className="attachment-thumbnail attachment-thumbnail-loading"
          style={{ width: size, height: size, borderRadius: 4, background: "#f0f0f0", flexShrink: 0 }}
        />
      );
    }

    return (
      <>
        <img
          src={objectUrl}
          alt={file.fileName}
          className="attachment-thumbnail"
          style={{ width: size, height: size, objectFit: "cover", borderRadius: 4, cursor: "pointer", flexShrink: 0 }}
          onClick={(e) => {
            e.stopPropagation();
            this.setState({ lightboxOpen: true });
          }}
        />
        <Dialog
          open={lightboxOpen}
          onClose={() => this.setState({ lightboxOpen: false })}
          maxWidth="md"
        >
          <img
            src={objectUrl}
            alt={file.fileName}
            style={{ maxWidth: "90vw", maxHeight: "90vh", display: "block" }}
          />
        </Dialog>
      </>
    );
  }
}

export default AttachmentThumbnail;
