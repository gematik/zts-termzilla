docker run --rm \
  --name nginx-fhir-cache \
  -p 8081:80 \
  -v "/Users/oro/Projekte/EHEALTH/git/tx/documentation/testing/nginx-cache.conf:/etc/nginx/conf.d/default.conf:ro" \
  nginx:stable-alpine