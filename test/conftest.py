from pytest import fixture
from utils import cleanup_blob_storage, cleanup_db

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
def cleanup_blobstorage():
    cleanup_blob_storage()
    cleanup_db()
    yield