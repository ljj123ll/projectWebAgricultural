package com.agricultural.assistplatform.job;

import com.agricultural.assistplatform.service.admin.AdminBackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduledJob {

    private final AdminBackupService adminBackupService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void autoBackup() {
        try {
            adminBackupService.createBackup("auto");
            log.info("每日自动备份执行完成");
        } catch (Exception e) {
            log.error("每日自动备份执行失败", e);
        }
    }
}
