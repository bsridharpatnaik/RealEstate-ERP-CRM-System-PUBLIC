package com.ec.crm.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.ec.crm.Data.AllActivitesForLeadDAO;
import com.ec.crm.Data.AllNotesForLeadDAO;
import com.ec.crm.Data.LeadActivityOnLeadInformationDTO;
import com.ec.crm.Data.LeadCreateData;
import com.ec.crm.Data.LeadDAO;
import com.ec.crm.Data.LeadDetailInfo;
import com.ec.crm.Data.LeadInformationAllTabData;
import com.ec.crm.Data.LeadInformationAllTabDataList;
import com.ec.crm.Data.LeadListWithTypeAheadData;
import com.ec.crm.Enums.LeadStatusEnum;
import com.ec.crm.Filters.FilterDataList;
import com.ec.crm.Filters.LeadSpecifications;
import com.ec.crm.Model.Address;
import com.ec.crm.Model.Lead;
import com.ec.crm.Model.Note;
import com.ec.crm.Repository.AddressRepo;
import com.ec.crm.Repository.BrokerRepo;
import com.ec.crm.Repository.LeadRepo;
import com.ec.crm.Repository.NoteRepo;
import com.ec.crm.Repository.SourceRepo;
import com.ec.crm.ReusableClasses.ReusableMethods;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class LeadService
{
	@Autowired
	LeadRepo lRepo;

	/*
	 * @Autowired SentimentRepo siRepo;
	 */

	@Autowired
	BrokerRepo bRepo;

	@Autowired
	AddressRepo aRepo;

	@Autowired
	NoteRepo nRepo;

	@Autowired
	NoteService noteService;

	@Autowired
	WebClient.Builder webClientBuilder;

	@Autowired
	HttpServletRequest request;

	@Autowired
	SourceRepo sourceRepo;

	@Autowired
	UserDetailsService userDetailsService;

	@Autowired
	PopulateDropdownService populateDropdownService;
	Long currentUserID;

	@PersistenceContext
	private EntityManager entityManager;;

	@Autowired
	LeadActivityService leadActivityService;

	@Value("${common.serverurl}")
	private String reqUrl;

	@Autowired
	ModelMapper leadToLeadDAOModelMapper;

	Logger log = LoggerFactory.getLogger(LeadService.class);

	@Transactional
	public Lead createLead(@Valid LeadCreateData payload) throws Exception
	{
		log.info("Create Lead invoked with payload " + payload.toString());
		currentUserID = userDetailsService.getCurrentUser().getId();
		log.info("Fetched current user from common-service " + currentUserID.toString());
		Lead lead = new Lead();
		log.info("Formatting mobile number received in payload");
		formatMobileNo(payload);
		log.info("Validating Payload");
		validatePayload(payload);
		log.info("Payload Validated");
		log.info("Checking if record exists with mobile number");
		exitIfMobileNoExists(payload);
		log.info("Setting lead fields from payload");
		setLeadFields(lead, payload, "create");
		log.info("Saving new lead record to database");
		lRepo.save(lead);
		log.info("Creating default activity for the lead");
		leadActivityService.createDefaultActivity(lead.getLeadId());
		log.info("Saving new lead record to database");
		return lead;
	}

	@Transactional
	public Lead updateLead(@Valid LeadCreateData payload, Long id) throws Exception
	{
		log.info("Update Lead invoked with payload " + payload.toString());
		currentUserID = userDetailsService.getCurrentUser().getId();
		log.info("Fetched current user from common-service " + currentUserID.toString());
		Optional<Lead> leadOpt = lRepo.findById(id);

		if (!leadOpt.isPresent())
			throw new Exception("Lead with ID -" + id + " Not Found");

		Lead leadForUpdate = leadOpt.get();
		log.info("Formatting mobile number received in payload");
		formatMobileNo(payload);
		log.info("Validating payload");
		validatePayload(payload);
		log.info("Payload Validated");
		exitIfUpdateNotAllowed(leadForUpdate, payload);
		if (!leadForUpdate.getPrimaryMobile().equals(payload.getPrimaryMobile()))
			exitIfMobileNoExists(payload);
		log.info("Setting lead fields from payload");
		setLeadFields(leadForUpdate, payload, "update");
		log.info("Saving new lead record to database");
		return lRepo.save(leadForUpdate);
	}

	private void exitIfUpdateNotAllowed(Lead leadForUpdate, @Valid LeadCreateData payload) throws Exception
	{
		if (leadForUpdate.getStatus().equals(LeadStatusEnum.Deal_Lost)
				|| leadForUpdate.getStatus().equals(LeadStatusEnum.Deal_Closed))
		{
			if (!leadForUpdate.getAsigneeId().equals(payload.getAssigneeId()))
				throw new Exception("Assignee cannot be changed after lead is closed/lost");
		}

	}

	public LeadDAO getSingleLead(Long id) throws Exception
	{
		Optional<Lead> leadOpt = lRepo.findById(id);

		if (!leadOpt.isPresent())
			throw new Exception("Lead with ID -" + id + " Not Found");
		LeadDAO leadDAO = leadToLeadDAOModelMapper.map(leadOpt.get(), LeadDAO.class);
		return leadDAO;
	}

	private void formatMobileNo(LeadCreateData payload) throws Exception
	{
		log.info("Invoked formatMobileNo");
		if (!(payload.getPrimaryMobile() == null) && !payload.getPrimaryMobile().equals(""))
			payload.setPrimaryMobile(ReusableMethods.normalizePhoneNumber(payload.getPrimaryMobile()));
		if (payload.getSecondaryMobile() != null && payload.getSecondaryMobile() != "")
			payload.setSecondaryMobile(ReusableMethods.normalizePhoneNumber(payload.getSecondaryMobile()));
	}

	private void setLeadFields(Lead lead, @Valid LeadCreateData payload, String type) throws Exception
	{
		log.info("Invoked setLeadFields");
		lead.setCustomerName(payload.getCustomerName());
		lead.setPrimaryMobile(payload.getPrimaryMobile());
		lead.setSecondaryMobile(payload.getSecondaryMobile());
		lead.setDateOfBirth(payload.getDateOfBirth());
		lead.setEmailId(payload.getEmailId());
		lead.setOccupation(payload.getOccupation());
		lead.setPurpose(payload.getPurpose());
		lead.setPropertyType(payload.getPropertyType());
		lead.setSentiment(payload.getSentiment());
		lead.setSource(payload.getSourceId() == null ? null : sourceRepo.findById(payload.getSourceId()).get());
		lead.setBroker(payload.getBrokerId() == null ? null : bRepo.findById(payload.getBrokerId()).get());
		lead.setAsigneeId(payload.getAssigneeId() == null ? currentUserID
				: userDetailsService.getUserFromId(payload.getAssigneeId()).getId());

		if (type.equalsIgnoreCase("create"))
		{
			lead.setStatus(LeadStatusEnum.New_Lead);
			lead.setCreatorId(currentUserID);
			lead.setAddress(setAddress(payload, new Address()));
		} else if (type.equalsIgnoreCase("update"))
			lead.setAddress(setAddress(payload, lead.getAddress()));
	}

	@Transactional
	private Address setAddress(@Valid LeadCreateData payload, Address address)
	{
		log.info("Invoked setAddress");
		address.setAddr_line1(payload.getAddressLine1());
		address.setAddr_line2(payload.getAddressLine2());
		address.setCity(payload.getCity());
		address.setPincode(payload.getPincode());
		return aRepo.save(address);
	}

	public Page<Lead> fetchAll(Pageable pageable)
	{
		return lRepo.findAll(pageable);
	}

	public LeadListWithTypeAheadData findFilteredList(FilterDataList leadFilterDataList, Pageable pageable)
			throws Exception
	{
		log.info("Invoked findFilteredList with payload - " + leadFilterDataList.toString());
		LeadListWithTypeAheadData leadListWithTypeAheadData = new LeadListWithTypeAheadData();

		log.info("Fetching filteration based on filter data received");
		Specification<Lead> spec = LeadSpecifications.getSpecification(leadFilterDataList);

		log.info("Fetching records based on specification");
		if (spec != null)
			leadListWithTypeAheadData.setLeadDetails(lRepo.findAll(spec, pageable));
		else
			leadListWithTypeAheadData.setLeadDetails(lRepo.findAll(pageable));
		log.info("Setting dropdown data");
		leadListWithTypeAheadData.setDropdownData(populateDropdownService.fetchData("lead"));
		log.info("Setting tyoeahead data");
		leadListWithTypeAheadData.setTypeAheadDataForGlobalSearch(fetchTypeAheadForLeadGlobalSearch());
		return leadListWithTypeAheadData;
	}

	List<String> fetchTypeAheadForLeadGlobalSearch()
	{
		log.info("Invoked fetchTypeAheadForLeadGlobalSearch");
		List<String> typeAhead = new ArrayList<String>();
		typeAhead.addAll(lRepo.getLeadNames());
		typeAhead.addAll(lRepo.getLeadMobileNos());
		return typeAhead;
	}

	public LeadDetailInfo findSingleLeadDetailInfo(long id) throws Exception
	{
		LeadDetailInfo leadDetails = new LeadDetailInfo();
		AllNotesForLeadDAO allNotes = noteService.getAllNotesForLead(id);
		AllActivitesForLeadDAO allActivities = leadActivityService.getAllActivitiesForLead(id);
		leadDetails.setAllNotes(allNotes);
		leadDetails.setAllActivities(allActivities);
		leadDetails.setAllNotedAndActivities(transformDataForAllTab(allNotes, allActivities));
		return leadDetails;
	}

	private LeadInformationAllTabDataList transformDataForAllTab(AllNotesForLeadDAO allNotes,
			AllActivitesForLeadDAO allActivities)
	{
		LeadInformationAllTabDataList list = new LeadInformationAllTabDataList();
		list.setPendingActivities(transformPendingActivitiesForAllTab(allActivities));
		list.setPinnedNotes(transformPinnedNotesForAllTab(allNotes));
		list.setPastNotesAndActivities(transformPastNotesAndActivitiesForAllTab(allNotes, allActivities));
		return list;
	}

	private List<LeadInformationAllTabData> transformPastNotesAndActivitiesForAllTab(AllNotesForLeadDAO allNotes,
			AllActivitesForLeadDAO allActivities)
	{
		List<Note> pastNotes = allNotes.getUnPinnedNotes().stream()
				.sorted(Comparator.comparing(Note::getCreated).reversed()).collect(Collectors.toList());
		;
		List<LeadActivityOnLeadInformationDTO> pastActivities = allActivities.getPastActivities().stream()
				.sorted(Comparator.comparing(LeadActivityOnLeadInformationDTO::getActivityDateTime).reversed())
				.collect(Collectors.toList());
		List<LeadInformationAllTabData> liaDataList = new ArrayList<LeadInformationAllTabData>();
		for (Note note : pastNotes)
		{
			LeadInformationAllTabData liad = new LeadInformationAllTabData();
			liad.setCreator(note.getCreatorId());
			liad.setDateTime(note.getCreated());
			liad.setType("pastNote");
			liad.setNote(note);
			liaDataList.add(liad);
		}
		for (LeadActivityOnLeadInformationDTO la : pastActivities)
		{
			LeadInformationAllTabData liad = new LeadInformationAllTabData();
			liad.setCreator(la.getCreatorId());
			liad.setDateTime(la.getActivityDateTime());
			liad.setLeadActivity(la);
			liad.setType("pastActivity");
			liaDataList.add(liad);
		}
		System.out.println("Before Sorting");
		for (LeadInformationAllTabData laid : liaDataList)
			System.out.println(laid.getDateTime());
		return liaDataList.stream().sorted(Comparator.comparing(LeadInformationAllTabData::getDateTime).reversed())
				.collect(Collectors.toList());
	}

	private List<LeadInformationAllTabData> transformPinnedNotesForAllTab(AllNotesForLeadDAO allNotes)
	{
		ArrayList<LeadInformationAllTabData> liaDataList = new ArrayList<LeadInformationAllTabData>();
		for (Note pinnedNote : allNotes.getPinnedNotes())
		{
			LeadInformationAllTabData liaData = new LeadInformationAllTabData();
			liaData.setCreator(pinnedNote.getCreatorId());
			liaData.setDateTime(pinnedNote.getCreated());
			liaData.setType("note");
			liaData.setNote(pinnedNote);
			liaDataList.add(liaData);
		}
		return liaDataList.stream().sorted(Comparator.comparing(LeadInformationAllTabData::getDateTime).reversed())
				.collect(Collectors.toList());
	}

	private List<LeadInformationAllTabData> transformPendingActivitiesForAllTab(AllActivitesForLeadDAO allActivities)
	{
		ArrayList<LeadInformationAllTabData> liaDataList = new ArrayList<LeadInformationAllTabData>();
		for (LeadActivityOnLeadInformationDTO laid : allActivities.getPendingActivities())
		{
			LeadInformationAllTabData liaData = new LeadInformationAllTabData();
			liaData.setCreator(laid.getCreatorId());
			liaData.setDateTime(laid.getActivityDateTime());
			liaData.setLeadActivity(laid);
			liaData.setType("activity");
			liaDataList.add(liaData);
		}
		return liaDataList.stream().sorted(Comparator.comparing(LeadInformationAllTabData::getDateTime).reversed())
				.collect(Collectors.toList());
	}

	public List<Lead> history(Long id)
	{
		AuditReader reader = AuditReaderFactory.get(entityManager);
		AuditQuery q = reader.createQuery().forRevisionsOfEntity(Lead.class, true, true);
		q.add(AuditEntity.id().eq(id));
		List<Lead> audit = q.getResultList();
		return audit;
	}

	private void validatePayload(LeadCreateData payload) throws Exception
	{
		log.info("Invoked validatePayload");
		if (!validateRequiredFields(payload).equals(""))
			throw new Exception("Required fields missing - " + validateRequiredFields(payload));

		if (!ReusableMethods.isValidMobileNumber(payload.getPrimaryMobile()))
			throw new Exception("Please enter valid mobile number.");

		if (payload.getSecondaryMobile() != null && payload.getSecondaryMobile() != "")
			if (!ReusableMethods.isValidMobileNumber(payload.getSecondaryMobile()))
				throw new Exception("Please enter valid mobile number.");

		if (payload.getEmailId() != null && payload.getEmailId() != "")
			if (!ReusableMethods.isValidEmail(payload.getEmailId()))
				throw new Exception("Please enter valid EmailId.");

		if (payload.getPincode() != null && payload.getPincode() != "")
			if (!payload.getPincode().matches("\\d{6}"))
				throw new Exception("Enter a valid pin code (6 Digits numeric)");
	}

	private void exitIfMobileNoExists(LeadCreateData payload) throws Exception
	{
		log.info("Invoked exitIfMobileNoExists");
		if (lRepo.findCountByPMobileNo(payload.getPrimaryMobile()) > 0)
			throw new Exception("Contact already exists by Primary Mobile Number.");
	}

	private String validateRequiredFields(LeadCreateData payload)
	{
		log.info("Invoked validateRequiredFields");
		String message = "";
		if (payload.getPrimaryMobile() == null || payload.getPrimaryMobile().equals(""))
			message = message == "" ? "Primary Mobile No." : message + ",Primary Mobile No.";

		if (payload.getCustomerName() == null || payload.getCustomerName().equals(""))
			message = message == "" ? "Customer Name" : message + ", Customer Name";

		return message;
	}
}
