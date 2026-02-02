#!/bin/bash
# Seed the participant's private key (PEM) into the participant namespace Vault.
# Key path matches EDC alias: did:web:{participant}-identityhub.{participant}%3A7083:{participant}#key-1
# (URL-encoded in Vault as secret/KEY)
#
# Usage: ./seed_vault_participant_key.sh PARTICIPANT [path_to_private_pem]
# Default PEM: ./assets/<PARTICIPANT>_private.pem (relative to connector-deployment)
# Run from: connector-deployment/ (or set PEM path explicitly)
set -e

PARTICIPANT="${1}"
PEM_PATH="${2:-$(dirname "$0")/../assets/${PARTICIPANT}_private.pem}"
NS="${PARTICIPANT}"

[ -z "$PARTICIPANT" ] && { echo "Usage: $0 PARTICIPANT [path_to_private_pem]" >&2; exit 1; }
[ ! -f "$PEM_PATH" ] && { echo "PEM not found: $PEM_PATH" >&2; exit 1; }

# Vault secret key (URL-encoded DID#key-1, same as participant_vault_csv.sh / EDC)
PREFIX="did%3Aweb%3A${PARTICIPANT}-identityhub.${PARTICIPANT}%253A7083%3A${PARTICIPANT}"
VAULT_KEY="${PREFIX}%23key-1"

echo "Waiting for Vault pod in namespace $NS..."
for i in $(seq 1 30); do
  VAULT_POD=$(kubectl get pods -n "$NS" -l app.kubernetes.io/name=vault -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)
  [ -n "$VAULT_POD" ] && kubectl get pod -n "$NS" "$VAULT_POD" -o jsonpath='{.status.phase}' 2>/dev/null | grep -q Running && break
  sleep 2
done
[ -z "$VAULT_POD" ] && { echo "Vault pod not found in $NS" >&2; exit 1; }

TMP_IN_POD="/tmp/participant_key_$$.pem"
kubectl cp "$PEM_PATH" "$NS/$VAULT_POD:$TMP_IN_POD"
kubectl exec -n "$NS" "$VAULT_POD" -- vault kv put "secret/$VAULT_KEY" content=@"$TMP_IN_POD"
kubectl exec -n "$NS" "$VAULT_POD" -- rm -f "$TMP_IN_POD" 2>/dev/null || true
echo "Seeded Vault secret/$VAULT_KEY from $PEM_PATH"
