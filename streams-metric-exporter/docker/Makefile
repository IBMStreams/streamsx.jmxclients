.PHONY: image push
all: image
#REPOSITORY=
#WHOAMI=$(shell whoami)
WHOAMI=bmwilli1
IMAGE_VERSION=4.1.0
IMAGE_NAME=streams-metric-exporter
FULL_IMAGE_NAME=${WHOAMI}/${IMAGE_NAME}
DOCKERFILE=Dockerfile

image: ${DOCKERFILE}
	docker build -t ${FULL_IMAGE_NAME}:${IMAGE_VERSION} -t ${FULL_IMAGE_NAME}:latest -f ${DOCKERFILE} ..

push: image
	docker push ${FULL_IMAGE_NAME}:${IMAGE_VERSION}
	docker push ${FULL_IMAGE_NAME}:latest
