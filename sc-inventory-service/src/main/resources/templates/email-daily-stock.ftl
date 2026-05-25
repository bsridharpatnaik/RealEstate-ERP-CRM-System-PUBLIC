<html>
<head>
<meta charset="utf-8">
<style>
  body{font-family:Arial,sans-serif;color:#333;margin:0;padding:0;background:#f4f4f4;}
  .wrap{max-width:620px;margin:20px auto;background:#fff;border-radius:6px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);}
  .hdr{background:#2E7D32;color:#fff;padding:24px 20px;text-align:center;}
  .hdr h2{margin:0;font-size:22px;letter-spacing:0.5px;}
  .hdr p{margin:6px 0 0;font-size:13px;opacity:.85;}
  .body{padding:24px 20px;}
  .body p{font-size:14px;margin:0 0 16px;}
  table{border-collapse:collapse;width:100%;margin-top:12px;}
  th{background:#4CAF50;color:#fff;padding:10px 14px;text-align:left;font-size:13px;}
  td{padding:10px 14px;font-size:13px;border-bottom:1px solid #eee;}
  tr:nth-child(even) td{background:#F1F8E9;}
  .badge-g{background:#E8F5E9;color:#2E7D32;padding:2px 12px;border-radius:12px;font-weight:700;font-size:13px;}
  .badge-r{background:#FFEBEE;color:#C62828;padding:2px 12px;border-radius:12px;font-weight:700;font-size:13px;}
  .note{font-size:12px;color:#777;margin-top:20px;line-height:1.8;background:#fafafa;padding:12px 14px;border-left:3px solid #4CAF50;border-radius:0 4px 4px 0;}
  .ftr{background:#f5f5f5;padding:14px;text-align:center;font-size:11px;color:#aaa;margin-top:0;}
</style>
</head>
<body>
<div class="wrap">
  <div class="hdr">
    <h2>Daily Stock Report</h2>
    <p>${currentDate}</p>
  </div>
  <div class="body">
    <p>Please find the complete stock report attached as an Excel file. Below is a quick summary across all projects:</p>
    <table>
      <thead>
        <tr>
          <th>Project</th>
          <th>Items in Stock</th>
          <th>Zero Stock Items</th>
        </tr>
      </thead>
      <tbody>
        <#list projects as p>
        <tr>
          <td><strong>${p.projectName}</strong></td>
          <td><span class="badge-g">${p.stockRows?size}</span></td>
          <td><span class="badge-r">${p.zeroStockItems?size}</span></td>
        </tr>
        </#list>
      </tbody>
    </table>
    <div class="note">
      The attached Excel contains one sheet per project with:<br>
      &bull; <strong>Stock Summary</strong> &mdash; all products with total quantities<br>
      &bull; <strong>Warehouse Breakdown</strong> &mdash; per-warehouse quantities (non-zero only)<br>
      &bull; <strong>Zero Stock Items</strong> &mdash; list of inventory with no stock
    </div>
  </div>
  <div class="ftr">Automated report &mdash; do not reply to this email</div>
</div>
</body>
</html>
