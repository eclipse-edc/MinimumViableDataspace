import subprocess
import os
import sys
import shutil
import time

pf_process = None

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

def toggle_cloudflare(enable=True):
    action = "connect" if enable else "disconnect"
    description = f"{'Enabling' if enable else 'Disabling'} Cloudflare WARP"
    if shutil.which("warp-cli"):
        return run_command(f"warp-cli {action}", description)
    else:
        print(f"Skipping {description}: 'warp-cli' not found.")
        return True

def start_mvd():
    toggle_cloudflare(enable=False)
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
        ("kubectl wait -A --selector=type=edc-job --for=condition=complete job --all --timeout=300s", "Waiting for seed jobs")
    ]
    
    for cmd, desc in commands:
        if not run_command(cmd, desc):
            print("Aborting start sequence due to failure.")
            toggle_cloudflare(enable=True)
            break
    else:
        print("\nMVD started successfully.")
        print("Note: You might need to run 'kubectl port-forward svc/traefik 80:80 -n traefik' manually (requires sudo).")
        toggle_cloudflare(enable=True)

def stop_mvd():
    print("\nStopping MVD (Deleting cluster)...")
    run_command("kind delete cluster -n mvd", "Deleting KinD cluster 'mvd'")

def status_mvd():
    print("\nChecking MVD Status...")
    run_command("kubectl get pods -A", "Current Pod Status")
    run_command("kubectl get jobs -A", "Current Job Status")

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

def start_port_forward():
    global pf_process
    if pf_process and pf_process.poll() is None:
        print("\nPort Forwarding is already running.")
        return

    print("\nStarting Port Forwarding in the background (requires sudo)...")
    # Validate sudo credentials in the foreground first to avoid blocking in background
    if not run_command("sudo -v", "Validating Sudo Credentials"):
        print("Sudo validation failed. Cannot start Port Forwarding.")
        return

    # Using sudo -E to preserve KUBECONFIG for the root user
    cmd = "sudo -E kubectl port-forward svc/traefik 80:80 -n traefik"
    try:
        # start_new_session=True helps decouple the process from the current terminal
        pf_process = subprocess.Popen(
            cmd, 
            shell=True, 
            stdout=subprocess.DEVNULL, 
            stderr=subprocess.DEVNULL,
            stdin=subprocess.DEVNULL,
            start_new_session=True
        )
        print("Port Forwarding started in background. Output redirected to /dev/null.")
    except Exception as e:
        print(f"Error starting Port Forwarding: {e}")

def stop_port_forward():
    global pf_process
    print("\nStopping Port Forwarding...")
    # Using pkill to ensure we catch any sudo-spawned processes
    # We use subprocess.run directly with check=False to avoid error messages if process is not found
    subprocess.run("sudo pkill -f 'kubectl port-forward.*traefik'", shell=True, check=False)
    if pf_process:
        pf_process.terminate()
        pf_process = None
    time.sleep(1) # Give it a moment to settle
    print("Port Forwarding stopped.")

def run_data_transfer():
    print("\nRunning Data Transfer...")
    # Using sys.executable to ensure we use the same python environment
    cmd = f"{sys.executable} scripts/transfer_data.py"
    run_command(cmd, "Data Transfer")

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
        print("4. Start Port Forwarding (Background)")
        print("5. Stop Port Forwarding")
        print("6. Run Data Transfer")
        print("0. Exit")
        
        choice = input("\nSelect an option (0-6): ")
        
        if choice == '1':
            start_mvd()
        elif choice == '2':
            stop_mvd()
        elif choice == '3':
            status_mvd()
        elif choice == '4':
            start_port_forward()
        elif choice == '5':
            stop_port_forward()
        elif choice == '6':
            run_data_transfer()
        elif choice == '0':
            stop_port_forward()
            print("Exiting...")
            sys.exit(0)
        else:
            print("Invalid option. Please try again.")

if __name__ == "__main__":
    main_menu()
