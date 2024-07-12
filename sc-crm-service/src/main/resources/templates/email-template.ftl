<!DOCTYPE html>
<html>
<head>
    <title>${title}</title>
</head>
<body>
    <h1>${heading}</h1>
    <table border="1">
        <thead>
            <tr>
                <#list headers as header>
                    <th>${header}</th>
                </#list>
            </tr>
        </thead>
        <tbody>
            <#list rows as row>
                <tr>
                    <#list row as column>
                        <td>${column}</td>
                    </#list>
                </tr>
            </#list>
        </tbody>
    </table>
</body>
</html>
