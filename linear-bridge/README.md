# linear-webhook-bridge

Cloud Function that bridges Linear issue events to GitHub Actions. When a Linear
issue moves to **In Progress**, this checks all three Flyship repos for a branch
prefixed with that issue's ID (e.g. `FLY-11`) and fires a `repository_dispatch`
event in each repo that has one, triggering that repo's `ci.yml` workflow.

## One-time setup

1. **Create a GitHub PAT** (classic token, `repo` scope) that this function will
   use to list branches and trigger dispatches. Store it in Secret Manager:
   ```
   echo -n "ghp_..." | gcloud secrets create linear-bridge-github-token \
       --project=peerpost-v2 --data-file=-
   ```

2. **Deploy the function first** (before you have a webhook secret, so you have
   a URL to register with Linear) — you can temporarily set a placeholder for
   `LINEAR_WEBHOOK_SECRET` and update it in step 4:
   ```
   echo -n "placeholder" | gcloud secrets create linear-webhook-secret \
       --project=peerpost-v2 --data-file=-
   ./deploy.sh
   ```

3. **Grant the function's service account access to both secrets** (one-time,
   only needed the first time either secret is created):
   ```
   PROJECT_NUMBER=$(gcloud projects describe peerpost-v2 --format="value(projectNumber)")
   SA="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"
   gcloud secrets add-iam-policy-binding linear-bridge-github-token \
       --project=peerpost-v2 --member="serviceAccount:${SA}" --role=roles/secretmanager.secretAccessor
   gcloud secrets add-iam-policy-binding linear-webhook-secret \
       --project=peerpost-v2 --member="serviceAccount:${SA}" --role=roles/secretmanager.secretAccessor
   ```

4. **Register the webhook in Linear**: Settings → API → Webhooks → New webhook.
   - URL: the `serviceConfig.uri` printed by `deploy.sh`
   - Events: Issues
   - Copy the signing secret Linear shows you, then update the real value:
     ```
     echo -n "<secret Linear gave you>" | gcloud secrets versions add linear-webhook-secret \
         --project=peerpost-v2 --data-file=-
     ```
   No redeploy needed — `--set-secrets` always resolves `:latest` at request time.

## Local testing

```
npm install
npm start
# in another terminal:
curl -X POST http://localhost:8080 \
  -H "Content-Type: application/json" \
  -H "Linear-Signature: <hmac-sha256 of the body below, hex, using your test secret>" \
  -d '{"action":"update","type":"Issue","data":{"identifier":"FLY-11","state":{"name":"In Progress"}}}'
```
