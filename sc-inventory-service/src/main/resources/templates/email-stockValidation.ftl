<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>Inventory Stock Validation</title>

<style>
#inventory {
  font-family: "Trebuchet MS", Arial, Helvetica, sans-serif;
  border-collapse: collapse;
  width: 100%;
}

#inventory td, #inventory th {
  border: 1px solid #ddd;
  padding: 8px;
}

#inventory tr:nth-child(even){background-color: #f2f2f2;}

#inventory tr{
background-color: white;
}
#inventory tr:hover {background-color: #ddd;}

#inventory th {
  padding-top: 12px;
  padding-bottom: 12px;
  text-align: left;
  background-color: #4CAF50;
  color: white;
}
</style>

</head>

<body>
	<table width="100%" border="0" cellspacing="0" cellpadding="0">
	<tr>
    <td>
        <label id="welcome" style="padding-left: 20px;">Hi User,</label><br><br>
    <label id="msg" style="padding: 40px;">Find the Inventory validation as of today - ${currentDate} </label><br><br>    
    </td>
</tr>
		<tr>
			<td align="center" valign="top" bgcolor="#838383"
				style="background-color: #838383;"><br> <br>
				<table width="600" border="0" cellspacing="0" cellpadding="0">
					<tr>
						<td align="center" valign="top" bgcolor="black"
							style="background-color: black; font-family: Arial, Helvetica, sans-serif; font-size: 13px; color: #000000; padding: 0px 15px 10px 15px;">
							
							<div style="font-size: 40px; color:white;">
								<b>Inventory Validation</b>
							</div>
								<div id="testScenariosResult">
						<table id="inventory" style="width:100%">
							<!-- Header Table -->
							<thead>
								<tr>
									<th>Inventory</th>
									<th>Total Inward Stock (A)</th>
									<th>Total Outward Stock (B)</th>
									<th>Total Stock In hand (C)</th>
									<th>Difference in Stock (A+B-C)</th>
								</tr>
							</thead>
							<tbody>
							<#list inventory as inv>
							<tr>
								
									<td>${inv.inventory}</td>
									<td>${inv.totalInward}</td>
									<td>${inv.totalOutward}</td>
									<td>${inv.currentStock}</td>
									<td>${inv.diffInStock}</td>
									
							</tr> 
							</#list>
							<tfoot>
								<tr>

								</tr>
							</tfoot>
						</table>
					</div>
							
						</td>
					</tr>
				</table> <br> <br></td>
		</tr>
	</table>
	
</body>
</html>
