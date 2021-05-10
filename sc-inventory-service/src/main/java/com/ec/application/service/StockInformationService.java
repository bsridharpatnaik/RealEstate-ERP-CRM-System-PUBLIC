package com.ec.application.service;

import com.ec.application.data.SingleStockInformationDTO;
import com.ec.application.data.StockInformationDTO;
import com.ec.application.data.StockInformationV2;
import com.ec.application.model.StockInformationFromView;
import com.ec.application.repository.StockInformationRepo;

import com.ec.common.Filters.FilterDataList;
import com.ec.common.Filters.StockInformationSpecification;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class StockInformationService {


}
