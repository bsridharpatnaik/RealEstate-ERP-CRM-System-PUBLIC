package Deserializers;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class ToSentenceCaseDeserializer extends StdDeserializer<String>
{

	private static final long serialVersionUID = 7527542687158493910L;

	public ToSentenceCaseDeserializer()
	{
		super(String.class);
	}

	@Override
	public String deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException
	{
		String text = jsonParser.getText();
		Pattern pattern = Pattern.compile("^\\W*([a-zA-Z])|\\.\\W*([a-zA-Z])");
		Matcher matcher = pattern.matcher(text);
		StringBuffer stringBuffer = new StringBuffer("");

		while (matcher.find())
		{
			matcher.appendReplacement(stringBuffer, matcher.group(0).toUpperCase());
		}

		matcher.appendTail(stringBuffer);
		return stringBuffer.toString();
	}

}