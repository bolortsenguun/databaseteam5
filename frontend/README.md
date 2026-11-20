# fatenexx 프론트엔드

React 18 + Vite 5 + React Router 6. 디자인은 `fatenexx 시안` HTML을 기준으로 옮겼다.

## 실행

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173, /api 요청은 http://localhost:8080 백엔드로 프록시
npm run dev:mock   # 백엔드 없이 전부 목업으로 실행
npm run build      # dist/ 생성
```

- 백엔드 주소가 다르면 `.env`의 `VITE_BACKEND_URL`을 바꾼다.
- 개발 서버가 `/api`를 프록시하므로 브라우저 입장에서는 같은 출처다. 쿠키(JSESSIONID)와 CORS 문제가 생기지 않는다.

## 목업 모드 (`VITE_MOCK`)

| 값 | 동작 |
| --- | --- |
| `missing` (기본, `.env`) | 백엔드에 아직 없는 API만 목업, 나머지는 실제 백엔드 |
| `all` (`.env.mock`) | 모든 API를 브라우저 안의 가짜 백엔드로 처리 |
| `off` | 목업 없이 전부 실제 백엔드 |

`missing`에서 목업으로 처리하는 API는 `src/api/mock.js`의 `MISSING` 목록이다.

- `/payments/*`, `/admin/payments`
- `/passes/me`, `/trainers/me/clients`
- `/kiosk/checkins`, `/attendances/me`, `/admin/attendances`
- `/admin/dashboard`, `/admin/statistics`, `/admin/settings`

B 파트 API가 완성되면 해당 줄을 지우고, 전부 끝나면 `.env`를 `VITE_MOCK=off`로 바꾼다.

`missing` 모드의 목업 데이터는 `db/test-data.sql`과 같은 ID(member1=1, trainer1=2, admin=3)를 쓴다. 다만 목업 결제로 산 이용권은 실제 백엔드의 예약 API에는 보이지 않는다.

목업 계정(`all` 모드)은 member1, member2, trainer1, trainer2(승인 대기), admin이고 비밀번호는 모두 `test1234`다. 데이터는 탭을 닫을 때까지 유지되고, 로그인 화면의 '목업 데이터 초기화'로 처음 상태로 되돌린다.

## 구조

```
src/
  api/        client.js(fetch, 오류 처리) · endpoints.js(API 58개) · mock.js · mockData.js
  lib/        auth.jsx(로그인 상태, 화면 이동) · format.js(날짜·전화번호·라벨) · hooks.js
  components/ ui.jsx · Layout.jsx · inputs.jsx · ProfileEditor.jsx · ScheduleManager.jsx
  pages/      auth/ · member/ · trainer/ · admin/
```

## 공통 규칙 반영

- 모든 요청에 `credentials: 'include'`를 붙이고 응답의 `success / data / code / message`로 처리한다.
- `401` → 로그인, `403 TRAINER_NOT_APPROVED` → 승인 대기, `403 PASSWORD_CHANGE_REQUIRED` → 비밀번호 변경 화면으로 이동한다.
- 전화번호는 하이픈 없이 보내고 화면에서만 `010-1234-5678`로 보여 준다.
- 역할·성별·PT 구분은 백엔드가 `MEMBER`/`회원`, `MALE`/`남`, `PERSONAL`/`개인` 중 어느 쪽으로 보내도 표시된다 (`lib/format.js`).

## 화면 경로

| 역할 | 경로 |
| --- | --- |
| 공통 | `/login` `/signup` `/signup/member` `/signup/trainer` `/pending` `/password-change` `/kiosk` |
| 회원 | `/member` `/member/book` `/member/reservations` `/member/passes` `/member/shop` `/member/trainers` `/member/history` `/member/profile` |
| 트레이너 | `/trainer` `/trainer/groups` `/trainer/clients` `/trainer/schedule` `/trainer/profile` |
| 관리자 | `/admin` `/admin/stats` `/admin/members` `/admin/members/:id` `/admin/trainers` `/admin/products` `/admin/payments` `/admin/reservations` `/admin/attendance` `/admin/settings` |
