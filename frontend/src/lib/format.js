const ROLE = { MEMBER: 'MEMBER', TRAINER: 'TRAINER', ADMIN: 'ADMIN', '회원': 'MEMBER', '트레이너': 'TRAINER', '관리자': 'ADMIN' }
const ROLE_LABEL = { MEMBER: '회원', TRAINER: '트레이너', ADMIN: '관리자' }
const GENDER = { MALE: 'MALE', FEMALE: 'FEMALE', '남': 'MALE', '여': 'FEMALE' }
const GENDER_LABEL = { MALE: '남', FEMALE: '여' }
const PT_TYPE = { PERSONAL: 'PERSONAL', GROUP: 'GROUP', '개인': 'PERSONAL', '그룹': 'GROUP' }
const PT_TYPE_LABEL = { PERSONAL: '개인', GROUP: '그룹' }

export const roleKey = v => ROLE[v] || null
export const roleLabel = v => ROLE_LABEL[roleKey(v)] || v || '-'
export const genderKey = v => GENDER[v] || null
export const genderLabel = v => GENDER_LABEL[genderKey(v)] || '-'
export const ptTypeKey = v => PT_TYPE[v] || null
export const ptTypeLabel = v => PT_TYPE_LABEL[ptTypeKey(v)] || v || '-'
export const isApproved = v => v === 'APPROVED' || v === '승인'

const pad = n => String(n).padStart(2, '0')
const WEEK = ['일', '월', '화', '수', '목', '금', '토']
export const DOW = ['', '월', '화', '수', '목', '금', '토', '일']

export function toISODate(d) {
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
export function today() {
  return toISODate(new Date())
}
export function parseDate(s) {
  if (!s) return null
  const [y, m, d] = s.slice(0, 10).split('-').map(Number)
  return new Date(y, m - 1, d)
}
export function addDays(s, n) {
  const d = parseDate(s)
  d.setDate(d.getDate() + n)
  return toISODate(d)
}
export function addMonths(s, n) {
  const d = parseDate(s)
  const day = d.getDate()
  d.setDate(1)
  d.setMonth(d.getMonth() + n)
  const last = new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate()
  d.setDate(Math.min(day, last))
  return toISODate(d)
}
export function diffDays(a, b) {
  return Math.round((parseDate(b) - parseDate(a)) / 86400000)
}
export function dowOf(s) {
  const w = parseDate(s).getDay()
  return w === 0 ? 7 : w
}
export function fmtDate(s) {
  if (!s) return '-'
  const [y, m, d] = s.slice(0, 10).split('-')
  return `${y}.${m}.${d}`
}
export function fmtMD(s) {
  if (!s) return '-'
  const d = parseDate(s)
  return `${d.getMonth() + 1}월 ${d.getDate()}일 (${WEEK[d.getDay()]})`
}
export function fmtMDShort(s) {
  const d = parseDate(s)
  return { w: WEEK[d.getDay()], md: `${d.getMonth() + 1}/${d.getDate()}` }
}
export function fmtDateTime(s) {
  if (!s) return '-'
  const date = s.slice(0, 10)
  const time = s.slice(11, 16)
  return `${fmtMD(date)} ${time}`
}
export function fmtTime(s) {
  if (!s) return '-'
  return s.length > 5 ? s.slice(11, 16) || s.slice(0, 5) : s
}
export function fmtMoney(n) {
  if (n === null || n === undefined) return '-'
  return Number(n).toLocaleString('ko-KR') + '원'
}
export function fmtPhone(p) {
  if (!p) return '-'
  const s = String(p).replace(/\D/g, '')
  if (s.length === 11) return `${s.slice(0, 3)}-${s.slice(3, 7)}-${s.slice(7)}`
  if (s.length === 10) return `${s.slice(0, 3)}-${s.slice(3, 6)}-${s.slice(6)}`
  return p
}
export function onlyDigits(s) {
  return String(s || '').replace(/\D/g, '')
}
export function nowLocalISO() {
  const d = new Date()
  return `${toISODate(d)}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}
export function hourStr(h) {
  return `${pad(h)}:00`
}

const STATUS_TONE = {
  '예약': 'info', '결과대기': 'warn', '출석': 'ok', '결석': 'bad', '취소': '',
  '개설': 'info', '확정': 'ok', '폐강': 'bad',
  '이용중': 'ok', '이용예정': 'info', '만료': '',
  '성공': 'ok', '실패': 'bad',
  '승인': 'ok', '승인대기': 'warn', 'APPROVED': 'ok', 'PENDING': 'warn',
  '판매 중': 'ok', '판매 중지': ''
}
export const toneOf = s => STATUS_TONE[s] ?? ''

const STATUS_TEXT = { '개설': '모집', '이용중': '이용 중', '이용예정': '이용 예정', '결과대기': '결과 대기', '승인대기': '승인 대기', 'PENDING': '승인 대기', 'APPROVED': '승인' }
export const statusText = s => STATUS_TEXT[s] || s

export const REASON_TEXT = {
  MY_RESERVATION: '내 예약',
  GROUP_CLASS: '그룹 수업',
  SLOT_UNAVAILABLE: '마감',
  BOOKING_CLOSED: '예약 마감',
  DAILY_LIMIT_REACHED: '하루 1회',
  REBOOK_NOT_ALLOWED: '재예약 불가',
  NO_VALID_PERSONAL_PASS: '이용권 없음',
  NO_VALID_GROUP_PASS: '이용권 없음',
  NO_AVAILABLE_COUNT: '횟수 부족',
  ALREADY_RESERVED: '신청 완료',
  CLASS_FULL: '정원 마감',
  DAY_OFF: '휴무',
  OUTSIDE_OPENING_RANGE: '개설 불가 기간',
  GROUP_CLASS_CONFLICT: '그룹 수업 있음',
  PERSONAL_PT_CONFLICT: '개인 PT 있음',
  OUTSIDE_WORK_HOURS: '근무 외'
}
export const reasonText = r => REASON_TEXT[r] || r || ''

export const REASON_LONG = {
  MY_RESERVATION: '이미 예약한 시간이에요.',
  GROUP_CLASS: '그룹 수업이 잡혀 있는 시간이에요.',
  SLOT_UNAVAILABLE: '다른 예약이 있는 시간이에요.',
  BOOKING_CLOSED: '수업 시작 1시간 전까지만 예약할 수 있어요.',
  DAILY_LIMIT_REACHED: 'PT는 하루에 한 번만 예약할 수 있어요.',
  REBOOK_NOT_ALLOWED: '차감 취소한 수업은 다시 예약할 수 없어요.',
  NO_VALID_PERSONAL_PASS: '이 트레이너의 개인 PT 이용권이 없어요.',
  NO_VALID_GROUP_PASS: '유효한 그룹 PT 이용권이 없어요.',
  NO_AVAILABLE_COUNT: '예약 가능한 횟수가 부족해요.',
  ALREADY_RESERVED: '이미 신청한 수업이에요.',
  CLASS_FULL: '정원이 다 찼어요.'
}
