from playwright.sync_api import Page, expect
from utils import cleanup_backups, cleanup_db, extract_table_data, get_db1_tables


def test_shows_total_record_count_in_db(page: Page):
    page.goto("http://localhost:8080/db")
    page.get_by_text("db1").click()
    expect(page.get_by_role("heading", name="Records")).to_have_text("Records 9")


def test_shows_total_table_count_in_db(page: Page):
    page.goto("http://localhost:8080/db")
    page.get_by_text("db1").click()
    expect(page.get_by_role("heading", name="Tables")).to_have_text("Tables 2")


def test_shows_tables_and_record_count_in_db(page: Page):
    page.goto("http://localhost:8080/db")
    page.get_by_text("db1").click()
    table_data = extract_table_data(page.locator((":text('Tables') + table")))
    assert table_data == [
        {"Name": "fruites", "Records": "4"},
        {"Name": "vegetables", "Records": "5"},
    ]


def test_restores_backup(page: Page):
    cleanup_backups()
    page.goto("http://localhost:8080/db")
    page.get_by_role("button", name="Backup").click()
    expect(page.get_by_role("status").filter(has_text="Backup created")).to_be_visible()
    cleanup_db()
    page.reload()
    page.get_by_text("db1").click()
    expect(page.get_by_role("heading", name="Records")).to_have_text("Records 0")
    expect(page.get_by_role("heading", name="Tables")).to_have_text("Tables 0")
    page.locator(":text('Backups') + table").get_by_text("1 day").click()
    page.get_by_role("button", name="Restore").click()
    expect(
        page.get_by_role("status").filter(has_text="Backup restored")
    ).to_be_visible()
    expect(page.get_by_role("heading", name="Records")).to_have_text("Records 9")
    expect(page.get_by_role("heading", name="Tables")).to_have_text("Tables 2")


def test_doesnt_restore_excluded_tables(page: Page):
    cleanup_backups()
    page.goto("http://localhost:8080/db")
    page.get_by_role("button", name="Backup").click()
    expect(page.get_by_role("status").filter(has_text="Backup created")).to_be_visible()
    cleanup_db()
    page.get_by_text("db1").click()
    page.locator(":text('Backups') + table").get_by_text("1 day").click()
    page.get_by_role("button", name="Restore").click()
    expect(
        page.get_by_role("status").filter(has_text="Backup restored")
    ).to_be_visible()
    assert get_db1_tables() == ["fruites", "vegetables"]
