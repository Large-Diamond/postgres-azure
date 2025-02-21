#!/usr/bin/env python3

from os import environ
import sys
from pathlib import Path
from publish_tools import ansible_utils, docker_utils, version_utils

root_directory = Path(__file__).parent.parent
secrets = ansible_utils.load_vars(
    sys.argv[2], root_directory / "vars/vault.yaml")
username = environ.get("GITHUB_REPOSITORY_OWNER")

if username == None:
    print("GitHub username is missing", flush=True, file=sys.stderr)
    exit(1)

server_version = version_utils.get_version(
    src=root_directory, ignore=[str(root_directory / 'backup-job')], tag_prefix="server")

docker_utils.build_and_push_docker_img(
    src=root_directory,
    version=server_version,
    tag_prefix="server",
    image_name="postgres-azure-backup",
    docker_username=username,
    docker_password=secrets["docker_password"],
    github_access_token=sys.argv[1],
)

job_version = version_utils.get_version(
    src=root_directory / 'backup-job', tag_prefix="job")

docker_utils.build_and_push_docker_img(
    src=root_directory / 'backup-job',
    version=job_version,
    tag_prefix="job",
    image_name="postgres-azure-backup-job",
    docker_username=username,
    docker_password=secrets["docker_password"],
    github_access_token=sys.argv[1],
)
