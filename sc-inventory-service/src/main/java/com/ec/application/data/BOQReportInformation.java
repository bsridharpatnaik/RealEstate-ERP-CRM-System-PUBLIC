package com.ec.application.data;

import org.springframework.data.domain.Page;

import lombok.Data;

@Data
public class BOQReportInformation {

	 Page<BOQReportDto> boqReport;
}
