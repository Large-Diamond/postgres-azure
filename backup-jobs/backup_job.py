import os
import requests
import logging
from azure.identity import WorkloadIdentityCredential

logging.basicConfig(level=logging.INFO, format="%(asctime)s - %(levelname)s - %(message)s")

scopes = f"{os.getenv('API_CLIENT_ID')}/createBackup {os.getenv('API_CLIENT_ID')}/cleanupBackups"

def get_access_token():
    try:
        credential = WorkloadIdentityCredential()
        token = credential.get_token(scopes)
        logging.info("Successfully obtained access token")
        return token.token
    except Exception as e:
        logging.error(f"Failed to obtain access token: {e}")
        exit(1)

def trigger_backup():
    token = get_access_token()
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

    try:
        response = requests.post("postgres-azure-backup:8080/api/backup?retention_period=1", headers=headers)
        response.raise_for_status()
        logging.info(f"Backup triggered successfully: {response.status_code}")
    except requests.exceptions.RequestException as e:
        logging.error(f"Backup request failed: {e}")
        exit(1)

if __name__ == "__main__":
    logging.info("Starting backup job...")
    trigger_backup()
    logging.info("Backup job completed.")
