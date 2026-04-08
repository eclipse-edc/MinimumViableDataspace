import requests
import time
import json
import sys

# Configuration from Bruno collection environment
CONSUMER_CP = "http://cp.consumer.localhost"
PROVIDER_DSP = "http://controlplane.provider.svc.cluster.local:8082/api/dsp/2025-1"
PROVIDER_ID = "did:web:identityhub.provider.svc.cluster.local%3A7083:provider"
PROVIDER_PUBLIC_URL = "http://dp.provider.localhost/public"
API_KEY = "password"

HEADERS = {
    "X-Api-Key": API_KEY,
    "Content-Type": "application/json"
}

ASSET_ID = "asset-1"

def log(message):
    print(f"[LOG] {message}")

def error(message):
    print(f"[ERROR] {message}")
    sys.exit(1)

def request_catalog():
    log("Requesting catalog...")
    url = f"{CONSUMER_CP}/api/mgmt/v4beta/catalog/request"
    payload = {
        "@context": ["https://w3id.org/edc/connector/management/v2"],
        "@type": "CatalogRequest",
        "counterPartyAddress": PROVIDER_DSP,
        "counterPartyId": PROVIDER_ID,
        "protocol": "dataspace-protocol-http:2025-1",
        "querySpec": {"offset": 0, "limit": 50}
    }
    response = requests.post(url, headers=HEADERS, json=payload)
    if response.status_code not in range(200, 300):
        error(f"Failed to get catalog: {response.text}")
    
    catalog = response.json()
    datasets = catalog.get("dataset", [])
    if isinstance(datasets, dict): # Handle single dataset case if returned as dict
        datasets = [datasets]
        
    asset = next((ds for ds in datasets if ds.get("@id") == ASSET_ID), None)
    if not asset:
        error(f"Asset {ASSET_ID} not found in catalog")
    
    policy_id = asset["hasPolicy"][0]["@id"]
    log(f"Found policy ID: {policy_id}")
    return policy_id

def initiate_negotiation(policy_id):
    log("Initiating negotiation...")
    url = f"{CONSUMER_CP}/api/mgmt/v4beta/contractnegotiations"
    payload = {
        "@context": ["https://w3id.org/edc/connector/management/v2"],
        "@type": "ContractRequest",
        "counterPartyAddress": PROVIDER_DSP,
        "counterPartyId": PROVIDER_ID,
        "protocol": "dataspace-protocol-http:2025-1",
        "policy": {
            "@type": "Offer",
            "@id": policy_id,
            "assigner": PROVIDER_ID,
            "permission": [],
            "prohibition": [],
            "obligation": {
                "action": "use",
                "constraint": {
                    "leftOperand": "ManufacturerCredential.part_types",
                    "operator": "eq",
                    "rightOperand": "non_critical"
                }
            },
            "target": ASSET_ID
        },
        "callbackAddresses": []
    }
    response = requests.post(url, headers=HEADERS, json=payload)
    if response.status_code not in range(200, 300):
        error(f"Failed to initiate negotiation: {response.text}")
    
    negotiation_id = response.json()["@id"]
    log(f"Negotiation initiated: {negotiation_id}")
    return negotiation_id

def wait_for_negotiation(negotiation_id):
    log(f"Waiting for negotiation {negotiation_id} to be finalized...")
    url = f"{CONSUMER_CP}/api/mgmt/v4beta/contractnegotiations/request"
    payload = {
        "@context": ["https://w3id.org/edc/connector/management/v2"],
        "@type": "QuerySpec"
    }
    
    while True:
        response = requests.post(url, headers=HEADERS, json=payload)
        if response.status_code in range(200, 300):
            negotiations = response.json()
            negotiation = next((n for n in negotiations if n.get("@id") == negotiation_id), None)
            if negotiation and "contractAgreementId" in negotiation:
                agreement_id = negotiation["contractAgreementId"]
                log(f"Negotiation finalized. Agreement ID: {agreement_id}")
                return agreement_id
        
        log("Still waiting for negotiation...")
        time.sleep(2)

def initiate_transfer(agreement_id):
    log("Initiating transfer...")
    url = f"{CONSUMER_CP}/api/mgmt/v4beta/transferprocesses"
    payload = {
        "@context": ["https://w3id.org/edc/connector/management/v2"],
        "assetId": ASSET_ID,
        "@type": "TransferRequest",
        "counterPartyAddress": PROVIDER_DSP,
        "connectorId": PROVIDER_ID,
        "contractId": agreement_id,
        "dataDestination": {
            "@type": "DataAddress",
            "type": "HttpProxy"
        },
        "protocol": "dataspace-protocol-http:2025-1",
        "transferType": "HttpData-PULL"
    }
    response = requests.post(url, headers=HEADERS, json=payload)
    if response.status_code not in range(200, 300):
        error(f"Failed to initiate transfer: {response.text}")
    
    transfer_id = response.json()["@id"]
    log(f"Transfer initiated: {transfer_id}")
    return transfer_id

def wait_for_edr(transfer_id):
    log(f"Waiting for EDR for transfer {transfer_id}...")
    url = f"{CONSUMER_CP}/api/mgmt/v4beta/edrs/request"
    payload = {
        "@context": ["https://w3id.org/edc/connector/management/v2"],
        "@type": "QuerySpec"
    }
    
    while True:
        response = requests.post(url, headers=HEADERS, json=payload)
        if response.status_code in range(200, 300):
            edrs = response.json()
            # The Bruno script just takes the first EDR: res.getBody()[0]["transferProcessId"]
            # We filter by our transfer_id
            edr = next((e for e in edrs if e.get("transferProcessId") == transfer_id), None)
            if edr:
                log("EDR found in cache.")
                return transfer_id
        
        log("Still waiting for EDR...")
        time.sleep(2)

def get_authorization_token(transfer_id):
    log(f"Fetching authorization token for transfer {transfer_id}...")
    url = f"{CONSUMER_CP}/api/mgmt/v4beta/edrs/{transfer_id}/dataaddress"
    response = requests.get(url, headers=HEADERS)
    if response.status_code not in range(200, 300):
        error(f"Failed to get EDR data address: {response.text}")
    
    auth_token = response.json().get("authorization")
    if not auth_token:
        error("Authorization token not found in EDR data address")
    
    log("Authorization token obtained.")
    return auth_token

def download_data(auth_token):
    log("Downloading data from provider...")
    url = f"{PROVIDER_PUBLIC_URL}/api/public"
    headers = {"Authorization": auth_token}
    response = requests.get(url, headers=headers)
    if response.status_code not in range(200, 300):
        error(f"Failed to download data: {response.status_code} - {response.text}")
    
    log("Data downloaded successfully!")
    print("\n--- DATA ---")
    print(response.text)
    print("------------\n")

def main():
    try:
        policy_id = request_catalog()
        negotiation_id = initiate_negotiation(policy_id)
        agreement_id = wait_for_negotiation(negotiation_id)
        transfer_id = initiate_transfer(agreement_id)
        wait_for_edr(transfer_id)
        auth_token = get_authorization_token(transfer_id)
        download_data(auth_token)
        log("Transfer flow completed successfully.")
    except Exception as e:
        error(f"An unexpected error occurred: {e}")

if __name__ == "__main__":
    main()
