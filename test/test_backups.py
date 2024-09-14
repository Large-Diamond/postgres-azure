from datetime import timedelta
from playwright.sync_api import Page, expect
from utils import (
    cleanup_backups,
    create_backup,
    extract_table_data,
)


def without_names(table_data):
    return list(
        map(lambda row: {k: v for k, v in row.items() if k != "Name"}, table_data)
    )


def test_shows_number_of_backups(page: Page):
    page.goto("http://localhost:8080/db")
    page.get_by_text("db1").click()
    expect(page.get_by_role("heading", name="Backups")).to_have_text("Backups 2")


def test_shows_last_backup_time(page: Page):
    page.goto("http://localhost:8080/db")
    page.get_by_text("db1").click()
    expect(page.get_by_role("heading", name="Last backup")).to_have_text(
        "Last backup 10 hours ago"
    )


def test_shows_backups(page: Page):
    page.goto("http://localhost:8080/db")
    page.get_by_text("db1").click()
    table_data = without_names(
        extract_table_data(page.locator(":text('Backups') + table"))
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
    page.goto("http://localhost:8080/db")
    page.get_by_text("db1").click()
    page.get_by_role("button", name="Backup").click()
    expect(page.get_by_role("status").filter(has_text="Backup created")).to_be_visible()
    table_data = without_names(
        extract_table_data(page.locator(":text('Backups') + table"))
    )
    assert table_data == [
        {
            "": "Restore",
            "Date": "1 second ago",
            "Records": "9",
            "Size": "1.7 KB",
            "Retention": "1 day",
        }
    ]


def test_creates_backup_with_retention(page: Page):
    cleanup_backups()
    page.goto("http://localhost:8080/db")
    page.get_by_text("db1").click()
    retention_period_input = page.get_by_label("Retention period")
    retention_period_input.fill("7")
    page.get_by_role("button", name="Backup").click()
    expect(page.get_by_role("status").filter(has_text="Backup created")).to_be_visible()
    table_data = without_names(
        extract_table_data(page.locator(":text('Backups') + table"))
    )
    assert table_data == [
        {
            "": "Restore",
            "Date": "1 second ago",
            "Records": "9",
            "Size": "1.7 KB",
            "Retention": "7 days",
        }
    ]


def test_cleans_up_outdated_backups(page: Page):
    cleanup_backups()
    create_backup(
        db_name="db1",
        rowsCount=1,
        retention=31,
        size=132,
        time_delta=timedelta(days=30),
    )
    create_backup(
        db_name="db1",
        rowsCount=2,
        retention=7,
        size=132,
        time_delta=timedelta(days=7),
    )
    create_backup(
        db_name="db1",
        rowsCount=3,
        retention=1,
        size=132,
        time_delta=timedelta(days=1),
    )
    create_backup(
        db_name="db1",
        rowsCount=4,
        retention=2,
        size=132,
        time_delta=timedelta(days=1),
    )
    page.goto("http://localhost:8080/db")
    page.get_by_text("db1").click()
    page.get_by_role("button", name="Cleanup").click()
    expect(
        page.get_by_role("status").filter(has_text="Cleanup finished")
    ).to_be_visible()

    table_data = without_names(
        extract_table_data(page.locator(":text('Backups') + table"))
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
