/*
 * package com.ec.crm.Service;
 * 
 * import java.util.List;
 * 
 * import org.springframework.beans.factory.annotation.Autowired; import
 * org.springframework.stereotype.Service;
 * 
 * import com.ec.crm.Data.CreateDealStructureDTO; import
 * com.ec.crm.Model.DealStructure; import com.ec.crm.Repository.ClosedLeadsRepo;
 * import com.ec.crm.Repository.DealStructureRepo; import
 * com.ec.crm.Repository.PropertyNameRepo; import
 * com.ec.crm.Repository.PropertyTypeRepo;
 * 
 * @Service public class DealStructureService {
 * 
 * @Autowired DealStructureRepo dealStructureRepo;
 * 
 * @Autowired ClosedLeadsRepo clRepo;
 * 
 * @Autowired PropertyTypeRepo ptRepo;
 * 
 * @Autowired PropertyNameRepo pnRepo;
 * 
 * public DealStructure updateDealStructure(CreateDealStructureDTO payload)
 * throws Exception { validatePayload(payload); List<DealStructure> dsList =
 * dealStructureRepo.getDealStructureByLeadID(payload.getLeadId());
 * DealStructure ds = null; if (dsList.size() > 1) throw new
 * Exception("More than one Deal Structure record found. Please contact Administrator"
 * ); else if (dsList.size() == 1) ds = dsList.get(1); else ds = new
 * DealStructure(); setFields(ds, payload); return dealStructureRepo.save(ds); }
 * 
 * private void setFields(DealStructure ds, CreateDealStructureDTO payload) {
 * ds.setAmount(payload.getAmount());
 * ds.setBookingDate(payload.getBookingDate());
 * ds.setDetails(payload.getDetails());
 * ds.setLead(clRepo.findById(payload.getLeadId()).get());
 * ds.setMode(payload.getMode());
 * 
 * ds.setPropertyName(payload.getPropertyName());
 * ds.setPropertyNumber(payload.getPropertyNumber());
 * ds.setPropertyType(payload.getPropertyType());
 * 
 * 
 * }
 * 
 * private void validatePayload(CreateDealStructureDTO payload) throws Exception
 * { if (payload.getLeadId() == null) throw new
 * Exception("Lead ID is mandatory field. Please provide LeadID");
 * 
 * if (!clRepo.existsById(payload.getLeadId())) throw new
 * Exception("Lead not found with ID - " + payload.getLeadId());
 * 
 * if (payload.getDetails().length() > 149) throw new
 * Exception("Details should be less than 150 characters"); } }
 */