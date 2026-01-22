package com.ec.application.scheduled;

import com.ec.application.aspects.UseDefaultTenant;
import com.ec.application.config.SchemaConfig;
import com.ec.application.model.Draft;
import com.ec.application.multitenant.ThreadLocalStorage;
import com.ec.application.repository.DraftRepository;
import com.ec.application.service.TenantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Service
@UseDefaultTenant
public class DraftCleanupScheduler {

    @Autowired
    private DraftRepository draftRepository;

    @Autowired
    SchemaConfig schemaConfig;

    /**
     * Runs every day at 00:00 (midnight)
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void softDeleteOldDrafts() {
        LocalDate todayIST = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        Date todayStartIST = Date.from(todayIST.atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant());
        List<Draft> oldDrafts = draftRepository.findDraftsBeforeToday(todayStartIST);
        oldDrafts.forEach(draft -> draftRepository.softDeleteById(draft.getDraftId())
        );
    }
}
