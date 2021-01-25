package com.ec.crm.Repository;

import org.springframework.stereotype.Repository;

import com.ec.crm.Model.PaymentSchedule;
import com.ec.crm.ReusableClasses.BaseRepository;

@Repository
public interface PaymentScheduleRepo extends BaseRepository<PaymentSchedule, Long>
{

}
