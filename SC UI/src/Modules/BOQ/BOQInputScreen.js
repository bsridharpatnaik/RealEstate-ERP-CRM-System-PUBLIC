import React, { useState, useEffect, Children } from 'react'

import * as XLSX from 'xlsx'
import IconButtons from '../../Shared/Button/IconButtons';
import IconButton from '@material-ui/core/IconButton';
import Button from '../../Shared/Button';
import { Input, TableBody, td, TableHead, TableRow, TableCell, Icon } from '@material-ui/core';
import Select from 'react-select';
import { useSnackbar } from "notistack";
import { getToken, removeToken } from "../../helper";
import { store } from "../../index";
import { apiEndpoints } from '../../endpoints';
import "./LoadingSpinner.css";
import LoadingOverlay from 'react-overlay-loader/lib/LoadingOverlay';
import { API } from "./../../axios";
import axios from 'axios';
import { DeleteIcon, RefreshIcon } from '../../Shared/Icons/Index';
import { color } from '@amcharts/amcharts4/core';
import $ from 'jquery';

const BOQInputScreen = () => {
  const token = getToken();
  const states = store.getState();

  useEffect(() => {
    getBuildingTypeDD();
    // console.log(states.tennant.tennant_id);
  }, []);

  const { enqueueSnackbar } = useSnackbar();

  const fileType = ['application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', 'application/vnd.ms-excel']
  const [isLoading, setIsLoading] = useState(false);
  const [isReset, setIsReset] = useState(false);
  const excelDataM = new Array();
  const [excelData, setExcelData] = useState(null);
  const [excelFile, setExcelFile] = useState(null);
  let [excelFileName, setExcelFileName] = useState("");
  const [excelFileError, setExcelFileError] = useState(null);
  const [buildingType, setBuildingType] = useState("");
  const [buildingUnit, setBuildingUnit] = useState([]);
  const [buildingUnitDD, setBuildingUnitDD] = useState("");
  const [buildingTypeDD, setBuildingTypeDD] = useState("");
  const [buildingUnitIds, setBuildingUnitIds] = useState([]);
  const [buildingUnitNameforTable, setBuildingUnitNameforTable] = useState([]);
  const bID = new Array();
  const uID = new Array();
  const buildingUnitName = new Array();
  const [buildingTypeIdForPost, setBuildingTypeIdForPost] = useState('');
  var buildingTypeId;
  const [buildingUnitIdForPost, setBuildingUnitIdForPost] = useState([]);
  let buildingUnitId = new Array();
  var excelLength = 0;
  var counter = 0;
  let key = 0;
  let key2 = 0;
  let key3 = 0;
  const p = [];
  let td = [null];
  let error = [null];
  let tableData = [null];
  const [dataErrorsHold, setDataErrorsHold] = useState('');
  let dataErrors;
  const [responseMsgHold, setResponseMsgHold] = useState('');
  let responseMsg = '';
  let responseMsgSet = new Set();
  let responseMsgList = new Array();
  let responseSNo;
  const [responseStatus, setResponseStatus] = useState(true);
  const [uploadMode, setUploadMode] = useState('new'); // 'new' = has Changes column, 'existing' = no Changes column (upsert)

  const tableHeading = [
    'S No.',
    'Building Type',
    'Building Unit',
    'Inventory',
    'Quantity',
    'Final Location',
    'Changes',
    'Delete Record'
  ]

  let responseData = [{
    "message": "",
    "sno": "",
    "columns": []
  },];

  // const [responseData, setResponseData] = useState(
  //   [
  //     {
  //       "message": "",
  //       "sno": "",
  //       "columns": []
  //     },
  //   ]
  // );

  const actions = [
    { label: "Add", value: 1 },
    { label: "Edit", value: 2 },
    { label: "Delete", value: 3 }
  ];

  const [buildingTypeData, setBuildingTypeData] = useState([
    // { value: 0, label: 'No Data' }
  ]);

  const [buildingUnitData, setBuildingUnitData] = useState([
    // { value: 0, label: 'No Data' },
  ]);

  const buildingUnitDataArray = new Array();
  const buildingUnitTypeArray = new Array();



  let addingResponse = [];
  let resItKey = 0;
  let tableId = [];
  var listOfCssIDs = new Array();
  var listOfCssIDsAll = new Array();

  const iterateResponse = () => {
    if (dataErrors !== null) {
      for (let index = 0; index < responseData.length; index++) {
        for (let index1 = 0; index1 < responseData[index].columns.length; index1++) {
          listOfCssIDs.push(responseData[index].sno + responseData[index].columns[index1]);
        }
      }
    } else {
      console.log('no Errors');
    }
    console.log('responseIterate', listOfCssIDs);
    iterateErrors();
  }

  const iterateErrors = () => {

    for (let i = 0; i < listOfCssIDsAll.length; i++) {

      if (listOfCssIDs.includes(listOfCssIDsAll[i])) {
        var myDiv = document.getElementById(listOfCssIDsAll[i]);
        myDiv.setAttribute("style", 'border: 2px solid rgba(255, 0, 0,0.7)');
      } else {
        var myDiv = document.getElementById(listOfCssIDsAll[i]);
        myDiv.setAttribute("style", 'border: 1px solid rgba(0, 0, 0, 0.15)');
      }
    }
  }

  // const getBuildingTypeDD = async () => {
  //   try {
  //     var myHeaders = new Headers();
  //     myHeaders.append("Authorization", "Bearer " + token.toString());
  //     myHeaders.append("tenant-id", states.tennant.tennant_id);

  //     var requestOptions = {
  //       method: 'GET',
  //       redirect: 'follow',
  //       headers: myHeaders,
  //     };
  //     const response = await fetch("/ec-common-service"+apiEndpoints.buildingType, requestOptions);
  //     const data = await response.json();
  //     if (data) {
  //       for (let i = 0; i < data.length; i++) {
  //         const element = data[i];
  //         buildingUnitTypeArray.push({ 'value': element.id, 'label': element.name })
  //       }
  //       setBuildingTypeData(buildingUnitTypeArray)
  //     }
  //     else {
  //       setBuildingTypeData({ value: 0, label: 'No Data' });

  //     }
  //   }
  //   catch (error) {
  //     console.log(error);
  //   }
  //   console.log('buildingTypeData', buildingTypeData);
  // };

  const getBuildingTypeDD = async () => {
    const response = await API.GET(apiEndpoints.buildingType);
    console.log('getBuildingTypeDD response: ', response);
    if (response.success) {
      if (response.data) {
        for (let i = 0; i < response.data.length; i++) {
          const element = response.data[i];
          buildingUnitTypeArray.push({ 'value': element.id, 'label': element.name })
        }
        setBuildingTypeData(buildingUnitTypeArray)
      }

    }
  }

  const getBuildingUnitsDDAPI = async (ID) => {
    try {
      const response = await API.GET(apiEndpoints.getBuildingUnit + ID);
      console.log('getBuildingUnitsDDAPI response: ', response);
      if (response.data) {
        for (let i = 0; i < response.data.usageLocation.length; i++) {
          const element = response.data.usageLocation[i];
          buildingUnitDataArray.push({ 'value': element.id, 'label': element.name })
        }
        setBuildingUnitData(buildingUnitDataArray)
      }

    }
    catch (error) {
      console.log(error);
    }
    console.log('buildingUnitsDataArray', buildingUnitDataArray);
    console.log('buildingUnitsData', buildingUnitData);
  };

  const findindError = () => {
    const table = document.querySelector("table");
    for (var i = 0, row; row = table.rows[i]; i++) {
      for (var j = 0, col; col = row.cells[j]; j++) {
        const cellItems = row.cells[j].id;
        listOfCssIDsAll.push(cellItems);
      }
    }
    console.log('listOfCssIDsAll', listOfCssIDsAll);
    iterateResponse()
  }


  const postTable = async () => {
    setIsLoading(true);
    setResponseStatus(false);
    const table = document.querySelector("table");
    console.log("TableLength: ", table.rows.length);
    for (var i = 0, row; row = table.rows[i]; i++) {
      const rowItem = table.rows[i].innerText;
      // console.log('rowItem', rowItem);

      let splt = rowItem.split('\n').filter(e => e != '\t').filter(e => e != '').filter(e => e != '\t\t\t\t\t\t\t');
      console.log('split', splt);
      // console.log('empty...'); 
      if (splt.length !== 0) {
        for (let j = 0; j < buildingTypeData.length; j++) {
          const typeId = buildingTypeData[j].label;
          // console.log('typeId ==>', typeId, '-----', 'split BuildingUnit ==>', splt[1])
          if (typeId === splt[1] && splt.length !== 0) {

            // console.log('buildingTypeLoop', splt[1], buildingTypeData[j].value);
            splt[1] = (buildingTypeData[j].value);
            console.log('splt[1]', splt[1]);
            // console.log('bID.length ',bID.length);
          }
        }
        for (let u = 0; u < buildingUnitData.length; u++) {
          const unitId = buildingUnitData[u].label;

          if (unitId === splt[2] && splt.length !== 0) {
            // console.log('buildingUnitLoop', splt[2], buildingUnitData[u].value);
            splt[2] = (buildingUnitData[u].value);
            console.log('splt[2]', splt[2]);
            // console.log('uID.length ', uID.length);
          }
        }

        td[key2++] = {
          'sno': splt[0],
          'buildingType': splt[1],
          'buildingUnit': splt[2],
          'inventory': splt[3],
          'quantity': splt[4],
          'location': splt[5],
          'changes': uploadMode === 'existing' ? 'upsert' : splt[6]
        };
      }

    }
    tableData = td;
    console.log('tableData', tableData);

    let body = { "upload": tableData };
    console.log('body: ', body);
    for (let i = 0; i < body.upload.length; i++) {
      const element = body.upload[i];
      if (element === null) {
        setExcelData(null);
      }
    }

    try {
      const response = await API.POST(apiEndpoints.BOQupload, body);
      const data = await response.data;

      console.log('postTable response: ', response);
      setIsLoading(false);
      if (response.success) {
        for (let i = 0; i < data.length; i++) {
          responseMsg = data[i].message;
          responseMsgSet.add(responseMsg);
          responseSNo = data[i].sno;
          // console.log('responseSNo:', responseSNo);
          dataErrors = data[i].columns;
          // console.log('dataErrors:', dataErrors);
        }
        console.log("responseMsgSet: ", responseMsgSet);
        responseMsgList = Array.from(responseMsgSet);
        console.log("responseMsgList: ", responseMsgList);

        for (let j = 0; j < responseMsgList.length; j++) {
          const msg = responseMsgList[j];
          console.log(msg);
          if (msg !== null) {
            if (msg !== 'Successfully done') {
              enqueueSnackbar('Highlighted ' + msg, {
                variant: "error",
              });
              // enqueueSnackbar('Highlighted row will be Overwritten', {
              //   variant: "error",
              // });
            }
            else {
              enqueueSnackbar('Uploaded Successfully', {
                variant: "success",
              });
              setExcelData(null);
              console.log('Excel Data at response', excelData);
            }
          } else {
            enqueueSnackbar('Please Correct the Highlighted Fields', {
              variant: "error",
            });
          }
        }

        // if (responseMsg !== null) {
        //   if (responseMsg !== 'Successfully done') {
        //     enqueueSnackbar('Highlighted ' + responseMsg, {
        //       variant: "error",
        //     });
        //     // enqueueSnackbar('Highlighted row will be Overwritten', {
        //     //   variant: "error",
        //     // });
        //   }
        //   else {
        //     enqueueSnackbar('Uploaded Successfully', {
        //       variant: "success",
        //     });
        //     setExcelData(null);

        //     console.log('Excel Data at response', excelData);
        //   }
        // } else {
        //   enqueueSnackbar('Please Correct the Highlighted Fields', {
        //     variant: "error",
        //   });
        // }
        responseData = data;
        // setResponseData(data);

        { dataErrors && (findindError()) };
        setResponseStatus(true);
        console.log('after', responseStatus);
      }
      if (response.errorMessage) {
        enqueueSnackbar(response.errorMessage, {
          variant: "error",
        });
        setResponseStatus(true);
      }
    }
    catch (error) {
      setIsLoading(false);
      setResponseStatus(true);
      console.log(error);
      enqueueSnackbar('Something went wrong', {
        variant: "error",
      });
      // setTimeout(function () {
      //   enqueueSnackbar('Reloading in 3 Seconds', {
      //     variant: "error",
      //   });
      // }, 1000);
      // setTimeout(function () {
      //   window.location.reload();
      //   console.log('Page Reloaded');
      // }, 4000);
    }
    console.log('responseData', responseData);
  };


  const fileHandler = (event) => {
    let selectedFile = event.target.files[0];
    //just pass the selectedFile as parameter
    setExcelFileName(selectedFile);
    if (selectedFile) {
      console.log(selectedFile.type);
      if (selectedFile && fileType.includes(selectedFile.type)) {
        let reader = new FileReader();
        reader.readAsArrayBuffer(selectedFile);
        reader.onload = (e) => {
          setExcelFileError(null);
          setExcelFile(e.target.result);
        }
      }
      else {
        setExcelFileError('Please Select Excel file type');
        setExcelFile(null);
      }
    }
    else {
      console.log('please select your file')
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault();
    if (excelFile !== null) {
      const workbook = XLSX.read(excelFile, { type: 'buffer' });
      const worksheetName = workbook.SheetNames[0];
      const worksheet = workbook.Sheets[worksheetName];
      const data = XLSX.utils.sheet_to_json(worksheet);

      function extractHeader(ws) {
        const header = [];
        const range = XLSX.utils.decode_range(ws['!ref']);
        const numCols = Math.min(range.e.c + 1, 5);
        for (let i = 0; i < numCols; ++i) {
          const cell = ws[`${XLSX.utils.encode_col(i)}1`];
          header[i] = cell ? cell.h : '';
        }
        return header;
      }

      const heads = extractHeader(worksheet);
      console.log('heads', heads);

      const isValidBase = heads[0] === "Inventory" && heads[1] === "Quantity" && heads[2] === "FinalLocation";
      if (!isValidBase) {
        enqueueSnackbar("The headers in your excel are not supported, Please use our provided Sample Excel", {
          variant: "error",
        });
        return;
      }

      const mode = (heads[3] === "Changes") ? 'new' : 'existing';
      setUploadMode(mode);

      excelLength = data.length;
      for (let i = 0; i < buildingUnitIds.length; i++) {
        counter++;
        for (let j = 0; j < data.length; j++) {
          buildingUnitName.push(buildingUnit[i]);
          excelDataM.push(data[j]);
        }
      }
      setBuildingUnitNameforTable(buildingUnitName);
      setExcelData(excelDataM);
    }
    else {
      setExcelData(null);
    }
  }

  const handleSelectBuildingType = (e) => {
    setBuildingUnitData(null);
    setBuildingUnitDD(null);
    console.log('BuildingType', e);
    setBuildingType(e.label);
    console.log('buildingTypeName', buildingType);
    setBuildingTypeDD(e);
    for (let i = 0; i < buildingTypeData.length; i++) {
      const element = buildingTypeData[i].label;
      console.log('elementType', element)
      if (element.includes(e.label)) {
        buildingTypeId = e.value;
      }
    }
    // buildingTypeId=e;
    console.log('buildingTypeId', buildingTypeId);
    if (e) {
      getBuildingUnitsDDAPI(buildingTypeId);
    }

    // setBuildingTypeIdForPost(buildingTypeId);
  }


  const handleSelectBuildingUnit = (e) => {
    console.log('BuildingUnit', e)
    setBuildingUnit(e.map(e => e.label));
    setBuildingUnitDD(e);
    for (let i = 0; i < e.length; i++) {
      buildingUnitId.push(e[i].value)
    }
    const form = document.querySelector("form");
    console.log(form);
    if (buildingUnitId) {
      form.removeAttribute("hidden");
    }
    // if(buildingUnitId){
    //   form.setAttribute("hidden");
    // }
    console.log('buildingUnitId:', buildingUnitId,);
    setBuildingUnitIds(buildingUnitId);
    setBuildingUnitIdForPost(buildingUnitId);
  }


  const Dropdown = ({ options }) => {
    return (
      <div className='form-group' style={{ 'marginTop': '20px' }}>
        <p>Building Type</p>
        <div className="dropdown">
          <Select
            options={options}
            placeholder="Select Building Type"
            value={buildingTypeDD}
            onChange={handleSelectBuildingType}
            isSearchable={true}
          /></div>
      </div>
    );
  };


  const Dropdown2 = ({ options }) => {
    return (
      <div className="form-group" style={{ 'marginTop': '20px' }}>
        <p>Building Unit</p>
        <div className="dropdown">
          <Select
            options={options}
            placeholder="Select Building Unit"
            value={buildingUnitDD}
            onChange={handleSelectBuildingUnit}
            isSearchable={true}
            isMulti
          /></div>
      </div>
    );
  };


  const downloadSample = async () => {
    try {
      const response = await axios.get(apiEndpoints.downloadBOQSample, {
        headers: {
          'Authorization': 'Bearer ' + token,
          'tenant-id': states.tennant.tennant_id
        },
        responseType: 'blob'
      });
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const a = document.createElement('a');
      a.href = url;
      a.download = 'BOQ_Sample_Template.xlsx';
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (error) {
      enqueueSnackbar('Failed to download template', { variant: 'error' });
    }
  };

  const downloadExistingBOQ = async () => {
    if (!buildingTypeDD || !buildingUnitDD || buildingUnitDD.length === 0) {
      enqueueSnackbar('Please select Building Type and Building Unit first', { variant: 'error' });
      return;
    }
    if (buildingUnitDD.length > 1) {
      enqueueSnackbar('Please select only one Building Unit to download existing BOQ', { variant: 'error' });
      return;
    }
    try {
      const btId = buildingTypeDD.value;
      const buId = buildingUnitDD[0].value;
      const response = await axios.get(
        `${apiEndpoints.downloadExistingBOQ}?buildingTypeId=${btId}&buildingUnitId=${buId}`,
        {
          headers: {
            'Authorization': 'Bearer ' + token,
            'tenant-id': states.tennant.tennant_id
          },
          responseType: 'blob'
        }
      );
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const a = document.createElement('a');
      a.href = url;
      a.download = 'Existing_BOQ.xlsx';
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (error) {
      enqueueSnackbar('Failed to download existing BOQ', { variant: 'error' });
    }
  };



  const handleDeleteRow = () => {

    $(".delete").click(function () {
      var row = $(this).closest("tr");    // Finds the row
      row.find(".row-data").empty().attr("contenteditable", "false");
    });

    var rowCount = $('.row-data').contents();
    console.log('rowCount', rowCount.length);
    if (rowCount.length == 0) {
      setExcelData(null);
    }

    // const table = document.querySelector("table");
    // // console.log("TableLength: ", table.rows.length);
    // // for (var i = 0, row; row = table.rows[i]; i++) {
    // //   const rowItem = table.rows[i].innerText;
    // //   console.log('rowItem', rowItem.length);

    // // }
    // for (var i = 0, row; row = table.rows[i]; i++) {
    //   for (var j = 0, col; col = row.cells[j]; j++) {
    //     const cellItems = row.cells[j];
    //     console.log(cellItems.length);
    //     // if (!cellItems) {
    //     //   setExcelData(null);
    //     // }
    //   }
    // }

    // setExcelData(null);

    // console.log(data); 
    // console.log(index);
    // console.log(excelData);
    // let newData = new Array();
    // newData = (excelData.filter((v, i) => i !== index));
    // console.log('newData: ', newData);

    // else {
    //   setExcelData(newData);
    // }
    // console.log(excelData);
  }

  function LoadingSpinner() {
    return (
      <div className="spinner-container">
        <div className="loading-spinner"></div>
      </div>
    );
  }

  const handleReset = () => {
    console.log('resetting..');
    setIsReset(true);
    setExcelData(null);
    setTimeout(
      function () {
        setIsReset(false);
      }, 1000);
  }
  // const hiddenFileInput = React.useRef(null);
  // const handleClick = event => {
  //   hiddenFileInput.current.click();
  // };
  return (
    <div className="list-section">
      <div className='filter-section' >
        <div className="flex" style={{ "display": "flex", gap: "1rem" }}>
          <div className="col-md-4">
            <Dropdown options={buildingTypeData} />
          </div>
          <div className="col-md-4">
            <Dropdown2 options={buildingUnitData} />
          </div>
          <div className="col-md-4"></div>
        </div>

        <div className='form-group' style={{ display: 'flex', gap: '1rem', marginTop: '8px' }}>
          <IconButtons
            icon={"DownloadSVG"}
            buttonClass="filterIcon"
            label={'Download Template'}
            onClick={() => downloadSample()}
          />
          <IconButtons
            icon={"DownloadSVG"}
            buttonClass="filterIcon"
            label={'Download Existing BOQ'}
            onClick={() => downloadExistingBOQ()}
          />
        </div>

        <form className='form'
          autoComplete='off'
          hidden
          onSubmit={handleSubmit}
        >
          <br></br>
          {isReset == true
            ? LoadingSpinner()
            :
            // <div> 
            <input
              type="file"
              className="file-control"
              required
              // ref={hiddenFileInput}
              onChange={fileHandler}
              style={{
                //  display: 'none', 
                color: 'blue'
              }} />
            // <button required for="file-control" onClick={handleClick}>{ excelFile ? "File Selected": "Select an excel"}</button>
            //  </div>
          }

          {excelFileError
            && <div className='text-danger'
              style={{ 'marginTop': 5 + 'px' }}>
              {excelFileError}
            </div>}
          <IconButtons
            type='submit'
            className='btn btn-success'
            label={'UPLOAD'}
            style={{ "marginTop": 5 + 'px' }}
          /><IconButton
            onClick={() => handleReset()}>
            {RefreshIcon({ fontSize: "medium" })}
          </IconButton>
        </form>

        {responseStatus === true
          ? <div>
            <br />
            <Button
              label={'SUBMIT'}
              onClick={() => excelData
                ? postTable()
                : enqueueSnackbar('Please upload the Excel first', { variant: "error", })}
            /></div>
          : <div>
            <br />
            <Button
              label={'SUBMIT'}
            />
          </div>}
      </div>
      <br></br>
      <br></br>
      {/* <br></br>
      <br></br> */}
      <div className='boq-table-wrapper'>
        {isLoading === true && LoadingSpinner()}
        {excelData === null ? <>Preview Here</>
          : (
            <div>
              <form>
                <table className='table' >
                  <thead>
                    <th style={{ border: "1px solid rgba(0, 0, 0, 0.5)" }} scope='col'>{tableHeading[0]}</th>
                    <th style={{ border: "1px solid rgba(0, 0, 0, 0.5)" }} scope='col'>{tableHeading[1]}</th>
                    <th style={{ border: "1px solid rgba(0, 0, 0, 0.5)" }} scope='col'>{tableHeading[2]}</th>
                    <th style={{ border: "1px solid rgba(0, 0, 0, 0.5)" }} scope='col'>{tableHeading[3]}</th>
                    <th style={{ border: "1px solid rgba(0, 0, 0, 0.5)" }} scope='col'>{tableHeading[4]}</th>
                    <th style={{ border: "1px solid rgba(0, 0, 0, 0.5)" }} scope='col'>{tableHeading[5]}</th>
                    {uploadMode === 'new' && <th style={{ border: "1px solid rgba(0, 0, 0, 0.5)" }} scope='col'>{tableHeading[6]}</th>}
                    <th style={{ border: "1px solid rgba(0, 0, 0, 0.5)" }} scope='col'><center>{tableHeading[7]}</center></th>
                  </thead>
                  <tbody>
                    {excelData.map((individualExcelData, key) => (
                      <tr key={key}>
                        <td className='row-data' index={key} id={key + 1 + tableHeading[0]} style={{ border: '1px solid rgba(0, 0, 0, 0.15)' }}><pre>{key + 1}</pre></td>
                        <td className='row-data' index={key} id={key + 1 + tableHeading[1]} style={{ border: '1px solid rgba(0, 0, 0, 0.15)' }}><pre>{buildingType}</pre></td>
                        <td className='row-data' index={key} id={key + 1 + tableHeading[2]} style={{ border: '1px solid rgba(0, 0, 0, 0.15)' }}><pre>{buildingUnitNameforTable[key]}</pre></td>
                        <td className='row-data' index={key} id={key + 1 + tableHeading[3]} style={{ border: '1px solid rgba(0, 0, 0, 0.15)' }} contenteditable='true'><pre>{individualExcelData.Inventory}</pre></td>
                        <td className='row-data' index={key} id={key + 1 + tableHeading[4]} style={{ border: '1px solid rgba(0, 0, 0, 0.15)' }} contenteditable='true'><pre>{individualExcelData.Quantity}</pre></td>
                        <td className='row-data' index={key} id={key + 1 + tableHeading[5]} style={{ border: '1px solid rgba(0, 0, 0, 0.15)' }} contenteditable='true'><pre>{individualExcelData.FinalLocation}</pre></td>
                        {uploadMode === 'new' && <td className='row-data' index={key} id={key + 1 + tableHeading[6]} style={{ border: '1px solid rgba(0, 0, 0, 0.15)' }} contenteditable='true'><pre>{individualExcelData.Changes}</pre></td>}
                        <td className='row-data' index={key} id={key + 1 + tableHeading[7]} style={{ border: '1px solid rgba(0, 0, 0, 0.15)' }}>
                          <center>
                            <IconButton
                              className='delete'
                              onClick={() => handleDeleteRow()}>
                              {DeleteIcon({ fontSize: "medium" })}
                            </IconButton>
                          </center>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </form>
            </div>
          )}
        <br></br>
        <br></br>
      </div>
    </div>
  )
}

export default BOQInputScreen;




