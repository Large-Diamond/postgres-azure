from typing import List, Dict
from playwright.sync_api import Locator
from azure.storage.blob import BlobServiceClient
import psycopg2
from datetime import datetime, timedelta, timezone

blob_service_client = BlobServiceClient.from_connection_string(
  "DefaultEndpointsProtocol=https;AccountName=devstoreaccount1;"
  + "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;"
  + "BlobEndpoint=https://localhost:8081"
  + "/devstoreaccount1;"
)


def extract_table_data(table: Locator) -> List[Dict[str, str]]:
    headers = [
        header.capitalize() for header in table.locator("thead th").all_inner_texts()
    ]
    rows = table.locator("tbody tr").all()
    table_data = []

    for row in rows:
        cells = row.locator("td").all_inner_texts()
        row_data = dict(zip(headers, cells))
        table_data.append(row_data)

    return table_data


def create_backup(
    prefix: str, rowsCount: int, retention: int, size: int, time_delta: timedelta
):
    container_client = blob_service_client.get_container_client('backups')
    if not container_client.exists():
        container_client.create_container()
    current_time = datetime.now(timezone.utc)
    one_day_ago = current_time - time_delta
    filename = one_day_ago.strftime(
        f"{prefix}/%Y%m%d-%H%M%S.{rowsCount}.{retention}.pgdump")
    container_client.get_blob_client(filename).upload_blob(
        "".join(["a" for _ in range(size)])
    )


def cleanup_backups():
    container_client = blob_service_client.get_container_client('backups')

    if not container_client.exists():
        container_client.create_container()
        return

    for blob in container_client.list_blobs():
        blob_client = container_client.get_blob_client(blob.name)
        blob_client.delete_blob()


def cleanup_db():
    conn1 = psycopg2.connect(
        database="test",
        host="localhost",
        user="postgres",
        password="postgres",
        port="8082",
    )
    conn2 = psycopg2.connect(
        database="test",
        host="localhost",
        user="postgres",
        password="postgres",
        port="8083",
    )
    cur1 = conn1.cursor()
    cur2 = conn2.cursor()

    cur1.execute("DROP SCHEMA IF EXISTS test1 CASCADE")
    cur2.execute("DROP SCHEMA IF EXISTS test2 CASCADE")

    conn1.commit()
    conn2.commit()
    cur1.close()
    cur2.close()


def populate_db():
    conn1 = psycopg2.connect(
        database="test",
        host="localhost",
        user="postgres",
        password="postgres",
        port="8082",
    )
    conn2 = psycopg2.connect(
        database="test",
        host="localhost",
        user="postgres",
        password="postgres",
        port="8083",
    )
    cur1 = conn1.cursor()
    cur2 = conn2.cursor()

    cur1.execute(
        """
        CREATE SCHEMA test1;
        CREATE TABLE test1.fruites (NAME VARCHAR(20));
        INSERT INTO test1.fruites (NAME) VALUES ('Apple');
        INSERT INTO test1.fruites (NAME) VALUES ('Orange');
        INSERT INTO test1.fruites (NAME) VALUES ('Banana');
        INSERT INTO test1.fruites (NAME) VALUES ('Rasberry');
        CREATE TABLE test1.vegetables (NAME VARCHAR(20));
        INSERT INTO test1.vegetables (NAME) VALUES ('Carrot');
        INSERT INTO test1.vegetables (NAME) VALUES ('Potato');
        INSERT INTO test1.vegetables (NAME) VALUES ('Spinach');
        INSERT INTO test1.vegetables (NAME) VALUES ('Broccoli');
        INSERT INTO test1.vegetables (NAME) VALUES ('Tomato');
        CREATE TABLE test1.passwords (NAME VARCHAR(20));
        INSERT INTO test1.passwords (NAME) VALUES ('123');
        INSERT INTO test1.passwords (NAME) VALUES ('123456');
        INSERT INTO test1.passwords (NAME) VALUES ('abc');
        INSERT INTO test1.passwords (NAME) VALUES ('abcd');
        CREATE TABLE test1.secrets (NAME VARCHAR(20));
        INSERT INTO test1.secrets (NAME) VALUES ('a');
        INSERT INTO test1.secrets (NAME) VALUES ('b');
        INSERT INTO test1.secrets (NAME) VALUES ('c');
    """
    )
    cur2.execute(
        """
        CREATE SCHEMA test2;
        CREATE TABLE test2.animals (name VARCHAR(20));
        INSERT INTO test2.animals (name) VALUES ('Dog');
        INSERT INTO test2.animals (name) VALUES ('Cat');
        INSERT INTO test2.animals (name) VALUES ('Bird');
        INSERT INTO test2.animals (name) VALUES ('Fish');
        INSERT INTO test2.animals (name) VALUES ('Rabbit');
        INSERT INTO test2.animals (name) VALUES ('Turtle');
        CREATE TABLE test2.countries (name VARCHAR(20));
        INSERT INTO test2.countries (name) VALUES ('USA');
        INSERT INTO test2.countries (name) VALUES ('Canada');
        INSERT INTO test2.countries (name) VALUES ('Germany');
        INSERT INTO test2.countries (name) VALUES ('Japan');
        INSERT INTO test2.countries (name) VALUES ('Australia');
        INSERT INTO test2.countries (name) VALUES ('Brazil');
        CREATE TABLE test2.books (title VARCHAR(50));
        INSERT INTO test2.books (title) VALUES ('Harry Potter');
        INSERT INTO test2.books (title) VALUES ('To Kill a Mockingbird');
        INSERT INTO test2.books (title) VALUES ('The Great Gatsby');
        INSERT INTO test2.books (title) VALUES ('1984');
        INSERT INTO test2.books (title) VALUES ('Pride and Prejudice');
        CREATE TABLE test2.secrets (secret VARCHAR(20));
        INSERT INTO test2.secrets (secret) VALUES ('alpha');
        INSERT INTO test2.secrets (secret) VALUES ('bravo');
        INSERT INTO test2.secrets (secret) VALUES ('charlie');
        INSERT INTO test2.secrets (secret) VALUES ('delta');
    """
    )

    conn1.commit()
    conn2.commit()
    cur1.close()
    cur2.close()


def get_db1_tables():
    conn1 = psycopg2.connect(
        database="test",
        host="localhost",
        user="postgres",
        password="postgres",
        port="8082",
    )
    cur1 = conn1.cursor()
    cur1.execute(
        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'test1'"
    )
    tables = [table[0] for table in cur1.fetchall()]
    cur1.close()
    return tables


def mock_window_open(page):
    page.evaluate(
        """
        window.open = (url) => {
            if (url && url.startsWith("https://blobstorage:10000")) {
                url = url.replace("https://blobstorage:10000", "https://localhost:8081");
                window.location.href = url;
            }
        };
        """
    )
