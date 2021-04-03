package com.ec.crm.Service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ec.crm.Data.CreateCustomerDocumentDTO;
import com.ec.crm.Model.CustomerDocument;
import com.ec.crm.Model.FileInformation;
import com.ec.crm.Repository.ClosedLeadsRepo;
import com.ec.crm.Repository.CustomerDocumentRepo;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class CustomerDocumentsService
{
	@Autowired
	CustomerDocumentRepo cdRepo;

	@Autowired
	ClosedLeadsRepo clRepo;

	public CustomerDocument createDocument(CreateCustomerDocumentDTO payload) throws Exception
	{
		CustomerDocument cd = new CustomerDocument();
		validatePayload(payload);
		exitIfDocumentNameExists(payload);
		setFields(cd, payload);
		return cdRepo.save(cd);
	}

	private void exitIfDocumentNameExists(CreateCustomerDocumentDTO payload) throws Exception 
	{
		int count = cdRepo.getCountByDocumentNameAndLead(payload.getDocumentName(), payload.getLeadId());
		if(count>0)
			throw new Exception("Document - "+payload.getDocumentName() +" already exists for customer");
	}

	public List<CustomerDocument> fetchDocumentsForLead(Long id) throws Exception
	{
		if (!clRepo.existsById(id))
			throw new Exception("Customer not found with ID - " + id);
		List<CustomerDocument> documents = cdRepo.findDocumentsForLead(id);
		return documents;

	}

	public CustomerDocument updateDocument(Long id, CreateCustomerDocumentDTO payload) throws Exception
	{
		if (!cdRepo.existsById(id))
			throw new Exception("Customer Document not found with ID - " + id);
		CustomerDocument cd = cdRepo.findById(id).get();
		validatePayload(payload);
		if(!cd.getDocumentName().equals(payload.getDocumentName()))
			exitIfDocumentNameExists(payload);
		setFields(cd, payload);
		return cdRepo.save(cd);
	}

	private void setFields(CustomerDocument cd, CreateCustomerDocumentDTO payload)
	{
		cd.setDocumentName(payload.getDocumentName());
		if (payload.getFileInformation() != null)
			cd.setFileInformation(new FileInformation(payload.getFileInformation().getFileUUId(),
					payload.getFileInformation().getFileName()));
		cd.setReceivedStatus(payload.getReceivedStatus());
		cd.setLead(clRepo.findById(payload.getLeadId()).get());
	}

	private void validatePayload(CreateCustomerDocumentDTO payload) throws Exception
	{
		if (payload.getLeadId() == null)
			throw new Exception("Missing Required field - Lead Information");
		if (payload.getDocumentName() == null)
			throw new Exception("Missing Required field - Document Name");
		if (payload.getDocumentName().equals(""))
			throw new Exception("Please provide valid Document Name");
		if (!clRepo.existsById(payload.getLeadId()))
			throw new Exception("Customer not found with ID - " + payload.getLeadId());
	}

	public CustomerDocument getById(Long id) throws Exception
	{
		if (!cdRepo.existsById(id))
			throw new Exception("Customer Document not found with ID - " + id);
		CustomerDocument cd = cdRepo.findById(id).get();
		return cdRepo.save(cd);
	}

	public Boolean getDocumentStatusByLead(Long id) throws Exception
	{
		if (!clRepo.existsById(id))
			throw new Exception("Customer not found with ID - " + id);
		List<CustomerDocument> documents = cdRepo.findDocumentsForLead(id);
		if (documents.size() > 0)
			return true;
		else
			return false;
	}

}
