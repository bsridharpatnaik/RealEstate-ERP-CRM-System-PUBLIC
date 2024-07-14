package com.ec.crm.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.UserReturnData;
import com.ec.crm.Filters.FilterAttributeData;
import com.ec.crm.Filters.FilterDataList;

@Service
public class UtilService {

    @Autowired
    UserDetailsService udService;

    public FilterDataList addAssigneeToFilterData(FilterDataList leadFilterDataList) throws Exception {
        UserReturnData currentUser = udService.getCurrentUser();
        if (!currentUser.getRoles().stream().map(String::toLowerCase).collect(Collectors.toList()).contains("crm-manager") && !currentUser.getRoles().contains("admin")) {
            List<String> values = new ArrayList<String>();
            values.add(currentUser.getId().toString());
            Boolean found = false;
            for (FilterAttributeData faData : leadFilterDataList.getFilterData()) {
                if (faData.getAttrName().equalsIgnoreCase("assignee")) {
                    faData.setAttrValue(values);
                    found = true;
                }

            }
            if (!found) {
                FilterAttributeData faData = new FilterAttributeData();
                faData.setAttrName("assignee");
                faData.setAttrValue(values);
                leadFilterDataList.getFilterData().add(faData);
            }
        }
        return leadFilterDataList;
    }

    public FilterDataList addTodaysDateToFilterData(FilterDataList leadFilterDataList) {
        List<String> attributes = Arrays.asList("activityStartDate", "activityEndDate");
        if (!checkIfAttributeExists(leadFilterDataList, attributes)) {
            for (String attribute : attributes) {
                FilterAttributeData faData = new FilterAttributeData();
                List<String> values = new ArrayList<String>();
                faData.setAttrName(attribute);
                String formattedDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                values.add(formattedDate);
                faData.setAttrValue(values);
                leadFilterDataList.getFilterData().add(faData);
            }
        }
        return leadFilterDataList;
    }

    private boolean checkIfAttributeExists(FilterDataList leadFilterDataList, List<String> attributes) {
        for(FilterAttributeData faData : leadFilterDataList.getFilterData()){
            if(faData.getAttrName().equalsIgnoreCase(attributes.get(0))){
                return true;
            }
        }
        return false;
    }

    public boolean isAdminOrManager(UserReturnData currentUser) throws Exception {
        return (currentUser.getRoles().stream().map(String::toLowerCase).collect(Collectors.toList()).contains("crm-manager") || currentUser.getRoles().contains("admin"));
    }
}
