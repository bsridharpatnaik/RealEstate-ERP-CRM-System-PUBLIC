package com.ec.application.service;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.data.AllBuildingTypesWithNames;
import com.ec.application.model.BuildingType;
import com.ec.application.repository.BuildingTypeRepo;
import com.ec.common.Filters.BuildingTypeSpecifications;
import com.ec.common.Filters.FilterDataList;

@Service
@Transactional
public class BuildingTypeService {
    @Autowired
    BuildingTypeRepo buildingTypeRepo;

    @Autowired
    CheckBeforeDeleteService checkBeforeDeleteService;

    Logger log = LoggerFactory.getLogger(BuildingTypeService.class);

    public Page<BuildingType> findAll(Pageable pageable) {
        return buildingTypeRepo.findAll(pageable);
    }

    public BuildingType createBuildingType(BuildingType payload) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        validatePayload(payload);
        long currentSize = buildingTypeRepo.count();
        /*
         * if (currentSize == 10) throw new
         * Exception("Limit reached. Cannot add more than 10 building types");
         */
        if (!buildingTypeRepo.existsBytypeName(payload.getTypeName().trim())) {
            buildingTypeRepo.save(payload);
            return payload;
        } else {
            throw new Exception("BuildingType already exists!");
        }
    }

    private void validatePayload(BuildingType payload) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (payload.getTypeName() == null)
            throw new Exception("Building Type name cannot be null or empty");
        if (payload.getTypeName().trim() == null || payload.getTypeName().trim() == "")
            throw new Exception("Building Type name cannot be null or empty");

    }

    public BuildingType updateBuildingType(Long id, BuildingType payload) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        validatePayload(payload);
        Optional<BuildingType> BuildingTypeForUpdateOpt = buildingTypeRepo.findById(id);
        if (!BuildingTypeForUpdateOpt.isPresent())
            throw new Exception("Building type with ID not found");

        BuildingType BuildingTypeForUpdate = BuildingTypeForUpdateOpt.get();

        if (!buildingTypeRepo.existsBytypeName(payload.getTypeName())
                && !payload.getTypeName().equalsIgnoreCase(BuildingTypeForUpdate.getTypeName())) {
            BuildingTypeForUpdate.setTypeName(payload.getTypeName());
            BuildingTypeForUpdate.setTypeDescription(payload.getTypeDescription());
        } else if (payload.getTypeName().equalsIgnoreCase(BuildingTypeForUpdate.getTypeName())) {
            BuildingTypeForUpdate.setTypeDescription(payload.getTypeDescription());
        } else {
            throw new Exception("BuildingType with same Name already exists");
        }

        return buildingTypeRepo.save(BuildingTypeForUpdate);

    }

    public BuildingType findSingleBuildingType(Long id) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Optional<BuildingType> types = buildingTypeRepo.findById(id);
        return types.get();
    }

    public void deleteBuildingType(Long id) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (!checkBeforeDeleteService.isBuildingTypeUsed(id))
            buildingTypeRepo.softDeleteById(id);
        else
            throw new Exception("Cannot delete BuildingType. BuildingType already assigned to Bulding Unit");
    }

    public List<IdNameProjections> findIdAndNames() {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        return buildingTypeRepo.findIdAndNames();
    }

    public AllBuildingTypesWithNames findFilteredBuildingTypesWithTA(FilterDataList filterDataList, Pageable pageable) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        AllBuildingTypesWithNames allBuildingTypesWithNamesData = new AllBuildingTypesWithNames();
        Specification<BuildingType> spec = BuildingTypeSpecifications.getSpecification(filterDataList);

        if (spec != null)
            allBuildingTypesWithNamesData.setBuildingTypes(buildingTypeRepo.findAll(spec, pageable));
        else
            allBuildingTypesWithNamesData.setBuildingTypes(buildingTypeRepo.findAll(pageable));

        allBuildingTypesWithNamesData
                .setNames(ReusableMethods.removeNullsFromStringList(buildingTypeRepo.getBuildingTypeNames()));
        return allBuildingTypesWithNamesData;

    }
}
