package io.github.mucsi96.postgresbackuptool.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
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
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import io.github.mucsi96.postgresbackuptool.configuration.DatabaseConfiguration;
import io.github.mucsi96.postgresbackuptool.model.DatabaseInfo;
import io.github.mucsi96.postgresbackuptool.model.Table;

@Service
public class DatabaseService {
    private final List<DatabaseConfiguration> databases;

    public DatabaseService(
            @Value("${databasesConfigPath}") String databasesConfigPath)
            throws StreamReadException, DatabindException, IOException {
        this.databases = Arrays
                .asList(new ObjectMapper().registerModule(new Jdk8Module())
                        .readValue(Paths.get(databasesConfigPath).toFile(),
                                DatabaseConfiguration[].class));
    }

    public List<DatabaseConfiguration> getDatabases() {
        return databases;
    }

    public List<String> getDatabaseNames() {
        return databases.stream().map(DatabaseConfiguration::getName).toList();
    }

    public DatabaseConfiguration getDatabaseConfiguration(String databaseName) {
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
                        "SELECT table_name FROM information_schema.tables WHERE table_schema = ?",
                        databaseConfiguration.getSchema());

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

    public File createDump(String databaseName, int retentionPeriod,
            String format, String timeString) throws IOException, InterruptedException {
        DatabaseConfiguration databaseConfiguration = getDatabaseConfiguration(
                databaseName);
        String filename = String.format("%s.%s.%s.%s", timeString,
                getDatabaseInfo(databaseName).getTotalRowCount(),
                retentionPeriod, "plain".equals(format) ? "sql" : "pgdump");
        List<String> commands = Stream.of(
                List.of("pg_dump", "--dbname",
                        databaseConfiguration.getConnectionString(), "--schema",
                        databaseConfiguration.getSchema(), "--format", format,
                        "--file", filename,
                        "plain".equals(format) ? "--column-inserts" : ""),
                databaseConfiguration.getExcludeTables().stream()
                        .flatMap(table -> {
                            String fullTableName = databaseConfiguration
                                    .getSchema() + "." + table;
                            return List.of("--exclude-table", fullTableName)
                                    .stream();
                        }).toList())
                .flatMap(x -> x.stream()).filter(arg -> !arg.isEmpty()).toList();

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
                databaseConfiguration.getDatabase()));
        jdbcTemplate.execute(String.format("DROP DATABASE IF EXISTS \"%s\";",
                databaseConfiguration.getDatabase()));
        jdbcTemplate.execute(String.format(
                "ALTER DATABASE \"%s\" RENAME TO \"%s\";", restoreDatabaseName,
                databaseConfiguration.getDatabase()));

        System.out.println("Switch complete");
    }

    private int getTableRowCount(String databaseName, String tableName) {
        DatabaseConfiguration databaseConfiguration = getDatabaseConfiguration(
                databaseName);
        String fullTableName = databaseConfiguration.getSchema() + "."
                + tableName;
        Integer count = databaseConfiguration.getJdbcTemplate().queryForObject(
                "SELECT COUNT(*) FROM " + fullTableName, Integer.class);

        return count != null ? count : 0;
    }
}
