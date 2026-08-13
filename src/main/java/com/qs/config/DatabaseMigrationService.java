package com.qs.config;

import com.qs.util.IdUtils;
import com.qs.util.PinyinCodeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        migrateTicketProcessTable();
        migrateTicketChangeTable();
        migrateTicketAttachmentTable();
        migrateTicketAttachmentTypeColumn();
        migrateTicketAttachmentConfirmedColumns();
        migrateDeliveryAttachmentTable();
        migrateReminderTable();
        migrateSysSeqTable();
        migrateTicketColumns();
        migrateUserEnabledColumn();
        migrateTicketUpgradeColumns();
        migrateTicketNoColumn();
        migrateProcessNotesToFollowUp();
        migrateAnalysisProjectTable();
        migrateFlowNodeTable();
        migrateFlowNodeDeletedColumns();
        migrateFlowNodePinyinCodeColumn();
        migrateFlowNodeChangeTable();
        migrateDeliveryNodeTable();
        migrateArchiveNodeStageTable();
        migrateUserMenuPermTable();
        migrateUserDeliveryPermTable();
        migrateDeliveryModel();
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

    private void migrateTicketProcessTable() {
        if (tableExists("T_TICKET_PROCESS")) {
            return;
        }
        log.info("创建表 T_TICKET_PROCESS");
        jdbcTemplate.execute("""
                CREATE TABLE T_TICKET_PROCESS (
                   ID VARCHAR(36) NOT NULL,
                   TICKET_ID VARCHAR(36) NOT NULL,
                   PARENT_ID VARCHAR(36) DEFAULT NULL,
                   ACTION_TYPE VARCHAR(20) NOT NULL,
                   HANDLE_METHOD VARCHAR(100) DEFAULT NULL,
                   CONTENT TEXT,
                   CREATE_BY VARCHAR(50),
                   CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   PRIMARY KEY (ID),
                   KEY IDX_T_PROCESS_TICKET (TICKET_ID),
                   KEY IDX_T_PROCESS_PARENT (PARENT_ID),
                   CONSTRAINT FK_T_PROCESS_TICKET FOREIGN KEY (TICKET_ID) REFERENCES T_TICKET (ID)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void migrateTicketChangeTable() {
        if (tableExists("T_TICKET_CHANGE")) {
            return;
        }
        log.info("创建表 T_TICKET_CHANGE");
        jdbcTemplate.execute("""
                CREATE TABLE T_TICKET_CHANGE (
                   ID VARCHAR(36) NOT NULL,
                   TICKET_ID VARCHAR(36) NOT NULL,
                   FIELD_NAME VARCHAR(50) NOT NULL,
                   FIELD_LABEL VARCHAR(50),
                   OLD_VALUE TEXT,
                   NEW_VALUE TEXT,
                   CHANGE_BY VARCHAR(50),
                   CHANGE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   PRIMARY KEY (ID),
                   KEY IDX_T_TICKET_CHANGE_TICKET (TICKET_ID)
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
                   ATTACHMENT_TYPE VARCHAR(20) NOT NULL,
                   ORIGINAL_NAME VARCHAR(255) NOT NULL,
                   STORED_NAME VARCHAR(255) NOT NULL,
                   RELATIVE_PATH VARCHAR(500) NOT NULL,
                   CONTENT_TYPE VARCHAR(100),
                   FILE_SIZE BIGINT,
                   CREATE_BY VARCHAR(50),
                   CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   CONFIRMED TINYINT(1) NOT NULL DEFAULT 0,
                   CONFIRMED_BY VARCHAR(50),
                   CONFIRMED_TIME DATETIME,
                   PRIMARY KEY (ID),
                   KEY IDX_T_ATTACH_TICKET (TICKET_ID),
                   CONSTRAINT FK_T_ATTACH_TICKET FOREIGN KEY (TICKET_ID) REFERENCES T_TICKET (ID)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    /** 确认报告类型 CONFIRM 等，扩展 ATTACHMENT_TYPE 长度 */
    private void migrateTicketAttachmentTypeColumn() {
        if (!tableExists("T_TICKET_ATTACHMENT") || !columnExists("T_TICKET_ATTACHMENT", "ATTACHMENT_TYPE")) {
            return;
        }
        Integer len = jdbcTemplate.queryForObject(
                "SELECT CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                "T_TICKET_ATTACHMENT",
                "ATTACHMENT_TYPE");
        if (len != null && len >= 20) {
            return;
        }
        log.info("扩展列 T_TICKET_ATTACHMENT.ATTACHMENT_TYPE -> VARCHAR(20)");
        jdbcTemplate.execute(
                "ALTER TABLE T_TICKET_ATTACHMENT MODIFY COLUMN ATTACHMENT_TYPE VARCHAR(20) NOT NULL");
    }

    private void migrateTicketAttachmentConfirmedColumns() {
        if (!tableExists("T_TICKET_ATTACHMENT")) {
            return;
        }
        if (!columnExists("T_TICKET_ATTACHMENT", "CONFIRMED")) {
            log.info("新增列 T_TICKET_ATTACHMENT.CONFIRMED");
            jdbcTemplate.execute(
                    "ALTER TABLE T_TICKET_ATTACHMENT ADD COLUMN CONFIRMED TINYINT(1) NOT NULL DEFAULT 0");
        }
        if (!columnExists("T_TICKET_ATTACHMENT", "CONFIRMED_BY")) {
            log.info("新增列 T_TICKET_ATTACHMENT.CONFIRMED_BY");
            jdbcTemplate.execute(
                    "ALTER TABLE T_TICKET_ATTACHMENT ADD COLUMN CONFIRMED_BY VARCHAR(50)");
        }
        if (!columnExists("T_TICKET_ATTACHMENT", "CONFIRMED_TIME")) {
            log.info("新增列 T_TICKET_ATTACHMENT.CONFIRMED_TIME");
            jdbcTemplate.execute(
                    "ALTER TABLE T_TICKET_ATTACHMENT ADD COLUMN CONFIRMED_TIME DATETIME");
        }
    }

    private void migrateDeliveryAttachmentTable() {
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

    private void migrateAnalysisProjectTable() {
        if (tableExists("T_ANALYSIS_PROJECT")) {
            return;
        }
        log.info("创建表 T_ANALYSIS_PROJECT");
        jdbcTemplate.execute("""
                CREATE TABLE T_ANALYSIS_PROJECT (
                   ID VARCHAR(36) NOT NULL,
                   NAME VARCHAR(200) NOT NULL,
                   DESCRIPTION TEXT,
                   CREATE_BY VARCHAR(50),
                   CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   PRIMARY KEY (ID)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void migrateFlowNodeTable() {
        if (tableExists("T_FLOW_NODE")) {
            return;
        }
        log.info("创建表 T_FLOW_NODE");
        jdbcTemplate.execute("""
                CREATE TABLE T_FLOW_NODE (
                   ID VARCHAR(36) NOT NULL,
                   PROJECT_ID VARCHAR(36) NOT NULL,
                   PARENT_ID VARCHAR(36),
                   TITLE VARCHAR(200) NOT NULL,
                   PINYIN_CODE VARCHAR(100),
                   DESCRIPTION TEXT,
                   SORT_ORDER INT NOT NULL DEFAULT 0,
                   CREATE_BY VARCHAR(50),
                   CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   DELETED TINYINT(1) NOT NULL DEFAULT 0,
                   DELETED_BY VARCHAR(50),
                   DELETED_TIME DATETIME,
                   PRIMARY KEY (ID),
                   KEY IDX_FLOW_NODE_PROJECT (PROJECT_ID),
                   KEY IDX_FLOW_NODE_PARENT (PARENT_ID)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void migrateFlowNodeDeletedColumns() {
        if (!tableExists("T_FLOW_NODE")) {
            return;
        }
        addColumnIfMissing("T_FLOW_NODE", "DELETED", "TINYINT(1) NOT NULL DEFAULT 0");
        addColumnIfMissing("T_FLOW_NODE", "DELETED_BY", "VARCHAR(50)");
        addColumnIfMissing("T_FLOW_NODE", "DELETED_TIME", "DATETIME");
        jdbcTemplate.update("UPDATE T_FLOW_NODE SET DELETED = 0 WHERE DELETED IS NULL");
    }

    private void migrateFlowNodePinyinCodeColumn() {
        if (!tableExists("T_FLOW_NODE")) {
            return;
        }
        addColumnIfMissing("T_FLOW_NODE", "PINYIN_CODE", "VARCHAR(100)");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT ID, TITLE FROM T_FLOW_NODE WHERE PINYIN_CODE IS NULL OR TRIM(PINYIN_CODE) = ''");
        int updated = 0;
        for (Map<String, Object> row : rows) {
            String id = String.valueOf(row.get("ID"));
            String title = row.get("TITLE") == null ? "" : String.valueOf(row.get("TITLE"));
            String code = PinyinCodeUtil.toJianpin(title);
            updated += jdbcTemplate.update("UPDATE T_FLOW_NODE SET PINYIN_CODE = ? WHERE ID = ?", code, id);
        }
        if (updated > 0) {
            log.info("已回填流程拼音简码 {} 条", updated);
        }
    }

    private void migrateFlowNodeChangeTable() {
        if (tableExists("T_FLOW_NODE_CHANGE")) {
            return;
        }
        log.info("创建表 T_FLOW_NODE_CHANGE");
        jdbcTemplate.execute("""
                CREATE TABLE T_FLOW_NODE_CHANGE (
                   ID VARCHAR(36) NOT NULL,
                   NODE_ID VARCHAR(36) NOT NULL,
                   PROJECT_ID VARCHAR(36) NOT NULL,
                   OLD_TITLE VARCHAR(200),
                   NEW_TITLE VARCHAR(200),
                   OLD_DESCRIPTION TEXT,
                   NEW_DESCRIPTION TEXT,
                   CHANGE_BY VARCHAR(50),
                   CHANGE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   PRIMARY KEY (ID),
                   KEY IDX_FLOW_NODE_CHANGE_NODE (NODE_ID),
                   KEY IDX_FLOW_NODE_CHANGE_PROJECT (PROJECT_ID)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void migrateDeliveryNodeTable() {
        if (tableExists("T_ARCHIVE_NODE")) {
            return;
        }
        log.info("创建表 T_ARCHIVE_NODE");
        jdbcTemplate.execute("""
                CREATE TABLE T_ARCHIVE_NODE (
                   ID VARCHAR(36) NOT NULL,
                   ARCHIVE_ID VARCHAR(36) NOT NULL,
                   STAGE VARCHAR(30) NOT NULL,
                   TITLE VARCHAR(200) NOT NULL,
                   NODE_TYPE VARCHAR(20) NOT NULL,
                   START_DATE DATE NOT NULL,
                   END_DATE DATE,
                   REMARK TEXT,
                   SORT_ORDER INT NOT NULL DEFAULT 0,
                   CREATE_BY VARCHAR(50),
                   CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   PRIMARY KEY (ID),
                   KEY IDX_ARCHIVE_NODE_ARCHIVE (ARCHIVE_ID),
                   KEY IDX_ARCHIVE_NODE_START (START_DATE)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void migrateArchiveNodeStageTable() {
        if (tableExists("T_ARCHIVE_NODE_STAGE")) {
            return;
        }
        log.info("创建表 T_ARCHIVE_NODE_STAGE");
        jdbcTemplate.execute("""
                CREATE TABLE T_ARCHIVE_NODE_STAGE (
                   ID VARCHAR(36) NOT NULL,
                   NAME VARCHAR(50) NOT NULL,
                   SORT_ORDER INT NOT NULL DEFAULT 0,
                   COLOR_KEY VARCHAR(30),
                   DELETED TINYINT(1) NOT NULL DEFAULT 0,
                   CREATE_BY VARCHAR(50),
                   CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   PRIMARY KEY (ID),
                   KEY IDX_ARCHIVE_NODE_STAGE_NAME (NAME)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
        String[] names = {"商务阶段", "调研阶段", "上线阶段", "质保阶段", "维保阶段"};
        String[] colors = {"business", "research", "launch", "warranty", "maint"};
        for (int i = 0; i < names.length; i++) {
            jdbcTemplate.update("""
                    INSERT INTO T_ARCHIVE_NODE_STAGE (ID, NAME, SORT_ORDER, COLOR_KEY, DELETED, CREATE_BY, CREATE_TIME)
                    VALUES (UUID(), ?, ?, ?, 0, 'system', NOW())
                    """, names[i], i, colors[i]);
        }
        log.info("已初始化默认项目节点阶段");
    }

    private void migrateUserMenuPermTable() {
        if (tableExists("T_USER_MENU_PERM")) {
            return;
        }
        log.info("创建表 T_USER_MENU_PERM");
        jdbcTemplate.execute("""
                CREATE TABLE T_USER_MENU_PERM (
                   ID VARCHAR(36) NOT NULL,
                   USER_ID VARCHAR(36) NOT NULL,
                   MENU_CODE VARCHAR(50) NOT NULL,
                   CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   PRIMARY KEY (ID),
                   UNIQUE KEY UK_USER_MENU (USER_ID, MENU_CODE),
                   KEY IDX_USER_MENU_USER (USER_ID)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void migrateUserDeliveryPermTable() {
        if (tableExists("T_USER_ARCHIVE_PERM")) {
            return;
        }
        log.info("创建表 T_USER_ARCHIVE_PERM");
        jdbcTemplate.execute("""
                CREATE TABLE T_USER_ARCHIVE_PERM (
                   ID VARCHAR(36) NOT NULL,
                   USER_ID VARCHAR(36) NOT NULL,
                   ARCHIVE_ID VARCHAR(36) NOT NULL,
                   CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   PRIMARY KEY (ID),
                   UNIQUE KEY UK_USER_ARCHIVE (USER_ID, ARCHIVE_ID),
                   KEY IDX_USER_ARCHIVE_USER (USER_ID),
                   KEY IDX_USER_ARCHIVE_ARCHIVE (ARCHIVE_ID)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    /** 档案分层：使用单位 / 产品 / 产品交付 / 服务商（T_ARCHIVE → T_DELIVERY） */
    private void migrateDeliveryModel() {
        migrateCustomerTable();
        migrateProductTable();
        seedProducts();
        migratePartnerTable();
        ensureDeliveryTable();
        migrateDeliveryColumns();
        migrateLegacyDeliveryData();
        renameDeliveryRelatedTables();
        migrateUserPartnerColumn();
        migratePartnerDeliveryPermTable();
    }

    private void migrateCustomerTable() {
        if (tableExists("T_CUSTOMER")) {
            return;
        }
        log.info("创建表 T_CUSTOMER");
        jdbcTemplate.execute("""
                CREATE TABLE T_CUSTOMER (
                   ID VARCHAR(36) NOT NULL,
                   CODE VARCHAR(50),
                   NAME VARCHAR(200) NOT NULL,
                   NAME_PY VARCHAR(100),
                   ORG_TYPE VARCHAR(30),
                   CONTACT TEXT,
                   REMARK TEXT,
                   STATUS VARCHAR(20) DEFAULT '启用',
                   CREATE_BY VARCHAR(50),
                   CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   PRIMARY KEY (ID),
                   KEY IDX_CUSTOMER_NAME (NAME),
                   KEY IDX_CUSTOMER_NAME_PY (NAME_PY)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void migrateProductTable() {
        if (tableExists("T_PRODUCT")) {
            return;
        }
        log.info("创建表 T_PRODUCT");
        jdbcTemplate.execute("""
                CREATE TABLE T_PRODUCT (
                   ID VARCHAR(36) NOT NULL,
                   CODE VARCHAR(50) NOT NULL,
                   NAME VARCHAR(100) NOT NULL,
                   NAME_PY VARCHAR(100),
                   SORT_ORDER INT NOT NULL DEFAULT 0,
                   ENABLED TINYINT(1) NOT NULL DEFAULT 1,
                   REMARK TEXT,
                   CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   PRIMARY KEY (ID),
                   UNIQUE KEY UK_PRODUCT_CODE (CODE)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void seedProducts() {
        if (!tableExists("T_PRODUCT")) {
            return;
        }
        seedProductIfMissing("EQUIP", "设备系统", 1);
        seedProductIfMissing("SPD_CONSUMABLE", "耗材SPD", 2);
    }

    private void seedProductIfMissing(String code, String name, int sortOrder) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM T_PRODUCT WHERE CODE = ?", Integer.class, code);
        if (count != null && count > 0) {
            return;
        }
        log.info("预置产品 {} / {}", code, name);
        jdbcTemplate.update("""
                INSERT INTO T_PRODUCT (ID, CODE, NAME, NAME_PY, SORT_ORDER, ENABLED, CREATE_TIME)
                VALUES (?, ?, ?, ?, ?, 1, NOW())
                """, IdUtils.dashedUuid7(), code, name, PinyinCodeUtil.toJianpin(name), sortOrder);
    }

    private void migratePartnerTable() {
        if (tableExists("T_PARTNER")) {
            return;
        }
        log.info("创建表 T_PARTNER");
        jdbcTemplate.execute("""
                CREATE TABLE T_PARTNER (
                   ID VARCHAR(36) NOT NULL,
                   CODE VARCHAR(50),
                   NAME VARCHAR(200) NOT NULL,
                   NAME_PY VARCHAR(100),
                   CONTACT TEXT,
                   REMARK TEXT,
                   STATUS VARCHAR(20) DEFAULT '启用',
                   CREATE_BY VARCHAR(50),
                   CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   PRIMARY KEY (ID),
                   KEY IDX_PARTNER_NAME (NAME)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void ensureDeliveryTable() {
        if (tableExists("T_DELIVERY")) {
            return;
        }
        if (tableExists("T_ARCHIVE")) {
            log.info("重命名 T_ARCHIVE → T_DELIVERY");
            jdbcTemplate.execute("RENAME TABLE T_ARCHIVE TO T_DELIVERY");
            return;
        }
        log.info("创建表 T_DELIVERY");
        jdbcTemplate.execute("""
                CREATE TABLE T_DELIVERY (
                   ID VARCHAR(36) NOT NULL,
                   CUSTOMER_ID VARCHAR(36),
                   PRODUCT_ID VARCHAR(36),
                   DELIVERY_NAME VARCHAR(200),
                   DELIVERY_CODE VARCHAR(50),
                   PARTNER_ID VARCHAR(36),
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
                   PRIMARY KEY (ID),
                   KEY IDX_DELIVERY_CUSTOMER (CUSTOMER_ID),
                   KEY IDX_DELIVERY_PRODUCT (PRODUCT_ID),
                   KEY IDX_DELIVERY_PARTNER (PARTNER_ID)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void migrateDeliveryColumns() {
        if (!tableExists("T_DELIVERY")) {
            return;
        }
        addColumnIfMissing("T_DELIVERY", "CUSTOMER_ID", "VARCHAR(36)");
        addColumnIfMissing("T_DELIVERY", "PRODUCT_ID", "VARCHAR(36)");
        addColumnIfMissing("T_DELIVERY", "DELIVERY_NAME", "VARCHAR(200)");
        addColumnIfMissing("T_DELIVERY", "DELIVERY_CODE", "VARCHAR(50)");
        addColumnIfMissing("T_DELIVERY", "PARTNER_ID", "VARCHAR(36)");
        createIndexIfMissing("T_DELIVERY", "IDX_DELIVERY_CUSTOMER",
                "CREATE INDEX IDX_DELIVERY_CUSTOMER ON T_DELIVERY (CUSTOMER_ID)");
        createIndexIfMissing("T_DELIVERY", "IDX_DELIVERY_PRODUCT",
                "CREATE INDEX IDX_DELIVERY_PRODUCT ON T_DELIVERY (PRODUCT_ID)");
    }

    private void migrateLegacyDeliveryData() {
        if (!tableExists("T_DELIVERY") || !columnExists("T_DELIVERY", "PROJECT_NAME")) {
            return;
        }
        log.info("迁移旧档案数据到使用单位/产品/交付模型");
        Map<String, String> customerCache = new HashMap<>();
        String defaultProductId = jdbcTemplate.queryForObject(
                "SELECT ID FROM T_PRODUCT WHERE CODE = 'EQUIP' LIMIT 1", String.class);
        String spdProductId = jdbcTemplate.queryForObject(
                "SELECT ID FROM T_PRODUCT WHERE CODE = 'SPD_CONSUMABLE' LIMIT 1", String.class);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT ID, PROJECT_NAME, PROJECT_TYPE FROM T_DELIVERY WHERE CUSTOMER_ID IS NULL OR CUSTOMER_ID = ''");
        for (Map<String, Object> row : rows) {
            String deliveryId = String.valueOf(row.get("ID"));
            String projectName = row.get("PROJECT_NAME") == null ? "" : String.valueOf(row.get("PROJECT_NAME")).trim();
            String projectType = row.get("PROJECT_TYPE") == null ? "" : String.valueOf(row.get("PROJECT_TYPE")).trim();
            if (projectName.isEmpty()) {
                projectName = "未命名使用单位";
            }
            String customerId = customerCache.computeIfAbsent(projectName, name -> findOrCreateCustomer(name));
            String productId = mapLegacyProductId(projectType, defaultProductId, spdProductId);
            String deliveryName = deriveDeliveryName(projectType, deliveryId);
            jdbcTemplate.update("""
                    UPDATE T_DELIVERY
                    SET CUSTOMER_ID = ?, PRODUCT_ID = ?, DELIVERY_NAME = ?
                    WHERE ID = ?
                    """, customerId, productId, deliveryName, deliveryId);
        }
        dropColumnIfExists("T_DELIVERY", "PROJECT_NAME");
        dropColumnIfExists("T_DELIVERY", "PROJECT_TYPE");
        jdbcTemplate.update("UPDATE T_DELIVERY SET DELIVERY_NAME = '默认模块' WHERE DELIVERY_NAME IS NULL OR TRIM(DELIVERY_NAME) = ''");
        deduplicateDeliveryNames();
        createIndexIfMissing("T_DELIVERY", "UK_DELIVERY_CUST_PROD_NAME",
                "CREATE UNIQUE INDEX UK_DELIVERY_CUST_PROD_NAME ON T_DELIVERY (CUSTOMER_ID, PRODUCT_ID, DELIVERY_NAME)");
        log.info("旧档案数据迁移完成");
    }

    private void deduplicateDeliveryNames() {
        List<Map<String, Object>> dupes = jdbcTemplate.queryForList("""
                SELECT CUSTOMER_ID, PRODUCT_ID, DELIVERY_NAME, COUNT(*) CNT
                FROM T_DELIVERY
                GROUP BY CUSTOMER_ID, PRODUCT_ID, DELIVERY_NAME
                HAVING COUNT(*) > 1
                """);
        for (Map<String, Object> dupe : dupes) {
            String customerId = String.valueOf(dupe.get("CUSTOMER_ID"));
            String productId = String.valueOf(dupe.get("PRODUCT_ID"));
            String deliveryName = String.valueOf(dupe.get("DELIVERY_NAME"));
            List<String> ids = jdbcTemplate.queryForList("""
                    SELECT ID FROM T_DELIVERY
                    WHERE CUSTOMER_ID = ? AND PRODUCT_ID = ? AND DELIVERY_NAME = ?
                    ORDER BY CREATE_TIME, ID
                    """, String.class, customerId, productId, deliveryName);
            for (int i = 1; i < ids.size(); i++) {
                String suffix = "-" + (i + 1);
                jdbcTemplate.update("UPDATE T_DELIVERY SET DELIVERY_NAME = ? WHERE ID = ?",
                        deliveryName + suffix, ids.get(i));
            }
        }
    }

    private String findOrCreateCustomer(String name) {
        List<String> ids = jdbcTemplate.queryForList(
                "SELECT ID FROM T_CUSTOMER WHERE NAME = ? LIMIT 1", String.class, name);
        if (!ids.isEmpty()) {
            return ids.get(0);
        }
        String id = IdUtils.dashedUuid7();
        jdbcTemplate.update("""
                INSERT INTO T_CUSTOMER (ID, NAME, NAME_PY, ORG_TYPE, STATUS, CREATE_BY, CREATE_TIME)
                VALUES (?, ?, ?, '医院', '启用', 'system', NOW())
                """, id, name, PinyinCodeUtil.toJianpin(name));
        return id;
    }

    private String mapLegacyProductId(String projectType, String defaultProductId, String spdProductId) {
        if (projectType == null || projectType.isBlank()) {
            return defaultProductId;
        }
        String lower = projectType.toLowerCase(Locale.ROOT);
        if (lower.contains("spd") || projectType.contains("耗材")) {
            return spdProductId;
        }
        return defaultProductId;
    }

    private String deriveDeliveryName(String projectType, String deliveryId) {
        if (projectType != null && !projectType.isBlank()) {
            return projectType.trim();
        }
        return "默认模块-" + deliveryId.substring(0, 8);
    }

    private void renameDeliveryRelatedTables() {
        renameDeliveryAttachmentToDelivery();
        renameDeliveryNodeToDelivery();
        renameTicketArchiveColumn();
        renameUserDeliveryPermToDelivery();
    }

    private void renameDeliveryAttachmentToDelivery() {
        if (tableExists("T_ARCHIVE_ATTACHMENT") && !tableExists("T_DELIVERY_ATTACHMENT")) {
            dropForeignKeysReferencing("T_ARCHIVE");
            dropForeignKeysOnColumn("T_ARCHIVE_ATTACHMENT", "ARCHIVE_ID");
            log.info("重命名 T_ARCHIVE_ATTACHMENT → T_DELIVERY_ATTACHMENT");
            jdbcTemplate.execute("RENAME TABLE T_ARCHIVE_ATTACHMENT TO T_DELIVERY_ATTACHMENT");
        }
        if (tableExists("T_DELIVERY_ATTACHMENT") && columnExists("T_DELIVERY_ATTACHMENT", "ARCHIVE_ID")) {
            log.info("重命名列 T_DELIVERY_ATTACHMENT.ARCHIVE_ID → DELIVERY_ID");
            jdbcTemplate.execute("ALTER TABLE T_DELIVERY_ATTACHMENT CHANGE ARCHIVE_ID DELIVERY_ID VARCHAR(36) NOT NULL");
            createIndexIfMissing("T_DELIVERY_ATTACHMENT", "IDX_T_ATTACH_DELIVERY",
                    "CREATE INDEX IDX_T_ATTACH_DELIVERY ON T_DELIVERY_ATTACHMENT (DELIVERY_ID)");
            addDeliveryForeignKey("T_DELIVERY_ATTACHMENT", "FK_T_ATTACH_DELIVERY", "DELIVERY_ID");
        } else if (!tableExists("T_DELIVERY_ATTACHMENT")) {
            migrateDeliveryAttachmentTableFresh();
        }
    }

    private void migrateDeliveryAttachmentTableFresh() {
        if (tableExists("T_DELIVERY_ATTACHMENT")) {
            return;
        }
        log.info("创建表 T_DELIVERY_ATTACHMENT");
        jdbcTemplate.execute("""
                CREATE TABLE T_DELIVERY_ATTACHMENT (
                   ID VARCHAR(36) NOT NULL,
                   DELIVERY_ID VARCHAR(36) NOT NULL,
                   ORIGINAL_NAME VARCHAR(255) NOT NULL,
                   STORED_NAME VARCHAR(255) NOT NULL,
                   RELATIVE_PATH VARCHAR(500) NOT NULL,
                   CONTENT_TYPE VARCHAR(100),
                   FILE_SIZE BIGINT,
                   CREATE_BY VARCHAR(50),
                   CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   PRIMARY KEY (ID),
                   KEY IDX_T_ATTACH_DELIVERY (DELIVERY_ID),
                   CONSTRAINT FK_T_ATTACH_DELIVERY FOREIGN KEY (DELIVERY_ID) REFERENCES T_DELIVERY (ID)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void renameDeliveryNodeToDelivery() {
        if (tableExists("T_ARCHIVE_NODE") && !tableExists("T_DELIVERY_NODE")) {
            log.info("重命名 T_ARCHIVE_NODE → T_DELIVERY_NODE");
            jdbcTemplate.execute("RENAME TABLE T_ARCHIVE_NODE TO T_DELIVERY_NODE");
        }
        if (tableExists("T_DELIVERY_NODE") && columnExists("T_DELIVERY_NODE", "ARCHIVE_ID")) {
            log.info("重命名列 T_DELIVERY_NODE.ARCHIVE_ID → DELIVERY_ID");
            jdbcTemplate.execute("ALTER TABLE T_DELIVERY_NODE CHANGE ARCHIVE_ID DELIVERY_ID VARCHAR(36) NOT NULL");
            createIndexIfMissing("T_DELIVERY_NODE", "IDX_DELIVERY_NODE_DELIVERY",
                    "CREATE INDEX IDX_DELIVERY_NODE_DELIVERY ON T_DELIVERY_NODE (DELIVERY_ID)");
        } else if (!tableExists("T_DELIVERY_NODE")) {
            migrateDeliveryNodeTableFresh();
        }
    }

    private void migrateDeliveryNodeTableFresh() {
        if (tableExists("T_DELIVERY_NODE")) {
            return;
        }
        log.info("创建表 T_DELIVERY_NODE");
        jdbcTemplate.execute("""
                CREATE TABLE T_DELIVERY_NODE (
                   ID VARCHAR(36) NOT NULL,
                   DELIVERY_ID VARCHAR(36) NOT NULL,
                   STAGE VARCHAR(30) NOT NULL,
                   TITLE VARCHAR(200) NOT NULL,
                   NODE_TYPE VARCHAR(20) NOT NULL,
                   START_DATE DATE NOT NULL,
                   END_DATE DATE,
                   REMARK TEXT,
                   SORT_ORDER INT NOT NULL DEFAULT 0,
                   CREATE_BY VARCHAR(50),
                   CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   PRIMARY KEY (ID),
                   KEY IDX_DELIVERY_NODE_DELIVERY (DELIVERY_ID),
                   KEY IDX_DELIVERY_NODE_START (START_DATE)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void renameTicketArchiveColumn() {
        if (!tableExists("T_TICKET")) {
            return;
        }
        if (columnExists("T_TICKET", "ARCHIVE_ID") && !columnExists("T_TICKET", "DELIVERY_ID")) {
            dropForeignKeysOnColumn("T_TICKET", "ARCHIVE_ID");
            log.info("重命名列 T_TICKET.ARCHIVE_ID → DELIVERY_ID");
            jdbcTemplate.execute("ALTER TABLE T_TICKET CHANGE ARCHIVE_ID DELIVERY_ID VARCHAR(36) NOT NULL");
            createIndexIfMissing("T_TICKET", "IDX_T_TICKET_DELIVERY",
                    "CREATE INDEX IDX_T_TICKET_DELIVERY ON T_TICKET (DELIVERY_ID)");
            addDeliveryForeignKey("T_TICKET", "FK_T_TICKET_DELIVERY", "DELIVERY_ID");
        }
    }

    private void renameUserDeliveryPermToDelivery() {
        if (tableExists("T_USER_ARCHIVE_PERM") && !tableExists("T_USER_DELIVERY_PERM")) {
            log.info("重命名 T_USER_ARCHIVE_PERM → T_USER_DELIVERY_PERM");
            jdbcTemplate.execute("RENAME TABLE T_USER_ARCHIVE_PERM TO T_USER_DELIVERY_PERM");
        }
        if (tableExists("T_USER_DELIVERY_PERM") && columnExists("T_USER_DELIVERY_PERM", "ARCHIVE_ID")) {
            log.info("重命名列 T_USER_DELIVERY_PERM.ARCHIVE_ID → DELIVERY_ID");
            jdbcTemplate.execute("ALTER TABLE T_USER_DELIVERY_PERM CHANGE ARCHIVE_ID DELIVERY_ID VARCHAR(36) NOT NULL");
            createIndexIfMissing("T_USER_DELIVERY_PERM", "IDX_USER_DELIVERY_DELIVERY",
                    "CREATE INDEX IDX_USER_DELIVERY_DELIVERY ON T_USER_DELIVERY_PERM (DELIVERY_ID)");
        } else if (!tableExists("T_USER_DELIVERY_PERM")) {
            log.info("创建表 T_USER_DELIVERY_PERM");
            jdbcTemplate.execute("""
                    CREATE TABLE T_USER_DELIVERY_PERM (
                       ID VARCHAR(36) NOT NULL,
                       USER_ID VARCHAR(36) NOT NULL,
                       DELIVERY_ID VARCHAR(36) NOT NULL,
                       CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       PRIMARY KEY (ID),
                       UNIQUE KEY UK_USER_DELIVERY (USER_ID, DELIVERY_ID),
                       KEY IDX_USER_DELIVERY_USER (USER_ID),
                       KEY IDX_USER_DELIVERY_DELIVERY (DELIVERY_ID)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                    """);
        }
    }

    private void migrateUserPartnerColumn() {
        addColumnIfMissing("T_USER", "PARTNER_ID", "VARCHAR(36)");
    }

    private void migratePartnerDeliveryPermTable() {
        if (tableExists("T_PARTNER_DELIVERY_PERM")) {
            return;
        }
        log.info("创建表 T_PARTNER_DELIVERY_PERM");
        jdbcTemplate.execute("""
                CREATE TABLE T_PARTNER_DELIVERY_PERM (
                   ID VARCHAR(36) NOT NULL,
                   PARTNER_ID VARCHAR(36) NOT NULL,
                   DELIVERY_ID VARCHAR(36) NOT NULL,
                   CREATE_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                   PRIMARY KEY (ID),
                   UNIQUE KEY UK_PARTNER_DELIVERY (PARTNER_ID, DELIVERY_ID),
                   KEY IDX_PARTNER_DELIVERY_PARTNER (PARTNER_ID),
                   KEY IDX_PARTNER_DELIVERY_DELIVERY (DELIVERY_ID)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
                """);
    }

    private void addDeliveryForeignKey(String table, String constraintName, String column) {
        if (foreignKeyExists(table, constraintName)) {
            return;
        }
        if (!tableExists("T_DELIVERY")) {
            return;
        }
        log.info("添加外键 {}.{}", table, constraintName);
        jdbcTemplate.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + constraintName
                + " FOREIGN KEY (" + column + ") REFERENCES T_DELIVERY (ID)");
    }

    private void dropForeignKeysReferencing(String referencedTable) {
        List<String> fks = jdbcTemplate.query("""
                SELECT TABLE_NAME, CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME = ?
                """, (rs, rowNum) -> rs.getString("TABLE_NAME") + "|" + rs.getString("CONSTRAINT_NAME"),
                referencedTable.toUpperCase());
        for (String pair : fks) {
            String[] parts = pair.split("\\|", 2);
            if (parts.length == 2) {
                log.info("删除外键 {}.{}", parts[0], parts[1]);
                jdbcTemplate.execute("ALTER TABLE " + parts[0] + " DROP FOREIGN KEY " + parts[1]);
            }
        }
    }

    private void dropForeignKeysOnColumn(String table, String column) {
        List<String> fks = jdbcTemplate.queryForList("""
                SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
                  AND REFERENCED_TABLE_NAME IS NOT NULL
                """, String.class, table.toUpperCase(), column.toUpperCase());
        for (String fk : fks) {
            log.info("删除外键 {}.{}", table, fk);
            jdbcTemplate.execute("ALTER TABLE " + table + " DROP FOREIGN KEY " + fk);
        }
    }

    private void dropColumnIfExists(String table, String column) {
        if (!columnExists(table, column)) {
            return;
        }
        log.info("删除列 {}.{}", table, column);
        jdbcTemplate.execute("ALTER TABLE " + table + " DROP COLUMN " + column);
    }

    private boolean foreignKeyExists(String table, String constraintName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND CONSTRAINT_NAME = ?
                  AND CONSTRAINT_TYPE = 'FOREIGN KEY'
                """, Integer.class, table.toUpperCase(), constraintName.toUpperCase());
        return count != null && count > 0;
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
