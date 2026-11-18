# AI 정리 · Groq STT 백엔드 연결

작업 브랜치: `feature/ai-stt-flow` / 프론트 구현과 GitHub 업로드는 포함하지 않음.

## 구현한 기능

- Upstage: 짧은 공감, 감정 후보, 욕구·원했던 반응, 3인칭 정리, 만약–그러면 계획 제안.
- Groq: 최대 60초 한국어 녹음의 텍스트 변환. 사용자가 전사문을 수정한 뒤 공감·감정 API로 보낸다.
- 사용자 확인: AI 초안은 자동으로 확정하지 않는다. 감정을 몰라도 진행하며 모든 정리 단계에서 직접 입력 가능.
- 결과 확인·수정: 확정한 내용만 현재 세션에서 조회. 앞 단계를 수정하면 뒤 단계 결과를 제거하고 다시 확정한다.
- 감정 버리기: 서버 임시 상태 제거, 진행 중 요청 확인, 늦게 도착한 AI·STT 결과 차단.

AI의 공감 품질·상담 효과·위험 상황 분류를 검증한 기능은 아니다. 실제 상대를 사칭하거나 메시지를 보내지 않는다.

## IntelliJ에서 키 입력

`src/main/resources/application-local.yml`에만 본인 키를 입력하고 서버를 다시 실행한다.
이미 있는 `upstage` 또는 `groq` 항목을 수정한다. 같은 항목을 두 번 만들지 않는다.

```yaml
upstage:
  api-key: "본인 Upstage 키"
groq:
  api-key: "본인 Groq 키"
```

이 파일은 Git에서 제외되어 있다. `.example` 파일·채팅·스크린샷·공유 실행 설정에 키를 넣지 않는다.
대안은 실행 환경변수 `UPSTAGE_API_KEY`, `GROQ_API_KEY`다. 로컬 YAML과 환경변수 중 한 방식으로 관리한다.
`.env` 파일은 Spring이 자동으로 읽지 않는다.

기본 모델은 Upstage `solar-pro4`, Groq `whisper-large-v3-turbo`이며 공통 설정은 `application.yml`에 있다.
키가 없어도 서버는 실행된다. AI 호출 시 `AI_001`, STT 호출 시 `STT_004`를 반환한다.
실제 계정의 모델 접근권·크레딧 적용·호출 한도는 콘솔에서 확인한다.

## 공통 사용법

- 기존 회원가입·로그인 API 사용. 모든 API에 `Authorization: Bearer <accessToken>` 필요.
- Swagger: `http://localhost:8080/swagger-ui/index.html` → Authorize → **5. 대화 세션**, **6. 음성 인식**.
- 응답 형식: `{ "success": true, "data": { ... }, "error": null }`. 아래 예시는 `data` 또는 요청 본문이다.
- 성공 응답 `Cache-Control: no-store`. 일반 텍스트로 표시하고 HTML로 삽입하지 않는다.
- `id`는 세션 생성 응답의 `sessionId`(UUID). 캐릭터 ID·로그인 토큰과 다른 값이다.
- 화면은 서버의 `stage`, `expiresAt`을 확인한다. 409이면 단계를 다시 조회하고, 410/404이면 종료 화면으로 처리한다.

## 상태와 API

```text
VENTING → CALMING → REFLECTION → SUMMARY → PLAN → RESULT
   어느 단계에서나 삭제·만료 → CLOSED → 메타데이터 제거
```

아래 경로의 `S`는 `/api/v1/vent/sessions/{id}`를 뜻한다.

