package Deserializers;

import java.io.IOException;
import java.text.DecimalFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.ec.crm.Service.LeadService;
import com.ec.crm.Service.UserDetailsService;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class ToUsernameSerializer extends JsonSerializer<Long>
{
	Logger log = LoggerFactory.getLogger(ToUsernameSerializer.class);
	
	@Autowired
	UserDetailsService userDetailsService;
	
	@Override
    public void serialize(Long value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonGenerationException 
	{
		log.info("Invoked username serialization");
        if (null == value) 
        	jgen.writeNull();
        else 
        {
        	log.info("Getting username for userid - "+value);
        	String username = userDetailsService.getUserFromId(value).getUsername();
        	log.info("Username Fetched is -"+username);
        	jgen.writeString(username);
    
        }
	}
}
