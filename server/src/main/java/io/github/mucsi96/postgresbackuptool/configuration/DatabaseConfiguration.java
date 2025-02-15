package io.github.mucsi96.postgresbackuptool.configuration;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;

import io.github.mucsi96.postgresbackuptool.model.DumpFormat;
import lombok.Getter;

@Getter
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
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
    private String schema;

    @JsonProperty(required = true)
    private String username;

    @JsonProperty(required = true)
    private String password;

    private List<String> excludeTables = List.of();

    private DumpFormat dumpFormat = DumpFormat.CUSTOM;

    private boolean createPlainDump = false;

    @JsonIgnore
    public String getPrefix() {
        return name.toLowerCase().replaceAll("[^a-zA-Z0-9]", "-");
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
}
