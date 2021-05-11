package com.ec.application.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ec.application.ReusableClasses.ApiOnlyMessageAndCodeError;
import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.data.UsageLocationData;
import com.ec.application.model.UsageLocation;
import com.ec.application.service.LocationService;
import com.ec.common.Filters.FilterDataList;

@RestController
@RequestMapping("/location")
public class LocationController {

    @Autowired
    LocationService locationService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public Page<UsageLocation> returnFilteredLocations(@RequestBody FilterDataList filterDataList,
                                                       @PageableDefault(page = 0, size = 10, sort = "creationDate", direction = Direction.DESC) Pageable pageable) {
        return locationService.findFilteredLocationsWithTA(filterDataList, pageable);
    }

    @GetMapping("/{id}")
    public UsageLocation findLocationbyvehicleNoLocations(@PathVariable long id) {
        return locationService.findSingleLocation(id);
    }

    @GetMapping("/typeahead/{name}")
    public List<String> findLocationforTypeAhead(@PathVariable String name) {
        return locationService.getTypeAheadForGlobalSearch(name);
    }

    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> deleteLocation(@PathVariable Long id) throws Exception {
        locationService.deleteLocation(id);
        return ResponseEntity.ok("Entity deleted");
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public UsageLocation createLocation(@RequestBody UsageLocationData payload) throws Exception {

        return locationService.createLocation(payload);
    }

    @PutMapping("/{id}")
    public UsageLocation updateLocation(@PathVariable Long id, @RequestBody UsageLocationData Location) throws Exception {
        return locationService.updateLocation(id, Location);
    }

    @GetMapping("/idandnames")
    public List<IdNameProjections> returnIdAndNames() {
        return locationService.findIdAndNames();
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
