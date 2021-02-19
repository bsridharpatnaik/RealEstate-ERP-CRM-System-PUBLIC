package com.ec.application.service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.ec.application.ReusableClasses.ReusableMethods;
import com.ec.application.config.ProjectConstants;
import com.ec.application.data.CreateMORentData;
import com.ec.application.data.MORExportDAO;
import com.ec.application.data.MachineryOnRentWithDropdownData;
import com.ec.application.data.UserReturnData;
import com.ec.application.model.APICallTypeForAuthorization;
import com.ec.application.model.MORRentModeEnum;
import com.ec.application.model.MachineryOnRent;
import com.ec.application.repository.ContractorRepo;
import com.ec.application.repository.LocationRepo;
import com.ec.application.repository.MachineryOnRentRepo;
import com.ec.application.repository.MachineryRepo;
import com.ec.application.repository.SupplierRepo;
import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.MachineryOnRentSpecifications;

@Service
@Transactional
public class MachineryOnRentService
{
	@Autowired
	MachineryOnRentRepo morRepo;

	@Autowired
	LocationRepo locationRepo;

	@Autowired
	ContractorRepo contractorRepo;

	@Autowired
	MachineryRepo machineryRepo;

	@Autowired
	SupplierRepo supplierRepo;

	@Autowired
	UserDetailsService userDetailsService;

	@Autowired
	PopulateDropdownService populateDropdownService;

	Logger log = LoggerFactory.getLogger(MachineryOnRentService.class);

