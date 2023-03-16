package com.ec.application.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.aspects.CheckAuthority;
import com.ec.application.data.AllInventoryReturnData;
import com.ec.application.data.BOQDto;
import com.ec.application.data.BOQInformation;
import com.ec.application.data.BOQReportInformation;
import com.ec.application.data.BOQReportResponse;
import com.ec.application.data.BOQStatusDataDto;
import com.ec.application.data.BOQStatusResponse;
import com.ec.application.data.BOQUploadValidationResponse;
import com.ec.application.data.UsageLocationResponse;
import com.ec.application.service.BOQService;
import com.ec.common.Filters.BOQStatusFilterDataList;
import com.ec.common.Filters.FilterDataList;

@RestController
@RequestMapping("/boqupload")
public class BOQController {

    @Autowired
    BOQService bOQService;

    @PostMapping("/boq_upload")
    @ResponseStatus(HttpStatus.CREATED)
    @CheckAuthority
    public List<BOQUploadValidationResponse> boqUpload(@RequestBody BOQDto boqDto) throws Exception {
        return bOQService.boqUpload(boqDto);
    }

    @PostMapping("/get_boq_status_details")
    @ResponseStatus(HttpStatus.OK)
    public BOQInformation getBoqStatueInformation(@RequestBody BOQStatusFilterDataList filterDataList,
                                                  @PageableDefault(page = 0, size = 10) Pageable pageable) throws Exception {
        Pageable newPageable = bOQService.getUpdatedPageable(pageable);

        return bOQService.fetchBoqStatusInformationv2(filterDataList, newPageable);
    }

    @GetMapping("/get_buildingunit_by_buildingtypeid/{buildingtypeid}")
    @ResponseStatus(HttpStatus.OK)
    public UsageLocationResponse getBuildingUnitByBuildingType(@PathVariable("buildingtypeid") long buildingtypeid) {
        return bOQService.getBuildingUnitByBuildingType(buildingtypeid);
    }

    @GetMapping("/get_boq_report")
    public BOQReportResponse getBoqReport() {
        return bOQService.getBoqReport();
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
