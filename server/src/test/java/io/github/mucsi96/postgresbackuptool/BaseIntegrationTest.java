package io.github.mucsi96.postgresbackuptool;

import java.nio.file.Paths;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.lifecycle.Startables;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.microsoft.playwright.Page;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ExtendWith(ScreenshotOnFailure.class)
public class BaseIntegrationTest {

  static AzuriteContainer blobstorageMock;

  static PostgreSQLContainer<?> dbMock;

  @Autowired
  Page page;

  @Autowired
  BlobServiceClient blobServiceClient;

  @Autowired
  JdbcTemplate jdbcTemplate;

  @Autowired
  DateTimeFormatter dateTimeFormatter;

  @Value("${blobstorage.container}")
  String containerName;

  @LocalServerPort
  private int port;

  @BeforeAll
  public static void setUp() {
    if (blobstorageMock != null) {
      return;
    }

    blobstorageMock = new AzuriteContainer();
    dbMock = new PostgreSQLContainer<>("postgres:16.2-bullseye");

    Startables.deepStart(List.of(dbMock, blobstorageMock)).join();
  }

  public void setupMocks(Runnable prepare) {
    cleanupBlobstorage();
    cleanupDB();
    initDB();
    prepare.run();
    page.navigate("http://localhost:" + port);
  }

  public void setupMocks() {
    setupMocks(() -> {
    });
  }

  @DynamicPropertySource
  public static void overrideProps(DynamicPropertyRegistry registry) {
    registry.add("blobstorage.connectionString",
        () -> blobstorageMock.getConnectionString());
    registry.add("blobstorage.container", () -> "test-bucket");
    registry.add("postgres.database-name", dbMock::getDatabaseName);
    registry.add("postgres.username", dbMock::getUsername);
    registry.add("postgres.root-url",
        () -> String.format("jdbc:postgresql://%s:%s/postgres",
            dbMock.getHost(), dbMock.getFirstMappedPort()));
    registry.add("postgres.connection-string",
        () -> String.format("postgresql://%s:%s@%s:%s/%s", dbMock.getUsername(),
            dbMock.getPassword(), dbMock.getHost(), dbMock.getFirstMappedPort(),
            dbMock.getDatabaseName()));
    registry.add("postgres.exclude-tables", () -> "passwords, secrets");
    registry.add("spring.datasource.type",
        () -> "org.springframework.jdbc.datasource.SimpleDriverDataSource");
    registry.add("spring.datasource.url", dbMock::getJdbcUrl);
    registry.add("spring.datasource.username", dbMock::getUsername);
    registry.add("spring.datasource.password", dbMock::getPassword);
  }

  public void cleanupDB() {
    List<String> tables = jdbcTemplate.queryForList(
        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")
        .stream().map(table -> (String) table.get("table_name")).toList();

    tables.stream().forEach(table -> jdbcTemplate
        .execute(String.format("DROP TABLE \"%s\" cascade;", table)));
  }

  public void initDB() {
    jdbcTemplate.execute("create table fruites (name varchar(20))");
    jdbcTemplate.execute("insert into fruites (name) values ('Apple')");
    jdbcTemplate.execute("insert into fruites (name) values ('Orange')");
    jdbcTemplate.execute("insert into fruites (name) values ('Banana')");
    jdbcTemplate.execute("insert into fruites (name) values ('Rasberry')");
    jdbcTemplate.execute("create table vegetables (name varchar(20))");
    jdbcTemplate.execute("insert into vegetables (name) values ('Carrot')");
    jdbcTemplate.execute("insert into vegetables (name) values ('Potato')");
    jdbcTemplate.execute("insert into vegetables (name) values ('Spinach')");
    jdbcTemplate.execute("insert into vegetables (name) values ('Broccoli')");
    jdbcTemplate.execute("insert into vegetables (name) values ('Tomato')");
    jdbcTemplate.execute("create table passwords (name varchar(20))");
    jdbcTemplate.execute("insert into passwords (name) values ('123')");
    jdbcTemplate.execute("insert into passwords (name) values ('123456')");
    jdbcTemplate.execute("insert into passwords (name) values ('abc')");
    jdbcTemplate.execute("insert into passwords (name) values ('abcd')");
    jdbcTemplate.execute("create table secrets (name varchar(20))");
    jdbcTemplate.execute("insert into secrets (name) values ('a')");
    jdbcTemplate.execute("insert into secrets (name) values ('b')");
    jdbcTemplate.execute("insert into secrets (name) values ('c')");
  }

  public void takeScreenshot(String name) {
    page.screenshot(new Page.ScreenshotOptions()
        .setPath(Paths.get("screenshots/" + name + ".png")));
  }

  private void cleanupBlobstorage() {
    if (blobServiceClient.getBlobContainerClient(containerName).exists()) {
      blobServiceClient.deleteBlobContainer(containerName);
    }
  }

  public void createMockBackup(Instant time, int rowCount,
      int retentionPeriod) {
    String timeString = dateTimeFormatter.format(time);
    String filename = String.format("%s.%s.%s.pgdump", timeString, rowCount,
        retentionPeriod);

    BlobContainerClient blobContainerClient = blobServiceClient
        .getBlobContainerClient(containerName);

    if (!blobContainerClient.exists()) {
      blobContainerClient.create();
    }

    blobContainerClient.getBlobClient(filename)
        .upload(BinaryData.fromString(""));
  }
}
