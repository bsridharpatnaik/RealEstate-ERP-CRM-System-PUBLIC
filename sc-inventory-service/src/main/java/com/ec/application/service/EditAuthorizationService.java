package com.ec.application.service;

import com.ec.application.ReusableClasses.ReusableMethods;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class EditAuthorizationService {

    private final ProjectConstantsService projectConstantsService;

    public void validateCreateDate(Date payloadDate) throws Exception {
        validateDate(payloadDate, "Cannot add inventory record with date older than ");
    }

    public void validateUpdateDates(Date payloadDate, Date entityDate) throws Exception {

        validateDate(entityDate, "Cannot modify record created more than ");

        if (!payloadDate.equals(entityDate)) {
            throw new IllegalStateException("Date should not be modified while updating inward inventory record");
        }
    }

    public void validateDeleteDate(Date entityDate) throws Exception {
        validateDate(entityDate, "Cannot delete inward inventory created more than ");
    }

    public void validateRejectReturnDate(Date entityDate) throws Exception {
        Long allowedDays = projectConstantsService.getRejectReturnDaysForCurrentUser();
        long diff = ReusableMethods.daysBetweenTwoDates(entityDate, new Date());
        if (diff > allowedDays) {
            throw new IllegalStateException("Cannot add reject/return for record older than " + allowedDays + " days.");
        }
    }

    // -------------------------
    // Common internal method
    // -------------------------
    private void validateDate(Date date, String errorPrefix) throws Exception {
        Long allowedDays = projectConstantsService.getInventoryEditDaysForCurrentUser();
        long diff = ReusableMethods.daysBetweenTwoDates(date, new Date());
        if (diff > allowedDays) {
            throw new IllegalStateException(errorPrefix + allowedDays + " days.");
        }
    }
}
