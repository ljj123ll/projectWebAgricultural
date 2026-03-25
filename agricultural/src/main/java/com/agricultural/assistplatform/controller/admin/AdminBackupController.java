package com.agricultural.assistplatform.controller.admin;

import com.agricultural.assistplatform.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Tag(name = "管理员端-数据备份")
@RestController
@RequestMapping("/admin/backup")
@RequiredArgsConstructor
@Slf4j
public class AdminBackupController {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path BACKUP_DIR = Paths.get(System.getProperty("user.dir"), "backups");

    @Operation(summary = "获取备份列表")
    @GetMapping
    public Result<List<Map<String, Object>>> list() throws IOException {
        return Result.ok(loadBackups());
    }

    @Operation(summary = "手动备份")
    @PostMapping
    public Result<Void> create() throws IOException {
        ensureBackupDir();
        String now = LocalDateTime.now().format(FILE_TIME);
        Path file = BACKUP_DIR.resolve("backup_" + now + ".sql");
        Files.writeString(file, buildSqlBackup(), StandardCharsets.UTF_8);
        return Result.ok();
    }

    @Operation(summary = "恢复备份")
    @PostMapping("/{id}/restore")
    public Result<Void> restore(@PathVariable Long id) throws Exception {
        Path backupFile = resolveBackupById(id);
        if (backupFile == null) return Result.fail(404, "备份文件不存在");

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                ScriptUtils.executeSqlScript(connection, new EncodedResource(new FileSystemResource(backupFile), StandardCharsets.UTF_8));
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
        return Result.ok();
    }

    @Operation(summary = "删除备份")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) throws IOException {
        Path backupFile = resolveBackupById(id);
        if (backupFile == null) return Result.fail(404, "备份文件不存在");
        Files.deleteIfExists(backupFile);
        return Result.ok();
    }

    private void ensureBackupDir() throws IOException {
        if (!Files.exists(BACKUP_DIR)) Files.createDirectories(BACKUP_DIR);
    }

    private List<Map<String, Object>> loadBackups() throws IOException {
        ensureBackupDir();
        List<Path> files = Files.list(BACKUP_DIR)
                .filter(path -> path.getFileName().toString().endsWith(".sql"))
                .sorted(Comparator.comparing(path -> path.toFile().lastModified(), Comparator.reverseOrder()))
                .collect(Collectors.toList());

        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            Path path = files.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", i + 1L);
            row.put("fileName", path.getFileName().toString());
            row.put("size", readableSize(Files.size(path)));
            row.put("type", "manual");
            row.put("status", 1);
            row.put("createTime", LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(path.toFile().lastModified()),
                    java.time.ZoneId.systemDefault()).format(DISPLAY_TIME));
            list.add(row);
        }
        return list;
    }

    private Path resolveBackupById(Long id) throws IOException {
        if (id == null || id < 1) return null;
        List<Map<String, Object>> backups = loadBackups();
        if (id > backups.size()) return null;
        String fileName = Objects.toString(backups.get(id.intValue() - 1).get("fileName"), "");
        if (fileName.isBlank()) return null;
        return BACKUP_DIR.resolve(fileName);
    }

    private String buildSqlBackup() {
        List<String> tables = jdbcTemplate.queryForList("SHOW TABLES", String.class);
        StringBuilder sql = new StringBuilder(1024 * 64);
        sql.append("SET FOREIGN_KEY_CHECKS = 0;\n");
        for (String table : tables) {
            if (table == null || table.isBlank()) continue;
            sql.append("\n-- TABLE: ").append(table).append('\n');
            sql.append("DELETE FROM `").append(table).append("`;\n");

            List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT * FROM `" + table + "`");
            for (Map<String, Object> row : rows) {
                if (row == null || row.isEmpty()) continue;
                String columns = row.keySet().stream()
                        .map(col -> "`" + col + "`")
                        .collect(Collectors.joining(","));
                String values = row.values().stream()
                        .map(this::toSqlLiteral)
                        .collect(Collectors.joining(","));
                sql.append("INSERT INTO `").append(table).append("`(").append(columns).append(") VALUES (")
                        .append(values).append(");\n");
            }
        }
        sql.append("SET FOREIGN_KEY_CHECKS = 1;\n");
        return sql.toString();
    }

    private String toSqlLiteral(Object value) {
        if (value == null) return "NULL";
        if (value instanceof Number) return String.valueOf(value);
        if (value instanceof Boolean b) return b ? "1" : "0";
        if (value instanceof Timestamp ts) return "'" + ts.toLocalDateTime().format(DISPLAY_TIME) + "'";
        if (value instanceof Date d) return "'" + d.toLocalDate().format(DateTimeFormatter.ISO_DATE) + "'";
        if (value instanceof Time t) return "'" + t.toLocalTime().format(DateTimeFormatter.ISO_TIME) + "'";
        if (value instanceof LocalDateTime dt) return "'" + dt.format(DISPLAY_TIME) + "'";
        if (value instanceof LocalDate date) return "'" + date.format(DateTimeFormatter.ISO_DATE) + "'";
        if (value instanceof LocalTime time) return "'" + time.format(DateTimeFormatter.ISO_TIME) + "'";
        if (value instanceof byte[] bytes) return "X'" + bytesToHex(bytes) + "'";
        return "'" + String.valueOf(value).replace("\\", "\\\\").replace("'", "''") + "'";
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private String readableSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return new DecimalFormat("#.##").format(bytes / 1024.0) + "KB";
        return new DecimalFormat("#.##").format(bytes / 1024.0 / 1024.0) + "MB";
    }
}
