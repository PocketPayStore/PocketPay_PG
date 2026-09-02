# PocketPay PG

PocketPay의 결제 승인·취소·거래 조회와 외부 결제사 장애를 재현하는 Mock PG 서버입니다. 실제 PG 없이 멱등 승인, 부분·전체 취소, 응답 지연과 HTTP 오류를 만들어 Core와 Batch의 장애 대응을 검증합니다.

## 주요 기능

- 결제 승인과 Mock PG 거래 ID 발급
- 멱등키 기반 동일 승인 결과 반환
- 부분 취소 및 전체 취소
- 거래 상태 조회와 승인 Webhook
- 요청 값 기반 응답 지연·HTTP 오류 주입
- 메모리 기반 거래 저장

~~~mermaid
sequenceDiagram
    participant Core as PocketPay Core
    participant PG as Mock PG
    participant Store as 거래 저장소
    Core->>PG: 승인 요청 + 멱등키
    PG->>Store: 기존 거래 조회
    alt 기존 요청
        PG-->>Core: 기존 승인 결과
    else 신규 요청
        PG->>Store: 승인 거래 저장
        PG-->>Core: 거래 ID와 승인 결과
        PG-->>Core: 승인 Webhook
    end
~~~

## API

| Method | Endpoint | 설명 |
|---|---|---|
| POST | /mock-pg/approve | 결제 승인 |
| POST | /mock-pg/cancel | 부분·전체 취소 |
| GET | /mock-pg/transactions/{txId} | 거래 상태 조회 |

승인은 Idempotency-Key 헤더를 사용하며 같은 키가 다시 오면 새 거래를 만들지 않습니다.

## 장애 재현

승인의 paymentKey 또는 취소의 merchantCancelId를 장애 트리거로 사용합니다. 규칙은 FaultTrigger에 정의돼 있으며 지연과 강제 HTTP 오류를 재현합니다. 거래 조회에도 지연이 포함돼 미확정 결제 대사의 외부 조회 비용을 확인할 수 있습니다.

## 기술 스택

Java 17, Spring Boot 4.1, Spring MVC, JUnit 5를 사용합니다.

## 실행

~~~bash
./gradlew bootRun --args='--server.port=8081'
~~~

Webhook 사용 시 Core 주소와 공유 비밀키를 설정합니다.

~~~yaml
pocketpay-core:
  webhook-url: http://localhost:8080/api/webhooks/pg
mock-pg:
  webhook:
    secret: change-me
~~~

## 테스트

~~~bash
./gradlew test
~~~

> 이 프로젝트는 장애 시나리오 검증용 Mock 서버입니다. 거래는 메모리에 저장돼 재시작하면 사라지며 실제 결제 용도로 사용할 수 없습니다.
