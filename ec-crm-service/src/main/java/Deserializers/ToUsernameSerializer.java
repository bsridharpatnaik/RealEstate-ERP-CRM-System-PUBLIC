package Deserializers;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Resource;

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

	@Resource
	private Map<Long, String> userIdNameMap;

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
					username = userIdNameMap.get(value);
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
