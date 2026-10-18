# 공공데이터포털 (public-data-viewer)

경기도 지역 공공데이터를 한 곳에서 조회하는 Android 앱입니다. Kotlin + Jetpack Compose로 만들었습니다.

## 기능

| 분류 | 기능 | 데이터 출처 |
|---|---|---|
| 교통정보 | 버스: 노선번호 검색 → 정류소 선택 → 도착 예정 시간, 잔여석 | [경기도 버스정보시스템(GBIS)](https://www.gbis.go.kr/) (공공데이터포털) |
| 교통정보 | 지하철: 역 이름 검색 → 열차별 도착 정보 | [서울 열린데이터광장](https://data.seoul.go.kr/) 지하철 실시간 도착정보 |
| 생활정보 | 유가정보: 시/도 → 시/군/구 휘발유·경유 평균가 | [오피넷](https://www.opinet.co.kr/) |
| 생활정보 | 유가 최저가: 내 위치 반경 3km 최저가 주유소, 지도 표시, 네이버 지도 길찾기 | 오피넷 + OpenStreetMap |

## 빌드 방법

### 1. API 키 발급

| 키 | 발급처 | 비고 |
|---|---|---|
| `GBIS_API_KEY` | [공공데이터포털](https://www.data.go.kr/) | "경기도_버스도착정보 조회"와 "경기도_버스노선 조회"를 **각각** 활용신청해야 합니다 |
| `SEOUL_SUBWAY_API_KEY` | [서울 열린데이터광장](https://data.seoul.go.kr/) | 지하철 실시간 도착정보 인증키 |
| `OPINET_API_KEY` | [오피넷 유가정보 API](https://www.opinet.co.kr/user/custapi/custApiInfo.do) | 일반 API 인증키 |

### 2. `local.properties` 설정

프로젝트 루트의 `local.properties`에 키를 추가합니다. 이 파일은 `.gitignore`로 제외되어 있어 저장소에 올라가지 않습니다.

```properties
sdk.dir=C\:\\Users\\<사용자>\\AppData\\Local\\Android\\Sdk
GBIS_API_KEY=발급받은_키
SEOUL_SUBWAY_API_KEY=발급받은_키
OPINET_API_KEY=발급받은_키
```

키는 빌드 시 `BuildConfig`로 주입됩니다. 키가 비어 있으면 해당 기능은 인증 오류를 표시합니다.

### 3. 빌드 및 실행

Android Studio에서 프로젝트를 열고 Run 하거나, 명령줄에서 빌드할 수 있습니다.

```bash
gradle assembleDebug
```

- JDK 17 필요 (Gradle 8.7 + AGP 8.5.2). Android Studio 내장 JDK가 25인 경우 Gradle JDK를 17로 지정하세요.
- minSdk 26, targetSdk 35

## 참고

- 서울 지하철 API는 HTTP만 지원해서 `network_security_config.xml`에서 해당 도메인만 평문 통신을 허용합니다.
- 주변 주유소 검색은 오피넷이 쓰는 KATEC 좌표계로 변환해서 호출합니다 (`KatecConverter`).
- 지도는 API 키가 필요 없는 [osmdroid](https://github.com/osmdroid/osmdroid)(OpenStreetMap)를 사용합니다.
- 길찾기는 네이버 지도 앱이 설치되어 있어야 하며, 없으면 스토어로 이동합니다.
