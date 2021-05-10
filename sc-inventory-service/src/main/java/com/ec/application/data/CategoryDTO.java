package com.ec.application.data;

import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Data;

@Data
public class CategoryDTO
{
	Long categoryId;
	String categoryName;

	@JsonDeserialize(using = ToSentenceCaseDeserializer.class)
	String categoryDescription;
}
