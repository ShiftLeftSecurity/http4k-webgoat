# http4k-webgoat

flask-webgoat is a deliberately-vulnerable application written with the `http4k` web framework.

## Build/test locally

```shell script
./gradlew test distZip
unzip build/distributions/Example.zip
Example/bin/Example
```

then:
```shell script
curl -v http://localhost:8080/
```

## Build/run in Docker

```shell script
./build_and_run.sh
```

then:
```shell script
curl -v http://localhost:8080/
```

## Vulnerabilities

// TODO: list them out