| 순서 | 메서드·경로 | 요청 / 반환 | 허용 단계 |
| --- | --- | --- | --- |
| 1 | POST `/api/v1/vent/sessions` | 본문 없음 → 세션 생성 | 로그인 후 |
| 2 | POST `/api/v1/stt` | multipart `sessionId`, `file` → `text`, `durationSeconds` | VENTING |
| 3 | POST `S/messages` | `{"text":"하고 싶은 말"}` → `empathy` | VENTING |
| 4 | POST `S/emotions` | `{"text":"감정을 살펴볼 말"}` → `empathy`, `candidates`, `canSkip` | VENTING |
| 5 | POST `S/emotions/confirm` | `{"emotion":"서운함"}` 또는 `{"emotion":""}` → 세션 | VENTING |
| 6 | GET `S/calming` | `suggestedSeconds:20`, `steps`, `canSkip:true` | CALMING |
| 7 | POST `S/calming/complete` | `{"skipped":true}`(건너뛰기) 또는 false → 세션 | CALMING |
| 8 | POST `S/needs/suggestions` | `{"situation":"새로 적은 상황","desiredResponse":"원했던 반응"}` → 욕구 후보·반응 초안 | REFLECTION 이후 |
| 9 | PUT `S/reflection` | 아래 확정 예시 → 세션 | REFLECTION 이후 |
| 10 | POST `S/summary/suggestions` | 본문 없음 → `summary`, `requiresConfirmation:true` | SUMMARY 이후 |
| 11 | PUT `S/summary` | `{"summary":"사용자가 고친 3인칭 정리"}` → 세션 | SUMMARY 이후 |
| 12 | POST `S/plan/suggestions` | 본문 없음 → `ifSituation`, `thenAction`, `requiresConfirmation:true` | PLAN, RESULT |
| 13 | PUT `S/plan` | 아래 확정 예시 → RESULT 세션 | PLAN, RESULT |
| 14 | GET `S` | 현재 단계·확정 결과 조회 | 종료 전 |
| 15 | DELETE `S` | 삭제 확인 응답 | 종료 전·삭제 확인 중 |

**단계 2는 선택 사항. 3·4·8·10·12의 AI 제안도 선택 사항**이다. AI 장애나 키 미설정 시 사용자 직접 입력으로 5·9·11·13을 호출해 진행할 수 있다.
감정 후보가 모두 틀리면 직접 입력, 빈 감정으로 진행, 같은 `/emotions` 재호출 중 선택한다. 재호출 결과가 반드시 바뀌지는 않는다.

`PUT S/reflection` 예시:

```json
{
  "situation": "친구와 정한 시간이 여러 번 바뀌었다.",
  "emotion": "",
  "need": "내 시간에 대한 존중",
  "desiredResponse": "변경이 생기면 미리 알려주기"
}
```

이 입력은 **감정 확인 이후 사용자가 새로 작성하고 확정한 내용**이다.
이전 하소연을 자동으로 복원·요약·재전송하지 않는다. 상황은 필수이고 감정·욕구·반응은 모르면 빈 값이 가능하다.
감정도 수정 가능하므로 요청에 현재 확정한 감정을 넣는다. 누락하면 빈 값으로 처리한다.

`PUT S/plan` 예시:

```json
{
  "ifSituation": "다시 약속 시간이 바뀌면",
  "thenAction": "내가 가능한 시간을 먼저 정리하고 차분하게 알려준다."
}
```

AI 제안만 받으면 단계가 바뀌지 않는다. `PUT`으로 사용자 확인이 끝나야 다음 단계로 진행한다.
RESULT에서 reflection 수정 → summary·plan 제거/SUMMARY로 이동.
summary 수정 → plan 제거/PLAN으로 이동. plan 수정 → RESULT 유지.

## 음성 업로드 계약

- `POST /api/v1/stt`, `multipart/form-data`, 파일 파트 이름 `file`, 문자 필드 `sessionId`.
- 녹음 최대 60초, 파일 최대 10MiB(10 × 1024 × 1024 바이트), 요청 전체 최대 11MiB.
- 요청 한도 내 파일 크기 초과는 `STT_002` 응답을 전달하도록 Tomcat의 잔여 본문 처리 한도도 11MiB로 맞췄다. 훨씬 큰 요청은 연결이 차단될 수 있다.
- webm, wav, mp3, mp4, m4a, ogg, flac 확장자와 대응 MIME/기본 컨테이너 헤더 검사.
- 브라우저는 실제 녹음 형식에 맞는 파일명을 지정한다. 예: `recording.webm`, `recording.m4a`.
- 헤더 검사는 오디오 전체 디코딩을 대체하지 않는다. 손상된 파일·오디오 트랙 없는 파일은 공급자가 거절할 수 있다.
- Groq에 일반 파일명 `recording.<확장자>`로 보내며 사용자 원래 파일명을 전송하지 않는다.
- 한국어 `ko`, `verbose_json`으로 요청하고 공급자 응답의 `duration`으로 최종 길이 검증.
- **60초 초과 여부는 외부 변환 후 확인**한다. 프론트는 60초에 녹음을 중지해야 한다. 긴 파일도 외부 호출 사용량은 발생할 수 있다.
- 서버는 FFmpeg 등 별도 디코더를 설치하지 않는다. 인증된 사용자만 호출할 수 있다.
- 전사문은 공백 제거 후 1~2000자. 사용자가 내용을 확인·수정한 뒤 `/messages` 또는 `/emotions`에 보낸다.
- 전사 결과를 서버에 저장하거나 자동으로 Upstage에 전달하지 않는다. 무음·오인식의 완전한 감지를 보장하지 않는다.

