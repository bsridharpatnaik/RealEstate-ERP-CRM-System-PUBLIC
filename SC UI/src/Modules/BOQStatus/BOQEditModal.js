import React, { useState, useEffect } from 'react';
import {
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Button, IconButton
} from '@material-ui/core';
import CloseIcon from '@material-ui/icons/Close';
import Select from 'react-select';
import { useSnackbar } from 'notistack';
import { API } from '../../axios';
import { apiEndpoints } from '../../endpoints';

const labelStyle = {
  fontSize: '12px', fontWeight: 600, color: '#555',
  marginBottom: '4px', display: 'block'
};

const BOQEditModal = ({ open, onClose, initialData, onSaved, stockDropdowns }) => {
  const { enqueueSnackbar } = useSnackbar();

  const [buildingTypeOptions, setBuildingTypeOptions] = useState([]);
  const [buildingUnitOptions, setBuildingUnitOptions] = useState([]);
  const [allProductOptions, setAllProductOptions]     = useState([]);
  const [productOptions, setProductOptions]           = useState([]);
  const [categoryOptions, setCategoryOptions]         = useState([]);
  const [locationOptions, setLocationOptions]         = useState([]);
  const [buildingType, setBuildingType]   = useState(null);
  const [buildingUnit, setBuildingUnit]   = useState(null);
  const [category, setCategory]           = useState(null);
  const [product, setProduct]             = useState(null);
  const [unit, setUnit]                   = useState('');
  const [finalLocation, setFinalLocation] = useState(null);
  const [quantity, setQuantity]           = useState('');
  const [saving, setSaving]               = useState(false);

  // Load building types, categories, products and usage areas once
  useEffect(() => {
    API.GET(apiEndpoints.buildingType).then(r => {
      if (r.success) setBuildingTypeOptions(r.data.map(d => ({ value: d.id, label: d.name })));
    });
    API.GET('/api/inventory/category/idandnames').then(r => {
      if (r.success) setCategoryOptions(r.data.map(d => ({ value: d.id, label: d.name })));
    });
    API.GET('/api/inventory/product').then(r => {
      if (r.success) {
        const opts = r.data
          .map(d => ({ value: d.ProductId, label: d.productName, unit: d.measurementUnit || '' }))
          .sort((a, b) => a.label.localeCompare(b.label));
        setAllProductOptions(opts);
        setProductOptions(opts);
      }
    });
    API.GET('/api/inventory/usagearea/idandnames').then(r => {
      if (r.success) setLocationOptions(r.data.map(d => ({ value: d.id, label: d.name })));
    });
  }, []);

  // Reset / pre-fill when modal opens
  useEffect(() => {
    if (!open) return;

    if (initialData) {
      // Pre-fill product
      const preProduct = productOptions.find(o => o.label === initialData.productName) || null;
      setProduct(preProduct);
      setUnit(preProduct ? (preProduct.unit || '') : '');

      // Pre-fill final location if single detail row
      if (initialData.finalLocation) {
        const preLocation = locationOptions.find(o => o.label === initialData.finalLocation) || null;
        setFinalLocation(preLocation);
        setQuantity(initialData.quantity !== undefined ? String(initialData.quantity) : '');
      } else {
        setFinalLocation(null);
        setQuantity('');
      }

      // Pre-fill building type then unit
      if (initialData.buildingTypeId && buildingTypeOptions.length > 0) {
        const preType = buildingTypeOptions.find(o => o.value === initialData.buildingTypeId) || null;
        setBuildingType(preType);
        if (preType) {
          loadBuildingUnits(initialData.buildingTypeId, initialData.buildingUnitId);
        }
      } else {
        setBuildingType(null);
        setBuildingUnit(null);
        setBuildingUnitOptions([]);
      }
    } else {
      // Reset for add
      setBuildingType(null);
      setBuildingUnit(null);
      setBuildingUnitOptions([]);
      setCategory(null);
      setProduct(null);
      setUnit('');
      setProductOptions(allProductOptions);
      setFinalLocation(null);
      setQuantity('');
    }
  }, [open, initialData, buildingTypeOptions, productOptions, locationOptions]); // eslint-disable-line react-hooks/exhaustive-deps

  const loadBuildingUnits = async (btId, preSelectUnitId) => {
    const r = await API.GET(apiEndpoints.getBuildingUnit + btId);
    if (r.success) {
      const opts = r.data.usageLocation.map(u => ({ value: u.id, label: u.name }));
      setBuildingUnitOptions(opts);
      if (preSelectUnitId) {
        const preUnit = opts.find(o => o.value === preSelectUnitId) || null;
        setBuildingUnit(preUnit);
      }
    }
  };

  const handleBuildingTypeChange = (opt) => {
    setBuildingType(opt);
    setBuildingUnit(null);
    setBuildingUnitOptions([]);
    if (opt) loadBuildingUnits(opt.value);
  };

  const handleCategoryChange = async (opt) => {
    setCategory(opt);
    setProduct(null);
    setUnit('');
    if (!opt) {
      setProductOptions(allProductOptions);
      return;
    }
    const r = await API.GET('/api/inventory/product?categoryId=' + opt.value);
    if (r.success && r.data) {
      setProductOptions(r.data.map(d => ({ value: d.ProductId, label: d.productName })));
    } else {
      setProductOptions(allProductOptions);
    }
  };

  const handleProductChange = (opt) => {
    setProduct(opt);
    setUnit(opt ? (opt.unit || '') : '');
  };

  const handleSave = async () => {
    if (!buildingType || !buildingUnit || !product || !finalLocation || !quantity) {
      enqueueSnackbar('Please fill all required fields', { variant: 'warning' });
      return;
    }
    setSaving(true);
    const body = {
      upload: [{
        sno: 1,
        buildingType: buildingType.value,
        buildingUnit: buildingUnit.value,
        inventory: product.label,
        location: finalLocation.label,
        quantity: String(quantity),
        changes: 'upsert',
      }]
    };
    const r = await API.POST(apiEndpoints.BOQupload, body);
    setSaving(false);
    if (r.success) {
      const msgs = r.data || [];
      const errorMsg = msgs.find(m => m.message && m.message !== 'Successfully done');
      if (errorMsg) {
        enqueueSnackbar(errorMsg.message || 'Error saving BOQ entry', { variant: 'error' });
      } else {
        enqueueSnackbar('BOQ entry saved successfully', { variant: 'success' });
        onSaved();
        onClose();
      }
    } else {
      enqueueSnackbar(r.errorMessage || 'Failed to save BOQ entry', { variant: 'error' });
    }
  };

  const isEdit = !!initialData;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span>{isEdit ? 'Edit BOQ Entry' : 'Add BOQ Entry'}</span>
        <IconButton size="small" onClick={onClose}><CloseIcon fontSize="small" /></IconButton>
      </DialogTitle>

      <DialogContent dividers>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>

          <div>
            <label style={labelStyle}>Building Type <span style={{ color: 'red' }}>*</span></label>
            <Select
              options={buildingTypeOptions}
              value={buildingType}
              onChange={handleBuildingTypeChange}
              placeholder="Select Building Type"
              isSearchable
            />
          </div>

          <div>
            <label style={labelStyle}>Building Unit <span style={{ color: 'red' }}>*</span></label>
            <Select
              options={buildingUnitOptions}
              value={buildingUnit}
              onChange={setBuildingUnit}
              placeholder={buildingType ? 'Select Building Unit' : 'Select Building Type first'}
              isDisabled={!buildingType}
              isSearchable
            />
          </div>

          <div>
            <label style={labelStyle}>Category <span style={{ fontSize: '11px', color: '#888', fontWeight: 400 }}>(optional — filters products below)</span></label>
            <Select
              options={categoryOptions}
              value={category}
              onChange={handleCategoryChange}
              placeholder="All categories"
              isSearchable
              isClearable
            />
          </div>

          <div>
            <label style={labelStyle}>Product / Inventory <span style={{ color: 'red' }}>*</span></label>
            <Select
              options={productOptions}
              value={product}
              onChange={handleProductChange}
              placeholder={category ? 'Select Product (filtered by category)' : 'Select Product'}
              isSearchable
            />
            {unit && (
              <div style={{ marginTop: '6px', fontSize: '12px', color: '#1976d2' }}>
                Unit of Measure: <strong>{unit}</strong>
              </div>
            )}
          </div>

          <div>
            <label style={labelStyle}>Final Location <span style={{ color: 'red' }}>*</span></label>
            <Select
              options={locationOptions}
              value={finalLocation}
              onChange={setFinalLocation}
              placeholder="Select Final Location"
              isSearchable
            />
          </div>

          <div>
            <label style={labelStyle}>BOQ Quantity <span style={{ color: 'red' }}>*</span></label>
            <TextField
              type="number"
              value={quantity}
              onChange={e => setQuantity(e.target.value)}
              variant="outlined"
              size="small"
              fullWidth
              inputProps={{ min: 0 }}
            />
          </div>

        </div>
      </DialogContent>

      <DialogActions style={{ padding: '12px 24px' }}>
        <Button onClick={onClose} color="default">Cancel</Button>
        <Button
          onClick={handleSave}
          color="primary"
          variant="contained"
          disabled={saving}
        >
          {saving ? 'Saving…' : 'Save'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default BOQEditModal;
