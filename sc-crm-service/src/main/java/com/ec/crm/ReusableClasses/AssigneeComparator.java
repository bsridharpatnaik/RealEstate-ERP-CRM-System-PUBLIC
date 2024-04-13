package com.ec.crm.ReusableClasses;

import com.ec.crm.Data.AssigneeDAO;

import java.util.Comparator;

public class AssigneeComparator  implements Comparator<AssigneeDAO> {
    @Override
    public int compare(AssigneeDAO o1, AssigneeDAO o2) {
        // Compare the students, return -1 if o1 is "greater" than o2. Return 1 if o2 is "greater" than o1
        if (o1.getUserName().equals("Y")) return -1;
        if (o2.getUserName().equals("Y")) return 1;
        return 0;
    }
}
