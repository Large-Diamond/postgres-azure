package io.github.mucsi96.postgresbackuptool.configuration;

import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.mucsi96.postgresbackuptool.model.DumpFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DatabaseConfiguration {
    @JsonProperty(required = true)
    private String name;

    @JsonProperty(required = true)
    private String host;

    @JsonProperty(required = true)
    private int port;

    @JsonProperty(required = true)
    private String database;

    @JsonProperty(required = true)
    private String username;

    @JsonProperty(required = true)
    private String password;

    private Optional<List<String>> excludeTables;

    private Optional<DumpFormat> dumpFormat;

    DatabaseConfiguration() {
    }

    @JsonIgnore
    public String getJdbcUrl() {
        return String.format("jdbc:postgresql://%s:%d/%s", host, port,
                database);
    }

    @JsonIgnore
    public String getConnectionString() {
        return String.format("postgresql://%s:%s@%s:%d/%s", username, password,
                host, port, database);
    }

    @JsonIgnore
    public String getRootUrl() {
        return String.format("jdbc:postgresql://%s:%d/postgres", host, port);
    }

    @JsonIgnore
    public JdbcTemplate getJdbcTemplate() {
        DataSource dataSource = new DriverManagerDataSource(getJdbcUrl(),
                username, password);

        return new JdbcTemplate(dataSource);
    }

    public Optional<List<String>> getExcludeTables() {
        return excludeTables == null ? Optional.empty() : excludeTables;
    }

    public void setExcludeTables(Optional<List<String>> excludeTables) {
        this.excludeTables = excludeTables;
    }

    public DumpFormat getDumpFormat() {
        return dumpFormat == null ? DumpFormat.CUSTOM : dumpFormat.orElse(DumpFormat.CUSTOM);
    }

    public void setDumpFormat(Optional<DumpFormat> dumpFormat) {
        this.dumpFormat = dumpFormat;
    }
}
