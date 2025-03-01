import os
import requests
import logging
from azure.identity import WorkloadIdentityCredential

logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s - %(levelname)s - %(message)s")

task = os.getenv("TASK")
retention_period = os.getenv("RETENTION_PERIOD")
api_client_id = os.getenv("API_CLIENT_ID")


def get_access_token():
    try:
        credential = WorkloadIdentityCredential()
        token = credential.get_token(f"{api_client_id}/.default")
        logging.info("Successfully obtained access token.")
        return token.token
    except Exception as e:
        logging.error(f"Failed to obtain access token: {e}")
        exit(1)


def validate_environment_variables():
    if not api_client_id:
        logging.error("API_CLIENT_ID is not set")
        exit(1)

    if task not in ["backup", "cleanup"]:
        logging.error("TASK must be either 'backup' or 'cleanup'")
        exit(1)

    if task == "backup":
        try:
            if retention_period is None:
                raise ValueError("RETENTION_PERIOD is not set")
            retention = int(retention_period)
            if not (1 <= retention <= 365):
                raise ValueError("RETENTION_PERIOD must be between 1 and 365")
        except ValueError as e:
            logging.error(f"Invalid RETENTION_PERIOD: {e}")
            exit(1)


def trigger_backup():
    token = get_access_token()
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

    try:
        response = requests.post(
            f"http://postgres-azure-backup:8080/api/backup?retention_period={retention_period}", headers=headers)
        response.raise_for_status()
        logging.info(f"Backup triggered successfully: {response.status_code}")
    except requests.exceptions.RequestException as e:
        logging.error(f"Backup request failed: {e}")
        exit(1)


def trigger_cleanup():
    token = get_access_token()
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

    try:
        response = requests.post(
            "http://postgres-azure-backup:8080/api/cleanup", headers=headers)
        response.raise_for_status()
        logging.info(f"Cleanup triggered successfully: {response.status_code}")
    except requests.exceptions.RequestException as e:
        logging.error(f"Cleanup request failed: {e}")
        exit(1)


if __name__ == "__main__":
    logging.info("Starting backup job...")
    validate_environment_variables()
    if task == "backup":
        trigger_backup()
    if task == "cleanup":
        trigger_cleanup()
    logging.info("Backup job completed.")
