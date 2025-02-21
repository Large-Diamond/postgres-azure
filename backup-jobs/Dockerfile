FROM python:3.13-slim AS builder

ENV DEBIAN_FRONTEND=noninteractive

WORKDIR /app

RUN pip install --no-cache-dir --prefix=/install azure-identity requests


# Final image
FROM python:3.13-slim

WORKDIR /app

COPY --from=builder /install /usr/local
COPY backup_job.py .

ENTRYPOINT ["python", "backup_job.py"]
