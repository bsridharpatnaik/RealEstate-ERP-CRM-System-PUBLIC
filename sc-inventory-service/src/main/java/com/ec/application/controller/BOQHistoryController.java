package com.ec.application.controller;

import com.ec.application.Filters.FilterDataList;
import com.ec.application.model.BOQHistory;
import com.ec.application.service.BOQHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/boqhistory")
public class BOQHistoryController {

    @Autowired
    private BOQHistoryService boqHistoryService;

    @PostMapping("/list")
    @ResponseStatus(HttpStatus.OK)
    public Page<BOQHistory> getHistory(
            @RequestBody FilterDataList filterDataList,
            @PageableDefault(page = 0, size = 50, sort = "changeDateTime") Pageable pageable) throws Exception {
        return boqHistoryService.getFiltered(filterDataList, pageable);
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> exportHistory(@RequestBody FilterDataList filterDataList) throws Exception {
        byte[] excel = boqHistoryService.exportExcel(filterDataList);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"BOQ_History.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
