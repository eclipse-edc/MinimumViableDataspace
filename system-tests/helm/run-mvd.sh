#!/usr/bin/bash
echo "installing azurite..."
helm install azurite ./helm-charts/azurite

echo "installing did-server..."
helm install did-server ./helm-charts/did-server

echo "installing registration-service..."
helm install registration-service ./helm-charts/registration-service

echo "installing company1..."
helm install company1 --set nameOverride=company1,ports.mgmt.nodePort=30091 ./helm-charts/company

echo "installing company2..."
helm install company2 --set nameOverride=company2,ports.mgmt.nodePort=30092 ./helm-charts/company

echo "installing company3..."
helm install company3 --set nameOverride=company3,ports.mgmt.nodePort=30093 ./helm-charts/company

echo "installing newman..."
helm install newman ./helm-charts/newman

echo "installing cli-tools..."
helm install cli-tools ./helm-charts/cli-tools

echo "installing company1 data dashboard..."
helm install company1-datadashboard --set nameOverride=company1-datadashboard,companyName=company1,ports.http.nodePort=31111 ./helm-charts/company-dashboard

echo "installing company2 data dashboard..."
helm install company2-datadashboard --set nameOverride=company2-datadashboard,companyName=company2,ports.http.nodePort=31112 ./helm-charts/company-dashboard

echo "installing company3 data dashboard..."
helm install company3-datadashboard --set nameOverride=company3-datadashboard,companyName=company3,ports.http.nodePort=31113 ./helm-charts/company-dashboard