## 감정 데이터 수명과 삭제 확인

| 데이터 | 보관 위치·수명 |
| --- | --- |
| 계정·캐릭터 설정 | 기존 DB에 지속 저장 |
| 음성·STT 결과·하소연·공감·미선택 AI 후보 | 해당 요청 처리 중 메모리에서만 사용. 세션/DB/파일/본문 로그에 저장하지 않음 |
| 사용자가 확정한 감정·새 상황·욕구·정리·계획 | 서버 메모리의 본인 세션에만 보관. 삭제/만료/정상 종료/재시작 시 소멸 |
| 종료 확인용 ID·소유자·종료시각·처리 중 여부 | 감정 내용 없이 최소 2분 후 정리. 처리 중 요청이 끝날 때까지 남을 수 있음 |

세션은 생성부터 **15분 고정 만료**이며 요청해도 연장되지 않는다. 만료 후 API에서 즉시 접근 차단,
백그라운드 정리는 기본 30초 간격이므로 접속하지 않은 만료 세션의 메모리 참조는 다음 정리 시 제거한다.
사용자당 활성 세션 최대 3개, 서버 전체 최대 256개(종료 확인용 항목 포함).
단일 서버용이다. 여러 서버에서 운영하려면 별도 세션 라우팅/수명 설계가 필요하다.

**감정 확인 경계**: 서버는 원문을 세션에 넣지 않으며, `/emotions/confirm` 이후 같은 세션의 메시지·감정 분석·STT를 차단한다.
프론트도 이 시점에 녹음 Blob, 전사문, 입력 원문, 공감, 미선택 후보를 제거하고 확정한 감정 또는 빈 값만 다음 화면으로 가져간다.
뒤 단계의 상황 정리에 필요한 정보는 사용자가 새로 입력한다.

`DELETE S` 응답:

```json
{
  "serverStateCleared": true,
  "requestsSettled": true,
  "clientCleanupRequired": true,
  "externalDeletionVerified": false
}
```

- 200: 서버 세션의 감정 참조가 제거되고 이 세션에서 실행 중인 AI·STT 작업이 없음.
- 202: 세션 감정 참조는 제거했지만 처리 중인 작업이 남음. 같은 DELETE를 잠시 뒤 다시 호출해 200 확인.
- 원격 작업이 돌아오는 동안에는 입력이 요청 메모리에 남을 수 있다. 종료된 세션의 늦은 결과는 반환하지 않는다.
- 네트워크에서 이미 전달 중인 응답은 회수할 수 없다. 프론트도 종료한 세션 ID의 늦은 응답을 무시하고 모든 표시·메모리·Blob URL을 정리한다.
- 삭제 확인 메타데이터까지 만료되어 404이면 더 이상 상세 확인할 수 없으므로 이를 별도 상태로 표시한다.
- 탭 종료/통신 중단만으로 즉시 삭제를 확약하지 않는다. 가능한 경우 DELETE하고, 실패 시 고정 만료로 정리한다.
- JVM 메모리의 즉시 물리적 덮어쓰기, OS 스왑·힙 덤프, 외부 제공자 삭제까지 완료했다는 의미가 아니다.
- 업로드 임계값을 요청 제한보다 높게 설정해 허용 파일은 서블릿 메모리에서 처리한다. 프록시/APM/인프라의 본문 저장은 별도 점검 대상이다.
- 탈퇴 트랜잭션이 성공하면 해당 계정의 진행 중 세션도 닫는다.

