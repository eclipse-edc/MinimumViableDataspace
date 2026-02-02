#!/bin/bash
# Sign a verifiable credential JSON with the dataspace-issuer key. Outputs JWT to stdout.
# Usage: sign_vc.sh <vc.json> [issuer_private.pem]
# Issuer DID and subject (aud/sub) are taken from the VC (issuer, credentialSubject.id).

set -e
VC="$1"
KEY="${2:-$(cd "$(dirname "$0")/.." && pwd)/assets/issuer_private.pem}"
[ -z "$VC" ] || [ ! -f "$VC" ] && { echo "Usage: $0 <vc.json> [issuer_private.pem]" >&2; exit 1; }
[ ! -f "$KEY" ] && { echo "Key not found: $KEY" >&2; exit 1; }

b64url() { base64 -w 0 2>/dev/null | tr '+/' '-_' | tr -d '=' || base64 | tr -d '\n' | tr '+/' '-_' | tr -d '='; }

ISSUER=$(jq -r '.issuer // "did:web:dataspace-issuer"' "$VC")
SUBJECT=$(jq -r '.credentialSubject.id // .credentialSubject[0].id // empty' "$VC")
[ -z "$SUBJECT" ] && SUBJECT="$ISSUER"

HEADER=$(jq -nc --arg kid "${ISSUER}#key-1" '{alg:"EdDSA",kid:$kid,typ:"JWT"}')
PAYLOAD=$(jq -nc --arg iss "$ISSUER" --arg aud "$SUBJECT" --arg sub "$SUBJECT" --slurpfile vc "$VC" '{iss:$iss,aud:$aud,sub:$sub,vc:($vc[0]),iat:(now|floor)}')

B64H=$(echo -n "$HEADER" | b64url)
B64P=$(echo -n "$PAYLOAD" | b64url)
SIGN_INPUT="${B64H}.${B64P}"
TMP=$(mktemp)
trap "rm -f $TMP ${TMP}.msg" EXIT
printf '%s' "$SIGN_INPUT" > "${TMP}.msg"
# Ed25519 (EdDSA) requires -rawin in OpenSSL 3.x (CodeBuild amazonlinux image)
openssl pkeyutl -sign -inkey "$KEY" -in "${TMP}.msg" -out "$TMP" -rawin
SIG=$(base64 -w 0 "$TMP" 2>/dev/null | tr '+/' '-_' | tr -d '=' || base64 < "$TMP" | tr -d '\n' | tr '+/' '-_' | tr -d '=')
echo "${SIGN_INPUT}.${SIG}"
