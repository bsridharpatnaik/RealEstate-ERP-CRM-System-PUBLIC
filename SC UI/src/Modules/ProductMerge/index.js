import React, { Component } from "react";
import {
  Button,
  CircularProgress,
  Paper,
  Step,
  StepLabel,
  Stepper,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from "@material-ui/core";
import Autocomplete from "@material-ui/lab/Autocomplete";
import { API } from "../../axios";
import { apiEndpoints } from "../../endpoints";
import "./style.scss";

const STEPS = ["Select Products", "Preview Impact", "Confirm & Execute", "Result"];

class ProductMerge extends Component {
  state = {
    activeStep: 0,
    allProducts: [],
    sourceProduct: null,
    targetProduct: null,
    preview: null,
    result: null,
    loading: false,
    error: null,
    confirmText: "",
  };

  componentDidMount() {
    this.loadProducts();
  }

  loadProducts = async () => {
    const res = await API.GET(apiEndpoints.getProductForDropdown);
    if (res.success && Array.isArray(res.data)) {
      const allProducts = res.data.map((p) => ({
        id: p.productId,
        name: p.productName,
        productCode: p.productCode || "",
        measurementUnit: p.measurementUnit || "",
      }));
      this.setState({ allProducts });
    }
  };

  handlePreview = async () => {
    const { sourceProduct, targetProduct } = this.state;
    if (!sourceProduct || !targetProduct) {
      this.setState({ error: "Please select both products." });
      return;
    }
    if (sourceProduct.id === targetProduct.id) {
      this.setState({ error: "Source and target cannot be the same product." });
      return;
    }
    this.setState({ loading: true, error: null });
    const res = await API.POST(apiEndpoints.productMergePreview, {
      sourceProductId: sourceProduct.id,
      targetProductId: targetProduct.id,
    });
    this.setState({ loading: false });
    if (res.success) {
      this.setState({ preview: res.data, activeStep: 1 });
    } else {
      this.setState({ error: res.errorMessage || "Failed to load preview." });
    }
  };

  handleExecute = async () => {
    const { sourceProduct, targetProduct, confirmText } = this.state;
    if (confirmText !== sourceProduct.name) {
      this.setState({ error: "Product name does not match. Please type exactly as shown." });
      return;
    }
    this.setState({ loading: true, error: null, activeStep: 2 });
    const res = await API.POST(apiEndpoints.productMergeExecute, {
      sourceProductId: sourceProduct.id,
      targetProductId: targetProduct.id,
    });
    this.setState({ loading: false });
    if (res.success) {
      this.setState({ result: res.data, activeStep: 3 });
    } else {
      this.setState({ error: res.errorMessage || "Merge failed.", activeStep: 1 });
    }
  };

  totalUsage = (summary) =>
    summary.stockEntries +
    summary.stockHistoryEntries +
    summary.inwardOutwardEntries +
    summary.rejectInwardEntries +
    summary.rejectOutwardEntries +
    summary.returnOutwardEntries +
    summary.transferItemEntries +
    summary.lostDamagedEntries +
    summary.boqUploadEntries +
    summary.boqInventoryEntries +
    summary.boqHistoryEntries +
    summary.pricingEntries;

  renderStep0() {
    const { allProducts, sourceProduct, targetProduct, error } = this.state;
    return (
      <div className="merge-step">
        <Typography variant="h6" gutterBottom>
          Select the products to merge
        </Typography>
        <Typography variant="body2" color="textSecondary" gutterBottom>
          The <strong>source product</strong> will be deleted. All its records will be
          reassigned to the <strong>target product</strong>.
        </Typography>

        <div className="merge-product-selectors">
          <Paper className="merge-product-card source-card" elevation={2}>
            <Typography variant="subtitle1" className="card-label source-label">
              Source Product (will be deleted)
            </Typography>
            <Autocomplete
              options={allProducts}
              getOptionLabel={(opt) => opt ? `${opt.name} (${opt.productCode})` : ""}
              getOptionSelected={(opt, val) => opt.id === val.id}
              value={sourceProduct}
              onChange={(_, val) => this.setState({ sourceProduct: val, error: null })}
              renderInput={(params) => (
                <TextField {...params} label="Search source product" variant="outlined" fullWidth />
              )}
            />
            {sourceProduct && (
              <div className="product-info-box">
                <div><span>Name:</span> {sourceProduct.name}</div>
                <div><span>Code:</span> {sourceProduct.productCode}</div>
                <div><span>Unit:</span> {sourceProduct.measurementUnit}</div>
              </div>
            )}
          </Paper>

          <div className="merge-arrow">→</div>

          <Paper className="merge-product-card target-card" elevation={2}>
            <Typography variant="subtitle1" className="card-label target-label">
              Target Product (will be kept)
            </Typography>
            <Autocomplete
              options={allProducts}
              getOptionLabel={(opt) => opt ? `${opt.name} (${opt.productCode})` : ""}
              getOptionSelected={(opt, val) => opt.id === val.id}
              value={targetProduct}
              onChange={(_, val) => this.setState({ targetProduct: val, error: null })}
              renderInput={(params) => (
                <TextField {...params} label="Search target product" variant="outlined" fullWidth />
              )}
            />
            {targetProduct && (
              <div className="product-info-box">
                <div><span>Name:</span> {targetProduct.name}</div>
                <div><span>Code:</span> {targetProduct.productCode}</div>
                <div><span>Unit:</span> {targetProduct.measurementUnit}</div>
              </div>
            )}
          </Paper>
        </div>

        {error && <Typography color="error" className="merge-error">{error}</Typography>}

        <div className="merge-actions">
          <Button
            variant="contained"
            color="primary"
            onClick={this.handlePreview}
            disabled={this.state.loading}
          >
            {this.state.loading ? <CircularProgress size={20} /> : "Preview Impact"}
          </Button>
        </div>
      </div>
    );
  }

  renderStep1() {
    const { preview, sourceProduct, targetProduct, confirmText, error, loading } = this.state;
    if (!preview) return null;

    return (
      <div className="merge-step">
        <Typography variant="h6" gutterBottom>
          Review impact before merging
        </Typography>
        <Typography variant="body2" color="textSecondary" gutterBottom>
          The following records will be reassigned from{" "}
          <strong>{preview.sourceProduct.productName}</strong> →{" "}
          <strong>{preview.targetProduct.productName}</strong>
        </Typography>

        <Typography variant="subtitle2" style={{ marginTop: 12, marginBottom: 4 }}>
          Project-specific records (per site)
        </Typography>
        <div className="preview-table-wrapper">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell><strong>Site</strong></TableCell>
                <TableCell align="right"><strong>Stock</strong></TableCell>
                <TableCell align="right"><strong>Inward/Outward</strong></TableCell>
                <TableCell align="right"><strong>BOQ</strong></TableCell>
                <TableCell align="right"><strong>Others</strong></TableCell>
                <TableCell align="right"><strong>Total</strong></TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {preview.tenantSummaries.map((s) => (
                <TableRow key={s.tenantSchema}>
                  <TableCell>{s.tenantSchema}</TableCell>
                  <TableCell align="right">{s.stockEntries + s.stockHistoryEntries}</TableCell>
                  <TableCell align="right">{s.inwardOutwardEntries + s.rejectInwardEntries + s.rejectOutwardEntries + s.returnOutwardEntries}</TableCell>
                  <TableCell align="right">{s.boqUploadEntries + s.boqInventoryEntries + s.boqHistoryEntries}</TableCell>
                  <TableCell align="right">{s.transferItemEntries + s.lostDamagedEntries + s.pricingEntries}</TableCell>
                  <TableCell align="right"><strong>{this.totalUsage(s)}</strong></TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>

        {preview.globalSummary && (
          <>
            <Typography variant="subtitle2" style={{ marginTop: 20, marginBottom: 4 }}>
              Global records (shared across all sites)
            </Typography>
            <div className="preview-table-wrapper">
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell><strong>Indents</strong></TableCell>
                    <TableCell align="right"><strong>Purchase Orders</strong></TableCell>
                    <TableCell align="right"><strong>Total</strong></TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  <TableRow>
                    <TableCell>{preview.globalSummary.indentEntries}</TableCell>
                    <TableCell align="right">{preview.globalSummary.purchaseOrderLineEntries}</TableCell>
                    <TableCell align="right">
                      <strong>{preview.globalSummary.indentEntries + preview.globalSummary.purchaseOrderLineEntries}</strong>
                    </TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </div>
          </>
        )}

        <div className="confirm-section">
          <Typography variant="body1" className="confirm-warning">
            ⚠ This action <strong>cannot be undone</strong>. Type the source product name to confirm:
            <br />
            <code>{sourceProduct && sourceProduct.name}</code>
          </Typography>
          <TextField
            variant="outlined"
            size="small"
            label="Type product name to confirm"
            value={confirmText}
            onChange={(e) => this.setState({ confirmText: e.target.value, error: null })}
            fullWidth
            className="confirm-input"
          />
        </div>

        {error && <Typography color="error" className="merge-error">{error}</Typography>}

        <div className="merge-actions">
          <Button
            variant="outlined"
            onClick={() => this.setState({ activeStep: 0, confirmText: "", error: null })}
            style={{ marginRight: 12 }}
          >
            Back
          </Button>
          <Button
            variant="contained"
            color="secondary"
            onClick={this.handleExecute}
            disabled={loading || confirmText !== (sourceProduct && sourceProduct.name)}
          >
            {loading ? <CircularProgress size={20} /> : "Execute Merge"}
          </Button>
        </div>
      </div>
    );
  }

  renderStep2() {
    return (
      <div className="merge-step merge-in-progress">
        <CircularProgress />
        <Typography variant="h6" style={{ marginTop: 16 }}>
          Merge in progress…
        </Typography>
        <Typography variant="body2" color="textSecondary">
          Processing all sites. Please wait.
        </Typography>
      </div>
    );
  }

  renderStep3() {
    const { result } = this.state;
    if (!result) return null;

    const failed = result.tenantResults.filter((r) => !r.success);

    return (
      <div className="merge-step">
        <Typography
          variant="h6"
          style={{ color: result.overallSuccess ? "green" : "#e65100" }}
          gutterBottom
        >
          {result.overallSuccess ? "✓ Merge Completed Successfully" : "⚠ Merge Completed with Errors"}
        </Typography>
        <Typography variant="body2" gutterBottom>{result.message}</Typography>

        {failed.length > 0 && (
          <>
            <Typography variant="subtitle2" style={{ marginTop: 12, marginBottom: 4 }}>
              Failed sites (retry to reprocess):
            </Typography>
            <div className="result-table-wrapper">
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell><strong>Site</strong></TableCell>
                    <TableCell><strong>Error</strong></TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {failed.map((r) => (
                    <TableRow key={r.tenantSchema}>
                      <TableCell>{r.tenantSchema}</TableCell>
                      <TableCell style={{ fontSize: 12, color: "#c62828" }}>{r.errorMessage}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </>
        )}

        <div className="merge-actions" style={{ marginTop: 24 }}>
          <Button
            variant="contained"
            color="primary"
            onClick={() =>
              this.setState({
                activeStep: 0,
                sourceProduct: null,
                targetProduct: null,
                preview: null,
                result: null,
                confirmText: "",
                error: null,
              })
            }
          >
            Merge Another Product
          </Button>
        </div>
      </div>
    );
  }

  render() {
    const { activeStep } = this.state;

    return (
      <div className="page product-merge-page">
        <div className="header-info">
          <Typography variant="h5">Merge Products</Typography>
        </div>
        <Typography variant="body2" color="textSecondary" style={{ marginBottom: 16 }}>
          Consolidate duplicate products into one. Admin only.
        </Typography>

        <Paper className="merge-container" elevation={1}>
          <Stepper activeStep={activeStep} alternativeLabel>
            {STEPS.map((label) => (
              <Step key={label}>
                <StepLabel>{label}</StepLabel>
              </Step>
            ))}
          </Stepper>

          <div className="merge-content">
            {activeStep === 0 && this.renderStep0()}
            {activeStep === 1 && this.renderStep1()}
            {activeStep === 2 && this.renderStep2()}
            {activeStep === 3 && this.renderStep3()}
          </div>
        </Paper>
      </div>
    );
  }
}

export default ProductMerge;
