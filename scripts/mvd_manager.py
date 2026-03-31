import subprocess
import os
import sys

def run_command(command, description):
    print(f"\n--- {description} ---")
    print(f"Running: {command}")
    try:
        # Using shell=True for complex commands with pipes/redirects as found in README
        result = subprocess.run(command, shell=True, check=True, text=True)
        return result.returncode == 0
    except subprocess.CalledProcessError as e:
        print(f"Error: Command failed with exit code {e.returncode}")
        return False

def start_mvd():
    print("\nStarting MVD (Kubernetes)...")
    commands = [
        ("kind create cluster -n mvd", "Creating KinD cluster"),
        ("./gradlew build dockerize", "Building and dockerizing (this may take a while)"),
        ("kind load docker-image ghcr.io/eclipse-edc/mvd/controlplane:latest ghcr.io/eclipse-edc/mvd/dataplane:latest ghcr.io/eclipse-edc/mvd/identity-hub:latest ghcr.io/eclipse-edc/mvd/issuerservice:latest -n mvd", "Loading images into KinD"),
        ("helm repo add traefik https://traefik.github.io/charts && helm repo update", "Setting up Helm repo"),
        ("helm upgrade --install --namespace traefik traefik traefik/traefik --create-namespace -f values.yaml", "Installing Traefik"),
        ("kubectl rollout status deployment/traefik -n traefik --timeout=120s", "Waiting for Traefik"),
        ("kubectl apply --server-side --force-conflicts -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.4.1/experimental-install.yaml", "Installing Gateway API CRDs"),
        ("kubectl apply -k k8s", "Deploying MVD via Kustomize"),
        ("kubectl wait -A --selector=type=edc-job --for=condition=complete job --all --timeout=90s", "Waiting for seed jobs")
    ]
    
    for cmd, desc in commands:
        if not run_command(cmd, desc):
            print("Aborting start sequence due to failure.")
            break
    else:
        print("\nMVD started successfully.")
        print("Note: You might need to run 'kubectl port-forward svc/traefik 80:80 -n traefik' manually (requires sudo).")

def stop_mvd():
    print("\nStopping MVD (Deleting cluster)...")
    run_command("kind delete cluster -n mvd", "Deleting KinD cluster 'mvd'")

def status_mvd():
    print("\nChecking MVD Status...")
    run_command("kubectl get pods -A", "Current Pod Status")
    run_command("kubectl get jobs -A", "Current Job Status")

import shutil

def check_dependencies():
    deps = ["docker", "kind", "helm", "java", "git", "kubectl"]
    missing = []
    for dep in deps:
        if shutil.which(dep) is None:
            missing.append(dep)
    
    if missing:
        print("\nERROR: The following required software is missing:")
        for m in missing:
            print(f"  - {m}")
        print("\nPlease install these before running the MVD manager.")
        return False
    return True

def main_menu():
    if not check_dependencies():
        sys.exit(1)
    
    while True:
        print("\n============================")
        print("   MVD Management Menu")
        print("============================")
        print("1. Start MVD (Build & Deploy)")
        print("2. Stop MVD (Delete Cluster)")
        print("3. Check MVD Status")
        print("0. Exit")
        
        choice = input("\nSelect an option (0-3): ")
        
        if choice == '1':
            start_mvd()
        elif choice == '2':
            stop_mvd()
        elif choice == '3':
            status_mvd()
        elif choice == '0':
            print("Exiting...")
            sys.exit(0)
        else:
            print("Invalid option. Please try again.")

if __name__ == "__main__":
    main_menu()
