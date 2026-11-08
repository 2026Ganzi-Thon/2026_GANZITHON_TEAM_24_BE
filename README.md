# 못다한 대화 - Backend

Java 21 · Spring Boot 3.5 · Gradle(Groovy) · MySQL 8 · JWT

## 로컬 실행

```bash
# 1. 설정 파일 만들기 (git에 안 올라감)
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml

# 2. MySQL 띄우기
docker compose up -d

# 3. 서버 실행 (기본 프로필: local)
./gradlew bootRun
```

- Swagger: http://localhost:8080/swagger-ui/index.html
- API 명세·ERD: 노션 "명세서" 페이지

## 패키지 구조 (도메인별)

```
com.ganzithon.motdahan
├── global                  공통 모듈
│   ├── response            ApiResponse { success, data, error }
│   ├── exception           ErrorCode(에러 코드 전체), BusinessException, GlobalExceptionHandler
│   ├── security            JWT 발급·검증, 인증 필터, 401 응답
│   ├── config              Security(CORS 포함), Swagger, JPA Auditing
│   ├── entity              BaseTimeEntity (created_at, updated_at)
│   └── logging             요청 로그 (본문은 남기지 않음)
└── domain
    ├── auth                /api/v1/auth
    ├── user                /api/v1/users
    ├── character           /api/v1/characters
    ├── stt                 /api/v1/stt
    ├── vent                /api/v1/vent
    └── reflect             /api/v1/reflect
```

도메인 안은 `controller / service / repository / entity / dto` 로 나눈다.

## 규칙

- 응답은 항상 `ApiResponse`로 감싼다. 실패는 `throw new BusinessException(ErrorCode.XXX)`.
- 새 에러 코드는 `ErrorCode`에 추가하고 노션 에러 코드 표도 같이 고친다.
- 로그인 사용자는 컨트롤러에서 `@AuthenticationPrincipal AuthUser user`로 받는다.
- 사용자가 털어놓은 내용은 DB·로그 어디에도 저장하지 않는다.
- 비밀값(API 키, 비밀번호)은 `.env`, `application-local.yml`에만 둔다. 레포가 public이다.
- 포매터: IntelliJ 기본
