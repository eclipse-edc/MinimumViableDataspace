#
#  Copyright (c) 2024 Metaform Systems, Inc.
#
#  This program and the accompanying materials are made available under the
#  terms of the Apache License, Version 2.0 which is available at
#  https://www.apache.org/licenses/LICENSE-2.0
#
#  SPDX-License-Identifier: Apache-2.0
#
#  Contributors:
#       Metaform Systems, Inc. - initial API and implementation
#
#

# download the JWK generator if not present
if [ ! -f jwkgen.jar ]; then
	curl -sL https://connect2id.com/assets/products/nimbus-jose-jwt/download/json-web-key-generator.jar -o jwkgen.jar
fi

# download and unpack the JWT-CLI if not present
if [ ! -f jwt-cli.tar.gz ]; then
	if [ "$(uname)" == "Darwin" ]; then
		curl -L https://github.com/mike-engel/jwt-cli/releases/download/6.0.0/jwt-macOS.tar.gz -o jwt-cli.tar.gz
	elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
		curl -L https://github.com/mike-engel/jwt-cli/releases/download/6.0.0/jwt-linux.tar.gz -o jwt-cli.tar.gz
	else
		echo "Only Linux anx macOS are currently supported! Aborting..."
		exit 1
	fi
	tar -xzvf jwt-cli.tar.gz
	chmod +x jwt >/dev/null
fi

ISSUER_DID="did:example:dataspace-issuer"
KID="key-1"
ALG="ES256" # EcDSA keys
CURVE="P-521"

# generate key-pair for the dataspace issuer
KEY_FILE_PREFIX="assets/issuer"
PRIVATE_KEY_FILE=${KEY_FILE_PREFIX}_private_jwk.json
PUBLIC_KEY_FILE=${KEY_FILE_PREFIX}_public_jwk.json

# uncomment only if you need to regenerate the Issuer's keypair!
java -jar jwkgen.jar -c $CURVE -i $KID -t EC u sig -a "ES512" | tail -n +2 >$PRIVATE_KEY_FILE

# extract public key - simply remove the "d"
jq 'del(.d)' $PRIVATE_KEY_FILE >$PUBLIC_KEY_FILE

##############
## FOR ALICE:
##############

K8S_AUD="did:web:alice-identityhub%3A7083:alice"
LOCAL_AUD="did:web:localhost%3A7083"

# 1. K8S deployment:  update the JSON files, that contains the CredentialResource

CRED_RES_FILE=assets/credentials/k8s/alice/alice-membership-credential.json
MEMBERSHIP_VC=$(./jwt encode --alg "$ALG" --kid "$ISSUER_DID#$KID" --iss $ISSUER_DID --sub $K8S_AUD --aud $K8S_AUD -S "@$PRIVATE_KEY_FILE" -P "vc=$(jq . ./assets/credentials/k8s/alice/membership_vc.json)")
cat <<<$(jq --arg mvc "$MEMBERSHIP_VC" '.verifiableCredential.rawVc = $mvc' $CRED_RES_FILE) >$CRED_RES_FILE

CRED_RES_FILE=assets/credentials/k8s/alice/alice-pcf-credential.json
PCF_VC=$(./jwt encode --alg "$ALG" --kid "$ISSUER_DID#$KID" --iss $ISSUER_DID --sub $K8S_AUD --aud $K8S_AUD -S "@$PRIVATE_KEY_FILE" -P "vc=$(jq . ./assets/credentials/k8s/alice/pcf_vc.json)")
cat <<<$(jq --arg mvc "$PCF_VC" '.verifiableCredential.rawVc = $mvc' $CRED_RES_FILE) >$CRED_RES_FILE

# 2. Local deployment:  update the JSON files, that contains the CredentialResourceAUD="did:web:localhost%3A7083"

