.PHONY: image push
all: image
#REPOSITORY=
#WHOAMI=$(shell whoami)
WHOAMI=bmwilli1
IMAGE_VERSION=5.0.0
IMAGE_MAJOR=`echo ${IMAGE_VERSION} | cut -d. -f1`
IMAGE_NAME=streams-jmx-client
FULL_IMAGE_NAME=${WHOAMI}/${IMAGE_NAME}
DOCKERFILE=Dockerfile

image: ${DOCKERFILE}
	docker build -t ${FULL_IMAGE_NAME}:${IMAGE_VERSION} -t ${FULL_IMAGE_NAME}:${IMAGE_MAJOR} -f ${DOCKERFILE} ..

push: image
	docker push ${FULL_IMAGE_NAME}:${IMAGE_VERSION}
	docker push ${FULL_IMAGE_NAME}:${IMAGE_MAJOR}
