import React, { useState, useEffect, useRef } from 'react'
import * as XLSX from 'xlsx'
import IconButton from '@material-ui/core/IconButton';
import Button from '../../Shared/Button';
import Select from 'react-select';
import { useSnackbar } from "notistack";
import { apiEndpoints } from '../../endpoints';
import "./LoadingSpinner.css";
import { API } from "./../../axios";
import { DeleteIcon, RefreshIcon } from '../../Shared/Icons/Index';
import { canEditBOQ } from '../../helper';

const BOQInputScreen = () => {
  const fileInputRef = useRef(null);

  useEffect(() => { getBuildingTypeDD(); }, []);

  const { enqueueSnackbar } = useSnackbar();

  const fileType = [
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/vnd.ms-excel'
  ];

  const [isLoading, setIsLoading]                         = useState(false);
  const [isReset, setIsReset]                             = useState(false);
  const [excelData, setExcelData]                         = useState(null);
  const [excelFile, setExcelFile]                         = useState(null);
  const [selectedFileName, setSelectedFileName]           = useState('');
  const [excelFileError, setExcelFileError]               = useState(null);
  const [buildingType, setBuildingType]                   = useState('');
  const [buildingUnit, setBuildingUnit]                   = useState([]);
  const [buildingUnitDD, setBuildingUnitDD]               = useState('');
  const [buildingTypeDD, setBuildingTypeDD]               = useState('');
  const [buildingUnitIds, setBuildingUnitIds]             = useState([]);
  const [buildingUnitNameforTable, setBuildingUnitNameforTable] = useState([]);
  const [buildingTypeData, setBuildingTypeData]           = useState([]);
  const [buildingUnitData, setBuildingUnitData]           = useState([]);
  const [responseStatus, setResponseStatus]               = useState(true);
  const [uploadMode, setUploadMode]                       = useState('new');
  const [dragOver, setDragOver]                           = useState(false);
  const [hasCategory, setHasCategory]                     = useState(false);

  const tableHeading = [
    'S No.', 'Structure Type', 'Structure',
    'Category', 'Inventory', 'Unit', 'Quantity', 'Work Area', 'Changes', 'Remark', 'Delete'
  ];

  let td = [null];
  let key2 = 0;
  let dataErrors;
  let responseMsg = '';
  let responseMsgSet = new Set();
  let responseMsgList = [];
  let responseData = [{ message: '', sno: '', columns: [] }];
  var listOfCssIDs = [];
  var listOfCssIDsAll = [];

  const buildingSelected = buildingTypeDD && buildingUnitDD && buildingUnitDD.length > 0;

  // ── API calls ──────────────────────────────────────────────────────────────

  const getBuildingTypeDD = async () => {
    const response = await API.GET(apiEndpoints.buildingType);
    if (response.success && response.data) {
      setBuildingTypeData(response.data.map(el => ({ value: el.id, label: el.name })));
    }
  };

  const getBuildingUnitsDDAPI = async (ID) => {
    try {
      const response = await API.GET(apiEndpoints.getBuildingUnit + ID);
      if (response.data) {
        setBuildingUnitData(
          response.data.usageLocation.map(el => ({ value: el.id, label: el.name }))
        );
      }
    } catch (e) { console.log(e); }
  };

  // ── Handlers ───────────────────────────────────────────────────────────────

  const handleSelectBuildingType = (e) => {
    setBuildingUnitData([]);
    setBuildingUnitDD('');
    setBuildingUnit([]);
    setBuildingUnitIds([]);
    setBuildingType(e.label);
    setBuildingTypeDD(e);
    getBuildingUnitsDDAPI(e.value);
    // Reset upload & preview
    setExcelData(null);
    setExcelFile(null);
    setSelectedFileName('');
    setExcelFileError(null);
    setHasCategory(false);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  const handleSelectBuildingUnit = (e) => {
    setBuildingUnit(e.map(u => u.label));
    setBuildingUnitDD(e);
    setBuildingUnitIds(e.map(u => u.value));
  };

  const processFile = (file) => {
    if (!file) return;
    if (!fileType.includes(file.type)) {
      setExcelFileError('Please select an Excel file (.xlsx or .xls)');
      setExcelFile(null);
      setSelectedFileName('');
      return;
    }
    setExcelFileError(null);
    setSelectedFileName(file.name);
    const reader = new FileReader();
    reader.readAsArrayBuffer(file);
    reader.onload = (e) => setExcelFile(e.target.result);
  };

  const fileHandler = (event) => processFile(event.target.files[0]);

  const handleDrop = (e) => {
    e.preventDefault();
    setDragOver(false);
    processFile(e.dataTransfer.files[0]);
  };

  const handlePreview = (e) => {
    e.preventDefault();
    if (!buildingSelected) {
      enqueueSnackbar('Please select Structure Type and Structure first', { variant: 'warning' });
      return;
    }
    if (!excelFile) {
      enqueueSnackbar('Please select an Excel file first', { variant: 'warning' });
      return;
    }

    const workbook = XLSX.read(excelFile, { type: 'buffer' });
    const worksheetName = workbook.SheetNames[0];
    const worksheet = workbook.Sheets[worksheetName];
    const data = XLSX.utils.sheet_to_json(worksheet)
      .filter(row => row.Inventory && String(row.Inventory).trim() !== '');

    function extractHeader(ws) {
      const header = [];
      const range = XLSX.utils.decode_range(ws['!ref']);
      const numCols = range.e.c + 1;
      for (let i = 0; i < numCols; ++i) {
        const cell = ws[`${XLSX.utils.encode_col(i)}1`];
        header[i] = cell ? cell.h : '';
      }
      return header;
    }

    const heads = extractHeader(worksheet);
    // New template: Category | Inventory | Unit | Quantity | Work Area (or FinalLocation) | Changes?
    // Old template (backwards compat): Inventory | Quantity | Work Area (or FinalLocation) | Changes?
    const isWorkAreaCol4 = heads[4] === 'Work Area' || heads[4] === 'FinalLocation';
    const isWorkAreaCol2 = heads[2] === 'Work Area' || heads[2] === 'FinalLocation';
    const newFormat = heads[0] === 'Category' && heads[1] === 'Inventory' && heads[2] === 'Unit' && heads[3] === 'Quantity' && isWorkAreaCol4;
    const oldFormat = heads[0] === 'Inventory' && heads[1] === 'Quantity' && isWorkAreaCol2;
    if (!newFormat && !oldFormat) {
      enqueueSnackbar('Headers not recognised. Please use the provided template.', { variant: 'error' });
      return;
    }

    setHasCategory(newFormat);
    const mode = newFormat ? (heads[5] === 'Changes' ? 'new' : 'existing')
                           : (heads[3] === 'Changes' ? 'new' : 'existing');
    setUploadMode(mode);

    const excelDataM = [];
    const buildingUnitName = [];
    for (let i = 0; i < buildingUnitIds.length; i++) {
      for (let j = 0; j < data.length; j++) {
        buildingUnitName.push(buildingUnit[i]);
        excelDataM.push(data[j]);
      }
    }
    setBuildingUnitNameforTable(buildingUnitName);
    setExcelData(excelDataM);
  };

  const iterateResponse = () => {
    if (dataErrors !== null) {
      for (let i = 0; i < responseData.length; i++) {
        for (let j = 0; j < responseData[i].columns.length; j++) {
          listOfCssIDs.push(responseData[i].sno + responseData[i].columns[j]);
        }
      }
    }
    for (let i = 0; i < listOfCssIDsAll.length; i++) {
      const el = document.getElementById(listOfCssIDsAll[i]);
      if (el) {
        el.setAttribute('style', listOfCssIDs.includes(listOfCssIDsAll[i])
          ? 'border: 2px solid rgba(255,0,0,0.7)'
          : 'border: 1px solid rgba(0,0,0,0.15)');
      }
    }
  };

  const findindError = () => {
    const table = document.querySelector('table');
    for (var i = 0, row; row = table.rows[i]; i++) {
      for (var j = 0, col; col = row.cells[j]; j++) {
        listOfCssIDsAll.push(row.cells[j].id);
      }
    }
    iterateResponse();
  };

  const postTable = async () => {
    setIsLoading(true);
    setResponseStatus(false);
    const table = document.querySelector('table');
    for (var i = 1, row; row = table.rows[i]; i++) {
      const rowItem = table.rows[i].innerText;
      let splt = rowItem.split('\n').filter(e => e !== '\t' && e !== '' && e !== '\t\t\t\t\t\t\t');
      if (splt.length !== 0) {
        for (let j = 0; j < buildingTypeData.length; j++) {
          if (buildingTypeData[j].label === splt[1]) splt[1] = buildingTypeData[j].value;
        }
        for (let u = 0; u < buildingUnitData.length; u++) {
          if (buildingUnitData[u].label === splt[2]) splt[2] = buildingUnitData[u].value;
        }
        // New format: sno(0) BT(1) BU(2) Category(3) Inventory(4) Unit(5) Qty(6) Loc(7) [Changes(8)] Remark(8|9)
        // Old format: sno(0) BT(1) BU(2) Inventory(3) Qty(4) Loc(5) [Changes(6)] Remark(6|7)
        td[key2++] = hasCategory ? {
          sno: splt[0], buildingType: splt[1], buildingUnit: splt[2],
          inventory: splt[4], quantity: splt[6], location: splt[7],
          changes: uploadMode === 'existing' ? 'upsert' : splt[8],
          remark: uploadMode === 'existing' ? (splt[8] || '') : (splt[9] || '')
        } : {
          sno: splt[0], buildingType: splt[1], buildingUnit: splt[2],
          inventory: splt[3], quantity: splt[4], location: splt[5],
          changes: uploadMode === 'existing' ? 'upsert' : splt[6],
          remark: uploadMode === 'existing' ? (splt[6] || '') : (splt[7] || '')
        };
      }
    }

    const body = { upload: td };
    for (let i = 0; i < body.upload.length; i++) {
      if (body.upload[i] === null) { setExcelData(null); }
    }

    try {
      const response = await API.POST(apiEndpoints.BOQupload, body);
      const data = response.data;
      setIsLoading(false);
      if (response.success) {
        for (let i = 0; i < data.length; i++) {
          responseMsg = data[i].message;
          responseMsgSet.add(responseMsg);
          dataErrors = data[i].columns;
        }
        responseMsgList = Array.from(responseMsgSet);
        for (let j = 0; j < responseMsgList.length; j++) {
          const msg = responseMsgList[j];
          if (msg !== null) {
            if (msg !== 'Successfully done') {
              enqueueSnackbar('Upload blocked — ' + msg + '. Check highlighted rows.', { variant: 'error' });
            } else {
              enqueueSnackbar('BOQ uploaded successfully!', { variant: 'success' });
              setExcelData(null);
            }
          } else {
            enqueueSnackbar('Please correct the highlighted fields', { variant: 'error' });
          }
        }
        responseData = data;
        if (dataErrors) findindError();
        setResponseStatus(true);
      }
      if (response.errorMessage) {
        enqueueSnackbar(response.errorMessage, { variant: 'error' });
        setResponseStatus(true);
      }
    } catch (error) {
      setIsLoading(false);
      setResponseStatus(true);
      enqueueSnackbar('Something went wrong', { variant: 'error' });
    }
  };

  const handleReset = () => {
    setIsReset(true);
    setExcelData(null);
    setExcelFile(null);
    setSelectedFileName('');
    setExcelFileError(null);
    setHasCategory(false);
    if (fileInputRef.current) fileInputRef.current.value = '';
    setTimeout(() => setIsReset(false), 500);
  };

  const downloadSample = async () => {
    const response = await API.GETBlob(apiEndpoints.downloadBOQSample);
    if (!response.success) {
      enqueueSnackbar('Failed to download template', { variant: 'error' });
      return;
    }
    const a = document.createElement('a');
    a.href = window.URL.createObjectURL(new Blob([response.data]));
    a.download = 'BOQ_Sample_Template.xlsx';
    a.click();
  };

  const downloadExistingBOQ = async () => {
    if (!buildingTypeDD || !buildingUnitDD || buildingUnitDD.length === 0) {
      enqueueSnackbar('Select Structure Type and Structure first', { variant: 'warning' });
      return;
    }
    if (buildingUnitDD.length > 1) {
      enqueueSnackbar('Select only one Structure to download existing BOQ', { variant: 'warning' });
      return;
    }
    const response = await API.GETBlob(
      `${apiEndpoints.downloadExistingBOQ}?buildingTypeId=${buildingTypeDD.value}&buildingUnitId=${buildingUnitDD[0].value}`
    );
    if (!response.success) {
      enqueueSnackbar('Failed to download existing BOQ', { variant: 'error' });
      return;
    }
    const a = document.createElement('a');
    a.href = window.URL.createObjectURL(new Blob([response.data]));
    a.download = 'Existing_BOQ.xlsx';
    a.click();
  };

  function LoadingSpinner() {
    return <div className="spinner-container"><div className="loading-spinner" /></div>;
  }

  // ── Styles ─────────────────────────────────────────────────────────────────

  const s = {
    page: { padding: '24px' },
    pageHeader: { marginBottom: '24px' },
    pageTitle: { fontSize: '20px', fontWeight: 600, color: '#333', margin: 0 },
    pageSubtitle: { fontSize: '13px', color: '#888', marginTop: '4px' },
    section: {
      background: '#fff', border: '1px solid #e8e8e8', borderRadius: '8px',
      padding: '20px 24px', marginBottom: '16px'
    },
    stepHeader: { display: 'flex', alignItems: 'center', marginBottom: '12px' },
    stepBadge: {
      width: '24px', height: '24px', borderRadius: '50%', background: '#1976d2',
      color: '#fff', fontSize: '12px', fontWeight: 700,
      display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
      marginRight: '10px', flexShrink: 0
    },
    stepBadgeDone: { background: '#44c4a1' },
    stepTitle: { fontSize: '15px', fontWeight: 600, color: '#333' },
    helpText: { fontSize: '12px', color: '#888', marginTop: '6px', lineHeight: 1.5 },
    row: { display: 'flex', gap: '16px', flexWrap: 'wrap' },
    col: { flex: '1', minWidth: '220px' },
    label: { fontSize: '12px', fontWeight: 600, color: '#555', marginBottom: '6px' },
    downloadBtn: {
      display: 'inline-flex', alignItems: 'center', gap: '6px',
      padding: '8px 16px', borderRadius: '6px', border: '1px solid #1976d2',
      background: '#fff', color: '#1976d2', cursor: 'pointer', fontSize: '13px',
      fontWeight: 500, transition: 'all 0.2s'
    },
    dropZone: {
      border: `2px dashed ${dragOver ? '#1976d2' : '#ccc'}`,
      borderRadius: '8px', padding: '28px 20px', textAlign: 'center',
      background: dragOver ? '#e3f2fd' : '#fafafa', cursor: 'pointer',
      transition: 'all 0.2s'
    },
    dropZoneDisabled: { opacity: 0.5, cursor: 'not-allowed' },
    dropIcon: { fontSize: '32px', marginBottom: '8px', color: '#aaa' },
    fileSelected: {
      display: 'inline-flex', alignItems: 'center', gap: '8px',
      background: '#e8f5e9', border: '1px solid #a5d6a7', borderRadius: '6px',
      padding: '6px 12px', fontSize: '13px', color: '#2e7d32', marginTop: '10px'
    },
    modeBadge: {
      display: 'inline-block', padding: '2px 10px', borderRadius: '12px',
      fontSize: '11px', fontWeight: 600, marginLeft: '10px'
    },
    modeBadgeNew:      { background: '#e3f2fd', color: '#1565c0' },
    modeBadgeExisting: { background: '#f3e5f5', color: '#6a1b9a' },
    previewHeader: {
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      marginBottom: '12px'
    },
    actionRow: { display: 'flex', gap: '12px', alignItems: 'center', marginTop: '16px' },
  };

  const stepDone1 = !!buildingSelected;
  const stepDone2 = false; // download is optional, can't verify
  const stepDone3 = !!excelData;

  const hasEditAccess = canEditBOQ();

  return (
    <div style={s.page}>

      {/* Access guard banner */}
      {!hasEditAccess && (
        <div style={{
          background: '#fff3e0', border: '1px solid #ffb300', borderRadius: '8px',
          padding: '14px 20px', marginBottom: '20px', color: '#e65100', fontWeight: 500, fontSize: '14px'
        }}>
          ⚠️ You do not have permission to upload or edit BOQ. This action is restricted to Project Managers and Admins only.
        </div>
      )}

      {/* Wrap entire form — disable interaction for unauthorised users */}
      <div style={hasEditAccess ? {} : { pointerEvents: 'none', opacity: 0.55 }}>

      {/* Page header */}
      <div style={s.pageHeader}>
        <h2 style={s.pageTitle}>BOQ Upload</h2>
        <p style={s.pageSubtitle}>
          Upload Bill of Quantities for your project buildings. Follow the steps below.
        </p>
      </div>

      {/* Step 1 — Select Building */}
      <div style={s.section}>
        <div style={s.stepHeader}>
          <span style={{ ...s.stepBadge, ...(stepDone1 ? s.stepBadgeDone : {}) }}>1</span>
          <span style={s.stepTitle}>Select Building</span>
        </div>
        <div style={s.row}>
          <div style={s.col}>
            <div style={s.label}>Structure Type <span style={{ color: 'red' }}>*</span></div>
            <Select
              options={buildingTypeData}
              placeholder="Select Structure Type"
              value={buildingTypeDD}
              onChange={handleSelectBuildingType}
              isSearchable
            />
          </div>
          <div style={s.col}>
            <div style={{ ...s.label, display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span>Structure <span style={{ color: 'red' }}>*</span></span>
              {buildingTypeDD && buildingUnitData.length > 0 && (
                <>
                  <button
                    type="button"
                    style={{ fontSize: '11px', color: '#1976d2', background: 'none', border: 'none', cursor: 'pointer', padding: '0', fontWeight: 500 }}
                    onClick={() => handleSelectBuildingUnit(buildingUnitData)}
                  >
                    Select All
                  </button>
                  <span style={{ color: '#ccc' }}>|</span>
                  <button
                    type="button"
                    style={{ fontSize: '11px', color: '#e57373', background: 'none', border: 'none', cursor: 'pointer', padding: '0', fontWeight: 500 }}
                    onClick={() => handleSelectBuildingUnit([])}
                  >
                    Clear
                  </button>
                </>
              )}
            </div>
            <Select
              options={buildingUnitData || []}
              placeholder={buildingTypeDD ? 'Select one or more units' : 'Select Structure Type first'}
              value={buildingUnitDD}
              onChange={handleSelectBuildingUnit}
              isSearchable
              isMulti
              isDisabled={!buildingTypeDD}
            />
          </div>
        </div>
        <p style={s.helpText}>
          You can select multiple building units. The same BOQ data will be applied to each selected unit.
        </p>
      </div>

      {/* Step 2 — Get Template */}
      <div style={s.section}>
        <div style={s.stepHeader}>
          <span style={s.stepBadge}>2</span>
          <span style={s.stepTitle}>Get Template</span>
        </div>
        <div style={{ ...s.row, gap: '12px' }}>
          <div>
            <button style={s.downloadBtn} onClick={downloadSample}>
              📥 Download New Template
            </button>
            <p style={{ ...s.helpText, marginTop: '4px' }}>
              First time? Download this template, fill in your BOQ data, and upload.
            </p>
          </div>
          <div>
            <button
              style={{ ...s.downloadBtn, borderColor: buildingSelected ? '#7b1fa2' : '#ccc', color: buildingSelected ? '#7b1fa2' : '#aaa' }}
              onClick={downloadExistingBOQ}
            >
              📥 Download Existing BOQ
            </button>
            <p style={{ ...s.helpText, marginTop: '4px' }}>
              Already have BOQ? Download, modify quantities, and re-upload.{' '}
              {!buildingSelected && <span style={{ color: '#e57373' }}>Select a single building unit first.</span>}
            </p>
          </div>
        </div>
        <div style={{ ...s.helpText, marginTop: '12px', background: '#fff8e1', padding: '8px 12px', borderRadius: '6px', color: '#795548' }}>
          💡 <strong>Changes column:</strong> Use <code>addition</code> to add new, <code>update</code> to change quantity,
          or <code>deletion</code> to remove. Not needed when uploading existing BOQ — quantities are updated automatically.
        </div>
      </div>

      {/* Step 3 — Upload */}
      <div style={s.section}>
        <div style={s.stepHeader}>
          <span style={{ ...s.stepBadge, ...(stepDone3 ? s.stepBadgeDone : {}) }}>3</span>
          <span style={s.stepTitle}>Upload &amp; Preview</span>
        </div>

        {!buildingSelected ? (
          <div style={{ ...s.helpText, color: '#e57373', fontSize: '13px' }}>
            ⚠ Complete Step 1 first — select a Structure Type and at least one Structure.
          </div>
        ) : (
          <>
            <input
              ref={fileInputRef}
              type="file"
              accept=".xlsx,.xls"
              style={{ display: 'none' }}
              onChange={fileHandler}
            />
            <div
              style={{ ...s.dropZone, ...(buildingSelected ? {} : s.dropZoneDisabled) }}
              onClick={() => buildingSelected && fileInputRef.current.click()}
              onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
              onDragLeave={() => setDragOver(false)}
              onDrop={handleDrop}
            >
              <div style={s.dropIcon}>📂</div>
              <div style={{ fontSize: '14px', color: '#555', fontWeight: 500 }}>
                Click to select or drag &amp; drop your Excel file here
              </div>
              <div style={{ fontSize: '12px', color: '#aaa', marginTop: '4px' }}>
                Supported: .xlsx, .xls
              </div>
            </div>

            {selectedFileName && (
              <div style={s.fileSelected}>
                📎 {selectedFileName}
              </div>
            )}
            {excelFileError && (
              <div style={{ color: '#e57373', fontSize: '12px', marginTop: '6px' }}>
                {excelFileError}
              </div>
            )}

            <div style={s.actionRow}>
              <Button
                label="Preview"
                onClick={handlePreview}
                disabled={!excelFile}
              />
              <IconButton onClick={handleReset} title="Reset">
                {RefreshIcon({ fontSize: 'medium' })}
              </IconButton>
            </div>
          </>
        )}
      </div>

      {/* Preview & Submit */}
      {excelData && (
        <div style={s.section}>
          <div style={s.previewHeader}>
            <div style={{ display: 'flex', alignItems: 'center' }}>
              <span style={{ fontSize: '15px', fontWeight: 600, color: '#333' }}>
                Preview — {excelData.length} row{excelData.length !== 1 ? 's' : ''}
              </span>
              <span style={{
                ...s.modeBadge,
                ...(uploadMode === 'new' ? s.modeBadgeNew : s.modeBadgeExisting)
              }}>
                {uploadMode === 'new' ? 'New BOQ' : 'Modify Existing'}
              </span>
            </div>
            <p style={{ ...s.helpText, margin: 0 }}>
              Review the data below. Cells are editable. Red borders indicate errors from a previous submit attempt.
            </p>
          </div>

          {isLoading && LoadingSpinner()}

          <div style={{ overflowX: 'auto' }}>
            <table className="table">
              <thead>
                <tr>
                  {/* S No, Structure Type, Structure */}
                  {tableHeading.slice(0, 3).map((h, i) => (
                    <th key={i} style={{ border: '1px solid rgba(0,0,0,0.5)', whiteSpace: 'nowrap' }}>{h}</th>
                  ))}
                  {/* Category and Unit cols — only when new-format file */}
                  {hasCategory && (
                    <th style={{ border: '1px solid rgba(0,0,0,0.5)', whiteSpace: 'nowrap' }}>{tableHeading[3]}</th>
                  )}
                  {/* Inventory */}
                  <th style={{ border: '1px solid rgba(0,0,0,0.5)', whiteSpace: 'nowrap' }}>{tableHeading[4]}</th>
                  {hasCategory && (
                    <th style={{ border: '1px solid rgba(0,0,0,0.5)', whiteSpace: 'nowrap' }}>{tableHeading[5]}</th>
                  )}
                  {/* Quantity, Work Area */}
                  <th style={{ border: '1px solid rgba(0,0,0,0.5)', whiteSpace: 'nowrap' }}>{tableHeading[6]}</th>
                  <th style={{ border: '1px solid rgba(0,0,0,0.5)', whiteSpace: 'nowrap' }}>{tableHeading[7]}</th>
                  {uploadMode === 'new' && (
                    <th style={{ border: '1px solid rgba(0,0,0,0.5)' }}>{tableHeading[8]}</th>
                  )}
                  <th style={{ border: '1px solid rgba(0,0,0,0.5)' }}>{tableHeading[9]}</th>
                  <th style={{ border: '1px solid rgba(0,0,0,0.5)', textAlign: 'center' }}>{tableHeading[10]}</th>
                </tr>
              </thead>
              <tbody>
                {excelData.map((row, idx) => (
                  <tr key={idx}>
                    <td className="row-data" id={idx + 1 + tableHeading[0]} style={{ border: '1px solid rgba(0,0,0,0.15)' }}><pre>{idx + 1}</pre></td>
                    <td className="row-data" id={idx + 1 + tableHeading[1]} style={{ border: '1px solid rgba(0,0,0,0.15)' }}><pre>{buildingType}</pre></td>
                    <td className="row-data" id={idx + 1 + tableHeading[2]} style={{ border: '1px solid rgba(0,0,0,0.15)' }}><pre>{buildingUnitNameforTable[idx]}</pre></td>
                    {hasCategory && (
                      <td className="row-data" id={idx + 1 + tableHeading[3]} style={{ border: '1px solid rgba(0,0,0,0.15)', color: '#888' }}><pre>{row.Category}</pre></td>
                    )}
                    <td className="row-data" id={idx + 1 + tableHeading[4]} style={{ border: '1px solid rgba(0,0,0,0.15)' }} contentEditable="true"><pre>{row.Inventory}</pre></td>
                    {hasCategory && (
                      <td className="row-data" id={idx + 1 + tableHeading[5]} style={{ border: '1px solid rgba(0,0,0,0.15)', color: '#888' }}><pre>{row.Unit || '—'}</pre></td>
                    )}
                    <td className="row-data" id={idx + 1 + tableHeading[6]} style={{ border: '1px solid rgba(0,0,0,0.15)' }} contentEditable="true"><pre>{row.Quantity}</pre></td>
                    <td className="row-data" id={idx + 1 + tableHeading[7]} style={{ border: '1px solid rgba(0,0,0,0.15)' }} contentEditable="true"><pre>{row['Work Area'] || row.FinalLocation}</pre></td>
                    {uploadMode === 'new' && (
                      <td className="row-data" id={idx + 1 + tableHeading[8]} style={{ border: '1px solid rgba(0,0,0,0.15)' }} contentEditable="true"><pre>{row.Changes}</pre></td>
                    )}
                    <td className="row-data" id={idx + 1 + tableHeading[9]} style={{ border: '1px solid rgba(0,0,0,0.15)' }} contentEditable="true"><pre>{row.Remark || ''}</pre></td>
                    <td className="row-data" id={idx + 1 + tableHeading[10]} style={{ border: '1px solid rgba(0,0,0,0.15)', textAlign: 'center' }}>
                      <IconButton
                        onClick={() => {
                          setExcelData(prev => prev.filter((_, i) => i !== idx));
                          setBuildingUnitNameforTable(prev => prev.filter((_, i) => i !== idx));
                        }}
                      >
                        {DeleteIcon({ fontSize: 'medium' })}
                      </IconButton>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div style={{ marginTop: '16px' }}>
            {responseStatus ? (
              <Button
                label="Submit BOQ"
                onClick={() => postTable()}
              />
            ) : (
              <Button label="Submitting…" />
            )}
            <span style={{ ...s.helpText, display: 'inline', marginLeft: '12px' }}>
              This will save the BOQ data to the system.
            </span>
          </div>
        </div>
      )}

      </div> {/* end hasEditAccess wrapper */}
    </div>
  );
};

export default BOQInputScreen;
