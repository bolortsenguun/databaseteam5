fatenexx — 헬스장 회원·PT 예약 관리 시스템

데이터베이스 수업 팀 프로젝트. 회원은 이용권을 사고 개인·그룹 PT를 예약하며, 트레이너는 근무 일정과 수업을 관리하고, 관리자는 회원·트레이너·상품·통계를 관리한다.

기술 스택
구분	사용 기술
Backend	Java 21, Spring Boot 4.1, Spring Data JPA, Gradle
Frontend	React 18, Vite 5, React Router 6
DB	MySQL 8.0.16 이상 (CHECK 제약·생성 컬럼 사용)
인증	세션 (JSESSIONID 쿠키), 비밀번호 BCrypt
폴더 구조
databaseteam5/
├── backend/    Spring Boot API 서버 (http://localhost:8080)
├── frontend/   React 화면 (http://localhost:5173)
└── db/
    ├── schema.sql      테이블 12개 + 정책값·기본 상품 초기 데이터
    └── test-data.sql   테스트 계정, PT 이용권, 트레이너 근무 일정
실행 방법

DB → 백엔드 → 프론트 순서로 띄운다.

1. DB 만들기
sql
CREATE DATABASE fatenexx DEFAULT CHARACTER SET utf8mb4;
USE fatenexx;
SOURCE db/schema.sql;
SOURCE db/test-data.sql;   -- 테스트 계정이 필요할 때

DB 이름은 예시다. 바꾸면 아래 application-local.yaml의 URL도 같이 바꾼다.

2. 백엔드 실행

application.yaml은 local 프로필을 쓴다. DB 접속 정보는 레포에 올리지 않으므로 각자 backend/src/main/resources/application-local.yaml을 만든다 (.gitignore에 들어 있어서 커밋되지 않는다).

yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fatenexx?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: root
    password: 본인_비밀번호
    driver-class-name: com.mysql.cj.jdbc.Driver
bash
cd backend
./gradlew bootRun          # Windows: gradlew.bat bootRun

ddl-auto: validate라서 테이블을 자동으로 만들지 않는다. 반드시 1번에서 schema.sql을 먼저 실행해야 한다.

3. 프론트 실행
bash
cd frontend
npm install
npm run dev        # http://localhost:5173
npm run dev:mock   # 백엔드 없이 목업 데이터로 화면만 볼 때

개발 서버가 /api 요청을 http://localhost:8080으로 프록시한다. 백엔드 주소가 다르면 frontend/.env의 VITE_BACKEND_URL을 바꾼다. 자세한 내용은 frontend/README.md에 있다.

테스트 계정

db/test-data.sql을 넣었을 때 쓸 수 있다. 비밀번호는 모두 test1234다.

아이디	역할	상태
admin	관리자	
trainer1	트레이너	승인됨, 월~금 10:00~18:00 근무
trainer2	트레이너	승인대기 (승인 화면 테스트용)
member1	회원	개인 PT 10회(trainer1) + 그룹 PT 10회
member2	회원	그룹 PT 10회

키오스크 출석은 /kiosk 화면에서 회원 전화번호로 한다 (예: member1 → 01012345678).

주요 기능
역할	기능
회원	회원가입, 이용권 구매(모의결제), 개인 PT 예약, 그룹 PT 신청, 예약 취소(10분 무료·24시간 규칙), 내 이용권·결제·출석 내역
트레이너	가입 후 관리자 승인, 근무 시간·휴무 관리, 수업 일정, 출석·결석 처리, 그룹 수업 개설·수정·취소, 담당 회원
관리자	대시보드, 매출·이용 통계, 회원 관리, 트레이너 승인·수정, 상품 관리(판매 중지), 결제·예약·출석 조회, 예약 취소, 운영 설정
키오스크	전화번호로 체크인 (회원권 입장 / PT 입장)
스케줄러	매분 실행. 끝난 수업을 결과대기로 바꾸고, 시작 24시간 전에 그룹 수업을 확정/폐강으로 판정
API 규칙
모든 API는 /api/v1로 시작한다.
응답 형식은 { success, data, code, message }로 통일되어 있고, 실패하면 message를 그대로 화면에 보여준다.
401 로그인 필요 · 403 TRAINER_NOT_APPROVED 승인 대기 · 403 PASSWORD_CHANGE_REQUIRED 비밀번호 변경 필요 · 409 업무 규칙 위반(예약 겹침, 정원 초과 등)
상태값은 DB에 저장된 한글 값을 그대로 쓴다 (예: 예약 예약 / 결과대기 / 출석 / 결석 / 취소).
