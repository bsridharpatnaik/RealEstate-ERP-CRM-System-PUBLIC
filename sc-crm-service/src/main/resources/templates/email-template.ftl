<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Email with Table</title>
    <style>
        table {
            width: 100%;
            border-collapse: collapse;
        }
        table, th, td {
            border: 1px solid black;
        }
        th, td {
            padding: 8px;
            text-align: left;
        }
        th {
            background-color: #f2f2f2;
        }
    </style>
</head>
<body>
    <h2>Leads Report</h2>
    <table>
        <thead>
            <tr>
                <th>Lead Id</th>
                <th>Customer Name</th>
                <th>Primary Mobile</th>
                <th>Source</th>
                <th>Property Type</th>
                <th>Assignee</th>
                <th>Lead Status</th>
                <th>Activity Date Time</th>
                <th>Title</th>
                <th>Description</th>
                <th>Is Open?</th>
                <th>Activity Type</th>
                <th>Is Latest Activity?</th>
                <th>Follow Up Count</th>
            </tr>
        </thead>
        <tbody>
            <#list leads as lead>
            <tr>
                <td>${lead.leadId}</td>
                <td>${lead.customerName}</td>
                <td>${lead.primaryMobile}</td>
                <td>${lead.source}</td>
                <td>${lead.propertyType}</td>
                <td>${lead.assignee}</td>
                <td>${lead.leadStatus}</td>
                <td>${lead.activityDateTime}</td>
                <td>${lead.title}</td>
                <td>${lead.description}</td>
                <td>${lead.isOpen}</td>
                <td>${lead.activityType}</td>
                <td>${lead.isLatestActivity}</td>
                <td>${lead.followUpCount}</td>
            </tr>
            </#list>
        </tbody>
    </table>
</body>
</html>
