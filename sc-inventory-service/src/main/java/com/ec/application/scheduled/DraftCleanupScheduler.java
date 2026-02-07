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
    private SchemaConfig schemaConfig;

    /**
     * Runs every day at 00:00 IST
     */
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void softDeleteOldDrafts() {

        try {
            // Always run cleanup in master schema
            ThreadLocalStorage.setTenantName(schemaConfig.getMasterSchema());
            ZoneId istZone = ZoneId.of("Asia/Kolkata");
            // Start of today (00:00 IST)
            LocalDate todayIST = LocalDate.now(istZone);
            Date todayStartIST = Date.from(
                    todayIST.atStartOfDay(istZone).toInstant()
            );
            List<Draft> oldDrafts = draftRepository.findDraftsBeforeToday(todayStartIST);

            if (oldDrafts == null || oldDrafts.isEmpty()) {
                return;
            }

            for (Draft draft : oldDrafts) {
                draftRepository.softDeleteById(draft.getDraftId());
            }

        } finally {
            ThreadLocalStorage.setTenantName(null);
        }
    }
}
