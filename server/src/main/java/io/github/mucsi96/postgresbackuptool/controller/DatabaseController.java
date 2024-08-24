package io.github.mucsi96.postgresbackuptool.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.mucsi96.postgresbackuptool.model.Database;
import io.github.mucsi96.postgresbackuptool.service.DatabaseService;
import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
public class DatabaseController {
  private final DatabaseService databaseService;

  @GetMapping
  @RequestMapping(value = "/databases", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public List<String> getDatabases() {
    return databaseService.getDatabases();
  }

  @GetMapping
  @RequestMapping(value = "/database/{database_name}/tables", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public Database getDatabaseInfo(@PathVariable String databaseName) {
    return databaseService.getDatabaseInfo(databaseName);
  }
}
