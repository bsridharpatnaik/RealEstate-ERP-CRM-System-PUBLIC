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
          'changes': splt[6]
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
      const heads = extractHeader(worksheet);
      const data = XLSX.utils.sheet_to_json(worksheet);

      excelLength = data.length;

      console.log('excelLength', excelLength);
      console.log('buildingUnitIds Length', buildingUnitIds.length);

      for (let i = 0; i < buildingUnitIds.length; i++) {
        counter++;
        console.log('counter', counter)
        for (let j = 0; j < data.length; j++) {
          console.log(buildingUnit[i])
          buildingUnitName.push(buildingUnit[i])
          excelDataM.push(data[j])
        }
      }
      console.log("ExcelData", data);

      function extractHeader(ws) {
        const header = []
        // const columnCount = XLSX.utils.decode_range(ws['!ref']).e.c
        for (let i = 0; i < 4; ++i) {
          header[i] = ws[`${XLSX.utils.encode_col(i)}1`].h
        }
        return header
      }

      console.log('heads', heads);
      if (heads) {
        if (heads[0] !== "Inventory" || heads[1] !== "Quantity" || heads[2] !== "FinalLocation" || heads[3] !== "Changes") {
          enqueueSnackbar("The headers in your excel are not supported, Please use our provided Sample Excel", {
            variant: "error",
          });
        }
        else {
          setBuildingUnitNameforTable(buildingUnitName)
          setExcelData(excelDataM);
          console.log('excelData', excelData);
          console.log('buildingUnitName', buildingUnitName);
        }
      }


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


  const downloadSample = () => {
    console.log("Downloading...");
    var a = document.createElement('a');
    a.href = "data:application/vnd.ms-excel;base64,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";
    a.style.display = "none";
    a.download = 'demo.xlsx';
    document.body.appendChild(a);
    a.click();
  }



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

        <div className='form-group'>
          <br />
          <IconButtons
            icon={"DownloadSVG"}
            buttonClass="filterIcon"
            label={'Sample'}
            onClick={() => downloadSample()}
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
                    <th style={{ border: "1px solid rgba(0, 0, 0, 0.5)" }} scope='col'>{tableHeading[6]}</th>
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
                        <td className='row-data' index={key} id={key + 1 + tableHeading[6]} style={{ border: '1px solid rgba(0, 0, 0, 0.15)' }} contenteditable='true'><pre>{individualExcelData.Changes}</pre></td>
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




