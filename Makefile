APP_NAME=tasks

.PHONY: build test run docker

build:
	mvn -q -DskipTests package

test:
	mvn -q test

run:
	SPRING_PROFILES_ACTIVE=local mvn -q spring-boot:run

docker:
	mvn -q -DskipTests spring-boot:build-image -Dspring-boot.build-image.imageName=$(APP_NAME):latest

