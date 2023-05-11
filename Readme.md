# RentLAW Transaction Service

This repository stores the codebase for transaction service as one of the service of our SOA app, RentLAW. This project is made as a final project for Web Service and Applications class.

## Transaction Service

- Handles order and payment
- Notifies rental provider upon payment through messaging service

### Dockerizing the App

Pre-requisite: docker

- ``./gradlew bootJar``: Build application jar file
- ``docker build -t <image_name> <directory>``: Create a docker image
- ``docker run -p <port_in>:<port_out> <image_name>``: Create and run a docker container
- ``docker start <container_id>``: Start an app
- ``docker stop <container_id>``: Stop an app
- ``docker ps -a``: Container list
- ``docker images``: Images list
- ``docker rm <container1> <container2> ...``: Delete container
- ``docker rmi <image-name>``: Delete an image
