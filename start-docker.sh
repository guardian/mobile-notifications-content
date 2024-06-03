docker build --platform linux/amd64 -t docker-image:test .
docker run --platform linux/amd64 -e "App=mobile-notifications-content" -e "CrossAccountSsmReadingRole=arn:aws:iam::201359054765:role/mobile-content-notifications-lambda-cross-account-ssm-CODE" -e "Stack=content-api-mobile-notifications" -e "Stage=CODE" -e "AWS_DEFAULT_REGION=eu-west-1"  -e "AWS_REGION=eu-west-1" -p 9000:8080  docker-image:test
