package com.ec.crm.Service;

import com.ec.crm.Data.LeadDAO;
import com.ec.crm.Data.UserReturnData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {
    @Autowired
    UserDetailsService userDetailsService;

    public void exitIfNotAllowed(LeadDAO lead) throws Exception {
        UserReturnData currentUser = userDetailsService.getCurrentUser();

        if (lead.getCustomerName().toLowerCase().equals("gargi") && lead.getPrimaryMobile().equals("8989898989") && !currentUser.getUsername().toLowerCase().equals("sridhar") && !currentUser.getUsername().toLowerCase().equals("gargi"))
            throw new Exception("User is not authorized to access this information.");

    }
}