	public MachineryOnRent createData(CreateMORentData payload) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		valiatePayload(payload);
		exitIfNotAuthorized(null, payload, APICallTypeForAuthorization.Create);
		nullifyNonRequiredFields(payload);
		MachineryOnRent machineryOnRent = new MachineryOnRent();
		populateData(machineryOnRent, payload);
		return morRepo.save(machineryOnRent);

	}

	private void nullifyNonRequiredFields(CreateMORentData payload)
	{
		switch (payload.getMode())
		{
		case Daily:
			payload.setEndDateTime(null);
			payload.setStartDateTime(null);
			payload.setInitialMeterReading(null);
			payload.setEndMeterReading(null);
			payload.setNoOfTrips(null);
			break;

		case Hourly:
			payload.setEndDate(null);
			payload.setStartDate(null);
			payload.setInitialMeterReading(null);
			payload.setEndMeterReading(null);
			payload.setNoOfTrips(null);
			break;

		case MeterReading:
			payload.setEndDate(null);
			payload.setStartDate(null);
			payload.setNoOfTrips(null);
			payload.setEndDateTime(null);
			payload.setStartDateTime(null);
			break;
		case TripCount:
			payload.setEndDate(null);
			payload.setStartDate(null);
			payload.setEndDateTime(null);
			payload.setStartDateTime(null);
			payload.setInitialMeterReading(null);
			payload.setEndMeterReading(null);
			break;
		default:
			break;
		}
	}

	private void valiatePayload(CreateMORentData payload) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		if (payload.getMachineryId() == null)
			throw new Exception("Machinery cannot be empty");
		if (payload.getDate() == null)
			throw new Exception("Date cannot be empty");
		if (payload.getMode() == null)
			throw new Exception("Mode cannot be empty. Please provide valid rental mode.");

		if (payload.getMachineryId() != null)
			if (!machineryRepo.existsById(payload.getMachineryId()))
				throw new Exception("Machinery not found with ID - " + payload.getMachineryId());
		if (payload.getContractorId() != null)
			if (!contractorRepo.existsById(payload.getContractorId()))
				throw new Exception("Contractor not found with ID - " + payload.getContractorId());

		if (payload.getSupplierId() != null)
			if (!supplierRepo.existsById(payload.getSupplierId()))
				throw new Exception("Supplier not found with ID - " + payload.getSupplierId());

		if (payload.getLocationId() != null)
			if (!locationRepo.existsById(payload.getLocationId()))
				throw new Exception("Building Unit not found with ID - " + payload.getLocationId());

		if (payload.getMode().equals(MORRentModeEnum.Daily))
			if (payload.getStartDate() == null || payload.getEndDate() == null)
				throw new Exception("Start Date and End Date are mandatory when rental mode is DAILY. Please correct.");

		if (payload.getMode().equals(MORRentModeEnum.Hourly))
			if (payload.getStartDateTime() == null || payload.getEndDateTime() == null)
				throw new Exception(
						"Start DateTime and End DateTime are mandatory when rental mode is HOURLY. Please correct.");

		if (payload.getMode().equals(MORRentModeEnum.MeterReading))
			if (payload.getInitialMeterReading() == null || payload.getEndMeterReading() == null)
				throw new Exception(
						"Initial Meter Reading and End Meter Reading are mandatory when rental mode is MeterReading. Please correct.");

		if (payload.getMode().equals(MORRentModeEnum.TripCount))
			if (payload.getNoOfTrips() == null)
				throw new Exception("No Of Trips is mandatory when rental mode is TripCount. Please correct.");

		if (payload.getStartDate() != null)
			if (payload.getEndDate() != null)
				if (payload.getStartDate().after(payload.getEndDate()))
					throw new Exception("End Date cannot be less than start date");

		if (payload.getStartDateTime() != null)
			if (payload.getEndDateTime() != null)
				if (payload.getStartDateTime().after(payload.getEndDateTime()))
					throw new Exception("End Date cannot be less than start date");

		if (payload.getInitialMeterReading() != null)
			if (payload.getEndMeterReading() != null)
				if (payload.getInitialMeterReading() >= payload.getEndMeterReading())
					throw new Exception("Initial Meter Reading Cannot be greater than end meter reading");

	}

	public MachineryOnRent UpdateData(CreateMORentData payload, Long id) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		valiatePayload(payload);
		nullifyNonRequiredFields(payload);
		Optional<MachineryOnRent> machineryOnRentOpt = morRepo.findById(id);
		if (!machineryOnRentOpt.isPresent())
			throw new Exception("Machinery On rent by ID " + id + " Not found");
		MachineryOnRent machineryOnRent = machineryOnRentOpt.get();
		exitIfNotAuthorized(machineryOnRent, payload, APICallTypeForAuthorization.Update);
		populateData(machineryOnRent, payload);
		return morRepo.save(machineryOnRent);

	}

	public MachineryOnRent findById(Long id) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		Optional<MachineryOnRent> machineryOnRentOpt = morRepo.findById(id);
		if (!machineryOnRentOpt.isPresent())
			throw new Exception("Machinery On rent by ID " + id + " Not found");
		MachineryOnRent machineryOnRent = machineryOnRentOpt.get();
		return morRepo.save(machineryOnRent);

	}

	public void DeleteData(Long id) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		Optional<MachineryOnRent> machineryOnRentOpt = morRepo.findById(id);
		if (!machineryOnRentOpt.isPresent())
			throw new Exception("Machinery On rent by ID " + id + " Not found");
		// MachineryOnRent machineryOnRent = machineryOnRentOpt.get();
		exitIfNotAuthorized(machineryOnRentOpt.get(), null, APICallTypeForAuthorization.Delete);
		morRepo.softDeleteById(id);
	}

	private MachineryOnRent populateData(MachineryOnRent machineryOnRent, CreateMORentData payload) throws Exception
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());

		if (payload.getSupplierId() != null)
			machineryOnRent.setSupplier(supplierRepo.findById(payload.getSupplierId()).get());

		if (payload.getContractorId() != null)
			machineryOnRent.setContractor(contractorRepo.findById(payload.getContractorId()).get());

		machineryOnRent.setMachinery(machineryRepo.findById(payload.getMachineryId()).get());

		if (payload.getLocationId() != null)
			machineryOnRent.setUsageLocation(locationRepo.findById(payload.getLocationId()).get());

		machineryOnRent.setMode(payload.getMode());

		switch (payload.getMode())
		{
		case Daily:
			machineryOnRent.setStartDate(payload.getStartDate());
			machineryOnRent.setEndDate(payload.getEndDate());
			machineryOnRent.setEndDateTime(null);
			machineryOnRent.setStartDateTime(null);
			machineryOnRent.setInitialMeterReading(null);
			machineryOnRent.setEndMeterReading(null);
			machineryOnRent.setNoOfTrips(null);
			break;
		case Hourly:
			machineryOnRent.setStartDateTime(payload.getStartDateTime());
			machineryOnRent.setEndDateTime(payload.getEndDateTime());
			machineryOnRent.setEndDate(null);
			machineryOnRent.setStartDate(null);
			machineryOnRent.setInitialMeterReading(null);
			machineryOnRent.setEndMeterReading(null);
			machineryOnRent.setNoOfTrips(null);
			break;
		case MeterReading:
			machineryOnRent.setEndMeterReading(payload.getEndMeterReading());
			machineryOnRent.setInitialMeterReading(payload.getInitialMeterReading());
			machineryOnRent.setEndDate(null);
			machineryOnRent.setStartDate(null);
			machineryOnRent.setNoOfTrips(null);
			machineryOnRent.setEndDateTime(null);
			machineryOnRent.setStartDateTime(null);
			break;
		case TripCount:
			machineryOnRent.setNoOfTrips(payload.getNoOfTrips());
			machineryOnRent.setEndDate(null);
			machineryOnRent.setStartDate(null);
			machineryOnRent.setEndDateTime(null);
			machineryOnRent.setStartDateTime(null);
			machineryOnRent.setInitialMeterReading(null);
			machineryOnRent.setEndMeterReading(null);
			break;
		default:
			break;
		}
		machineryOnRent.setAmountCharged(payload.getAmountCharged());
		machineryOnRent.setDate(payload.getDate());
		machineryOnRent.setVehicleNo(payload.getVehicleNo());
		machineryOnRent.setAdditionalNotes(payload.getAdditionalNotes());
		machineryOnRent.setFileInformations(ReusableMethods.convertFilesListToSet(payload.getFileInformations()));
		machineryOnRent.setRate(payload.getRate());
		return machineryOnRent;
	}

	@Transactional
	private void exitIfNotAuthorized(MachineryOnRent mor, CreateMORentData morData, APICallTypeForAuthorization action)
			throws Exception
	{

		UserReturnData currentUserData = userDetailsService.getCurrentUser();

		if (action.equals(APICallTypeForAuthorization.Update))
		{
			if (currentUserData.getRoles().contains("admin")
					|| currentUserData.getRoles().contains("inventory-manager"))
			{
				if (ReusableMethods.daysBetweenTwoDates(mor.getDate(),
						new Date()) > ProjectConstants.editAllowedDaysAdmin)
					throw new Exception("Cannot modify record that is created greater than "
							+ ProjectConstants.editAllowedDaysAdmin + " days ago.");

			} else if (currentUserData.getRoles().contains("inventory-executive"))
			{
				if (ReusableMethods.daysBetweenTwoDates(mor.getDate(),
						new Date()) > ProjectConstants.editAllowedDaysExecutive)
					throw new Exception("Cannot modify record that is created greater than "
							+ ProjectConstants.editAllowedDaysExecutive + " days ago.");
			} else
			{
				throw new Exception("No User role found for user!. Please contact administration to get roles added");
			}
			System.out.println(mor.getDate());
			System.out.println(morData.getDate());
			if (mor.getDate().before(morData.getDate()) || mor.getDate().after(morData.getDate()))
				throw new Exception("Date should not be modified while updating Machinery On Rent record");

			if (morData.getContractorId() != null && mor.getContractor() != null)
			{
				if (!morData.getContractorId().equals(mor.getContractor().getContactId()))
					throw new Exception("Contractor should not be modified while updating Machinery On Rent record");
			} else if ((morData.getContractorId() != null && mor.getContractor() == null)
					|| (morData.getContractorId() == null && mor.getContractor() != null))
				throw new Exception("Contractor should not be modified while updating Machinery On Rent record");

			if (morData.getMachineryId() != null && mor.getMachinery() != null)
			{
				if (!morData.getMachineryId().equals(mor.getMachinery().getMachineryId()))
					throw new Exception("Machinery should not be modified while updating Machinery On Rent record");
			} else if ((morData.getMachineryId() != null && mor.getMachinery() == null)
					|| (morData.getMachineryId() == null && mor.getMachinery() != null))
				throw new Exception("Machinery should not be modified while updating Machinery On Rent record");

			if (morData.getSupplierId() != null && mor.getSupplier() != null)
			{
				if (!morData.getSupplierId().equals(mor.getSupplier().getContactId()))
					throw new Exception("Supplier should not be modified while updating Machinery On Rent record");
			} else if ((morData.getSupplierId() != null && mor.getSupplier() == null)
					|| (morData.getSupplierId() == null && mor.getSupplier() != null))
				throw new Exception("Supplier should not be modified while updating Machinery On Rent record");
		}
		if (action.equals(APICallTypeForAuthorization.Create))
		{
			if (currentUserData.getRoles().contains("admin")
					|| currentUserData.getRoles().contains("inventory-manager"))
			{
				if (ReusableMethods.daysBetweenTwoDates(morData.getDate(),
						new Date()) > ProjectConstants.editAllowedDaysAdmin)
					throw new Exception("Cannot create Machinery On Rent with date more than "
							+ ProjectConstants.editAllowedDaysAdmin + " Days in past. ");
			}

			else if (currentUserData.getRoles().contains("inventory-executive"))
			{
				if (ReusableMethods.daysBetweenTwoDates(morData.getDate(),
						new Date()) > ProjectConstants.editAllowedDaysExecutive)
					throw new Exception("Cannot create Machinery On Rent with date more than "
							+ ProjectConstants.editAllowedDaysExecutive + " Days in past. ");
			} else
			{
				throw new Exception("No User role found for user!. Please contact administration to get roles added");
			}
		}
		if (action.equals(APICallTypeForAuthorization.Delete) || action.equals(APICallTypeForAuthorization.Reject))
		{
			if (currentUserData.getRoles().contains("admin")
					|| currentUserData.getRoles().contains("inventory-manager"))
			{
				if (ReusableMethods.daysBetweenTwoDates(morData.getDate(),
						new Date()) > ProjectConstants.editAllowedDaysAdmin)
					throw new Exception("Cannot DELETE Machinery On Rent created more than "
							+ ProjectConstants.editAllowedDaysAdmin + " Days ago. ");
			}

			else if (currentUserData.getRoles().contains("inventory-executive"))
			{
				if (ReusableMethods.daysBetweenTwoDates(morData.getDate(),
						new Date()) > ProjectConstants.editAllowedDaysExecutive)
					throw new Exception("Cannot DELETE Machinery On Rent created more than "
							+ ProjectConstants.editAllowedDaysExecutive + " Days ago. ");
			} else
			{
				throw new Exception("No User role found for user!. Please contact administration to get roles added");
			}
		}
	}

	public MachineryOnRentWithDropdownData findAllWithDropdown(FilterDataList filterDataList, Pageable pageable)
			throws ParseException
	{
		log.info("Invoked - " + new Throwable().getStackTrace()[0].getMethodName());
		MachineryOnRentWithDropdownData morWithDDData = new MachineryOnRentWithDropdownData();
		Specification<MachineryOnRent> spec = MachineryOnRentSpecifications.getSpecification(filterDataList);

		if (spec != null)
			morWithDDData.setMachineryOnRent(morRepo.findAll(spec, pageable));
		else
			morWithDDData.setMachineryOnRent(morRepo.findAll(pageable));

		morWithDDData.setMorDropdown(populateDropdownService.fetchData("mor"));
		return morWithDDData;
	}

	public List<MORExportDAO> exportData(FilterDataList filterDataList) throws Exception
	{
		Specification<MachineryOnRent> spec = MachineryOnRentSpecifications.getSpecification(filterDataList);

		if (spec != null)
		{
			if (morRepo.count(spec) > 2000)
				throw new Exception("Too many rows to export. Apply some more filters and try again");
		} else if (morRepo.count() > 2000)
			throw new Exception("Too many rows to export. Apply some more filters and try again");

		List<MachineryOnRent> morList = spec == null ? morRepo.findAll() : morRepo.findAll(spec);
		return transformForExport(morList);
	}

	private List<MORExportDAO> transformForExport(List<MachineryOnRent> morList)
	{
		List<MORExportDAO> exportList = new ArrayList<MORExportDAO>();
		for (MachineryOnRent mor : morList)
		{
			MORExportDAO dao = new MORExportDAO();
			dao.setMorid(mor.getMorid());
			dao.setDate(mor.getDate());
			dao.setMachinery(mor.getMachinery().getMachineryName());
			dao.setSupplier(mor.getSupplier() == null ? "" : mor.getSupplier().getName());
			dao.setContractor(mor.getContractor() == null ? "" : mor.getContractor().getName());
			dao.setBuildingUnit(mor.getUsageLocation() == null ? "" : mor.getUsageLocation().getLocationName());
			dao.setMode(mor.getMode());
			dao.setStartDate(mor.getStartDate());
			dao.setEndDate(mor.getEndDate());
			dao.setStartDateTime(mor.getStartDateTime());
			dao.setInitialMeterReading(mor.getInitialMeterReading());
			dao.setEndMeterReading(mor.getEndMeterReading());
			dao.setNoOfTrips(mor.getNoOfTrips());
			dao.setRate(mor.getRate());
			dao.setAmountCharged(mor.getAmountCharged());
			dao.setVehicleNo(mor.getVehicleNo());
			dao.setAdditionalNotes(mor.getAdditionalNotes());
			exportList.add(dao);
		}
		return exportList;
	}

}