CRED_RES_FILE=assets/credentials/local/alice/alice-membership-credential.json
MEMBERSHIP_VC=$(./jwt encode --alg "$ALG" --kid "$ISSUER_DID#$KID" --iss $ISSUER_DID --sub $LOCAL_AUD --aud $LOCAL_AUD -S "@$PRIVATE_KEY_FILE" -P "vc=$(jq . ./assets/credentials/local/alice/membership_vc.json)")
cat <<<$(jq --arg mvc "$MEMBERSHIP_VC" '.verifiableCredential.rawVc = $mvc' $CRED_RES_FILE) >$CRED_RES_FILE

CRED_RES_FILE=assets/credentials/local/alice/alice-pcf-credential.json
PCF_VC=$(./jwt encode --alg "$ALG" --kid "$ISSUER_DID#$KID" --iss $ISSUER_DID --sub $LOCAL_AUD --aud $LOCAL_AUD -S "@$PRIVATE_KEY_FILE" -P "vc=$(jq . ./assets/credentials/local/alice/pcf_vc.json)")
cat <<<$(jq --arg mvc "$PCF_VC" '.verifiableCredential.rawVc = $mvc' $CRED_RES_FILE) >$CRED_RES_FILE

##############
## FOR BOB:
##############

K8S_AUD="did:web:bob-identityhub%3A7083:bob"
LOCAL_AUD="did:web:localhost%3A7093"

# 1. K8S deployment:  update the JSON files, that contains the CredentialResource

CRED_RES_FILE=assets/credentials/k8s/bob/bob-membership-credential.json
MEMBERSHIP_VC=$(./jwt encode --alg "$ALG" --kid "$ISSUER_DID#$KID" --iss $ISSUER_DID --sub $K8S_AUD --aud $K8S_AUD -S "@$PRIVATE_KEY_FILE" "$(jq . ./assets/credentials/k8s/bob/membership_vc.json)")
cat <<<$(jq --arg mvc "$MEMBERSHIP_VC" '.verifiableCredential.rawVc = $mvc' $CRED_RES_FILE) >$CRED_RES_FILE

CRED_RES_FILE=assets/credentials/k8s/bob/bob-pcf-credential.json
PCF_VC=$(./jwt encode --alg "$ALG" --kid "$ISSUER_DID#$KID" --iss $ISSUER_DID --sub $K8S_AUD --aud $K8S_AUD -S "@$PRIVATE_KEY_FILE" "$(jq . ./assets/credentials/k8s/bob/pcf_vc.json)")
cat <<<$(jq --arg mvc "$PCF_VC" '.verifiableCredential.rawVc = $mvc' $CRED_RES_FILE) >$CRED_RES_FILE

# 2. Local deployment:  update the JSON files, that contains the CredentialResourceAUD="did:web:localhost%3A7083"

CRED_RES_FILE=assets/credentials/local/bob/bob-membership-credential.json
MEMBERSHIP_VC=$(./jwt encode --alg "$ALG" --kid "$ISSUER_DID#$KID" --iss $ISSUER_DID --sub $LOCAL_AUD --aud $LOCAL_AUD -S "@$PRIVATE_KEY_FILE" "$(jq . ./assets/credentials/local/bob/membership_vc.json)")
cat <<<$(jq --arg mvc "$MEMBERSHIP_VC" '.verifiableCredential.rawVc = $mvc' $CRED_RES_FILE) >$CRED_RES_FILE

CRED_RES_FILE=assets/credentials/local/bob/bob-pcf-credential.json
PCF_VC=$(./jwt encode --alg "$ALG" --kid "$ISSUER_DID#$KID" --iss $ISSUER_DID --sub $LOCAL_AUD --aud $LOCAL_AUD -S "@$PRIVATE_KEY_FILE" "$(jq . ./assets/credentials/local/bob/pcf_vc.json)")
cat <<<$(jq --arg mvc "$PCF_VC" '.verifiableCredential.rawVc = $mvc' $CRED_RES_FILE) >$CRED_RES_FILE

####################
# Update Issuer DID
####################
DID=../runtimes/extensions/common-mocks/src/main/resources/did_example_dataspace-issuer.json
cat <<<$(jq --argjson pub "$(cat $PUBLIC_KEY_FILE)" '.verificationMethod[].publicKeyJwk = $pub' $DID) >$DID
