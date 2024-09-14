from calendar import c
import re
from typing import List, Dict
from playwright.sync_api import Page
from azure.storage.blob import BlobClient, BlobServiceClient
import psycopg2
from datetime import datetime, timedelta, timezone

blob_service_client = BlobServiceClient.from_connection_string(
    "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;"
    + "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;"
    + "BlobEndpoint=http://localhost:8081"
    + "/devstoreaccount1;"
)

conn1 = psycopg2.connect(
    database="test", host="localhost", user="postgres", password="postgres", port="8082"
)
conn2 = psycopg2.connect(
    database="test", host="localhost", user="postgres", password="postgres", port="8083"
)


def extract_table_data(page: Page) -> List[Dict[str, str]]:
    headers = [
        header.capitalize()
        for header in page.locator("table thead th").all_inner_texts()
    ]
    assert headers == ["", "Name", "Tables", "Records", "Backups", "Last backup"]
    rows = page.locator("table tbody tr").all()
    table_data = []

    for row in rows:
        cells = row.locator("td").all_inner_texts()
        row_data = dict(zip(headers, cells))
        table_data.append(row_data)

    return table_data


def create_backup(
    db_name: str, rowsCount: int, retention: int, size: int, time_delta: timedelta
):
    container_client = blob_service_client.get_container_client(db_name)
    if not container_client.exists():
        container_client.create_container()
    current_time = datetime.now(timezone.utc)
    one_day_ago = current_time - time_delta
    filename = one_day_ago.strftime(f"%Y%m%d-%H%M%S.{rowsCount}.{retention}.pgdump")
    container_client.get_blob_client(filename).upload_blob(
        "".join(["a" for _ in range(size)])
    )


def cleanup_blob_storage():
    for container in blob_service_client.list_containers():
        container_client = blob_service_client.get_container_client(container.name)
        for blob in container_client.list_blobs():
            blob_client = container_client.get_blob_client(blob.name)
            blob_client.delete_blob()
        container_client.delete_container()
    create_backup(
        db_name="db1",
        rowsCount=8,
        time_delta=timedelta(hours=10),
        retention=1,
        size=100,
    )
    create_backup(
        db_name="db1",
        rowsCount=7,
        time_delta=timedelta(days=3, hours=10),
        retention=7,
        size=150,
    )


def cleanup_db():
    cur1 = conn1.cursor()
    cur2 = conn2.cursor()

    cur1.execute(
        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
    )

    table_names1 = [table[0] for table in cur1.fetchall()]
    for table_name in table_names1:
        cur1.execute(f"DROP TABLE {table_name}")

    cur2.execute(
        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'"
    )

    table_names2 = [table[0] for table in cur2.fetchall()]
    for table_name in table_names2:
        cur2.execute(f"DROP TABLE {table_name}")

    cur1.execute(
        """
        CREATE TABLE fruites (NAME VARCHAR(20));
        INSERT INTO fruites (NAME) VALUES ('Apple');
        INSERT INTO fruites (NAME) VALUES ('Orange');
        INSERT INTO fruites (NAME) VALUES ('Banana');
        INSERT INTO fruites (NAME) VALUES ('Rasberry');
        CREATE TABLE vegetables (NAME VARCHAR(20));
        INSERT INTO vegetables (NAME) VALUES ('Carrot');
        INSERT INTO vegetables (NAME) VALUES ('Potato');
        INSERT INTO vegetables (NAME) VALUES ('Spinach');
        INSERT INTO vegetables (NAME) VALUES ('Broccoli');
        INSERT INTO vegetables (NAME) VALUES ('Tomato');
        CREATE TABLE passwords (NAME VARCHAR(20));
        INSERT INTO passwords (NAME) VALUES ('123');
        INSERT INTO passwords (NAME) VALUES ('123456');
        INSERT INTO passwords (NAME) VALUES ('abc');
        INSERT INTO passwords (NAME) VALUES ('abcd');
        CREATE TABLE secrets (NAME VARCHAR(20));
        INSERT INTO secrets (NAME) VALUES ('a');
        INSERT INTO secrets (NAME) VALUES ('b');
        INSERT INTO secrets (NAME) VALUES ('c');
    """
    )
    cur2.execute(
        """
        CREATE TABLE animals (name VARCHAR(20));
        INSERT INTO animals (name) VALUES ('Dog');
        INSERT INTO animals (name) VALUES ('Cat');
        INSERT INTO animals (name) VALUES ('Bird');
        INSERT INTO animals (name) VALUES ('Fish');
        INSERT INTO animals (name) VALUES ('Rabbit');
        INSERT INTO animals (name) VALUES ('Turtle');
        CREATE TABLE countries (name VARCHAR(20));
        INSERT INTO countries (name) VALUES ('USA');
        INSERT INTO countries (name) VALUES ('Canada');
        INSERT INTO countries (name) VALUES ('Germany');
        INSERT INTO countries (name) VALUES ('Japan');
        INSERT INTO countries (name) VALUES ('Australia');
        INSERT INTO countries (name) VALUES ('Brazil');
        CREATE TABLE books (title VARCHAR(50));
        INSERT INTO books (title) VALUES ('Harry Potter');
        INSERT INTO books (title) VALUES ('To Kill a Mockingbird');
        INSERT INTO books (title) VALUES ('The Great Gatsby');
        INSERT INTO books (title) VALUES ('1984');
        INSERT INTO books (title) VALUES ('Pride and Prejudice');
        CREATE TABLE secrets (secret VARCHAR(20));
        INSERT INTO secrets (secret) VALUES ('alpha');
        INSERT INTO secrets (secret) VALUES ('bravo');
        INSERT INTO secrets (secret) VALUES ('charlie');
        INSERT INTO secrets (secret) VALUES ('delta');
    """
    )
    conn1.commit()
    conn2.commit()
    cur1.close()
    cur2.close()
