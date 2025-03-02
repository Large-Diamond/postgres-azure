from datetime import timedelta
import re
from playwright.sync_api import Page, expect
from typing import Any, Dict, List
from utils import (
    cleanup_backups,
    create_backup,
    extract_table_data,
    mock_window_open,
)


def without_keys(data: Dict[str, Any], keys: List[str]) -> Dict[str, Any]:
    return {k: v for k, v in data.items() if k not in keys}


def list_without_keys(
    data: List[Dict[str, Any]], keys: List[str]
) -> List[Dict[str, Any]]:
    return [without_keys(row, keys) for row in data]


def test_shows_number_of_backups(page: Page):
    page.goto("http://localhost:8080")
    page.get_by_text("db1").click()
    expect(page.get_by_role("heading", name="Backups")).to_have_text("Backups 2")


def test_shows_last_backup_time(page: Page):
    page.goto("http://localhost:8080")
    page.get_by_text("db1").click()
    expect(page.get_by_role("heading", name="Last backup")).to_have_text(
        "Last backup 10 hours ago"
    )


def test_shows_backups(page: Page):
    page.goto("http://localhost:8080")
    page.get_by_text("db1").click()
    expect(page.get_by_role("heading", name="Backups")).to_have_text("Backups 2")
    table_data = list_without_keys(
        extract_table_data(page.locator(":text('Backups') + table")), ["Name"]
    )

    assert table_data == [
        {
            "": "Restore",
            "Date": "10 hours ago",
            "Records": "8",
            "Size": "100.0 B",
            "Retention": "1 day",
        },
        {
            "": "Restore",
            "Date": "3 days ago",
            "Records": "7",
            "Size": "150.0 B",
            "Retention": "7 days",
        },
    ]


def test_creates_backup(page: Page):
    cleanup_backups()
    page.goto("http://localhost:8080")
    page.get_by_role("button", name="Backup").click()
    expect(page.get_by_role("status").filter(
        has_text="Backup created")).to_be_visible()
    page.get_by_text("db1").click()
    expect(page.get_by_role("heading", name="Backups")).to_have_text("Backups 1")
    table_data = list_without_keys(
        extract_table_data(page.locator(":text('Backups') + table")),
        ["Name", "Date"],
    )
    assert table_data == [
        {
            "": "Restore",
            "Records": "9",
            "Size": "1.9 KB",
            "Retention": "1 day",
        }
    ]


def test_creates_backup_with_retention(page: Page):
    cleanup_backups()
    page.goto("http://localhost:8080")
    retention_period_input = page.get_by_label("Retention period")
    retention_period_input.fill("7")
    page.get_by_role("button", name="Backup").click()
    expect(page.get_by_role("status").filter(
        has_text="Backup created")).to_be_visible()
    page.get_by_text("db1").click()
    expect(page.get_by_role("heading", name="Backups")).to_have_text("Backups 1")
    table_data = list_without_keys(
        extract_table_data(page.locator(":text('Backups') + table")),
        ["Name", "Date"],
    )
    assert table_data == [
        {
            "": "Restore",
            "Records": "9",
            "Size": "1.9 KB",
            "Retention": "7 days",
        }
    ]


def test_cleans_up_outdated_backups(page: Page):
    cleanup_backups()
    create_backup(
        prefix="db1",
        rowsCount=1,
        retention=31,
        size=132,
        time_delta=timedelta(days=30),
    )
    create_backup(
        prefix="db1",
        rowsCount=2,
        retention=7,
        size=132,
        time_delta=timedelta(days=7),
    )
    create_backup(
        prefix="db1",
        rowsCount=3,
        retention=1,
        size=132,
        time_delta=timedelta(days=1),
    )
    create_backup(
        prefix="db1",
        rowsCount=4,
        retention=2,
        size=132,
        time_delta=timedelta(days=1),
    )
    page.goto("http://localhost:8080")
    page.get_by_role("button", name="Cleanup").click()
    expect(
        page.get_by_role("status").filter(has_text="Cleanup finished")
    ).to_be_visible()
    page.get_by_text("db1").click()
    expect(page.get_by_role("heading", name="Backups")).to_have_text("Backups 2")
    table_data = list_without_keys(
        extract_table_data(page.locator(":text('Backups') + table")),
        ["Name"],
    )
    assert table_data == [
        {
            "": "Restore",
            "Date": "yesterday",
            "Records": "4",
            "Size": "132.0 B",
            "Retention": "2 days",
        },
        {
            "": "Restore",
            "Date": "last month",
            "Records": "1",
            "Size": "132.0 B",
            "Retention": "31 days",
        },
    ]


def test_downloads_backup_archive(page: Page):
    cleanup_backups()
    page.goto("http://localhost:8080")
    page.get_by_role("button", name="Backup").click()
    expect(page.get_by_role("status").filter(
        has_text="Backup created")).to_be_visible()
    page.get_by_text("db1").click()
    page.locator(":text('Backups') + table").get_by_text("1 day").click()

    mock_window_open(page)

    with page.expect_download() as download_info:
        page.get_by_role("button", name="Download archive").click()

    assert re.match(
        r"https://localhost:8081/devstoreaccount1/backups/db1%2F.*-.*.9.1.pgdump", download_info.value.url
    )

def test_downloads_backup_plain(page: Page):
    cleanup_backups()
    page.goto("http://localhost:8080")
    page.get_by_role("button", name="Backup").click()
    expect(page.get_by_role("status").filter(
        has_text="Backup created")).to_be_visible()
    page.get_by_text("db1").click()
    page.locator(":text('Backups') + table").get_by_text("1 day").click()

    mock_window_open(page)

    with page.expect_download() as download_info:
        page.get_by_role("button", name="Download plain dump").click()

    assert re.match(
        r"https://localhost:8081/devstoreaccount1/backups/db1%2F.*-.*.9.1.sql", download_info.value.url
    )

    response = page.request.get(download_info.value.url)
    assert response.status == 200
    assert "CREATE SCHEMA test1" in response.text()
    assert "COPY test1.fruites (name) FROM stdin" in response.text()
    assert "Apple" in response.text()
    assert "Orange" in response.text()
    assert "Banana" in response.text()
    assert "Rasberry" in response.text()


