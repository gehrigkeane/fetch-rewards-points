# Fetch Rewards - Points REST API

This application, titled `fetch-rewards-points`, is build with 
[Spring Boot](https://spring.io/projects/spring-boot) and includes a modest 
suite of unit and feature tests. Data objects and their validation criteria are 
well covered by unit tests. Though controller, service, and domain 
implementations rely on feature tests for the sake of timeliness.

API documentation is self-hosted, allows live API interaction, and is available once the application is started at 
[`http://127.0.0.1:8080/swagger-ui/`](http://127.0.0.1:8080/swagger-ui/).

There you'll find documentation for three REST requests beneath the `User` resource:

- `GET /user/{name}/points`: Retrieves a Users point balance by payer, always in order from the first encountered payer to most recent. 
- `POST /user/{name}/points`: Add points to a Users balance, accepts an JSON object with requires fields `"payer"`, `"point"`, and `"date"`. 
	Note: `"date"` is optional and defaults to current time, though if supplied it must be of the form `yyyy-MM-dd'T'HH:mm:ss.SSSz` (ISO-8601)
	e.g. `"date": "2021-01-01T08:00:00.000Z"`.
- `DELETE /user/{name}/points`: Deduct points from a Users balance, returning the deductions by payer.

## Bootstrapping

The following is pertinent only for development or native application execution (i.e. sans Docker)

```shell
# Ensure maven is installed
brew install maven

# Ensure Java 15 is installed, the following is preferential:
brew install asdf
asdf plugin add java
asdf install
```

## Build and Run

To build or run the application, we've several options:

### Native Maven

If the Java 15 and the latest maven are installed and available locally:

```shell
# Build with Maven
mvn clean install

# Run with Native Maven
mvn spring-boot:run

# Tests are run automatically on build, but they can be run manually via:
mvn test
```

### Maven in Docker

To construct build artifacts:

```shell
# Build the Application Jar with Maven-In-Docker
docker run -it --rm \
	--name fetch-rewards-points_build \
	--volume "$(pwd)":/usr/src/ \
	--workdir /usr/src/ \
	maven:3.6-adoptopenjdk-15 mvn clean install
	
# Build Dockerfile with resulting Jar
docker build . --tag fetch-rewards-points:latest

# Run Build Image
docker run -it --rm \
	--name fetch-rewards-points \
	--publish "8080:8080" \
	fetch-rewards-points:latest

# Alternatively, we can both build and run with Maven, though it's a bit slower:
docker run -it --rm \
	--name fetch-rewards-points_run \
	--volume "$(pwd)":/usr/src/ \
	--workdir /usr/src/ \
	--publish "8080:8080" \
	maven:3.6-adoptopenjdk-15 mvn spring-boot:run
```


