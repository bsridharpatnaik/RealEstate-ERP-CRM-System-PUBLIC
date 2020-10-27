package Deserializers;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.ec.crm.Service.UserDetailsService;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class ToUsernameSerializer extends JsonSerializer<Long>
{
	Logger log = LoggerFactory.getLogger(ToUsernameSerializer.class);

	@Autowired
	UserDetailsService userDetailsService;

	@Override
	public void serialize(Long value, JsonGenerator jgen, SerializerProvider provider)
			throws IOException, JsonGenerationException
	{
		log.info("Invoked username serialization");
		if (null == value)
			jgen.writeNull();
		else
		{
			if (value != 404) // 404 will always be system user
			{
				log.info("Getting username for userid - " + value);
				String username;
				try
				{
					username = userDetailsService.getUserFromId(value).getUsername();
					jgen.writeString(username);
				} catch (Exception e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else
				jgen.writeString("System");
		}

	}
}
