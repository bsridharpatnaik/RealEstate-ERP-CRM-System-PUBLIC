<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>Nightly Stock Balance Validation</title>
<style>
  body { font-family: Arial, Helvetica, sans-serif; font-size: 13px; color: #333; }
  h2   { color: #333; }
  .ok  { color: #2e7d32; font-size: 16px; font-weight: bold; }
  .err { color: #c62828; font-size: 16px; font-weight: bold; }
  table {
    border-collapse: collapse;
    width: 100%;
    font-size: 12px;
  }
  th {
    background-color: #c62828;
    color: white;
    padding: 8px 10px;
    text-align: left;
  }
  td {
    border: 1px solid #ddd;
    padding: 7px 10px;
  }
  tr:nth-child(even) { background-color: #fafafa; }
  tr:hover           { background-color: #fff3e0; }
  .neg { color: #c62828; font-weight: bold; }
  .pos { color: #1565c0; font-weight: bold; }
</style>
</head>
<body>
  <p>Hi,</p>
  <p>Nightly stock balance validation completed on <strong>${currentDate}</strong>.</p>
  <p>Formula checked: <em>Inward + Transfer In − Outward − Lost/Damaged − Transfer Out − Write-Off = Stock in Hand</em></p>
  <p>Tolerance: ±0.001</p>

  <#if discrepancies?has_content>
    <p class="err">⚠ ${discrepancies?size} discrepancy(ies) found:</p>
    <table>
      <thead>
        <tr>
          <th>Project (Tenant)</th>
          <th>Product</th>
          <th>Inward</th>
          <th>Transfer In</th>
          <th>Outward</th>
          <th>Lost / Damaged</th>
          <th>Transfer Out</th>
          <th>Write-Off</th>
          <th>Expected Stock</th>
          <th>Actual Stock</th>
          <th>Discrepancy</th>
        </tr>
      </thead>
      <tbody>
        <#list discrepancies as row>
        <tr>
          <td>${row.tenant}</td>
          <td>${row.productName}</td>
          <td>${row.totalInward?string["0.###"]}</td>
          <td>${row.totalTransferIn?string["0.###"]}</td>
          <td>${row.totalOutward?string["0.###"]}</td>
          <td>${row.totalLostDamaged?string["0.###"]}</td>
          <td>${row.totalTransferOut?string["0.###"]}</td>
          <td>${row.totalWriteOff?string["0.###"]}</td>
          <td>${row.expectedStock?string["0.###"]}</td>
          <td>${row.actualStock?string["0.###"]}</td>
          <td class="${(row.discrepancy < 0)?string('neg','pos')}">${row.discrepancy?string["0.###"]}</td>
        </tr>
        </#list>
      </tbody>
    </table>
    <p style="margin-top:16px; color:#555;">
      A <strong>positive discrepancy</strong> means expected stock is higher than actual (possible missing debit).<br/>
      A <strong>negative discrepancy</strong> means actual stock is higher than expected (possible missing credit).
    </p>
  <#else>
    <p class="ok">✅ All stock balances are correct. No discrepancies found across all projects.</p>
  </#if>

  <p style="color:#999; font-size:11px; margin-top:24px;">This is an automated message from the Inventory Service nightly job.</p>
</body>
</html>
