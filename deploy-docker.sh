docker build --platform linux/amd64 -t docker-image:test .
docker tag docker-image:test 308506855511.dkr.ecr.eu-west-1.amazonaws.com/mobile-notifications-content:latest
docker push 308506855511.dkr.ecr.eu-west-1.amazonaws.com/mobile-notifications-content:latest