package com.ec.application.service;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.ReusableClasses.IdNameProjections;
import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.data.AllMachineriesWithNamesData;
import com.ec.application.model.Machinery;
import com.ec.application.repository.MachineryRepo;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.MachinerySpecifications;

@Service
@Transactional
public class MachineryService {

    @Autowired
    MachineryRepo machineryRepo;

    @Autowired
    CheckBeforeDeleteService checkBeforeDeleteService;

    Logger log = LoggerFactory.getLogger(MachineryService.class);

    public Machinery createMachinery(Machinery payload) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        validatePayload(payload);
        if (!machineryRepo.existsByMachineryName(payload.getMachineryName().trim())) {
            machineryRepo.save(payload);
            return payload;
        } else {
            throw new Exception("Mahcinery already exists!");
        }
    }

    private void validatePayload(Machinery payload) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (payload.getMachineryName() == null || payload.getMachineryName().trim() == "")
            throw new Exception("Mahcinery Name cannot be empty!");

    }

    public Machinery updateMachinery(Long id, Machinery payload) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        validatePayload(payload);
        Optional<Machinery> MachineryForUpdateOpt = machineryRepo.findById(id);
        Machinery MachineryForUpdate = MachineryForUpdateOpt.get();

        Machinery newMachinery = new Machinery();
        newMachinery = payload;
        if (!machineryRepo.existsByMachineryName(newMachinery.getMachineryName())
                && !newMachinery.getMachineryName().equalsIgnoreCase(MachineryForUpdate.getMachineryName())) {
            MachineryForUpdate.setMachineryName(newMachinery.getMachineryName());
            MachineryForUpdate.setMachineryDescription(newMachinery.getMachineryDescription());
        } else if (newMachinery.getMachineryName().equalsIgnoreCase(MachineryForUpdate.getMachineryName())) {
            MachineryForUpdate.setMachineryDescription(newMachinery.getMachineryDescription());
        } else {
            throw new Exception("Machinery with same Name already exists");
        }

        return machineryRepo.save(MachineryForUpdate);

    }

    public Machinery findSingleMachinery(Long id) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        Optional<Machinery> Machinerys = machineryRepo.findById(id);
        return Machinerys.get();
    }

    public void deleteMachinery(Long id) throws Exception {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        if (!checkBeforeDeleteService.isMachineryUsed(id))
            machineryRepo.softDeleteById(id);
        else
            throw new Exception("Machinery already in use");
    }

    public List<IdNameProjections> findIdAndNames() {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        // TODO Auto-generated method stub
        return machineryRepo.findIdAndNames();
    }

    public AllMachineriesWithNamesData findFilteredMachineriesWithTA(FilterDataList filterDataList, Pageable pageable) {
        log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
        AllMachineriesWithNamesData allMachineriesWithNamesData = new AllMachineriesWithNamesData();
        Specification<Machinery> spec = MachinerySpecifications.getSpecification(filterDataList);

        if (spec != null)
            allMachineriesWithNamesData.setMachineries(machineryRepo.findAll(spec, pageable));
        else
            allMachineriesWithNamesData.setMachineries(machineryRepo.findAll(pageable));

        allMachineriesWithNamesData
                .setMachineryNames(ReusableMethods.removeNullsFromStringList(machineryRepo.getNames()));
        return allMachineriesWithNamesData;
    }
}
