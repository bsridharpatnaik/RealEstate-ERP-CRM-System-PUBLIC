import React, { Component } from "react";
import "./boqtable.scss";
import { EditIcon, DeleteIcon } from "./../../Shared/Icons/Index.js";
import IconButton from "@material-ui/core/IconButton";
import DialogTitle from "@material-ui/core/DialogTitle";
import DialogContent from "@material-ui/core/DialogContent";
import DialogActions from "@material-ui/core/DialogActions";
import Dialog from "@material-ui/core/Dialog";
import Button from "./../../Shared/Button";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";
import CloseIcon from "@material-ui/icons/Close";
import TextField from "@material-ui/core/TextField";
import TF from "./../../Shared/TextField";
import { messages } from "./../../messages";
import CheckIcon from "@material-ui/icons/Check";
import { withSnackbar } from "notistack";
import { connect } from "react-redux";
import Pagination from "@material-ui/lab/Pagination";
import AddIcon from "@material-ui/icons/Add";
import Autocomplete from "@material-ui/lab/Autocomplete";
import Skeleton from "@material-ui/lab/Skeleton";

class BOQTable extends Component {
  state = { editRow: null, inventory: null, quantity: null, addQuantity: null };
  editQuantity;
  async deleteBOQ() {
    const response = await API.DELETE(
      apiEndpoints.deleteBOQ + this.deleteRow.entryId
    );
    if (response.success) {
      this.props.refresh();
    }
  }
  async updateBOQ() {
    if (!this.editQuantity) {
      this.props.enqueueSnackbar("Enter Quantity", {
        variant: "error",
      });
    }
    const params = {
      productId: this.state.editRow.product.productId,
      quantity: Number(this.editQuantity),
    };
    const response = await API.PUT(
      apiEndpoints.deleteBOQ + this.state.editRow.entryId,
      params
    );
    if (response.success) {
      this.props.refresh();
    }else{
       this.props.enqueueSnackbar(response.errorMessage, {
         variant: "error",
       });
    }
  }
  async getProductListType() {
    const type = this.props.selectedUnit ? "BuildingUnit" : "BuildingType";
    const id = this.props.selectedUnit
      ? this.props.selectedUnit.id
      : this.props.selectedType.id;
    const response = await API.GET(
      apiEndpoints.productListType + id + "?boqtype=" + type
    );
    if (response.success) {
      this.setState({ productlist: response.data });
    }
  }
  renderRow(row, index) {
    const isEditRow = this.state.editRow === row;
    return (
      <div key={index} className="table-row inv">
        <div className="inv-name">{row.product.productName}</div>
        <div className="inv-quantity">
          {isEditRow ? (
            <TF
              variant="standard"
              className="no-border-select"
              margin="dense"
              required
              fullWidth
              type="number"
              validation={this.props.selectedUnit ? undefined : "positive"}
              placeholderText={"Enter Quantity"}
              onChange={(e) => (this.editQuantity = e)}
            />
          ) : (
            row.quantity
          )}
        </div>
        <div className="inv-unit">{row.product.measurementUnit}</div>
        <div className="actions">
          <IconButton
            aria-label="back"
            onClick={() => {
              if (isEditRow) {
                this.updateBOQ();
              } else {
                this.editQuantity = null;
                this.setState({ editRow: row });
                this.clearAdd();
              }
            }}
            className="back-icon"
          >
            {isEditRow ? <CheckIcon /> : EditIcon({ fontSize: "medium" })}
          </IconButton>
          <IconButton
            aria-label="back"
            onClick={() => {
              if (isEditRow) {
                this.setState({ editRow: null });
                this.clearAdd();
              } else {
                this.deleteRow = row;
                this.setState({ deleteConfirmOpen: true });
              }
            }}
            className="back-icon"
          >
            {isEditRow ? <CloseIcon /> : DeleteIcon({ fontSize: "medium" })}
          </IconButton>
        </div>
      </div>
    );
  }
  async add() {
    let id;
    if (this.props.selectedUnit) {
      id = this.props.selectedUnit.id;
    } else {
      id = this.props.selectedType.id;
    }
    const params = {
      productId: this.state.inventory.id,
      quantity: Number(this.state.addQuantity),
      id: id,
      boqType: this.props.selectedUnit ? "BuildingUnit" : "BuildingType",
    };
    const response = await API.POST(apiEndpoints.boqadd, params);
    if (response.success) {
      this.props.refresh();
    }else{
       this.props.enqueueSnackbar(response.errorMessage, {
         variant: "error",
       });
    }
  }
  clearAdd() {
    this.setState({ addQuantity: null, inventory: null, isAdd: false });
  }
  renderAdd() {
    return (
      <div className="table-row inv">
        <div className="inv-name">
          <Autocomplete
            className="no-border-select"
            disableClearable
            options={this.state.productlist}
            onChange={(e, value) => {
              this.setState({ inventory: value });
            }}
            getOptionLabel={(option) => option.name}
            filterSelectedOptions={true}
            renderInput={(params) => (
              <TextField
                {...params}
                margin="normal"
                placeholder={"Select Inventory"}
                InputLabelProps={{ shrink: true }}
              />
            )}
          />
        </div>
        <div className="inv-quantity">
          <TF
            variant="standard"
            className="no-border-select"
            margin="dense"
            required
            fullWidth
            type="number"
            validation={this.props.selectedUnit ? undefined : "positive"}
            name="quantity"
            placeholderText={"Enter Quantity"}
            onChange={(e) => this.setState({ addQuantity: e })}
          />
        </div>
        <div className="inv-unit">
          <TextField
            className="no-border-select"
            disabled
            required
            fullWidth
            placeholder={"Enter Unit"}
            value={
              this.state.inventory
                ? this.props.units[this.state.inventory.id]
                : null
            }
          />
        </div>
        <div className="actions add">
          <Button
            label="Save"
            buttonClass="boq-save"
            onClick={() => this.add()}
            disabled={!(this.state.inventory && this.state.addQuantity)}
          />
          <Button
            label="Cancel"
            buttonClass="boq-cancel"
            onClick={() => this.clearAdd()}
          />
        </div>
      </div>
    );
  }
  render() {
    const data = this.props.data.content || [];
    return (
      <div className="boq-table-wrapper">
        <Dialog
          disableBackdropClick
          disableEscapeKeyDown
          maxWidth="xs"
          aria-labelledby="confirmation-dialog-title"
          open={this.state.deleteConfirmOpen}
        >
          <DialogTitle id="confirmation-dialog-title">Confirm</DialogTitle>
          <DialogContent dividers>
            Are you sure you want to delete this record?
          </DialogContent>
          <DialogActions>
            <Button
              onClick={() => {
                this.deleteRow = null;
                this.setState({ deleteConfirmOpen: false });
              }}
              buttonClass="grey"
              label="Cancel"
            ></Button>
            <Button
              onClick={() => {
                this.deleteBOQ(this.deleteRow);
                this.setState({ deleteConfirmOpen: false });
              }}
              buttonClass="blue"
              label="Confirm"
            ></Button>
          </DialogActions>
        </Dialog>

        <div className="table-title">
          Statistics
          <Button
            color="primary"
            variant="contained"
            buttonClass={"boq"}
            disabled={this.state.isAdd}
            onClick={() => {
              this.getProductListType();
              this.setState({ isAdd: true, editRow: null });
            }}
            label={"Add"}
          ></Button>
        </div>

        <div className="table-scroll-wrapper">
          <div>
            <div className="boq-table-heading">
              <div className="inv-name">Inventory</div>
              <div className="inv-quantity">Inventory Limit</div>
              <div className="inv-unit">Measurement Unit</div>
              <div className="actions">
                <div />
              </div>
            </div>
            {this.state.isAdd && this.renderAdd()}
            {this.props.isLoading ? (
              <>
                <Skeleton variant="rect" height={50} />
              </>
            ) : (
              <>
                {data.length ? (
                  data.map((row, index) => this.renderRow(row, index))
                ) : (
                  <div className="table-row inv nodata">No data</div>
                )}
              </>
            )}
          </div>
        </div>

        <div className="pagination">
          <form
            className="go-to-form"
            onSubmit={(e) => {
              e.preventDefault();
              e.stopPropagation();
              const value = Number(this.inputRef.current.value);
              if (value >= 1 && value <= this.state.pages) {
                this.search(value - 1);
              }
            }}
          >
            <div className="goto-page-label">
              {/* <span>Go to page:</span>
              <TextField
                defaultValue={this.page + 1}
                variant="outlined"
                type="number"
                onBlur={() => {
                  this.inputRef.current.value = Number(this.page) + 1;
                }}
                InputProps={{ inputProps: { min: 1, max: this.state.pages } }}
                max={this.state.pages}
                inputRef={this.inputRef}
              /> */}
            </div>
          </form>
          <Pagination
            count={this.props.data.totalPages}
            variant="outlined"
            shape="rounded"
            onChange={(e, page) => this.props.refresh(page - 1)}
            page={this.page + 1}
          />
        </div>
      </div>
    );
  }
}
export default connect(null, null, null, { forwardRef: true })(
  withSnackbar(BOQTable)
);
