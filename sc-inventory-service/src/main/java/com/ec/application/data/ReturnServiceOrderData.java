package com.ec.application.data;

import com.ec.application.model.ServiceOrder;
import lombok.Data;
import org.springframework.data.domain.Page;

@Data
public class ReturnServiceOrderData {
    Page<ServiceOrder> serviceOrders;
}
