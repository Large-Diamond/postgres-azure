package io.github.mucsi96.postgresbackuptool.configuration;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class DatabaseConfiguration {
  private String name;
  private String host;
  private int port;
  private String database;
  private String username;
  private String password;
  private List<String> excludeTables;

  DatabaseConfiguration() {
  }

  @JsonIgnore
  public String getJdbcUrl() {
    return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
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
    DataSource dataSource = new DriverManagerDataSource(getJdbcUrl(), username,
        password);

    return new JdbcTemplate(dataSource);
  }
}
