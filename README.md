# TownRise Mod

TownRise: 시장의 시대 NeoForge 기반 모드 프로토타입입니다.

## 현재 기준

- Minecraft: 1.21.1
- NeoForge: 21.1.233
- Java: 21
- Mod ID: `townrise`

## 빌드

```bash
./gradlew --no-daemon test build
```

빌드 산출물:

```text
build/libs/townrise-0.1.3-dev.jar
```

## 모드 내장 Self-Updater

클라이언트에서 모드가 로드되면 백그라운드 스레드로 manifest를 확인할 수 있습니다.
새 jar가 있으면 다운로드 후 SHA-256을 검증하고, 안내 메시지를 띄운 다음 Minecraft를 종료합니다.
실행 중인 jar를 직접 덮어쓰지 않고 `.pending` 파일과 적용 스크립트를 사용해 다음 실행에 반영합니다.

업데이트 확인은 기본 비활성화이며, 아래 JVM 옵션이나 환경변수로 켭니다.

```bash
-Dtownrise.updateManifest=https://github.com/Jeong3954/townrise-mod/releases/latest/download/manifest.json
```

또는:

```bash
TOWNRISE_UPDATE_MANIFEST=https://github.com/Jeong3954/townrise-mod/releases/latest/download/manifest.json
```

`manifest.json` 자체 URL은 고정입니다. 버전이 바뀌면 이 manifest 파일 안의 jar URL만 새 GitHub Release asset으로 갱신됩니다.

## 업데이트 manifest 재생성

```bash
scripts/publish-local-update.sh
```

이 스크립트는 다음을 수행합니다.

1. `./gradlew --no-daemon clean build`
2. `updates/files/townrise-<version>.jar` 복사
3. `updates/manifest.json`의 SHA-256/size/version 갱신

## 클라이언트 테스트

1. `updates/files/townrise-0.1.3-dev.jar` 또는 `build/libs/townrise-0.1.3-dev.jar`를 클라이언트 `mods/townrise.jar`로 배치
2. JVM 옵션에 GitHub Release latest manifest URL 추가
3. 새 버전을 GitHub에 push하면 다음 실행 시 업데이트 확인
