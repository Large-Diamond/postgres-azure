from datetime import timedelta
from pytest import fixture
from utils import cleanup_backups, cleanup_db, create_backup, populate_db

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

@fixture(autouse=True)
def cleanup():
    cleanup_backups()
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
    cleanup_db()
    populate_db()
    yield