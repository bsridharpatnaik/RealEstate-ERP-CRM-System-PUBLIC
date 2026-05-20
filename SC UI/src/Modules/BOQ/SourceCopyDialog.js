import React, { useState } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  Button, IconButton,
} from '@material-ui/core';
import CloseIcon from '@material-ui/icons/Close';
import Select from 'react-select';
import { API } from '../../axios';
import { apiEndpoints } from '../../endpoints';

const labelStyle = {
  fontSize: '12px', fontWeight: 600, color: '#555',
  marginBottom: '4px', display: 'block',
};

const selectPortal = {
  menuPortalTarget: document.body,
  styles: { menuPortal: b => ({ ...b, zIndex: 9999 }) },
};

const SourceCopyDialog = ({
  open, onClose, buildingTypeData,
  allProductOptions, workAreaOptions, onCopy,
}) => {
  const [buildingType, setBuildingType]       = useState(null);
  const [buildingUnit, setBuildingUnit]       = useState([]);
  const [unitOptions, setUnitOptions]         = useState([]);
  const [preview, setPreview]                 = useState([]);
  const [loading, setLoading]                 = useState(false);
  const [loaded, setLoaded]                   = useState(false);

  const reset = () => {
    setBuildingType(null);
    setBuildingUnit([]);
    setUnitOptions([]);
    setPreview([]);
    setLoaded(false);
  };

  const handleBTChange = async (opt) => {
    setBuildingType(opt);
    setBuildingUnit([]);
    setPreview([]);
    setLoaded(false);
    if (!opt) { setUnitOptions([]); return; }
    const r = await API.GET(apiEndpoints.getBuildingUnit + opt.value);
    if (r.success) {
      setUnitOptions(r.data.usageLocation.map(u => ({ value: u.id, label: u.name })));
    }
  };

  const handleBUChange = (opt) => {
    setBuildingUnit(opt);
    setPreview([]);
    setLoaded(false);
  };

  const handleLoad = async () => {
    if (!buildingUnit.length) return;
    setLoading(true);
    const allRows = [];
    const seen = new Set();
    let globalIdx = 0;
    for (const unit of buildingUnit) {
      const r = await API.GET(apiEndpoints.boqUploadByUnit + unit.value);
      if (!r.success) continue;
      for (const item of (r.data || [])) {
        const productOpt = allProductOptions.find(p => p.label === item.product?.productName)
          || (item.product ? { value: item.product.productId, label: item.product.productName } : null);
        const workAreaOpt = workAreaOptions.find(w => w.label === item.location?.usageAreaName)
          || (item.location ? { value: item.location.usageAreaId, label: item.location.usageAreaName } : null);
        const dedupeKey = `${productOpt?.label || ''}__${workAreaOpt?.label || ''}`;
        if (seen.has(dedupeKey)) continue;
        seen.add(dedupeKey);
        allRows.push({
          _copyId: `copy_${item.id}_${globalIdx++}`,
          category: item.product?.category?.categoryName || null,
          product: productOpt,
          workArea: workAreaOpt,
          quantity: String(item.quantity || ''),
          wastagePercent: String(item.wastagePercent || '0'),
          remark: '',
          source: unit.label,
        });
      }
    }
    setLoading(false);
    setPreview(allRows);
    setLoaded(true);
  };

  const removeRow = (copyId) => setPreview(prev => prev.filter(r => r._copyId !== copyId));

  const handleConfirm = () => {
    onCopy(preview);
    reset();
    onClose();
  };

  const th = {
    background: '#f5f5f5', padding: '8px 10px',
    border: '1px solid #e0e0e0', textAlign: 'left', fontWeight: 600, fontSize: '13px',
  };
  const td = { padding: '7px 10px', border: '1px solid #e0e0e0', fontSize: '13px' };

  return (
    <Dialog open={open} onClose={() => { reset(); onClose(); }} maxWidth="md" fullWidth>
      <DialogTitle style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span>Copy from Existing BOQ</span>
        <IconButton size="small" onClick={() => { reset(); onClose(); }}>
          <CloseIcon fontSize="small" />
        </IconButton>
      </DialogTitle>

      <DialogContent dividers>
        <div style={{ display: 'flex', gap: '16px', marginBottom: '20px', flexWrap: 'wrap', alignItems: 'flex-end' }}>
          <div style={{ flex: 1, minWidth: '200px' }}>
            <label style={labelStyle}>Structure Type</label>
            <Select
              options={buildingTypeData}
              value={buildingType}
              onChange={handleBTChange}
              placeholder="Select Structure Type"
              isSearchable
              {...selectPortal}
            />
          </div>
          <div style={{ flex: 1, minWidth: '200px' }}>
            <label style={labelStyle}>Structure</label>
            <Select
              options={unitOptions}
              value={buildingUnit}
              onChange={handleBUChange}
              placeholder={buildingType ? 'Select one or more structures' : 'Select Type first'}
              isDisabled={!buildingType}
              isMulti
              isSearchable
              {...selectPortal}
            />
          </div>
          <div>
            <button
              onClick={handleLoad}
              disabled={!buildingUnit.length || loading}
              style={{
                padding: '8px 20px', borderRadius: '6px', fontSize: '13px', fontWeight: 500,
                border: '1px solid #1976d2', cursor: buildingUnit.length ? 'pointer' : 'not-allowed',
                background: buildingUnit.length ? '#1976d2' : '#e0e0e0',
                color: buildingUnit.length ? '#fff' : '#999',
              }}
            >
              {loading ? 'Loading…' : 'Load BOQ'}
            </button>
          </div>
        </div>

        {loaded && preview.length === 0 && (
          <div style={{ textAlign: 'center', color: '#888', padding: '24px', fontSize: '13px' }}>
            No BOQ records found for this structure.
          </div>
        )}

        {preview.length > 0 && (
          <>
            <div style={{ fontSize: '13px', color: '#555', marginBottom: '8px' }}>
              <strong>{preview.length}</strong> row{preview.length !== 1 ? 's' : ''} found.
              Remove any you don't want to copy.
            </div>
            <div style={{ overflowX: 'auto' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <thead>
                  <tr>
                    <th style={th}>Product / Inventory</th>
                    <th style={th}>Work Area</th>
                    <th style={{ ...th, width: 100 }}>Quantity</th>
                    <th style={{ ...th, width: 100 }}>Wastage %</th>
                    <th style={th}>Structure</th>
                    <th style={{ ...th, width: 80 }}></th>
                  </tr>
                </thead>
                <tbody>
                  {preview.map(row => (
                    <tr key={row._copyId}>
                      <td style={td}>{row.category ? `${row.category} — ${row.product?.label || '—'}` : (row.product?.label || '—')}</td>
                      <td style={td}>{row.workArea?.label || '—'}</td>
                      <td style={td}>{row.quantity}</td>
                      <td style={td}>{row.wastagePercent || '0'}</td>
                      <td style={{ ...td, textAlign: 'center' }}>
                        <Button size="small" color="secondary" onClick={() => removeRow(row._copyId)}>
                          Remove
                        </Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </DialogContent>

      <DialogActions style={{ padding: '12px 24px' }}>
        <Button onClick={() => { reset(); onClose(); }}>Cancel</Button>
        <Button
          onClick={handleConfirm}
          color="primary"
          variant="contained"
          disabled={preview.length === 0}
        >
          Copy {preview.length > 0 ? preview.length : ''} Row{preview.length !== 1 ? 's' : ''} to Preview
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default SourceCopyDialog;
