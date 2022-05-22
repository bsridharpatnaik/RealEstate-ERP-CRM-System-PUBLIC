package com.ec.application.controller;

import java.util.List;

import com.ec.application.aspects.CheckAuthority;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.ec.application.data.AllBuildingTypesWithNames;
import com.ec.application.model.BuildingType;
import com.ec.application.service.BuildingTypeService;
import com.ec.common.Filters.FilterDataList;

@RestController
@RequestMapping("/buildingtype")
public class BuildingTypeController {

    @Autowired
    BuildingTypeService buildingTypeService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public AllBuildingTypesWithNames returnFilteredCategories(@RequestBody FilterDataList filterDataList,
                                                              @PageableDefault(page = 0, size = 10, sort = "creationDate", direction = Direction.DESC) Pageable pageable) {
        return buildingTypeService.findFilteredBuildingTypesWithTA(filterDataList, pageable);
    }

    @GetMapping("/{id}")
    public BuildingType findBuildingTypebyvehicleNoBuildingTypes(@PathVariable long id) {
        return buildingTypeService.findSingleBuildingType(id);
    }

    @DeleteMapping(value = "/{id}")
    @CheckAuthority
    public ResponseEntity<?> deleteBuildingType(@PathVariable Long id) throws Exception {
        buildingTypeService.deleteBuildingType(id);
        return ResponseEntity.ok("Entity deleted");
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @CheckAuthority
    public BuildingType createBuildingType(@RequestBody BuildingType payload) throws Exception {
        return buildingTypeService.createBuildingType(payload);
    }

    @PutMapping("/{id}")
    @CheckAuthority
    public BuildingType updateBuildingType(@PathVariable Long id, @RequestBody BuildingType BuildingType)
            throws Exception {
        return buildingTypeService.updateBuildingType(id, BuildingType);
    }

    @GetMapping("/idandnames")
    public List<IdNameProjections> returnIDandNames() {
        return buildingTypeService.findIdAndNames();
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
