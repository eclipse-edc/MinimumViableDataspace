#!/bin/bash
# Produce participant PEM (private key) and signed -credential.json from participant_id.
# Tested with: public.ecr.aws/codebuild/amazonlinux-x86_64-standard:5.0 (jq, openssl preinstalled).
# Requirements: jq, openssl (on other AL images: dnf install -y jq)
#
# Usage: produce_participant_credentials.sh <participant_id> [output_dir] [dataspace_issuer_did]
# Example: produce_participant_credentials.sh testcloud4
# Output: assets/<participant_id>_private.pem, assets/<participant_id>_public.pem; assets/credentials/k8s/<participant_id>/*-credential.json
set -e

# Require jq and openssl (amazonlinux-x86_64-standard:5.0 has both; else: dnf install -y jq)
command -v jq >/dev/null 2>&1 || { echo "Missing: jq. On Amazon Linux: dnf install -y jq" >&2; exit 1; }
command -v openssl >/dev/null 2>&1 || { echo "Missing: openssl" >&2; exit 1; }

PARTICIPANT_ID="${1}"
OUT_DIR_ARG="${2}"
DATASPACE_ISSUER_DID="${3:-did:web:dataspace-issuer}"

ISSUANCEDATE_DATE=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
TIMESTAMP=$(date -u +%s)

[ -z "$PARTICIPANT_ID" ] && { echo "Usage: $0 <participant_id> [output_dir] [dataspace_issuer_did]" >&2; exit 1; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONNECTOR_DEPLOYMENT="$(cd "$SCRIPT_DIR/.." && pwd)"
CREDENTIALS_DIR="$CONNECTOR_DEPLOYMENT/assets/credentials"
TEMPLATE_DIR="$CREDENTIALS_DIR/k8s/participant"
OUT_DIR="${OUT_DIR_ARG:-$CREDENTIALS_DIR/k8s/$PARTICIPANT_ID}"
ISSUER_PEM="${ISSUER_PEM:-$CONNECTOR_DEPLOYMENT/assets/issuer_private.pem}"
SIGN_VC_SH="${SIGN_VC_SH:-$SCRIPT_DIR/sign_vc.sh}"
[ ! -f "$SIGN_VC_SH" ] && { echo "sign_vc.sh not found: $SIGN_VC_SH (set SIGN_VC_SH)" >&2; exit 1; }
[ ! -f "$ISSUER_PEM" ] && { echo "Issuer key not found: $ISSUER_PEM (set ISSUER_PEM)" >&2; exit 1; }
ASSETS_DIR="$CONNECTOR_DEPLOYMENT/assets"
mkdir -p "$OUT_DIR"
mkdir -p "$ASSETS_DIR"

PARTICIPANT_PRIVATE_PEM="$ASSETS_DIR/${PARTICIPANT_ID}_private.pem"
PARTICIPANT_PUBLIC_PEM="$ASSETS_DIR/${PARTICIPANT_ID}_public.pem"

# 1) Generate participant EC P256 private key and public key in assets/
openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:P-256 -out "$PARTICIPANT_PRIVATE_PEM" 2>/dev/null
openssl pkey -in "$PARTICIPANT_PRIVATE_PEM" -pubout -out "$PARTICIPANT_PUBLIC_PEM" 2>/dev/null
echo "Wrote $PARTICIPANT_PRIVATE_PEM"
echo "Wrote $PARTICIPANT_PUBLIC_PEM"

# 2) Substitute <VARIABLE> in templates and sign VC; build credential JSON
# Variables: <PARTICIPANTE>, <ISSUANCEDATE_DATE>, <CREDENTIAL_ID>, <TIMESTAMP>, <DATASPACE_ISSUER_DID>
subst_vc() {
  sed -e "s|<PARTICIPANTE>|$PARTICIPANT_ID|g" \
      -e "s|<ISSUANCEDATE_DATE>|$ISSUANCEDATE_DATE|g" \
      -e "s|<DATASPACE_ISSUER_DID>|$DATASPACE_ISSUER_DID|g" \
      "$1"
}
subst_cred() {
  sed -e "s|<PARTICIPANTE>|$PARTICIPANT_ID|g" \
      -e "s|<CREDENTIAL_ID>|$CRED_ID|g" \
      -e "s|<TIMESTAMP>|$TIMESTAMP|g" \
      -e "s|<DATASPACE_ISSUER_DID>|$DATASPACE_ISSUER_DID|g" \
      "$1"
}
# Portable UUID (Linux /proc first, then uuidgen, no dependency on util-linux in minimal images)
gen_uuid() { cat /proc/sys/kernel/random/uuid 2>/dev/null || uuidgen 2>/dev/null || echo "00000000-0000-0000-0000-000000000000"; }

for kind in dataprocessor membership; do
  VC_TMP=$(mktemp)
  subst_vc "$TEMPLATE_DIR/${kind}_vc.json" > "$VC_TMP"
  JWT=$("$SIGN_VC_SH" "$VC_TMP" "$ISSUER_PEM")
  rm -f "$VC_TMP"
  CRED_ID=$(gen_uuid)
  CRED_TMP=$(mktemp)
  JWT_TMP=$(mktemp)
  printf '%s' "$JWT" > "$JWT_TMP"
  subst_cred "$TEMPLATE_DIR/${kind}-credential.json" > "$CRED_TMP"
  jq --rawfile raw "$JWT_TMP" '.verifiableCredential.rawVc = $raw' "$CRED_TMP" > "$OUT_DIR/${kind}-credential.json"
  rm -f "$CRED_TMP" "$JWT_TMP"
  echo "Wrote $OUT_DIR/${kind}-credential.json"
done
