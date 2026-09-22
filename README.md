
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

`.env`에서 MySQL 접속 정보를 설정하고, `JWT_SECRET`을 추가합니다.

```env
MYSQLHOST=localhost
MYSQLPORT=3306
MYSQLDATABASE=team02
MYSQLUSER=team02_user
MYSQLPASSWORD=your_password
MYSQL_ROOT_PASSWORD=your_root_password
JWT_SECRET=your_jwt_secret
```

#### 2. MySQL 실행

```bash
docker compose up -d
```

#### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

서버 실행 후 API 문서는 [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)에서 확인할 수 있습니다.

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
NEXT_PUBLIC_API_URL=http://localhost:8080
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
