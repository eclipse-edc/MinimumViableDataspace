#!/bin/bash
# Complete idempotent script to fix all participant issues
# Combines registration, vault fixes, credential cleanup, and DNS setup
# Safe to run multiple times - checks before making changes
# Usage: ./fix_participant.sh NAMESPACE
#
# Used by the connector pipeline (post_build) so STS client secret keys
# are properly deployed in Vault under the raw alias EDC uses.
# Also kept in repo root scripts/ for manual runs.

NAMESPACE=${1}

if [ -z "$NAMESPACE" ]; then
    echo "Usage: ./fix_participant.sh NAMESPACE"
    echo "Example: ./fix_participant.sh testcloud3"
    exit 1
fi

PARTICIPANT=$NAMESPACE
API_KEY="c3VwZXItdXNlcg==.c3VwZXItc2VjcmV0LWtleQo="
NEEDS_IH_RESTART=false

echo "=========================================="
echo "Complete Participant Fix: $NAMESPACE"
echo "=========================================="
echo ""

# Get backend pod
BACKEND_POD=$(kubectl get pods -n kordat | grep backend | grep Running | head -1 | awk '{print $1}')

if [ -z "$BACKEND_POD" ]; then
    echo "❌ No running backend pod found in kordat namespace"
    exit 1
fi

# Get vault pod
VAULT_POD=$(kubectl get pods -n $NAMESPACE | grep vault | grep Running | head -1 | awk '{print $1}')

if [ -z "$VAULT_POD" ]; then
    echo "❌ No running vault pod found in $NAMESPACE"
    exit 1
fi

echo "Backend Pod: $BACKEND_POD"
echo "Vault Pod: $VAULT_POD"
echo ""

# ==========================================
# Step 1: Fix vault super-user-apikey
# ==========================================
echo "Step 1: Checking vault super-user-apikey..."

if kubectl exec -n $NAMESPACE $VAULT_POD -- vault kv get secret/super-user-apikey &>/dev/null; then
    echo "  ✅ super-user-apikey exists"
else
    echo "  ➕ Adding super-user-apikey..."
    kubectl exec -n $NAMESPACE $VAULT_POD -- vault kv put secret/super-user-apikey content="$API_KEY" > /dev/null
    echo "  ✅ Added super-user-apikey"
    NEEDS_IH_RESTART=true
fi

echo ""

# ==========================================
# Step 2: Check/create dataspace-issuer ExternalName services
# ==========================================
echo "Step 2: Checking dataspace-issuer services..."

if kubectl get service dataspace-issuer -n $NAMESPACE &>/dev/null; then
    echo "  ✅ dataspace-issuer service exists"
else
    echo "  ➕ Creating dataspace-issuer service..."
    kubectl apply -f - <<EOF >/dev/null
apiVersion: v1
kind: Service
metadata:
  name: dataspace-issuer
  namespace: $NAMESPACE
spec:
  type: ExternalName
  externalName: dataspace-issuer.kordat.svc.cluster.local
EOF
    echo "  ✅ Created dataspace-issuer service"
fi

if kubectl get service dataspace-issuer-service -n $NAMESPACE &>/dev/null; then
    echo "  ✅ dataspace-issuer-service exists"
else
    echo "  ➕ Creating dataspace-issuer-service..."
    kubectl apply -f - <<EOF >/dev/null
apiVersion: v1
kind: Service
metadata:
  name: dataspace-issuer-service
  namespace: $NAMESPACE
spec:
  type: ExternalName
  externalName: dataspace-issuer-service.kordat.svc.cluster.local
EOF
    echo "  ✅ Created dataspace-issuer-service"
fi

echo ""

# ==========================================
# Step 3: Restart IdentityHub if needed
# ==========================================
if [ "$NEEDS_IH_RESTART" = true ]; then
    echo "Step 3: Restarting IdentityHub to pick up vault changes..."
    IH_POD=$(kubectl get pods -n $NAMESPACE | grep identityhub | grep Running | head -1 | awk '{print $1}')
    if [ -n "$IH_POD" ]; then
        kubectl delete pod -n $NAMESPACE $IH_POD &>/dev/null
        echo "  ✅ IdentityHub pod deleted, waiting for restart..."
        sleep 5
    fi
else
    echo "Step 3: IdentityHub restart not needed"
fi

echo ""

# ==========================================
# Step 4: Wait for identityhub to be ready
# ==========================================
echo "Step 4: Waiting for identityhub to be ready..."