## 오류·호출 제한

| 코드 | HTTP | 처리 |
| --- | --- | --- |
| COMMON_001 | 400 | 필수 필드·입력 길이·형식 확인 |
| COMMON_002 | 401 | 로그인 확인 |
| SESSION_001 | 404 | 없거나 본인 소유가 아닌 세션 |
| SESSION_002 | 410 | 종료·만료된 세션, 홈으로 이동 |
| SESSION_003 | 409 | 단계 불일치 또는 같은 세션의 요청 처리 중, 현재 단계 확인 |
| SESSION_004 | 429 | 세션 개수 제한 |
| AI_001 / STT_004 | 503 | 해당 제공자 키 미설정, 직접 입력 가능 |
| AI_002 / STT_003 | 502 | 연결·공급자 인증·타임아웃·기타 실패 |
| AI_003 | 502 | AI 출력 형식 검증 실패, 직접 입력 가능 |
| AI_004 / STT_005 | 503 | 공급자 요청 제한, 잠시 후 사용자 선택으로 재시도 |
| STT_001 | 400 | 비어 있거나 지원하지 않는 음성/공급자 입력 거절 |
| STT_002 | 400 | 10MiB 또는 60초 초과 |
| STT_006 | 400 | 빈 전사 결과, 다시 녹음 또는 텍스트 입력 |
| CHAT_001 | 400 | 전사문 2000자 초과 |

각 AI·STT 요청은 외부 호출 한 번, 자동 재시도 없음. 세션별 외부 호출은 한 번에 하나만 허용한다.
연결 제한 5초, 응답 읽기 제한 Upstage 30초/Groq 45초. 사용자별 일일 예산·횟수 제한은 아직 없다.
연속 요청·느린 스트림의 전체 처리 시간을 별도로 제한하는 게이트웨이는 구현하지 않았다.
프론트에서 요청 중 버튼 비활성화, 오류 시 무한 재시도 금지. 무료 계정도 사용량 제한이 있으므로 무제한 무료로 안내하지 않는다.

## 외부 제공자 정책 확인

Groq는 일반 추론 데이터의 기본 미보관을 안내하지만, 신뢰성·오용 조사 예외가 있다.
실서비스 전에 조직 관리자에게 Data Controls의 Zero Data Retention 적용과 예외 조건을 확인한다.
우리 DELETE API는 Groq/Upstage의 내부 삭제 API를 호출하지 않으며 `externalDeletionVerified`는 항상 false다.
공급자 보관·학습 정책 및 대회 계정의 별도 조건은 운영 전 확인 사항이다.
이 구현만으로 서비스 전체의 완전 삭제를 보증하지 않는다.

구현에 참고한 공식 문서(2026-11-17 확인):
- [Groq 음성 인식 API](https://console.groq.com/docs/speech-to-text)
- [Groq 데이터 보관 정책](https://console.groq.com/docs/your-data)
- [Upstage Chat API](https://console.upstage.ai/api/chat)

## 검증

```bash
./gradlew test
```

모의 외부 응답으로 API 요청 형식·응답 검증·오류를 테스트한다.
실제 JWT 필터를 사용하는 MVC 테스트로 음성 → 사용자 확정 → 결과 → 삭제 흐름과 접근 제어를 확인한다.
실제 Tomcat 업로드 테스트로 처리 중 임시 파일 미생성 및 파일 크기 초과 거절을 확인한다.
메모리 세션 테스트로 단계 순서, 빈 감정, 수정에 따른 결과 무효화, 만료, 처리 중 삭제를 확인한다.
테스트에서는 실제 제공자 API·사용자 키·크레딧을 사용하지 않는다.
실제 한국어 녹음 정확도·AI 응답 품질·계정의 키 유효성은 키 입력 후 별도로 확인해야 한다.

기존 `/api/v1/vent/emotions`는 이전 연동 호환용으로 남아 있다. 세션 없이 호출되므로
신규 프론트는 반드시 세션 하위 API를 사용하고 이전 단독 API를 섞지 않는다.
