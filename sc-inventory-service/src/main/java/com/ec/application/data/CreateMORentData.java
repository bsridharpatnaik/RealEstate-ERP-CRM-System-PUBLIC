package com.ec.application.data;

import java.util.Date;
import java.util.List;

import org.springframework.lang.NonNull;

import com.ec.application.Deserializers.ToSentenceCaseDeserializer;
import com.ec.application.Deserializers.ToUpperCaseDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import lombok.Data;

@Data
public class CreateMORentData
{

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	@NonNull
	@JsonProperty("date")
	private Date date;

	@NonNull
	private Long machineryId;

	@NonNull
	private Long contactId;

	@NonNull
	private Long locationId;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date startDate;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private Date endDate;

	Double initialMeterReading;
	Double endMeterReading;
	Double noOfTrips;
	Double amountCharged;
	@NonNull
	String mode;
	@JsonDeserialize(using = ToUpperCaseDeserializer.class)
	String vehicleNo;
	@JsonDeserialize(using = ToSentenceCaseDeserializer.class)
	String additionalNotes;
	@NonNull
	List<FileInformationDAO> fileInformations;
	Double rate;

}
