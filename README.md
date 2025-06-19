# Geohash 기반 P2P CDN 시뮬레이션

Decentralized Supernode 아키텍처 기반의 P2P 네트워크에서 CDN 동작을 시뮬레이션 하는 것을 목표로 합니다.   
부트스트랩노드와 슈퍼노드, 일반노드가 각자의 역할을 수행하며  
지리적 위치(Geohash) 기반 Grouping을 통해 네트워크를 구축합니다.

---

## 빌드

`./gradlew clean shadowJar`

---

## 실행 방법

### 1. 부트스트랩 노드 실행

미리 등록된 포트(10001 ~ 10004) 중 하나를 사용

`java -jar build/libs/p2p-net.jar bootstrap {port}`

**예시**

서울 부트스트랩 노드 실행<br>
`java -jar build/libs/p2p-net.jar bootstrap 10001`

뉴욕 부트스트랩 노드 실행<br>
`java -jar build/libs/p2p-net.jar bootstrap 10002`

---

### 2. 부트스트랩 Redundancy 노드 실행

미리 등록된 포트(10005 ~ 10008) 중 하나를 사용
각각의 포트는 부트스트랩 노드(10001 ~ 10004)에 대응 

`java -jar build/libs/p2p-net.jar redundancy {port}`

**예시**

서울 부트스트랩 레둔던시 노드 실행<br>
`java -jar build/libs/p2p-net.jar bootstrap 10005`

뉴욕 부트스트랩 레둔던시 노드 실행<br>
`java -jar build/libs/p2p-net.jar bootstrap 10006`

---

### 3. 부트스트랩 노드 복구 실행

미리 등록된 포트(10001 ~ 10004) 중 하나를 사용
구동 중이던 부트스트랩 노드의 중지 시 네트워크 복귀를 위해 사용

`java -jar build/libs/p2p-net.jar revive {port}`

**예시**

서울 부트스트랩 노드 복구 실행<br>
`java -jar build/libs/p2p-net.jar revive 10001`

뉴욕 부트스트랩 노드 복구 실행<br>
`java -jar build/libs/p2p-net.jar revive 10002`

---

### 4. 일반 노드 실행

위도(latitude), 경도(longitude), 포트(port)를 입력

`java -jar build/libs/p2p-net.jar node {latitude} {longitude} {port}`

**예시**

첫번째 노드 실행 (포트 9011)<br>
`java -jar build/libs/p2p-net.jar node 35.6822 139.6914 9011`

인근 두 번째 노드 실행 (포트 9012)<br>
`java -jar build/libs/p2p-net.jar node 35.682256 139.691311 9012`

---

## 실행 시나리오 예시

1. **부트스트랩 노드 실행**
    ```
    java -jar build/libs/p2p-net.jar bootstrap 10001
    ```

2. **부트스트랩 Redundancy 노드 실행**
    ```
    java -jar build/libs/p2p-net.jar redundancy 10005
    ```

3. **일반 노드 실행**
    ```
    java -jar build/libs/p2p-net.jar node 35.6822 139.6914 9011
    java -jar build/libs/p2p-net.jar node 35.682256 139.691311 9012
    ```

---

## 내부 명령어 (노드 구동 이후 터미널 입력)

### 1. 다운로드
`download {File Name}`

### 2. 로컬 라우팅 테이블 출력
`routing`

### 3. 슈퍼노드 테이블 출력
`supernode`

### 4. 파일 메타데이터 테이블 출력
`metadata`

### 5. 프로그램 종료
`exit`

## 참고

- 위도(`latitude`): -90 ~ 90  
- 경도(`longitude`): -180 ~ 180  
- 포트(`port`): 1024 ~ 65535 (10001-10004는 부트스트랩 노드, 10005-10008은 부트스트랩 Redundancy 노드)
- 최소 하나의 부트스트랩 노드와 그에 대응되는 하나의 레둔던시 노드의 구동은 필수적임
- 하나의 부트스트랩 노드만 실행시켜도 작동하지만, 완전한 구동 시 4개의 부트스트랩 노드와 4개의 대응 레둔던시 노드를 모두 실행시켜야 함.
