#!/bin/bash
set -e

cd "$(dirname "$0")/.."

PROJECT_ID="peerpost-v2"
REGION="us-central1"
ARTIFACT_REPO="flyship-repo"
DB_INSTANCE_NAME="flyship-mysql-db"
DB_USER="logistics_user"
DB_NAME="logistics_app"

# ── Secrets: read from env vars (CI supplies these via GitHub Actions
# Secrets), falling back to the values used in every environment so far for
# local/manual runs. Nothing sensitive should be hardcoded below this point. ──
DB_PASSWORD="${DB_PASSWORD:-7812}"
APP_JWT_SECRET="${APP_JWT_SECRET:-supersecretkey}"
SPRING_MAIL_USERNAME="${SPRING_MAIL_USERNAME:-nwwahjgnp3kst72a@ethereal.email}"
SPRING_MAIL_PASSWORD="${SPRING_MAIL_PASSWORD:-aEPEuCqXfERHXuhQnE}"

INSTANCE_CONNECTION_NAME="${PROJECT_ID}:${REGION}:${DB_INSTANCE_NAME}"
BACKEND_IMG="${REGION}-docker.pkg.dev/${PROJECT_ID}/${ARTIFACT_REPO}/backend:latest"

# ── Stripe keys: never hardcode these in the script. Read from env vars, or
# fall back to the gitignored local src/main/resources/stripe.properties. ──────
if [[ -z "$STRIPE_SECRET_KEY" || -z "$STRIPE_PUBLISHABLE_KEY" ]]; then
  STRIPE_PROPS="src/main/resources/stripe.properties"
  if [[ -f "$STRIPE_PROPS" ]]; then
    STRIPE_SECRET_KEY="${STRIPE_SECRET_KEY:-$(grep '^stripe.secret.key=' "$STRIPE_PROPS" | cut -d'=' -f2-)}"
    STRIPE_PUBLISHABLE_KEY="${STRIPE_PUBLISHABLE_KEY:-$(grep '^stripe.publishable.key=' "$STRIPE_PROPS" | cut -d'=' -f2-)}"
  fi
fi

if [[ -z "$STRIPE_SECRET_KEY" || -z "$STRIPE_PUBLISHABLE_KEY" ]]; then
  echo "ERROR: Stripe keys not found."
  echo "Set STRIPE_SECRET_KEY and STRIPE_PUBLISHABLE_KEY env vars, or create"
  echo "src/main/resources/stripe.properties with:"
  echo "  stripe.secret.key=sk_test_..."
  echo "  stripe.publishable.key=pk_test_..."
  exit 1
fi

echo "======================================================"
echo " Building & Deploying Backend to Cloud Run"
echo "======================================================"

# Ensure Cloud SQL instance exists
echo "--> Checking Cloud SQL instance..."
if ! gcloud sql instances describe $DB_INSTANCE_NAME --project $PROJECT_ID &>/dev/null; then
    echo "--> Creating Cloud SQL instance..."
    gcloud sql instances create $DB_INSTANCE_NAME \
        --database-version=MYSQL_8_0 \
        --tier=db-f1-micro \
        --region=$REGION \
        --project=$PROJECT_ID
    gcloud sql databases create $DB_NAME --instance=$DB_INSTANCE_NAME --project=$PROJECT_ID
    gcloud sql users set-password $DB_USER --instance=$DB_INSTANCE_NAME --password=$DB_PASSWORD --project=$PROJECT_ID
fi

# Ensure Artifact Registry exists
echo "--> Checking Artifact Registry..."
if ! gcloud artifacts repositories describe $ARTIFACT_REPO --location=$REGION --project=$PROJECT_ID &>/dev/null; then
    gcloud artifacts repositories create $ARTIFACT_REPO \
        --repository-format=docker \
        --location=$REGION \
        --project=$PROJECT_ID
fi

echo "--> Building Backend Image..."
BUILD_ID=$(gcloud builds submit --tag $BACKEND_IMG --project $PROJECT_ID --async --format="value(id)")
echo "--> Build $BUILD_ID submitted, polling for completion..."
while true; do
    BUILD_STATUS=$(gcloud builds describe $BUILD_ID --project $PROJECT_ID --format="value(status)")
    case "$BUILD_STATUS" in
        SUCCESS) echo "--> Build succeeded."; break ;;
        WORKING|QUEUED) sleep 10 ;;
        *) echo "Build failed with status: $BUILD_STATUS"; exit 1 ;;
    esac
done

echo "--> Deploying Backend to Cloud Run..."
gcloud run deploy flyship-backend \
    --image $BACKEND_IMG \
    --region $REGION \
    --project $PROJECT_ID \
    --allow-unauthenticated \
    --memory 512Mi \
    --set-env-vars "SERVER_PORT=8080" \
    --set-env-vars "SPRING_DATASOURCE_URL=jdbc:mysql:///${DB_NAME}?cloudSqlInstance=${INSTANCE_CONNECTION_NAME}&socketFactory=com.google.cloud.sql.mysql.SocketFactory&useSSL=false" \
    --set-env-vars "SPRING_DATASOURCE_USERNAME=${DB_USER}" \
    --set-env-vars "SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}" \
    --set-env-vars "SPRING_JPA_HIBERNATE_DDL_AUTO=update" \
    --set-env-vars "APP_JWT_SECRET=${APP_JWT_SECRET}" \
    --set-env-vars "APP_JWT_EXPIRATION_MS=86400000" \
    --set-env-vars "STRIPE_SECRET_KEY=${STRIPE_SECRET_KEY}" \
    --set-env-vars "STRIPE_PUBLISHABLE_KEY=${STRIPE_PUBLISHABLE_KEY}" \
    --set-env-vars "SPRING_MAIL_HOST=smtp.ethereal.email" \
    --set-env-vars "SPRING_MAIL_PORT=587" \
    --set-env-vars "SPRING_MAIL_USERNAME=${SPRING_MAIL_USERNAME}" \
    --set-env-vars "SPRING_MAIL_PASSWORD=${SPRING_MAIL_PASSWORD}" \
    --set-env-vars "APP_UPLOAD_DIR=/tmp/uploads" \
    --set-env-vars "APP_GCS_BUCKET=peerpost-v2-uploads" \
    --add-cloudsql-instances ${INSTANCE_CONNECTION_NAME}

BACKEND_URL=$(gcloud run services describe flyship-backend --region $REGION --project $PROJECT_ID --format="value(status.url)")
echo "======================================================"
echo " Backend Deployment Complete!"
echo " Backend URL: $BACKEND_URL"
echo ""
echo " To update the mobile/frontend apps, run their set_api_url.sh"
echo " scripts with this URL."
echo "======================================================"
