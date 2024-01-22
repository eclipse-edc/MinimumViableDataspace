for url in 'http://127.0.0.1:8081' 'http://127.0.0.1:8091'
do
  newman run --folder "Seed" \
    --env-var "HOST=$url" \
    ./deployment/postman/IATP_Demo.postman_collection.json

done
