import React, { useState, useEffect } from 'react';
import { IconButton, TextField } from '@material-ui/core';
import DeleteOutlineIcon from '@material-ui/icons/DeleteOutline';
import FileCopyOutlined from '@material-ui/icons/FileCopyOutlined';
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
  wastagePercent: '0',
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
  const [draftLoaded, setDraftLoaded]               = useState(false);
  const [targets, setTargets]                       = useState([newTarget()]);
  const [showCopyDialog, setShowCopyDialog]         = useState(false);
  const [saving, setSaving]                         = useState(false);
  const [previewMode, setPreviewMode]               = useState(false);

  useEffect(() => {
    try {
      const raw = localStorage.getItem('boq_ui_draft');
      if (raw) {
        const parsed = JSON.parse(raw);
        if (parsed && Array.isArray(parsed.rows) && parsed.rows.length > 0) {
          setRows(parsed.rows.map(r => ({ ...r, id: ++_rowCounter })));
          setDraftLoaded(true);
        }
      }
    } catch (_) {}

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

  useEffect(() => {
    localStorage.setItem('boq_ui_draft', JSON.stringify({ rows }));
  }, [rows]);

  useEffect(() => {
    const handler = (e) => {
      if (rows.some(r => r.product || r.workArea || r.quantity)) {
        e.preventDefault();
        e.returnValue = '';
      }
    };
    window.addEventListener('beforeunload', handler);
    return () => window.removeEventListener('beforeunload', handler);
  }, [rows]);

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

  const duplicateRow = (id) => setRows(prev => {
    const idx = prev.findIndex(r => r.id === id);
    if (idx === -1) return prev;
    const copy = { ...prev[idx], id: ++_rowCounter };
    return [...prev.slice(0, idx + 1), copy, ...prev.slice(idx + 1)];
  });

  const startFresh = () => {
    localStorage.removeItem('boq_ui_draft');
    setRows([newRow()]);
    setDraftLoaded(false);
  };

  const handleCopiedRows = (copiedRows) => {
    const resolvedRows = copiedRows.map(r => {
      const categoryOpt = r.category
        ? categoryOptions.find(c => c.label === r.category) || null
        : null;
      return { ...r, category: categoryOpt };
    });
    const uniqueCategoryIds = [...new Set(resolvedRows.filter(r => r.category).map(r => r.category.value))];
    uniqueCategoryIds.forEach(id => loadProductsForCategory(id));
    setRows(prev => {
      const nonEmpty = prev.filter(r => r.product || r.workArea || r.quantity);
      return [...nonEmpty, ...resolvedRows.map(r => ({ ...r, id: ++_rowCounter }))];
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

  const applyRemarkToAll = (remark) => {
    if (!remark.trim()) return;
    setRows(prev => prev.map(r => ({ ...r, remark: remark.trim() })));
  };

  // ── Preview & Save ────────────────────────────────────────────────────────

  const handlePreviewAndSave = () => {
    const badRows = rows.filter(r =>
      !r.product || !r.workArea || !r.quantity || isNaN(Number(r.quantity)) || Number(r.quantity) <= 0 ||
      isNaN(Number(r.wastagePercent)) || Number(r.wastagePercent) < 0 ||
      !r.remark?.trim()
    );
    if (badRows.length > 0) {
      enqueueSnackbar('Fill Product, Work Area, Quantity, Wastage % and Remark for every row.', { variant: 'warning' });
      return;
    }
    const validTargets = targets.filter(t => t.buildingType && t.buildingUnit);
    if (validTargets.length === 0) {
      enqueueSnackbar('Select at least one target Structure Type and Structure.', { variant: 'warning' });
      return;
    }
    setPreviewMode(true);
  };

  const handleConfirmSave = async () => {
    const validTargets = targets.filter(t => t.buildingType && t.buildingUnit);
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
        wastagePercent: String(r.wastagePercent || '0'),
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
      localStorage.removeItem('boq_ui_draft');
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

  // ── Preview panel ─────────────────────────────────────────────────────────

  const renderPreview = () => {
    const validTargets = targets.filter(t => t.buildingType && t.buildingUnit);

    // Group rows by work area name, sorted alphabetically
    const groups = {};
    rows.forEach(r => {
      const key = r.workArea?.label || '(No Work Area)';
      if (!groups[key]) groups[key] = [];
      groups[key].push(r);
    });
    const sortedGroups = Object.keys(groups).sort();

    const pTh = {
      background: '#f0f4f8', padding: '8px 12px', border: '1px solid #dde3ea',
      fontWeight: 600, fontSize: '12px', textAlign: 'left', whiteSpace: 'nowrap',
    };
    const pTd = { padding: '7px 12px', border: '1px solid #e8edf2', fontSize: '13px' };
    const groupHdr = {
      background: '#e8f0fe', padding: '8px 12px', fontWeight: 600, fontSize: '13px',
      color: '#1565c0', borderLeft: '3px solid #1976d2',
    };

    return (
      <div>
        {/* Summary header */}
        <div style={{ background: '#e8f5e9', border: '1px solid #a5d6a7', borderRadius: '8px', padding: '14px 18px', marginBottom: '20px' }}>
          <div style={{ fontWeight: 600, fontSize: '14px', color: '#2e7d32', marginBottom: '8px' }}>
            Review before saving — {rows.length} row{rows.length !== 1 ? 's' : ''} across {sortedGroups.length} work area{sortedGroups.length !== 1 ? 's' : ''}
          </div>
          <div style={{ fontSize: '13px', color: '#555' }}>
            <strong>Target structure{validTargets.length > 1 ? 's' : ''}:</strong>{' '}
            {validTargets.map(t => `${t.buildingType.label} › ${t.buildingUnit.label}`).join(', ')}
          </div>
        </div>

        {/* Grouped table */}
        <div style={{ overflowX: 'auto', border: '1px solid #e8edf2', borderRadius: '6px' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '13px' }}>
            <thead>
              <tr>
                <th style={{ ...pTh, width: 36 }}>#</th>
                <th style={pTh}>Product / Inventory</th>
                <th style={{ ...pTh, width: 90 }}>Qty</th>
                <th style={{ ...pTh, width: 90 }}>Wastage %</th>
                <th style={pTh}>Remark</th>
              </tr>
            </thead>
            <tbody>
              {sortedGroups.map(groupName => (
                <React.Fragment key={groupName}>
                  <tr>
                    <td colSpan={5} style={groupHdr}>
                      {groupName}
                      <span style={{ fontWeight: 400, fontSize: '12px', marginLeft: '8px', color: '#1976d2' }}>
                        ({groups[groupName].length} item{groups[groupName].length !== 1 ? 's' : ''})
                      </span>
                    </td>
                  </tr>
                  {groups[groupName].map((row, idx) => (
                    <tr key={row.id} style={{ background: idx % 2 === 0 ? '#fff' : '#fafbfd' }}>
                      <td style={{ ...pTd, color: '#999', textAlign: 'center' }}>{idx + 1}</td>
                      <td style={pTd}>
                        {row.product?.label}
                        {row.product?.unit && (
                          <span style={{ color: '#1976d2', fontSize: '11px', marginLeft: '6px' }}>
                            ({row.product.unit})
                          </span>
                        )}
                      </td>
                      <td style={pTd}>{row.quantity}</td>
                      <td style={pTd}>{row.wastagePercent || '0'}</td>
                      <td style={{ ...pTd, color: '#555' }}>{row.remark}</td>
                    </tr>
                  ))}
                </React.Fragment>
              ))}
            </tbody>
          </table>
        </div>

        {/* Preview actions */}
        <div style={{ display: 'flex', gap: '12px', marginTop: '24px', alignItems: 'center' }}>
          <button
            style={{ ...s.btn, ...s.btnPrimary, opacity: saving ? 0.7 : 1 }}
            onClick={handleConfirmSave}
            disabled={saving}
          >
            {saving ? 'Saving…' : '✓ Confirm & Save'}
          </button>
          <button
            style={{ ...s.btn, ...s.btnCancel }}
            onClick={() => setPreviewMode(false)}
            disabled={saving}
          >
            ← Back to Edit
          </button>
        </div>
      </div>
    );
  };

  if (previewMode) {
    return (
      <div>
        {renderPreview()}
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
  }

  return (
    <div>
      {/* Toolbar */}
      <div style={s.toolbar}>
        <button style={s.btn} onClick={() => setShowCopyDialog(true)}>
          📋 Copy from Existing BOQ
        </button>
      </div>

      {draftLoaded && (
        <div style={{ background: '#fff8e1', border: '1px solid #ffe082', borderRadius: '6px', padding: '8px 14px', marginBottom: '12px', display: 'flex', alignItems: 'center', gap: '12px', fontSize: '13px' }}>
          <span>📋 Draft from your previous session has been loaded.</span>
          <button style={{ ...s.btn, ...s.btnCancel, padding: '3px 10px', fontSize: '12px' }} onClick={startFresh}>Start Fresh</button>
        </div>
      )}

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
              <th style={{ ...s.th, width: 100 }}>Wastage % <span style={{ color: 'red' }}>*</span></th>
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
                    onWheel={e => e.target.blur()}
                    variant="outlined"
                    size="small"
                    inputProps={{ min: 0, style: { width: '80px' } }}
                  />
                </td>

                <td style={s.td}>
                  <TextField
                    type="number"
                    value={row.wastagePercent}
                    onChange={e => updateRow(row.id, 'wastagePercent', e.target.value)}
                    onWheel={e => e.target.blur()}
                    variant="outlined"
                    size="small"
                    inputProps={{ min: 0, max: 100, style: { width: '70px' } }}
                  />
                </td>

                <td style={{ ...s.td, minWidth: '180px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                    <TextField
                      value={row.remark}
                      onChange={e => updateRow(row.id, 'remark', e.target.value)}
                      variant="outlined"
                      size="small"
                      fullWidth
                      placeholder="Required"
                    />
                    {row.remark?.trim() && (
                      <IconButton
                        size="small"
                        title="Apply this remark to all rows"
                        onClick={() => applyRemarkToAll(row.remark)}
                        style={{ flexShrink: 0, color: '#1976d2' }}
                      >
                        <span style={{ fontSize: '14px', lineHeight: 1 }}>⇩</span>
                      </IconButton>
                    )}
                  </div>
                </td>

                <td style={{ ...s.td, textAlign: 'center', whiteSpace: 'nowrap' }}>
                  <IconButton
                    size="small"
                    onClick={() => duplicateRow(row.id)}
                    title="Duplicate row"
                  >
                    <FileCopyOutlined fontSize="small" style={{ color: '#1976d2' }} />
                  </IconButton>
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
          style={{ ...s.btn, ...s.btnPrimary }}
          onClick={handlePreviewAndSave}
        >
          Preview &amp; Save →
        </button>
        <button
          style={{ ...s.btn, ...s.btnCancel }}
          onClick={() => {
            if (rows.some(r => r.product || r.workArea || r.quantity)) {
              if (window.confirm('You have unsaved changes. Leave anyway?')) onDone();
            } else {
              onDone();
            }
          }}
        >
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
