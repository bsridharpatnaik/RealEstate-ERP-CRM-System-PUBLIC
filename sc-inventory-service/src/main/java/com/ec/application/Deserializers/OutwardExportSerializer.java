package com.ec.application.Deserializers;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.springframework.stereotype.Component;

import com.ec.application.data.OutwardInventoryExportDAO;
import com.ec.application.model.InwardOutwardList;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@Component
public class OutwardExportSerializer extends StdSerializer<OutwardInventoryExportDAO>
{
	private static final long serialVersionUID = 1L;

	protected OutwardExportSerializer(Class<OutwardInventoryExportDAO> t)
	{
		super(t);
	}

	protected OutwardExportSerializer()
	{
		this(null);
	}

	@Override
	public void serialize(OutwardInventoryExportDAO value, JsonGenerator gen, SerializerProvider provider)
			throws IOException
	{
		DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
		for (InwardOutwardList ioList : value.getInwardOutwardList())
		{
			System.out.println("Serialization starts for OutWardID" + value.getOutwardid());
			gen.writeStartObject();
			gen.writeNumberField("outwardid", value.getOutwardid());
			gen.writeStringField("date", df.format(value.getDate()));
			gen.writeStringField("purpose", value.getPurpose() != null ? value.getPurpose() : "");
			gen.writeStringField("slipNo", value.getSlipNo() != null ? value.getSlipNo() : "");
			gen.writeStringField("product",
					ioList.getProduct().getProductName() != null ? ioList.getProduct().getProductName() : "");
			gen.writeStringField("measurement unit",
					ioList.getProduct().getMeasurementUnit() != null ? ioList.getProduct().getMeasurementUnit() : "");
			gen.writeNumberField("quantity", ioList.getQuantity() != null ? ioList.getQuantity() : 0.0);
			gen.writeNumberField("closing stock", ioList.getClosingStock() != null ? ioList.getClosingStock() : 0.0);
			gen.writeStringField("warehouse", value.getWarehouse() != null ? value.getWarehouse() : "");
			gen.writeStringField("contractor", value.getContractor() != null ? value.getContractor() : "");
			gen.writeStringField("additionalInfo", value.getAdditionalInfo() != null ? value.getAdditionalInfo() : "");
			gen.writeStringField("buildingUnit", value.getBuildingUnit() != null ? value.getBuildingUnit() : "");
			gen.writeStringField("finalLocation", value.getFinalLocation() != null ? value.getFinalLocation() : "");
			gen.writeEndObject();
			System.out.println("Serialization ends for OutWardID" + value.getOutwardid());
		}

	}
}
