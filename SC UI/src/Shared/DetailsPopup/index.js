import React from "react";
import Dialog from "@material-ui/core/Dialog";
import IconButton from "@material-ui/core/IconButton";
import ArrowBack from "@material-ui/icons/ArrowBack";
import "./style.scss";

/**
 * Wrapper that shows details content in a popup/modal.
 * - Close (x) is handled by the child Details component.
 * - Clicking the backdrop (outside) closes the popup.
 * - Pressing ESC closes the popup.
 * - When showBackButton and onBack are provided, a Back button is shown to return to the previous entity.
 */
function DetailsPopup({ open, onClose, onBack, showBackButton, children }) {
  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="xl"
      fullWidth
      scroll="paper"
      classes={{
        paper: "details-popup-paper",
        paperScrollPaper: "details-popup-scroll",
      }}
      className="details-popup-dialog"
    >
      {showBackButton && onBack && (
        <div className="details-popup-back-bar">
          <IconButton
            aria-label="Back to previous"
            onClick={onBack}
            size="small"
            className="details-popup-back-btn"
          >
            <ArrowBack />
          </IconButton>
          <span className="details-popup-back-label">Back</span>
        </div>
      )}
      <div className="details-popup-content">{children}</div>
    </Dialog>
  );
}

export default DetailsPopup;
