#!/bin/sh
set -e
echo "Initializing local S3 bucket: ${AWS_S3_BUCKET:-notebooklm-sources}"
aws s3 mb s3://${AWS_S3_BUCKET:-notebooklm-sources} || true
echo "Local S3 bucket ready."
