<html>
<head>
<meta charset="utf-8">
<style>
  body{font-family:Arial,sans-serif;color:#333;margin:0;padding:0;background:#f4f4f4;}
  .wrap{max-width:960px;margin:20px auto;background:#fff;border-radius:6px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);}
  .hdr{background:#2E7D32;color:#fff;padding:24px 20px;text-align:center;}
  .hdr h2{margin:0;font-size:22px;letter-spacing:0.5px;}
  .hdr p{margin:6px 0 0;font-size:13px;opacity:.85;}
  .body{padding:24px 20px;}
  .body p{font-size:13px;margin:0 0 12px;color:#555;}
  .section-title{font-size:14px;font-weight:bold;color:#2E7D32;margin:28px 0 8px;border-bottom:2px solid #E8F5E9;padding-bottom:4px;}
  table{border-collapse:collapse;width:100%;margin-top:6px;}
  th{background:#2E7D32;color:#fff;padding:9px 12px;text-align:center;font-size:12px;white-space:nowrap;}
  th.left{text-align:left;}
  td{padding:8px 12px;font-size:12px;border-bottom:1px solid #eee;text-align:center;vertical-align:middle;}
  td.left{text-align:left;}
  td.total-col{font-weight:700;color:#2E7D32;background:#E8F5E9;}
  tr:nth-child(even) td{background:#F9FBF9;}
  tr:nth-child(even) td.total-col{background:#C8E6C9;}
  td.zero{color:#ccc;}
  .badge-g{background:#E8F5E9;color:#2E7D32;padding:2px 10px;border-radius:12px;font-weight:700;font-size:12px;}
  .badge-r{background:#FFEBEE;color:#C62828;padding:2px 10px;border-radius:12px;font-weight:700;font-size:12px;}
  .note{font-size:12px;color:#777;margin-top:20px;line-height:1.8;background:#fafafa;padding:12px 14px;border-left:3px solid #4CAF50;border-radius:0 4px 4px 0;}
  .ftr{background:#f5f5f5;padding:14px;text-align:center;font-size:11px;color:#aaa;}
</style>
</head>
<body>
<div class="wrap">
  <div class="hdr">
    <h2>Daily Stock Report</h2>
    <p>${currentDate}</p>
  </div>
  <div class="body">

    <!-- Cross-project consolidated table -->
    <div class="section-title">Stock Summary — All Projects</div>
    <p>Total stock per product across all projects. Zero values shown as —.</p>
    <table>
      <thead>
        <tr>
          <th class="left">Product</th>
          <#list projectNames as pn>
          <th>${pn}</th>
          </#list>
          <th>Total</th>
        </tr>
      </thead>
      <tbody>
        <#list consolidatedRows as row>
        <tr>
          <td class="left">${row.productName}</td>
          <#list row.quantities as qty>
          <#if qty == 0>
          <td class="zero">—</td>
          <#else>
          <td>${qty?string["0.##"]}</td>
          </#if>
          </#list>
          <td class="total-col">${row.total?string["0.##"]}</td>
        </tr>
        </#list>
      </tbody>
    </table>

    <!-- Per-project summary -->
    <div class="section-title">Project Summary</div>
    <table>
      <thead>
        <tr>
          <th class="left">Project</th>
          <th>Items in Stock</th>
          <th>Zero Stock Items</th>
        </tr>
      </thead>
      <tbody>
        <#list projects as p>
        <tr>
          <td class="left"><strong>${p.projectName}</strong></td>
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
