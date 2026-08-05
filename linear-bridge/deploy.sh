#!/usr/bin/env bash
# deploy.sh — Deploy the linear-webhook-bridge Cloud Function.
#
# Requires LINEAR_WEBHOOK_SECRET and GITHUB_TOKEN to already exist in Secret
# Manager (see README.md in this folder for one-time setup).
#
# Usage:
#   ./deploy.sh

set -eo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

PROJECT_ID="peerpost-v2"
REGION="us-central1"
FUNCTION_NAME="linear-webhook-bridge"

echo "--> Deploying $FUNCTION_NAME to Cloud Functions (2nd gen)..."
gcloud functions deploy "$FUNCTION_NAME" \
    --project="$PROJECT_ID" \
    --region="$REGION" \
    --gen2 \
    --runtime=nodejs20 \
    --source=. \
    --entry-point=linearWebhookBridge \
    --trigger-http \
    --allow-unauthenticated \
    --set-secrets="LINEAR_WEBHOOK_SECRET=linear-webhook-secret:latest,GITHUB_TOKEN=linear-bridge-github-token:latest"

FUNCTION_URL=$(gcloud functions describe "$FUNCTION_NAME" --project="$PROJECT_ID" --region="$REGION" --gen2 --format="value(serviceConfig.uri)")
echo ""
echo "======================================================"
echo " Deployed. Webhook URL to register in Linear:"
echo " $FUNCTION_URL"
echo "======================================================"
