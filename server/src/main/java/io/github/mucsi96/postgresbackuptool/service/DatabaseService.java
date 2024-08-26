package io.github.mucsi96.postgresbackuptool.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.mucsi96.postgresbackuptool.configuration.DatabaseConfiguration;
import io.github.mucsi96.postgresbackuptool.model.DatabaseInfo;
import io.github.mucsi96.postgresbackuptool.model.Table;

@Service
public class DatabaseService {
    private final DateTimeFormatter dateTimeFormatter;
    private final List<DatabaseConfiguration> databases;

    public DatabaseService(
            @Value("${databasesConfigPath}") String databasesConfigPath,
            DateTimeFormatter dateTimeFormatter)
            throws StreamReadException, DatabindException, IOException {
        this.databases = Arrays.asList(new ObjectMapper().readValue(
                Paths.get(databasesConfigPath).toFile(),
                DatabaseConfiguration[].class));
        this.dateTimeFormatter = dateTimeFormatter;
    }

    public List<String> getDatabases() {
        return databases.stream().map(DatabaseConfiguration::getName).toList();
    }

    DatabaseConfiguration getDatabaseConfiguration(String databaseName) {
        return databases.stream()
                .filter(db -> db.getName().equals(databaseName)).findFirst()
                .orElseThrow(() -> new RuntimeException("Database with name "
                        + databaseName + " not found in configuration"));
    }

    public DatabaseInfo getDatabaseInfo(String databaseName) {
        DatabaseConfiguration databaseConfiguration = getDatabaseConfiguration(
                databaseName);
        List<Map<String, Object>> result = databaseConfiguration
                .getJdbcTemplate().queryForList(
                        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'");

        List<Table> tables = result.stream()
                .filter(table -> !databaseConfiguration.getExcludeTables()
                        .contains((String) table.get("table_name")))
                .map(table -> {
                    String tableName = (String) table.get("table_name");
                    return Table.builder().name(tableName)
                            .rowCount(getTableRowCount(databaseName, tableName))
                            .build();
                }).toList();

        int totalRowCount = tables.stream().reduce(0,
                (acc, table) -> acc + table.getRowCount(), (a, b) -> a + b);

        return DatabaseInfo.builder().tables(tables)
                .totalRowCount(totalRowCount).build();

    }

    public File createDump(String databaseName, int retentionPeriod)
            throws IOException, InterruptedException {
        DatabaseConfiguration databaseConfiguration = getDatabaseConfiguration(
                databaseName);
        String timeString = dateTimeFormatter.format(Instant.now());
        String filename = String.format("%s.%s.%s.pgdump", timeString,
                getDatabaseInfo(databaseName).getTotalRowCount(),
                retentionPeriod);
        List<String> commands = Stream.of(
                List.of("pg_dump", "--dbname",
                        databaseConfiguration.getConnectionString(), "--format",
                        "c", "--file", filename),
                databaseConfiguration.getExcludeTables().stream().flatMap(
                        table -> List.of("--exclude-table", table).stream())
                        .toList())
                .flatMap(x -> x.stream()).toList();

        System.out.println("Creating dump: " + String.join(", ", commands));

        int status = new ProcessBuilder(commands).inheritIO().start().waitFor();

        if (status != 0) {
            throw new RuntimeException("Unable to create dump. pg_dump failed");
        }

        File file = new File(filename);

        if (!file.exists()) {
            throw new RuntimeException(
                    "Unable to create dump. " + file + " was not created.");
        }

        System.out.println("Dump created");

        return file;
    }

    public void restoreDump(String databaseName, File dumpFile)
            throws IOException, InterruptedException {
        DatabaseConfiguration databaseConfiguration = getDatabaseConfiguration(
                databaseName);
        DataSource dataSource = new DriverManagerDataSource(
                databaseConfiguration.getRootUrl(),
                databaseConfiguration.getUsername(),
                databaseConfiguration.getPassword());
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        String restoreDatabaseName = databaseConfiguration.getDatabase()
                + "_restore";
        String restoreConnectionString = databaseConfiguration
                .getConnectionString() + "_restore";

        System.out.println("Preparig restore db");

        jdbcTemplate.execute(String.format("DROP DATABASE IF EXISTS \"%s\";",
                restoreDatabaseName));
        jdbcTemplate.execute(
                String.format("CREATE DATABASE \"%s\";", restoreDatabaseName));

        System.out.println("Restore db prepared");

        new ProcessBuilder("pg_restore", "--dbname", restoreConnectionString,
                "--verbose", dumpFile.getName()).inheritIO().start().waitFor();

        System.out.println("Restore complete");

        jdbcTemplate.execute(String.format(
                "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '%s' AND pid <> pg_backend_pid();",
                databaseName));
        jdbcTemplate.execute(
                String.format("DROP DATABASE IF EXISTS \"%s\";", databaseName));
        jdbcTemplate.execute(
                String.format("ALTER DATABASE \"%s\" RENAME TO \"%s\";",
                        restoreDatabaseName, databaseName));

        System.out.println("Switch complete");
    }

    private int getTableRowCount(String databaseName, String tableName) {
        DatabaseConfiguration databaseConfiguration = getDatabaseConfiguration(
                databaseName);
        Integer count = databaseConfiguration.getJdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM " + tableName, Integer.class);

        return count != null ? count : 0;
    }
}
