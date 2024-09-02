from playwright.sync_api import Page, expect
from pytest import fixture


@fixture(scope="session")
def browser_context_args(browser_context_args):
    return {
        **browser_context_args,
        "ignore_https_errors": True,
        "record_har_path": "test-results/test.har",
    }


@fixture(scope="session")
def browser_type_launch_args(browser_type_launch_args):
    return {
        **browser_type_launch_args,
        # "devtools": True,
        # "headless": False,
    }


def test_shows_number_of_databases(page: Page):
    page.goto("http://localhost:8080/db")
    expect(page.get_by_text("Databases")).to_have_text("Databases 2")