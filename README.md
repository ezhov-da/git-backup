# git-backup

## Usage

https://github-api.kohsuke.org/
https://github.com/eclipse-jgit/jgit/wiki/User-Guide

## Build

```shell
gradlew clean build
```

Jar after building: [git-backup.jar](build/libs/git-backup.jar)

## Run

```shell
java jar -Dgithub.user=_username_ -Dgithub.token=_token_
```