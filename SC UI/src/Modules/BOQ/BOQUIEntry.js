import React, { useState, useEffect } from 'react';
import { IconButton, TextField } from '@material-ui/core';
import DeleteOutlineIcon from '@material-ui/icons/DeleteOutline';
import Select from 'react-select';
import { useSnackbar } from 'notistack';
import { API } from '../../axios';
import { apiEndpoints } from '../../endpoints';
import SourceCopyDialog from './SourceCopyDialog';

let _rowCounter = 0;
const newRow = () => ({
  id: ++_rowCounter,
  category: null,
  product: null,
  workArea: null,
  quantity: '',
  remark: '',
});

let _targetCounter = 0;
const newTarget = () => ({
  id: ++_targetCounter,
  buildingType: null,
  buildingUnit: null,
  unitOptions: [],
});

const selectPortal = {
  menuPortalTarget: document.body,
  styles: { menuPortal: b => ({ ...b, zIndex: 9999 }) },
};

const BOQUIEntry = ({ buildingTypeData, onDone }) => {
  const { enqueueSnackbar } = useSnackbar();

  const [categoryOptions, setCategoryOptions]       = useState([]);
  const [allProductOptions, setAllProductOptions]   = useState([]);
  const [productsByCategory, setProductsByCategory] = useState({});
  const [workAreaOptions, setWorkAreaOptions]       = useState([]);
  const [rows, setRows]                             = useState([newRow()]);
  const [targets, setTargets]                       = useState([newTarget()]);
  const [showCopyDialog, setShowCopyDialog]         = useState(false);
  const [saving, setSaving]                         = useState(false);
  const [bulkRemark, setBulkRemark]                 = useState('');

  useEffect(() => {
    API.GET(apiEndpoints.getCategoryIdAndNames).then(r => {
      if (r.success) setCategoryOptions(r.data.map(d => ({ value: d.id, label: d.name })));
    });
    API.GET(apiEndpoints.getMeasurement).then(r => {
      if (!r.success) return;
      const opts = r.data
        .map(d => ({
          value: d.productId,
          label: d.productName,
          unit: d.measurementUnit || '',
        }))
        .sort((a, b) => a.label.localeCompare(b.label));
      setAllProductOptions(opts);
    });
    API.GET('/api/inventory/usagearea/idandnames').then(r => {
      if (r.success) setWorkAreaOptions(r.data.map(d => ({ value: d.id, label: d.name })));
    });
  }, []);

  const getProductOptions = (row) => {
    if (!row.category) return allProductOptions;
    return productsByCategory[row.category.value] || allProductOptions;
  };

  const loadProductsForCategory = async (categoryId) => {
    if (productsByCategory[categoryId]) return;
    const r = await API.GET(`/api/inventory/product?categoryId=${categoryId}`);
    if (!r.success) return;
    const opts = r.data
      .map(d => ({
        value: d.productId,
        label: d.productName,
        unit: d.measurementUnit || '',
      }))
      .sort((a, b) => a.label.localeCompare(b.label));
    setProductsByCategory(prev => ({ ...prev, [categoryId]: opts }));
  };

  // ── Row operations ────────────────────────────────────────────────────────

  const updateRow = (id, field, value) => {
    if (field === 'category' && value) loadProductsForCategory(value.value);
    setRows(prev => prev.map(r => {
      if (r.id !== id) return r;
      const updated = { ...r, [field]: value };
      if (field === 'category') updated.product = null;
      return updated;
    }));
  };

  const deleteRow = (id) => setRows(prev => prev.filter(r => r.id !== id));

  const addRow = () => setRows(prev => [...prev, newRow()]);

  const handleCopiedRows = (copiedRows) => {
    setRows(prev => {
      const nonEmpty = prev.filter(r => r.product || r.workArea || r.quantity);
      return [...nonEmpty, ...copiedRows.map(r => ({ ...r, id: ++_rowCounter }))];
    });
  };

  // ── Target operations ─────────────────────────────────────────────────────

  const addTarget = () => setTargets(prev => [...prev, newTarget()]);

  const removeTarget = (id) => setTargets(prev => prev.filter(t => t.id !== id));

  const handleTargetBTChange = async (targetId, opt) => {
    setTargets(prev => prev.map(t =>
      t.id !== targetId ? t : { ...t, buildingType: opt, buildingUnit: null, unitOptions: [] }
    ));
    if (!opt) return;
    const r = await API.GET(apiEndpoints.getBuildingUnit + opt.value);
    if (r.success) {
      const opts = r.data.usageLocation.map(u => ({ value: u.id, label: u.name }));
      setTargets(prev => prev.map(t =>
        t.id !== targetId ? t : { ...t, unitOptions: opts }
      ));
    }
  };

  const handleTargetBUChange = (targetId, opt) =>
    setTargets(prev => prev.map(t =>
      t.id !== targetId ? t : { ...t, buildingUnit: opt }
    ));

  const applyBulkRemark = () => {
    if (!bulkRemark.trim()) return;
    setRows(prev => prev.map(r => ({ ...r, remark: bulkRemark.trim() })));
  };

  // ── Save ──────────────────────────────────────────────────────────────────

  const handleSave = async () => {
    const badRows = rows.filter(r => !r.product || !r.workArea || !r.quantity || isNaN(Number(r.quantity)) || Number(r.quantity) <= 0 || !r.remark?.trim());
    if (badRows.length > 0) {
      enqueueSnackbar('Fill Product, Work Area, Quantity and Remark for every row.', { variant: 'warning' });
      return;
    }
    const validTargets = targets.filter(t => t.buildingType && t.buildingUnit);
    if (validTargets.length === 0) {
      enqueueSnackbar('Select at least one target Structure Type and Structure.', { variant: 'warning' });
      return;
    }

    setSaving(true);
    let allOk = true;

    for (const target of validTargets) {
      const upload = rows.map((r, idx) => ({
        sno: idx + 1,
        buildingType: target.buildingType.value,
        buildingUnit: target.buildingUnit.value,
        inventory: r.product.label,
        location: r.workArea.label,
        quantity: String(r.quantity),
        changes: 'upsert',
        remark: r.remark || '',
      }));

      const res = await API.POST(apiEndpoints.BOQupload, { upload });
      if (!res.success) {
        allOk = false;
        enqueueSnackbar(
          `${target.buildingUnit.label}: ${res.errorMessage || 'Save failed'}`,
          { variant: 'error' }
        );
      } else {
        const errItem = (res.data || []).find(m => m.message && m.message !== 'Successfully done');
        if (errItem) {
          allOk = false;
          enqueueSnackbar(`${target.buildingUnit.label}: ${errItem.message}`, { variant: 'error' });
        }
      }
    }

    setSaving(false);
    if (allOk) {
      enqueueSnackbar(
        `BOQ saved to ${validTargets.length} structure${validTargets.length > 1 ? 's' : ''} successfully!`,
        { variant: 'success' }
      );
      onDone();
    }
  };

  // ── Styles ────────────────────────────────────────────────────────────────

  const s = {
    toolbar: {
      display: 'flex', gap: '10px', marginBottom: '16px',
      alignItems: 'center', flexWrap: 'wrap',
    },
    btn: {
      padding: '7px 16px', borderRadius: '6px',
      border: '1px solid #1976d2', background: '#fff',
      color: '#1976d2', cursor: 'pointer', fontSize: '13px', fontWeight: 500,
    },
    btnPrimary: { background: '#1976d2', color: '#fff', border: 'none' },
    btnCancel: { border: '1px solid #ccc', color: '#555' },
    table: { width: '100%', borderCollapse: 'collapse', fontSize: '13px' },
    th: {
      background: '#f5f5f5', padding: '8px 10px',
      border: '1px solid #e0e0e0', fontWeight: 600,
      whiteSpace: 'nowrap', textAlign: 'left',
    },
    td: { padding: '6px 8px', border: '1px solid #e0e0e0', verticalAlign: 'middle' },
    sectionLabel: {
      fontSize: '14px', fontWeight: 600, color: '#333',
      margin: '24px 0 10px',
    },
    targetRow: { display: 'flex', gap: '12px', alignItems: 'center', marginBottom: '10px' },
    selectBox: { minWidth: '220px' },
    actions: { display: 'flex', gap: '12px', marginTop: '28px', alignItems: 'center' },
    hint: { fontSize: '12px', color: '#888' },
  };

  return (
    <div>
      {/* Toolbar */}
      <div style={s.toolbar}>
        <button style={s.btn} onClick={() => setShowCopyDialog(true)}>
          📋 Copy from Existing BOQ
        </button>
        <button style={s.btn} onClick={addRow}>
          + Add Row
        </button>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginLeft: 'auto' }}>
          <TextField
            value={bulkRemark}
            onChange={e => setBulkRemark(e.target.value)}
            placeholder="Remark for all rows…"
            variant="outlined"
            size="small"
            inputProps={{ style: { width: '220px', fontSize: '13px' } }}
          />
          <button style={s.btn} onClick={applyBulkRemark} title="Apply this remark to all rows">
            Apply to All
          </button>
        </div>
      </div>

      {/* Editable table */}
      <div style={{ overflowX: 'auto' }}>
        <table style={s.table}>
          <thead>
            <tr>
              <th style={{ ...s.th, width: 36 }}>#</th>
              <th style={s.th}>
                Category{' '}
                <span style={{ color: '#888', fontWeight: 400, fontSize: '11px' }}>(optional)</span>
              </th>
              <th style={s.th}>Product / Inventory <span style={{ color: 'red' }}>*</span></th>
              <th style={s.th}>Work Area <span style={{ color: 'red' }}>*</span></th>
              <th style={{ ...s.th, width: 110 }}>Quantity <span style={{ color: 'red' }}>*</span></th>
              <th style={s.th}>Remark <span style={{ color: 'red' }}>*</span></th>
              <th style={{ ...s.th, width: 40 }}></th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row, idx) => (
              <tr key={row.id}>
                <td style={{ ...s.td, color: '#999', textAlign: 'center' }}>{idx + 1}</td>

                <td style={{ ...s.td, minWidth: '180px' }}>
                  <Select
                    options={categoryOptions}
                    value={row.category}
                    onChange={val => updateRow(row.id, 'category', val)}
                    placeholder="All"
                    isClearable
                    isSearchable
                    {...selectPortal}
                  />
                </td>

                <td style={{ ...s.td, minWidth: '220px' }}>
                  <Select
                    options={getProductOptions(row)}
                    value={row.product}
                    onChange={val => updateRow(row.id, 'product', val)}
                    placeholder="Select product"
                    isSearchable
                    {...selectPortal}
                  />
                  {row.product?.unit && (
                    <div style={{ fontSize: '11px', color: '#1976d2', marginTop: '3px' }}>
                      Unit: <strong>{row.product.unit}</strong>
                    </div>
                  )}
                </td>

                <td style={{ ...s.td, minWidth: '180px' }}>
                  <Select
                    options={workAreaOptions}
                    value={row.workArea}
                    onChange={val => updateRow(row.id, 'workArea', val)}
                    placeholder="Select work area"
                    isSearchable
                    {...selectPortal}
                  />
                </td>

                <td style={s.td}>
                  <TextField
                    type="number"
                    value={row.quantity}
                    onChange={e => updateRow(row.id, 'quantity', e.target.value)}
                    variant="outlined"
                    size="small"
                    inputProps={{ min: 0, style: { width: '80px' } }}
                  />
                </td>

                <td style={{ ...s.td, minWidth: '160px' }}>
                  <TextField
                    value={row.remark}
                    onChange={e => updateRow(row.id, 'remark', e.target.value)}
                    variant="outlined"
                    size="small"
                    fullWidth
                    placeholder="Required"
                  />
                </td>

                <td style={{ ...s.td, textAlign: 'center' }}>
                  <IconButton
                    size="small"
                    onClick={() => deleteRow(row.id)}
                    disabled={rows.length === 1}
                    title="Remove row"
                  >
                    <DeleteOutlineIcon
                      fontSize="small"
                      style={{ color: rows.length === 1 ? '#ccc' : '#c62828' }}
                    />
                  </IconButton>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div style={{ marginTop: '8px' }}>
        <button style={s.btn} onClick={addRow}>+ Add Row</button>
      </div>

      {/* Target structures */}
      <div style={s.sectionLabel}>
        Target Structure(s)
        <span style={{ ...s.hint, fontWeight: 400, marginLeft: '8px' }}>
          — BOQ will be saved to each selected structure
        </span>
      </div>

      {targets.map((t, idx) => (
        <div key={t.id} style={s.targetRow}>
          <span style={{ ...s.hint, minWidth: '20px' }}>{idx + 1}.</span>

          <div style={s.selectBox}>
            <Select
              options={buildingTypeData}
              value={t.buildingType}
              onChange={opt => handleTargetBTChange(t.id, opt)}
              placeholder="Structure Type"
              isSearchable
              isClearable
              {...selectPortal}
            />
          </div>

          <div style={s.selectBox}>
            <Select
              options={t.unitOptions}
              value={t.buildingUnit}
              onChange={opt => handleTargetBUChange(t.id, opt)}
              placeholder={t.buildingType ? 'Select Structure' : 'Select Type first'}
              isDisabled={!t.buildingType}
              isSearchable
              {...selectPortal}
            />
          </div>

          {targets.length > 1 && (
            <IconButton size="small" onClick={() => removeTarget(t.id)} title="Remove target">
              <DeleteOutlineIcon fontSize="small" style={{ color: '#c62828' }} />
            </IconButton>
          )}
        </div>
      ))}

      <button
        style={{ ...s.btn, fontSize: '12px', padding: '5px 12px' }}
        onClick={addTarget}
      >
        + Add another structure
      </button>

      {/* Actions */}
      <div style={s.actions}>
        <button
          style={{ ...s.btn, ...s.btnPrimary, opacity: saving ? 0.7 : 1 }}
          onClick={handleSave}
          disabled={saving}
        >
          {saving ? 'Saving…' : 'Save BOQ'}
        </button>
        <button style={{ ...s.btn, ...s.btnCancel }} onClick={onDone}>
          Cancel
        </button>
      </div>

      <SourceCopyDialog
        open={showCopyDialog}
        onClose={() => setShowCopyDialog(false)}
        buildingTypeData={buildingTypeData}
        allProductOptions={allProductOptions}
        workAreaOptions={workAreaOptions}
        onCopy={handleCopiedRows}
      />
    </div>
  );
};

export default BOQUIEntry;
