<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Upcoming Lead Activities</title>
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
            <#list activities as activity>
            <tr>
                <td>${activity.leadId}</td>
                <td>${activity.customerName}</td>
                <td>${activity.primaryMobile}</td>
                <td>${activity.source}</td>
                <td>${activity.propertyType}</td>
                <td>${activity.assignee}</td>
                <td>${activity.leadStatus}</td>
                <td>${activity.activityDateTime}</td>
                <td>${activity.title}</td>
                <td>${activity.description}</td>
                <td>${activity.isOpen}</td>
                <td>${activity.activityType}</td>
                <td>${activity.isLatestActivity}</td>
                <td>${activity.followUpCount}</td>
            </tr>
            </#list>
        </tbody>
    </table>
</body>
</html>
