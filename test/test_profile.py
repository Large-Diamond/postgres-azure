from playwright.sync_api import Page, expect


def test_title(page: Page):
    page.goto("http://localhost:8080")
    expect(page.get_by_role("link", name="Postgres Backup Tool")).to_be_visible()


def test_shows_user_initials_in_header(page: Page):
    page.goto("http://localhost:8080")
    expect(page.get_by_role("button", name="TU")).to_be_visible()


def test_shows_user_name_in_popup(page: Page):
    page.goto("http://localhost:8080")
    page.get_by_role("button", name="TU").click()
    expect(page.get_by_text("Test User")).to_be_visible()
