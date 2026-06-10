package com.qs.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabaseMigrationService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationService.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void migrate() {
        log.info("开始检查数据库结构...");
        migrateUserTable();
        migrateArchiveTable();
        migrateTicketTable();
        migrateTicketFollowUpTable();
        migrateTicketAttachmentTable();
        migrateArchiveAttachmentTable();
        migrateReminderTable();
        migrateSysSeqTable();
        migrateTicketColumns();
        migrateUserEnabledColumn();
        migrateTicketUpgradeColumns();
        migrateTicketNoColumn();
        migrateProcessNotesToFollowUp();
        log.info("数据库结构检查完成");
    }

    private void migrateUserTable() {
        if (tableExists("T_USER")) {
            return;
        }
        log.info("创建表 T_USER");
        jdbcTemplate.execute("""
                CREATE TABLE T_USER (
                   ID VARCHAR(36) NOT NULL,
                   USERNAME VARCHAR(50) NOT NULL,
                   PASSWORD VARCHAR(100) NOT NULL,
                   DISPLAY_NAME VARCHAR(50) NOT NULL,
                   ENABLED TINYINT(1) NOT NULL DEFAULT 0,
                   CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   PRIMARY KEY (ID),
                   UNIQUE KEY UK_T_USER_USERNAME (USERNAME)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void migrateArchiveTable() {
        if (tableExists("T_ARCHIVE")) {
            return;
        }
        log.info("创建表 T_ARCHIVE");
        jdbcTemplate.execute("""
                CREATE TABLE T_ARCHIVE (
                   ID VARCHAR(36) NOT NULL,
                   PROJECT_NAME VARCHAR(200) NOT NULL,
                   PROJECT_TYPE VARCHAR(100),
                   LAUNCH_DATE DATE,
                   MAINT_EXPIRE_DATE DATE,
                   LAUNCH_PLAN TEXT,
                   SPECIAL_PROCESS TEXT,
                   CONTACT_INFO TEXT,
                   REMOTE_METHOD TEXT,
                   ONSITE_MANAGER VARCHAR(100),
                   IMPL_MANAGER VARCHAR(100),
                   CREATE_BY VARCHAR(50),
                   CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   PRIMARY KEY (ID)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void migrateTicketTable() {
        if (tableExists("T_TICKET")) {
            return;
        }
        log.info("创建表 T_TICKET");
        jdbcTemplate.execute("""
                CREATE TABLE T_TICKET (
                   ID VARCHAR(36) NOT NULL,
                   ARCHIVE_ID VARCHAR(36) NOT NULL,
                   ORDER_TYPE VARCHAR(30) NOT NULL,
                   CONTENT TEXT,
                   CONTACT_INFO TEXT,
                   SUBMITTER VARCHAR(50),
                   HANDLER VARCHAR(50),
                   STATUS VARCHAR(20) DEFAULT '已提交',
                   EXPECTED_COMPLETE_DATE DATE,
                   TARGET_COMPLETE_DATE DATE,
                   ATTENTION_NOTE TEXT,
                   PROCESS_NOTE TEXT,
                   UPGRADE_BY VARCHAR(50),
                   UPGRADE_TIME DATETIME,
                   TICKET_NO BIGINT,
                   CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   PRIMARY KEY (ID),
                   UNIQUE KEY UK_T_TICKET_NO (TICKET_NO),
                   KEY IDX_T_TICKET_ARCHIVE (ARCHIVE_ID),
                   CONSTRAINT FK_T_TICKET_ARCHIVE FOREIGN KEY (ARCHIVE_ID) REFERENCES T_ARCHIVE (ID)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void migrateTicketFollowUpTable() {
        if (tableExists("T_TICKET_FOLLOWUP")) {
            return;
        }
        log.info("创建表 T_TICKET_FOLLOWUP");
        jdbcTemplate.execute("""
                CREATE TABLE T_TICKET_FOLLOWUP (
                   ID VARCHAR(36) NOT NULL,
                   TICKET_ID VARCHAR(36) NOT NULL,
                   CONTENT TEXT NOT NULL,
                   CREATE_BY VARCHAR(50),
                   CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   PRIMARY KEY (ID),
                   KEY IDX_T_FOLLOWUP_TICKET (TICKET_ID),
                   CONSTRAINT FK_T_FOLLOWUP_TICKET FOREIGN KEY (TICKET_ID) REFERENCES T_TICKET (ID)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void migrateTicketAttachmentTable() {
        if (tableExists("T_TICKET_ATTACHMENT")) {
            return;
        }
        log.info("创建表 T_TICKET_ATTACHMENT");
        jdbcTemplate.execute("""
                CREATE TABLE T_TICKET_ATTACHMENT (
                   ID VARCHAR(36) NOT NULL,
                   TICKET_ID VARCHAR(36) NOT NULL,
                   ATTACHMENT_TYPE VARCHAR(10) NOT NULL,
                   ORIGINAL_NAME VARCHAR(255) NOT NULL,
                   STORED_NAME VARCHAR(255) NOT NULL,
                   RELATIVE_PATH VARCHAR(500) NOT NULL,
                   CONTENT_TYPE VARCHAR(100),
                   FILE_SIZE BIGINT,
                   CREATE_BY VARCHAR(50),
                   CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   PRIMARY KEY (ID),
                   KEY IDX_T_ATTACH_TICKET (TICKET_ID),
                   CONSTRAINT FK_T_ATTACH_TICKET FOREIGN KEY (TICKET_ID) REFERENCES T_TICKET (ID)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void migrateArchiveAttachmentTable() {
        if (tableExists("T_ARCHIVE_ATTACHMENT")) {
            return;
        }
        log.info("创建表 T_ARCHIVE_ATTACHMENT");
        jdbcTemplate.execute("""
                CREATE TABLE T_ARCHIVE_ATTACHMENT (
                   ID VARCHAR(36) NOT NULL,
                   ARCHIVE_ID VARCHAR(36) NOT NULL,
                   ORIGINAL_NAME VARCHAR(255) NOT NULL,
                   STORED_NAME VARCHAR(255) NOT NULL,
                   RELATIVE_PATH VARCHAR(500) NOT NULL,
                   CONTENT_TYPE VARCHAR(100),
                   FILE_SIZE BIGINT,
                   CREATE_BY VARCHAR(50),
                   CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   PRIMARY KEY (ID),
                   KEY IDX_T_ATTACH_ARCHIVE (ARCHIVE_ID),
                   CONSTRAINT FK_T_ATTACH_ARCHIVE FOREIGN KEY (ARCHIVE_ID) REFERENCES T_ARCHIVE (ID)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void migrateReminderTable() {
        if (tableExists("T_REMINDER")) {
            return;
        }
        log.info("创建表 T_REMINDER");
        jdbcTemplate.execute("""
                CREATE TABLE T_REMINDER (
                   ID VARCHAR(36) NOT NULL,
                   TICKET_ID VARCHAR(36) NOT NULL,
                   TARGET_USER VARCHAR(50) NOT NULL,
                   MESSAGE VARCHAR(500) NOT NULL,
                   REMIND_DATE DATE NOT NULL,
                   REMIND_HOUR TINYINT NOT NULL,
                   IS_READ VARCHAR(1) NOT NULL DEFAULT '0',
                   CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   PRIMARY KEY (ID),
                   CONSTRAINT FK_T_REMINDER_TICKET FOREIGN KEY (TICKET_ID) REFERENCES T_TICKET (ID)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void migrateSysSeqTable() {
        if (!tableExists("T_SYS_SEQ")) {
            log.info("创建表 T_SYS_SEQ");
            jdbcTemplate.execute("""
                    CREATE TABLE T_SYS_SEQ (
                       NAME VARCHAR(50) NOT NULL,
                       NEXT_VAL BIGINT NOT NULL,
                       PRIMARY KEY (NAME)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM T_SYS_SEQ WHERE NAME = 'TICKET_NO'",
                Integer.class);
        if (count == null || count == 0) {
            Long max = tableExists("T_TICKET")
                    ? jdbcTemplate.queryForObject("SELECT COALESCE(MAX(TICKET_NO), 0) FROM T_TICKET", Long.class)
                    : 0L;
            long startWith = max == null ? 1 : max + 1;
            log.info("初始化序号 TICKET_NO，起始值 {}", startWith);
            jdbcTemplate.update("INSERT INTO T_SYS_SEQ (NAME, NEXT_VAL) VALUES ('TICKET_NO', ?)", startWith);
        }
    }

    private void migrateTicketColumns() {
        addColumnIfMissing("T_TICKET", "TARGET_COMPLETE_DATE", "DATE");
        addColumnIfMissing("T_TICKET", "ATTENTION_NOTE", "TEXT");
    }

    private void migrateTicketUpgradeColumns() {
        addColumnIfMissing("T_TICKET", "UPGRADE_BY", "VARCHAR(50)");
        addColumnIfMissing("T_TICKET", "UPGRADE_TIME", "DATETIME");
    }

    private void migrateTicketNoColumn() {
        addColumnIfMissing("T_TICKET", "TICKET_NO", "BIGINT");
        backfillTicketNo();
        createIndexIfMissing("T_TICKET", "UK_T_TICKET_NO",
                "CREATE UNIQUE INDEX UK_T_TICKET_NO ON T_TICKET (TICKET_NO)");
        syncTicketNoSeq();
    }

    private void backfillTicketNo() {
        int updated = jdbcTemplate.update("""
                UPDATE T_TICKET t
                INNER JOIN (
                    SELECT ID, ROW_NUMBER() OVER (ORDER BY CREATE_TIME, ID) rn FROM T_TICKET
                ) x ON x.ID = t.ID
                SET t.TICKET_NO = x.rn
                WHERE t.TICKET_NO IS NULL
                """);
        if (updated > 0) {
            log.info("已为 {} 条工单补全序号", updated);
        }
    }

    private void syncTicketNoSeq() {
        if (!tableExists("T_SYS_SEQ")) {
            return;
        }
        Long max = jdbcTemplate.queryForObject("SELECT COALESCE(MAX(TICKET_NO), 0) FROM T_TICKET", Long.class);
        long nextVal = max == null ? 1 : max + 1;
        jdbcTemplate.update(
                "UPDATE T_SYS_SEQ SET NEXT_VAL = ? WHERE NAME = 'TICKET_NO' AND NEXT_VAL < ?",
                nextVal, nextVal);
    }

    private void migrateUserEnabledColumn() {
        if (addColumnIfMissing("T_USER", "ENABLED", "TINYINT(1) NOT NULL DEFAULT 0")) {
            jdbcTemplate.update("UPDATE T_USER SET ENABLED = 1 WHERE ENABLED IS NULL");
            log.info("已将现有用户账号设为启用");
        } else {
            jdbcTemplate.update("UPDATE T_USER SET ENABLED = 1 WHERE ENABLED IS NULL");
        }
    }

    private void migrateProcessNotesToFollowUp() {
        if (!tableExists("T_TICKET_FOLLOWUP") || !columnExists("T_TICKET", "PROCESS_NOTE")) {
            return;
        }
        int inserted = jdbcTemplate.update("""
                INSERT INTO T_TICKET_FOLLOWUP (ID, TICKET_ID, CONTENT, CREATE_BY, CREATE_TIME)
                SELECT UUID(), ID, PROCESS_NOTE, HANDLER, CREATE_TIME
                FROM T_TICKET
                WHERE PROCESS_NOTE IS NOT NULL AND LENGTH(PROCESS_NOTE) > 0
                  AND NOT EXISTS (SELECT 1 FROM T_TICKET_FOLLOWUP f WHERE f.TICKET_ID = T_TICKET.ID)
                """);
        if (inserted > 0) {
            log.info("已将 {} 条旧 PROCESS_NOTE 迁移到跟进记录表", inserted);
        }
    }

    /** @return true 表示本次新加了字段 */
    private boolean addColumnIfMissing(String table, String column, String definition) {
        if (columnExists(table, column)) {
            return false;
        }
        log.info("添加字段 {}.{}", table, column);
        jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        return true;
    }

    private void createIndexIfMissing(String table, String indexName, String ddl) {
        if (indexExists(table, indexName)) {
            return;
        }
        log.info("创建索引 {}", indexName);
        jdbcTemplate.execute(ddl);
    }

    private boolean columnExists(String table, String column) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                table.toUpperCase(),
                column.toUpperCase());
        return count != null && count > 0;
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?",
                Integer.class,
                table.toUpperCase());
        return count != null && count > 0;
    }

    private boolean indexExists(String table, String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                Integer.class,
                table.toUpperCase(),
                indexName.toUpperCase());
        return count != null && count > 0;
    }
}
