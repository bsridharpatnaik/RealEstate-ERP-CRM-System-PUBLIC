<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
<title>Job Failure Alert</title>
<style>
  body { font-family: Arial, Helvetica, sans-serif; font-size: 13px; color: #333; }
  h2   { color: #c62828; }
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
    vertical-align: top;
  }
  tr:nth-child(even) { background-color: #fafafa; }
  .err-msg { color: #c62828; font-family: monospace; font-size: 11px; word-break: break-all; }
</style>
</head>
<body>
  <p>Hi,</p>
  <p>The following job failures were detected on <strong>${currentDate}</strong>.</p>

  <h2>⚠ ${jobName} — ${failures?size} failure(s)</h2>

  <table>
    <thead>
      <tr>
        <th>Tenant</th>
        <th>Additional Info</th>
        <th>Error</th>
        <th>Time</th>
      </tr>
    </thead>
    <tbody>
      <#list failures as f>
      <tr>
        <td>${f.tenant!"-"}</td>
        <td>${f.additionalInfo!"-"}</td>
        <td class="err-msg">${f.errorMessage!"-"}</td>
        <td>${f.failedAt?string("dd-MM-yyyy HH:mm:ss")}</td>
      </tr>
      </#list>
    </tbody>
  </table>

  <p style="color:#999; font-size:11px; margin-top:24px;">This is an automated alert from the Inventory Service.</p>
</body>
</html>
