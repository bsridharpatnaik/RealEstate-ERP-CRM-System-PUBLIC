package com.ec.application.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.ec.application.aspects.CheckAuthority;
import com.ec.application.aspects.AllowOnly;
import com.ec.application.data.BOQDashboardResponse;
import com.ec.application.data.BOQDto;
import com.ec.application.data.BOQInformation;
import com.ec.application.data.BOQReportResponse;
import com.ec.application.data.BOQUploadValidationResponse;
import com.ec.application.data.UsageLocationResponse;
import com.ec.application.service.BOQService;
import com.ec.application.Filters.BOQStatusFilterDataList;

@RestController
@RequestMapping("/boqupload")
public class BOQController {

    @Autowired
    BOQService bOQService;

    @PostMapping("/boq_upload")
    @ResponseStatus(HttpStatus.CREATED)
    //@AllowOnly(roles = {"admin", "project-manager"})
    public List<BOQUploadValidationResponse> boqUpload(@RequestBody BOQDto boqDto) throws Exception {
        return bOQService.boqUpload(boqDto);
    }

    @PostMapping("/get_boq_status_details")
    @ResponseStatus(HttpStatus.OK)
    public BOQInformation getBoqStatueInformation(@RequestBody BOQStatusFilterDataList filterDataList,
                                                  @PageableDefault(page = 0, size = 10) Pageable pageable) {
        return bOQService.fetchBoqStatusInformationv2(filterDataList, pageable);
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

    @GetMapping("/getboqquantity")
    public String getBoqQuantityForOutward(@RequestParam Long productId, @RequestParam Long locationId, @RequestParam Long finalLocationId) {
        return bOQService.getBoqQuantityForOutward(productId, locationId, finalLocationId);
    }

    @GetMapping("/boq-dashboard-summary")
    @ResponseStatus(HttpStatus.OK)
    public BOQDashboardResponse getBOQDashboardSummary() {
        return bOQService.getBOQDashboardData();
    }

    @GetMapping("/download-sample")
    public ResponseEntity<byte[]> downloadSampleExcel() throws Exception {
        byte[] excel = bOQService.generateSampleExcel();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"BOQ_Sample_Template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    @GetMapping("/export-status")
    public ResponseEntity<byte[]> exportBOQStatus(
            @RequestParam(required = false) List<String> buildingType,
            @RequestParam(required = false) List<String> buildingUnit,
            @RequestParam(required = false) List<String> product,
            @RequestParam(required = false) List<String> category,
            @RequestParam(required = false) List<String> consumedPercentage) throws Exception {
        byte[] excel = bOQService.exportBOQStatusExcel(buildingType, buildingUnit, product, category, consumedPercentage);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"BOQ_Status.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    @GetMapping("/download-existing")
    public ResponseEntity<byte[]> downloadExistingBOQ(
            @RequestParam Long buildingTypeId,
            @RequestParam Long buildingUnitId) throws Exception {
        byte[] excel = bOQService.generateExistingBoqExcel(buildingTypeId, buildingUnitId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Existing_BOQ.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    @DeleteMapping("/boq_upload/{id}")
    @ResponseStatus(HttpStatus.OK)
    @CheckAuthority
    public void deleteBOQEntry(@PathVariable("id") int id) {
        bOQService.deleteBoqById(id);
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
