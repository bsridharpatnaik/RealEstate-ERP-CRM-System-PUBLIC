/*
 * package com.ec.application.Deserializers;
 * 
 * import java.io.IOException; import java.text.DateFormat; import
 * java.text.SimpleDateFormat;
 * 
 * import org.springframework.beans.factory.annotation.Autowired; import
 * org.springframework.stereotype.Component;
 * 
 * import com.ec.application.model.InwardOutwardList; import
 * com.ec.application.model.InwardOutwardList_; import
 * com.ec.application.service.AllInventoryService; import
 * com.fasterxml.jackson.core.JsonGenerator; import
 * com.fasterxml.jackson.databind.SerializerProvider; import
 * com.fasterxml.jackson.databind.ser.std.StdSerializer;
 * 
 * @Component public class InwardOutwardListClosingStockSerializer extends
 * StdSerializer<InwardOutwardList> {
 * 
 * private static final long serialVersionUID = 1L;
 * 
 * @Autowired AllInventoryService aiService;
 * 
 * protected InwardOutwardListClosingStockSerializer(Class<InwardOutwardList> t)
 * { super(t); }
 * 
 * protected InwardOutwardListClosingStockSerializer() { this(null); }
 * 
 * @Override public void serialize(InwardOutwardList value, JsonGenerator gen,
 * SerializerProvider provider) throws IOException {
 * 
 * DateFormat df = new SimpleDateFormat("dd/MM/yyyy"); gen.writeStartObject();
 * gen.writeObjectField(InwardOutwardList_.PRODUCT, value.getProduct());
 * gen.writeNumberField(InwardOutwardList_.ENTRYID, value.getEntryid());
 * gen.writeNumberField(InwardOutwardList_.QUANTITY, value.getQuantity());
 * gen.writeStringField(InwardOutwardList_.CREATION_DATE,
 * df.format(value.getCreationDate()));
 * gen.writeStringField(InwardOutwardList_.LAST_MODIFIED_DATE,
 * df.format(value.getLastModifiedDate()));
 * gen.writeStringField(InwardOutwardList_.LAST_MODIFIED_BY,
 * value.getLastModifiedBy());
 * gen.writeStringField(InwardOutwardList_.CREATED_BY, value.getCreatedBy());
 * try { gen.writeNumberField(InwardOutwardList_.CLOSING_STOCK, Double.valueOf(
 * String.format("%1.2f",
 * aiService.getRecordByEntryId(value.getEntryid()).getClosingStock())));
 * 
 * } catch (Exception e) {
 * gen.writeNumberField(InwardOutwardList_.CLOSING_STOCK, 0); }
 * gen.writeEndObject(); } }
 */