# CoCal Server

**CoCal (Calendar + Collaboration)** - 마감일까지의 남은 시간을 시각화하고, 팀 전체가 일정을 공유하는 공동 캘린더 서비스의 백엔드 API입니다.

프론트엔드 레포지토리: [CoCal-front](https://github.com/Sojiyeon/CoCal-front)

---

## 미리 보기

### Web
<p>
  <img src="https://github.com/user-attachments/assets/b2fe9bac-a180-42a5-a9bf-c9a4cd988294" width="49%" />
  <img src="https://github.com/user-attachments/assets/06e00825-5f08-4363-966e-dcd007804ea1" width="49%" />
</p>

### Mobile
<p>
  <img src="https://github.com/user-attachments/assets/7558a7e1-83a0-4825-a59b-624ff46f4120" width="24%" />
  <img src="https://github.com/user-attachments/assets/d7e521c8-8989-4ee6-8d99-d72d1523d6fe" width="24%" />
  <img src="https://github.com/user-attachments/assets/b32db065-9f05-4a57-8b71-26f55458e9d9" width="24%" />
  <img src="https://github.com/user-attachments/assets/3d8b1aea-9bea-4ed0-9cc6-02a25ea1cfdc" width="24%" />
</p>

---

## 개요

CoCal은 팀 프로젝트를 진행하는 사람들을 위한 협업 캘린더 플랫폼입니다. 마감일까지의 진행률을 퍼센트로 표시하고, 공동 일정/메모/투두/초대 관리를 통합 제공합니다.

**주요 특징**
- 프로젝트 단위 일정 관리 및 진행률 표시
- 단체 메모 및 투두 공유
- 링크/이메일 기반 초대 시스템
- SSE(Server-Sent Events) 실시간 알림
- JWT + Refresh Token 인증 (기기별 토큰 관리)
- Google OAuth2 소셜 로그인

---

## 개발 기간

2025.09 ~ 2025.10 (약 1.5개월)

---

## 팀 구성

| 역할 | 이름 |
|------|------|
| Backend | [dodo5517](https://github.com/dodo5517), [sungAh123](https://github.com/sungAh123) |
| Frontend | [Sojiyeon](https://github.com/Sojiyeon), [kimmmddh](https://github.com/kimmmddh) |

---

## 데모

[데모 바로가기](https://co-cal.vercel.app/)

---

## 기술 스택

**Backend**
- Java 17
- Spring Boot 3.5
- Spring Security (JWT, OAuth2)
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- AWS S3
- Gradle

**Infra**
- Render (서버 및 DB)
- Vercel (프론트엔드)

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| 인증 | JWT + Refresh Token, Google OAuth2 소셜 로그인 |
| 알림 | SSE 기반 실시간 알림 스트리밍 |
| 일정 | 프로젝트별 일정 CRUD, 남은 시간 퍼센트 표시 |
| 멤버 | 초대, 역할, 상태 관리 |
| 초대 | 이메일/링크 기반 초대 및 수락 처리 |
| 메모 | 날짜별 단체 메모 |
| 투두 | 개인/단체 투두 관리 |
| 프로젝트 | 팀 단위 일정, 메모, 투두 통합 관리 |

---

## 패키지 구조

```
cocal
├── auth                    # 인증, 로그인, 토큰 재발급
├── cal                     # 캘린더
├── common                  # 공통 유틸리티
├── config                  # 설정
├── event                   # 일정
├── eventLink               # 일정 링크
├── eventMember             # 일정 참여자
├── invite                  # 초대
├── memo                    # 메모
├── notification            # 알림
├── project                 # 프로젝트
├── projectMember           # 프로젝트 멤버
├── s3                      # 이미지 업로드
├── todo                    # 투두
└── user                    # 유저
```

---

## API 예시

**로그인**
```http
POST /api/auth/login
```

**일정 생성**
```http
POST /api/projects/{projectId}/events
```

**메모 조회**
```http
GET /api/projects/{projectId}/memos?date=2025-10-13
```

**초대 수락**
```http
POST /api/invites/{token}/accept
```

---

## 실행 방법

**1. Clone**
```bash
git clone https://github.com/dodo5517/CoCal-server.git
cd cocal-server
```

**2. 환경 설정**
```bash
cp .env.example .env
# .env 파일에 DB, JWT, OAuth2, S3 설정 입력
```

**3. Build & Run**
```bash
./gradlew bootRun
```

로컬 실행 시 기본 포트는 8080입니다.
