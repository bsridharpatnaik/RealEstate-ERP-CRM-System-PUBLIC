package com.ec.application.controller;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.aspects.CheckAuthority;
import com.ec.application.data.AllMachineriesWithNamesData;
import com.ec.application.model.Firm;
import com.ec.application.service.FirmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/firm")
public class FirmController {

    @Autowired
    FirmService firmService;

    @GetMapping("/{id}")
    public Firm getFirm(@PathVariable long id) throws Exception {
        return firmService.findSingleFirm(id);
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @CheckAuthority
    public Firm createFirm(@RequestBody Firm payload) throws Exception {
        return firmService.createFirm(payload);
    }

    @PutMapping("/{id}")
    @CheckAuthority
    public Firm updateFirm(@PathVariable Long id, @RequestBody Firm Firm) throws Exception {
        return firmService.updateFirm(id, Firm);
    }

    @GetMapping("/idandnames")
    public List<IdNameProjections> returnIdAndNames() {
        return firmService.findIdAndNames();
    }

    @ExceptionHandler(
            {JpaSystemException.class})
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiOnlyMessageAndCodeError sqlError(Exception ex) {
        ApiOnlyMessageAndCodeError apiError = new ApiOnlyMessageAndCodeError(500,
                "Something went wrong while handling data. Contact Administrator.");
        return apiError;
    }
}