for i in {1..60}; do
    IDENTITYHUB_POD=$(kubectl get pods -n $NAMESPACE | grep identityhub | grep Running | head -1 | awk '{print $1}' 2>/dev/null)
    if [ -n "$IDENTITYHUB_POD" ]; then
        if kubectl exec -n $NAMESPACE $IDENTITYHUB_POD -- curl -s -f http://localhost:7080/api/check/health &>/dev/null; then
            echo "  ✅ IdentityHub ready: $IDENTITYHUB_POD"
            break
        fi
    fi
    if [ $i -eq 60 ]; then
        echo "  ⚠️  IdentityHub not ready after 60 seconds, continuing anyway..."
        IDENTITYHUB_POD=$(kubectl get pods -n $NAMESPACE | grep identityhub | head -1 | awk '{print $1}')
        if [ -z "$IDENTITYHUB_POD" ]; then
            echo "  ❌ No IdentityHub pod found"
            exit 1
        fi
    fi
    sleep 1
done

echo ""

# ==========================================
# Step 5: Check STS client secret and register if missing
# ==========================================
echo "Step 5: Checking STS client secret..."

STS_SECRET="did%3Aweb%3A${NAMESPACE}-identityhub.${NAMESPACE}%253A7083%3A${NAMESPACE}-sts-client-secret"
STS_SECRET_SHORT="${NAMESPACE}-sts-client-secret"
STS_SECRET_RAW="did:web:${NAMESPACE}-identityhub.${NAMESPACE}%3A7083:${NAMESPACE}-sts-client-secret"

if kubectl exec -n $NAMESPACE $VAULT_POD -- vault kv get "secret/$STS_SECRET_RAW" &>/dev/null; then
    SECRET_VALUE=$(kubectl exec -n $NAMESPACE $VAULT_POD -- vault kv get -field=content "secret/$STS_SECRET_RAW" 2>/dev/null)
elif kubectl exec -n $NAMESPACE $VAULT_POD -- vault kv get "secret/$STS_SECRET" &>/dev/null; then
    SECRET_VALUE=$(kubectl exec -n $NAMESPACE $VAULT_POD -- vault kv get -field=content "secret/$STS_SECRET" 2>/dev/null)
fi

if [ -n "$SECRET_VALUE" ]; then
    echo "  ✅ STS client secret exists: $SECRET_VALUE"
    if ! kubectl exec -n $NAMESPACE $VAULT_POD -- vault kv get "secret/$STS_SECRET_RAW" &>/dev/null; then
        echo "  ➕ Storing STS client secret under raw alias (EDC lookup)..."
        kubectl exec -n $NAMESPACE $VAULT_POD -- sh -c "vault kv put 'secret/$STS_SECRET_RAW' content='$SECRET_VALUE'" &>/dev/null
        echo "  ✅ Raw alias stored"
        NEEDS_IH_RESTART=true
    fi
    if ! kubectl exec -n $NAMESPACE $VAULT_POD -- vault kv get "secret/$STS_SECRET_SHORT" &>/dev/null; then
        echo "  ➕ Storing STS client secret under short alias..."
        kubectl exec -n $NAMESPACE $VAULT_POD -- vault kv put "secret/$STS_SECRET_SHORT" content="$SECRET_VALUE" &>/dev/null
        echo "  ✅ Short alias $STS_SECRET_SHORT stored"
        NEEDS_IH_RESTART=true
    fi
else
    echo "  ➕ STS client secret missing, registering participant..."

    PARTICIPANT_ID=$(kubectl exec -n kordat $BACKEND_POD -- python -c "
import django
import os
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'kordat.settings')
django.setup()

from participants.models import Participant
try:
    p = Participant.objects.get(short_name='$NAMESPACE')
    print(p.id)
except Participant.DoesNotExist:
    print('NOT_FOUND')
" 2>/dev/null)

    if [ "$PARTICIPANT_ID" == "NOT_FOUND" ]; then
        echo "  ❌ Participant '$NAMESPACE' not found in database"
        exit 1
    fi

    echo "  ✅ Found participant ID: $PARTICIPANT_ID"

    RESULT=$(kubectl exec -n kordat $BACKEND_POD -- python -c "
import django
import os
import json
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'kordat.settings')
django.setup()

from participants.services.participant import register_participant_in_identityhub

