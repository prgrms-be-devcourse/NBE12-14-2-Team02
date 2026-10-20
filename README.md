
# "모임 구축해주겠어"

여러 명이 함께하는 모임에서 흩어지기 쉬운 **일정 조율, 콘텐츠 투표, 지출 및 정산 과정**을 하나의 모임 안에서 관리할 수 있도록 돕는 서비스입니다.


[서비스 바로가기](https://nbe-12-14-2-team02.vercel.app)  
[swagger 문서](https://nbe12-14-2-team02-production.up.railway.app/swagger-ui/index.html#/)  
[ERD](https://www.erdcloud.com/d/MQ5G84GCBHH3ghK2N)  

---

## 📌 프로젝트 소개

모임을 준비할 때는 가능한 날짜를 맞추고, 무엇을 할지 정하고, 모임이 끝난 뒤에는 여러 지출을 다시 정산해야 합니다.

**모임 구축해주겠어**는 이 과정을 하나의 서비스에서 관리합니다.

- 후보별 선호도를 반영한 일정 조율
- 모임원이 함께 참여하는 콘텐츠 결정
- 지출 참여자를 기준으로 한 통합 정산
- 초대 링크를 통한 모임 참여
- 투표 종료 및 정산 완료 알림

---
## 🛠 Tech Stack

### Backend

| | Technology |
| --- | --- |
| Language | Java 25 |
| Framework | Spring Boot 4.1.1 |
| ORM | Spring Data JPA |
| Security | Spring Security, JWT (JJWT 0.13.0) |
| Database | MySQL |
| DB Migration | Flyway |
| API Docs | SpringDoc OpenAPI / Swagger |
| Build | Gradle |

### Frontend

| | Technology |
| --- | --- |
| Framework | Next.js 16 |
| UI | React 19 |
| Language | TypeScript |
| Styling | Tailwind CSS 4 |
| Deployment | Vercel |

### Infra & Collaboration

| | Technology |
| --- | --- |
| Backend Deployment | Railway |
| Database | MySQL |
| Local DB | Docker Compose |

---
## Getting Started
### Backend
#### 1. 환경변수 설정

`backend/.env.example`을 복사해 `backend/.env`를 만듭니다.

```bash
cd backend
cp .env.example .env
```

`.env`에서 MySQL 접속 정보와 `JWT_ACCESS_SECRET`, `JWT_REFRESH_SECRET`을 설정합니다. 두 JWT 시크릿은 서로 다른 랜덤 값으로, 각각 UTF-8 기준 최소 32바이트로 설정합니다.

```env
SPRING_PROFILES_ACTIVE=local
MYSQLHOST=localhost
MYSQLPORT=3306
MYSQLDATABASE=team02
MYSQLUSER=team02_user
MYSQLPASSWORD=your_password
MYSQL_ROOT_PASSWORD=your_root_password
JWT_ACCESS_SECRET=your_jwt_access_secret
JWT_REFRESH_SECRET=your_jwt_refresh_secret
```

#### 2. MySQL 실행

```bash
docker compose up -d
```

#### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

프로필을 지정하지 않으면 기본으로 `local` 프로필이 적용됩니다. 운영 환경에서는 배포 환경변수에 `SPRING_PROFILES_ACTIVE=prod`를 추가합니다.

```env
SPRING_PROFILES_ACTIVE=prod
```

서버 실행 후 API 문서는 [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)에서 확인할 수 있습니다.

#### 4. DB 스키마 관리 (Flyway)

이 프로젝트는 테이블 생성이나 컬럼 추가 같은 DB 구조 변경을 SQL 파일로 관리합니다. Flyway는 애플리케이션이 시작될 때 아직 적용하지 않은 SQL 파일을 버전 순서대로 실행해 DB 구조를 맞춰주는 도구입니다. 로컬과 운영 환경 모두 같은 방식을 사용합니다.

처음 실행할 때는 앞 단계에서 MySQL을 띄우고 애플리케이션을 실행하면 됩니다. 빈 DB에 필요한 테이블은 `V1__init__schema.sql`로 자동 생성됩니다. Flyway는 실행한 파일의 이력을 `flyway_schema_history` 테이블에 남겨 다음 실행부터는 새로 추가된 변경만 적용합니다.

테이블이나 컬럼을 변경할 때는 다음 순서로 작업합니다.

1. 엔티티 변경에 맞춰 `backend/src/main/resources/db/migration`에 SQL 파일을 추가합니다.
2. 파일 이름은 `V버전__설명.sql` 형식으로 작성합니다. 예를 들어 다음 버전이 2라면 `V2__add_user_nickname.sql`로 만들고, 컬럼을 추가하는 SQL을 작성합니다. 버전과 설명 사이에는 밑줄 두 개(`__`)를 사용합니다.
3. 애플리케이션을 실행해 SQL이 적용되고 정상적으로 시작되는지 확인합니다.

이미 적용된 SQL 파일은 수정하지 않고, 새로운 버전의 파일을 추가합니다. JPA는 엔티티와 DB 구조가 일치하는지만 검사하도록 설정되어 있으므로(`ddl-auto: validate`), 엔티티를 수정했다면 필요한 DB 변경 SQL도 함께 작성해야 합니다.

> 기존 DB를 연결하는 경우: 테이블이 있지만 Flyway 이력이 없는 DB는 초기 스키마가 준비된 것으로 간주해 `V1`을 건너뜁니다. 따라서 연결 전에 기존 테이블 구조가 `V1__init__schema.sql`과 일치하는지 확인해야 합니다.

### Frontend

#### 1. 의존성 설치

```bash
cd frontend
npm install
```

#### 2. 환경변수 설정

로컬 프론트 환경변수는 `frontend/.env.local`에 둡니다. 

```env
# frontend/.env.local
BACKEND_URL=http://localhost:8080
```

#### 3. 개발 서버 실행

```bash
npm run dev
```

브라우저에서 [http://localhost:3000](http://localhost:3000)을 엽니다.


---

## Team

**Programmers Backend Devcourse 12기 14회차 · Team 02**

| GitHub |
| --- |
| [@sowon11](https://github.com/sowon11) |
| [@jihoo414-tech](https://github.com/jihoo414-tech) |
| [@jinni1](https://github.com/jinni1) |
| [@jjjonghyeon](https://github.com/jjjonghyeon) |
| [@minj00-kim](https://github.com/minj00-kim) |
