package com.ec.application.Deserializers;

import java.io.IOException;
import java.util.Date;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.ec.application.data.InwardInventoryExportDAO;
import com.ec.application.model.InwardInventory;
import com.ec.application.model.InwardOutwardList;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@Component
public class InwardExportSerializer extends StdSerializer<InwardInventoryExportDAO> 
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	protected InwardExportSerializer(Class<InwardInventoryExportDAO> t) 
	{
		super(t);
	}
	protected InwardExportSerializer() 
	{
		this(null);
	}
	@Override
	public void serialize(InwardInventoryExportDAO value, JsonGenerator gen, SerializerProvider provider) throws IOException 
	{
		for(InwardOutwardList ioList:value.getInwardOutwardList())
		{
			System.out.println("Serialization starts for inwardid"+value.getInwardid());
			gen.writeStartObject();
			gen.writeNumberField("inwardid", value.getInwardid());
			gen.writeStringField("date",value.getDate().toString());
			gen.writeStringField("vehicleNo",value.getVehicleNo()!=null?value.getVehicleNo():"");
			gen.writeStringField("supplierSlipNo",value.getVehicleNo()!=null?value.getVehicleNo():"");
			gen.writeStringField("ourSlipNo",value.getOurSlipNo()!=null?value.getOurSlipNo():"");
			gen.writeStringField("product",ioList.getProduct().getProductName()!=null?ioList.getProduct().getProductName():"");
			gen.writeNumberField("quantity",ioList.getQuantity()!=null?ioList.getQuantity():0.0);
			gen.writeNumberField("closing stock",ioList.getClosingStock()!=null?ioList.getClosingStock():0.0);
			gen.writeStringField("warehouse",value.getWarehouse()!=null?value.getWarehouse():"");
			gen.writeStringField("supplier",value.getSupplier()!=null?value.getSupplier():"");
			gen.writeStringField("additionalInfo",value.getAdditionalInfo()!=null?value.getAdditionalInfo():"");
			gen.writeStringField("invoiceReceived",value.getInvoiceReceived()!=null?value.getInvoiceReceived().toString():"");
			gen.writeEndObject();
			System.out.println("Serialization ends for inwardid"+value.getInwardid());
		}
		
	}
		
}