result = register_participant_in_identityhub('$PARTICIPANT_ID')
print(json.dumps(result, indent=2, default=str))
" 2>&1)

    if echo "$RESULT" | grep -q '"status": "success"'; then
        echo "  ✅ Registration successful!"
    elif echo "$RESULT" | grep -q '"status": "exists"'; then
        echo "  ✅ Participant already registered!"
    else
        echo "  ⚠️  Registration status unclear, continuing..."
    fi

    if kubectl exec -n $NAMESPACE $VAULT_POD -- vault kv get "secret/$STS_SECRET_RAW" &>/dev/null; then
        SECRET_VALUE=$(kubectl exec -n $NAMESPACE $VAULT_POD -- vault kv get -field=content "secret/$STS_SECRET_RAW" 2>/dev/null)
        echo "  ✅ STS client secret now exists: $SECRET_VALUE"
    elif kubectl exec -n $NAMESPACE $VAULT_POD -- vault kv get "secret/$STS_SECRET" &>/dev/null; then
        SECRET_VALUE=$(kubectl exec -n $NAMESPACE $VAULT_POD -- vault kv get -field=content "secret/$STS_SECRET" 2>/dev/null)
        echo "  ✅ STS client secret now exists: $SECRET_VALUE"
        kubectl exec -n $NAMESPACE $VAULT_POD -- sh -c "vault kv put 'secret/$STS_SECRET_RAW' content='$SECRET_VALUE'" &>/dev/null
        echo "  ✅ Stored under raw alias for EDC"
    else
        echo "  ⚠️  STS client secret not created automatically"
        echo "  ➕ Generating and storing STS client secret manually..."

        CLIENT_SECRET=$(tr -dc 'A-Za-z0-9' </dev/urandom | head -c 16)

        kubectl exec -n $NAMESPACE $VAULT_POD -- vault kv put "secret/$STS_SECRET" content="$CLIENT_SECRET" &>/dev/null
        kubectl exec -n $NAMESPACE $VAULT_POD -- sh -c "vault kv put 'secret/$STS_SECRET_RAW' content='$CLIENT_SECRET'" &>/dev/null
        kubectl exec -n $NAMESPACE $VAULT_POD -- vault kv put "secret/$STS_SECRET_SHORT" content="$CLIENT_SECRET" &>/dev/null

        if kubectl exec -n $NAMESPACE $VAULT_POD -- vault kv get "secret/$STS_SECRET_RAW" &>/dev/null; then
            echo "  ✅ Created STS client secret: $CLIENT_SECRET"

            kubectl exec -n kordat $BACKEND_POD -- python -c "
import django
import os
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'kordat.settings')
django.setup()

from participants.models import Participant

p = Participant.objects.get(short_name='$NAMESPACE')
metadata = p.metadata or {}
metadata['client_secret'] = '$CLIENT_SECRET'
p.metadata = metadata
p.save()
print('✅ Stored client_secret in participant metadata')
" 2>&1 | grep "✅"
        else
            echo "  ❌ Failed to create STS client secret in vault"
        fi
    fi
fi

echo ""

# ==========================================
# Step 6: Check and delete corrupted credentials
# ==========================================
echo "Step 6: Checking for corrupted credentials..."

RESPONSE=$(kubectl exec -n $NAMESPACE $IDENTITYHUB_POD -- curl -s \
  -H "x-api-key: $API_KEY" \
  "http://localhost:7081/api/identity/v1alpha/participants/did:web:${PARTICIPANT}-identityhub.${NAMESPACE}%3A7083:${PARTICIPANT}/credentials" 2>&1)

if echo "$RESPONSE" | grep -q "HTTP ERROR"; then
    echo "  ⚠️  Could not fetch credentials (API may still be initializing)"
else
    TOTAL=$(echo "$RESPONSE" | jq '. | length' 2>/dev/null || echo "0")
    BAD=$(echo "$RESPONSE" | jq --arg expected "did:web:${PARTICIPANT}-identityhub.${NAMESPACE}%3A7083:${PARTICIPANT}" '[.[] | select((.verifiableCredential.rawVc | split(".")[1] | @base64d | fromjson | .vc.credentialSubject.id) != $expected)] | length' 2>/dev/null || echo "0")

    echo "  Total credentials: $TOTAL"
    echo "  Corrupted credentials: $BAD"

    if [ "$BAD" -gt 0 ]; then
        echo "  🗑️  Deleting corrupted credentials..."
        CORRUPTED_IDS=$(echo "$RESPONSE" | jq -r --arg expected "did:web:${PARTICIPANT}-identityhub.${NAMESPACE}%3A7083:${PARTICIPANT}" '
          .[] |
          select((.verifiableCredential.rawVc | split(".")[1] | @base64d | fromjson | .vc.credentialSubject.id) != $expected) |
          .id
        ')
        for CRED_ID in $CORRUPTED_IDS; do
            echo "    Deleting: $CRED_ID"
            kubectl exec -n $NAMESPACE $IDENTITYHUB_POD -- \
              curl -s -X DELETE \
              -H "x-api-key: $API_KEY" \
              "http://localhost:7081/api/identity/v1alpha/participants/did:web:${PARTICIPANT}-identityhub.${NAMESPACE}%3A7083:${PARTICIPANT}/credentials/${CRED_ID}" > /dev/null
        done
        echo "  ✅ Deleted $BAD corrupted credential(s)"
    else
        echo "  ✅ No corrupted credentials found"
    fi
fi

echo ""

# ==========================================
# Step 7: Final verification
# ==========================================
echo "Step 7: Final verification..."

if ! kubectl exec -n $NAMESPACE $VAULT_POD -- vault kv get "secret/$STS_SECRET_RAW" &>/dev/null; then
    echo "❌ STS client secret missing (raw alias - EDC uses this)"
else
    echo "✅ SUCCESS: $NAMESPACE is fully configured!"
    echo "  STS client secret present under raw alias."
fi

echo ""
