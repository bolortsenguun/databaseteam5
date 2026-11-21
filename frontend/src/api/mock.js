import { seed } from './mockData'

const MODE = (import.meta.env.VITE_MOCK || 'missing').toLowerCase()

// 백엔드에 아직 없는 API만 목업으로 처리하는 목록 (지금은 전부 구현되어 비어 있음)
const MISSING = []

export const mockMode = MODE

export function isMocked(method, path) {
  if (MODE === 'all') return true
  if (MODE === 'off') return false
  return MISSING.some(re => re.test(path))
}

const DB_KEY = 'mockDb:v1'
function loadDb() {
  try {
    const raw = window.sessionStorage.getItem(DB_KEY)
    if (raw) return JSON.parse(raw)
  } catch {}
  return seed()
}
function saveDb() {
  try { window.sessionStorage.setItem(DB_KEY, JSON.stringify(db)) } catch {}
}
export function resetMockDb() {
  db = seed()
  saveDb()
}

let db = loadDb()
let externalUid = null

export function setMockUser(uid) {
  externalUid = uid
}

const store = {
  get() {
    try { return Number(window.sessionStorage.getItem('mockUid')) || null } catch { return null }
  },
  set(v) {
    try {
      if (v) window.sessionStorage.setItem('mockUid', String(v))
      else window.sessionStorage.removeItem('mockUid')
    } catch {}
  }
}

const pad = n => String(n).padStart(2, '0')
const isoD = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
const isoDT = d => `${isoD(d)}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
const now = () => new Date()
const todayS = () => isoD(now())
const dt = (date, time) => {
  const [y, m, d] = date.split('-').map(Number)
  const [h, mi] = time.split(':').map(Number)
  return new Date(y, m - 1, d, h, mi)
}
const pd = s => {
  const [y, m, d] = s.slice(0, 10).split('-').map(Number)
  return new Date(y, m - 1, d)
}
const addD = (s, n) => {
  const d = pd(s)
  d.setDate(d.getDate() + n)
  return isoD(d)
}
const endByMonths = (s, m) => {
  const d = pd(s)
  const x = new Date(d.getFullYear(), d.getMonth() + m, d.getDate())
  x.setDate(x.getDate() - 1)
  return isoD(x)
}
const dow = s => {
  const w = pd(s).getDay()
  return w === 0 ? 7 : w
}
const hh = h => `${pad(h)}:00`
const toH = t => Number(t.split(':')[0])
const nextId = arr => arr.reduce((m, x) => Math.max(m, x.id), 0) + 1
const setting = k => db.settings.find(s => s.settingKey === k)?.settingValue
const userById = id => db.users.find(u => u.id === id)
const ROLE_LABEL = { MEMBER: '회원', TRAINER: '트레이너', ADMIN: '관리자' }
const GENDER_LABEL = { MALE: '남', FEMALE: '여' }

class MockError {
  constructor(status, code, message) {
    this.status = status
    this.code = code
    this.message = message
  }
}
const fail = (status, code, message) => { throw new MockError(status, code, message) }
const ok = (data, message = '요청이 완료되었습니다.', status = 200) => ({ status, data, message })

function page(list, pageNo, size, base = 1) {
  const p = Number(pageNo ?? base)
  const s = Number(size ?? 20)
  if (p < base || s < 1) fail(400, 'INVALID_INPUT', '올바른 페이지 정보를 입력해 주세요.')
  const start = (p - base) * s
  return { items: list.slice(start, start + s), page: p, size: s, total: list.length, totalPages: Math.max(1, Math.ceil(list.length / s)) }
}

function tick() {
  const n = now()
  const min = setting('GROUP_MIN_PARTICIPANTS')
  const confirmH = setting('GROUP_CONFIRM_HOURS')
  for (const g of db.groupClasses) {
    if (g.status !== '개설') continue
    const start = dt(g.classDate, g.startTime)
    if (start - n <= confirmH * 3600000) {
      const parts = db.reservations.filter(r => r.groupClassId === g.id && r.status !== '취소')
      if (parts.length >= min) g.status = '확정'
      else {
        g.status = '폐강'
        g.canceledAt = isoDT(n)
        parts.forEach(r => Object.assign(r, { status: '취소', canceledBy: '폐강', canceledAt: isoDT(n) }))
      }
    }
  }
  for (const r of db.reservations) {
    if (r.status === '예약' && dt(r.lessonDate, r.endTime) <= n) r.status = '결과대기'
  }
}

const userResponse = u => ({
  userId: u.id, loginId: u.loginId, name: u.name, phone: u.phone, birthDate: u.birthDate, gender: u.gender,
  role: u.role, approvalStatus: u.approval, career: u.career, introduction: u.introduction, createdAt: u.createdAt
})
const authUser = u => ({ userId: u.id, loginId: u.loginId, name: u.name, role: u.role, approvalStatus: u.approval, mustChangePassword: u.mustChange })
const workTimes = tid => db.schedules.filter(s => s.trainerId === tid).sort((a, b) => a.dayOfWeek - b.dayOfWeek || a.startTime.localeCompare(b.startTime)).map(s => ({ dayOfWeek: s.dayOfWeek, startTime: s.startTime, endTime: s.endTime }))

function passStatus(p) {
  const t = todayS()
  if (t < p.startDate) return '이용예정'
  if (t > p.endDate) return '만료'
  return '이용중'
}
function reservedCount(passId) {
  return db.reservations.filter(r => r.ptPassId === passId && (r.status === '예약' || r.status === '결과대기')).length
}
function passView(p) {
  const status = passStatus(p)
  const reserved = reservedCount(p.id)
  const prod = db.ptProducts.find(x => x.id === p.productId)
  return {
    ptPassId: p.id, productId: p.productId, productName: prod?.name, ptType: p.ptType, trainerId: p.trainerId,
    trainerName: p.trainerId ? userById(p.trainerId)?.name : null, startDate: p.startDate, endDate: p.endDate, status,
    totalCount: p.totalCount, remainingCount: p.remainingCount, reservedCount: reserved,
    availableCount: status === '만료' ? 0 : Math.max(0, p.remainingCount - reserved), usedCount: p.totalCount - p.remainingCount
  }
}
function usablePass(uid, type, date, trainerId) {
  const list = db.ptPasses.filter(p => p.userId === uid && p.ptType === type && p.startDate <= date && date <= p.endDate && (type === '그룹' || p.trainerId === trainerId))
  if (!list.length) return { reason: type === '그룹' ? 'NO_VALID_GROUP_PASS' : 'NO_VALID_PERSONAL_PASS' }
  const withCount = list.sort((a, b) => a.endDate.localeCompare(b.endDate)).find(p => p.remainingCount - reservedCount(p.id) > 0)
  if (!withCount) return { reason: 'NO_AVAILABLE_COUNT' }
  return { pass: withCount, reason: null }
}

function individualReason(uid, trainerId, date, start) {
  const end = hh(toH(start) + 1)
  const mineSameSlot = db.reservations.some(r => r.userId === uid && r.status !== '취소' && !r.groupClassId && r.trainerId === trainerId && r.lessonDate === date && r.startTime === start)
  if (mineSameSlot) return 'MY_RESERVATION'
  const g = db.groupClasses.some(c => c.trainerId === trainerId && c.classDate === date && ['개설', '확정'].includes(c.status) && c.startTime < end && c.endTime > start)
  if (g) return 'GROUP_CLASS'
  const taken = db.reservations.some(r => r.trainerId === trainerId && r.status !== '취소' && !r.groupClassId && r.lessonDate === date && r.startTime < end && r.endTime > start)
  if (taken) return 'SLOT_UNAVAILABLE'
  if (dt(date, start) - now() < setting('BOOKING_CLOSE_HOURS') * 3600000) return 'BOOKING_CLOSED'
  if (db.reservations.some(r => r.userId === uid && r.lessonDate === date && r.status !== '취소')) return 'DAILY_LIMIT_REACHED'
  if (db.reservations.some(r => r.userId === uid && !r.groupClassId && r.trainerId === trainerId && r.lessonDate === date && r.startTime === start && r.status === '취소' && r.isDeducted)) return 'REBOOK_NOT_ALLOWED'
  return usablePass(uid, '개인', date, trainerId).reason
}

function groupReason(uid, g) {
  const parts = db.reservations.filter(r => r.groupClassId === g.id && r.status !== '취소')
  if (parts.some(r => r.userId === uid)) return 'ALREADY_RESERVED'
  if (parts.length >= g.capacity) return 'CLASS_FULL'
  if (dt(g.classDate, g.startTime) - now() < setting('BOOKING_CLOSE_HOURS') * 3600000) return 'BOOKING_CLOSED'
  if (db.reservations.some(r => r.userId === uid && r.lessonDate === g.classDate && r.status !== '취소')) return 'DAILY_LIMIT_REACHED'
  if (db.reservations.some(r => r.userId === uid && r.groupClassId === g.id && r.status === '취소' && r.isDeducted)) return 'REBOOK_NOT_ALLOWED'
  return usablePass(uid, '그룹', g.classDate).reason
}

const REASON_ERR = {
  MY_RESERVATION: 'SLOT_UNAVAILABLE', GROUP_CLASS: 'SLOT_UNAVAILABLE', SLOT_UNAVAILABLE: 'SLOT_UNAVAILABLE',
  BOOKING_CLOSED: 'BOOKING_CLOSED', DAILY_LIMIT_REACHED: 'DAILY_LIMIT_REACHED', REBOOK_NOT_ALLOWED: 'REBOOK_NOT_ALLOWED',
  NO_VALID_PERSONAL_PASS: 'NO_VALID_PERSONAL_PASS', NO_VALID_GROUP_PASS: 'NO_VALID_GROUP_PASS', NO_AVAILABLE_COUNT: 'NO_AVAILABLE_COUNT',
  ALREADY_RESERVED: 'ALREADY_RESERVED', CLASS_FULL: 'CLASS_FULL'
}
const ERR_MSG = {
  SLOT_UNAVAILABLE: '선택한 시간에는 예약할 수 없습니다.', BOOKING_CLOSED: '수업 시작 1시간 전까지만 예약할 수 있습니다.',
  DAILY_LIMIT_REACHED: '하루에 한 번만 PT를 예약할 수 있습니다.', REBOOK_NOT_ALLOWED: '이미 차감 취소한 수업은 다시 예약할 수 없습니다.',
  NO_VALID_PERSONAL_PASS: '해당 트레이너의 유효한 개인 PT 이용권이 없습니다.', NO_VALID_GROUP_PASS: '유효한 그룹 PT 이용권이 없습니다.',
  NO_AVAILABLE_COUNT: '예약 가능한 PT 횟수가 부족합니다.', ALREADY_RESERVED: '이미 신청한 그룹 수업입니다.', CLASS_FULL: '그룹 수업 정원이 마감되었습니다.'
}
const failReason = reason => {
  const code = REASON_ERR[reason] || 'SLOT_UNAVAILABLE'
  fail(409, code, ERR_MSG[code])
}

function resView(r) {
  const g = r.groupClassId ? db.groupClasses.find(c => c.id === r.groupClassId) : null
  return {
    reservationId: r.id, ptPassId: r.ptPassId, ptType: g ? '그룹' : '개인', groupClassId: g?.id ?? null, className: g?.name ?? null,
    trainerId: r.trainerId, trainerName: userById(r.trainerId)?.name, lessonDate: r.lessonDate, startTime: r.startTime, endTime: r.endTime,
    status: r.status, isDeducted: r.isDeducted, reservedAt: r.reservedAt, canceledBy: r.canceledBy, cancelReason: r.cancelReason, canceledAt: r.canceledAt
  }
}

function previewOf(r) {
  if (r.status !== '예약' || dt(r.lessonDate, r.startTime) <= now()) {
    return { reservationId: r.id, canCancel: false, willDeduct: false, deductionCount: 0, message: '이미 취소되었거나 처리된 예약은 취소할 수 없습니다.' }
  }
  const sinceReserve = (now() - new Date(r.reservedAt)) / 60000
  const untilStart = (dt(r.lessonDate, r.startTime) - now()) / 3600000
  const free = sinceReserve <= setting('FREE_CANCEL_MINUTES') || untilStart >= setting('CANCEL_DEADLINE_HOURS')
  return free
    ? { reservationId: r.id, canCancel: true, willDeduct: false, deductionCount: 0, message: '지금 취소하면 횟수가 차감되지 않습니다.' }
    : { reservationId: r.id, canCancel: true, willDeduct: true, deductionCount: 1, message: `수업 시작 ${setting('CANCEL_DEADLINE_HOURS')}시간 이내라 취소하면 PT 1회가 차감됩니다.` }
}

function trainerSlotsForGroup(tid, date) {
  const sch = db.schedules.filter(s => s.trainerId === tid && s.dayOfWeek === dow(date))
  const off = db.daysOff.some(d => d.trainerId === tid && d.offDate === date)
  const slots = []
  for (const s of sch) {
    for (let h = toH(s.startTime); h < toH(s.endTime); h++) {
      const start = hh(h)
      const end = hh(h + 1)
      const st = dt(date, start)
      let reason = null
      if (off) reason = 'DAY_OFF'
      else if (st - now() < setting('GROUP_OPEN_MIN_HOURS') * 3600000 || pd(date) - pd(todayS()) > setting('GROUP_OPEN_MAX_DAYS') * 86400000) reason = 'OUTSIDE_OPENING_RANGE'
      else if (db.groupClasses.some(g => g.trainerId === tid && g.classDate === date && ['개설', '확정'].includes(g.status) && g.startTime < end && g.endTime > start)) reason = 'GROUP_CLASS_CONFLICT'
      else if (db.reservations.some(r => r.trainerId === tid && !r.groupClassId && r.status !== '취소' && r.lessonDate === date && r.startTime < end && r.endTime > start)) reason = 'PERSONAL_PT_CONFLICT'
      slots.push({ startTime: start, endTime: end, available: !reason, unavailableReason: reason })
    }
  }
  return slots.sort((a, b) => a.startTime.localeCompare(b.startTime))
}

function membershipStatus(m) {
  const t = todayS()
  if (t < m.startDate) return '이용예정'
  if (t > m.endDate) return '만료'
  return '이용중'
}

function paymentProduct(p) {
  const m = db.memberships.find(x => x.paymentId === p.id)
  if (m) return { productType: '회원권', productName: db.membershipProducts.find(x => x.id === m.productId)?.name }
  const pt = db.ptPasses.find(x => x.paymentId === p.id)
  if (pt) return { productType: pt.ptType === '개인' ? '개인 PT' : '그룹 PT', productName: db.ptProducts.find(x => x.id === pt.productId)?.name }
  return { productType: null, productName: null }
}

function membershipDefaultStart(uid) {
  const t = todayS()
  const ends = db.memberships.filter(m => m.userId === uid && m.endDate >= t).map(m => m.endDate).sort()
  return ends.length ? addD(ends[ends.length - 1], 1) : t
}

function inRange(dateStr, start, end) {
  const d = dateStr.slice(0, 10)
  if (start && d < start) return false
  if (end && d > end) return false
  return true
}

function trainerOfPath(params, ctx) {
  const tid = Number(params[0])
  const t = userById(tid)
  if (!t || t.role !== 'TRAINER') fail(404, 'TRAINER_NOT_FOUND', '해당 트레이너를 찾을 수 없습니다.')
  if (ctx.user?.role === 'TRAINER' && ctx.user.id !== tid) fail(403, 'FORBIDDEN', '접근 권한이 없습니다.')
  return t
}

const PUBLIC = ['/auth/login', '/auth/register', '/auth/check-login-id', '/kiosk/checkins']

// 그룹 수업 운동 종류·사진 검사 (백엔드 GroupClassService와 같은 규칙)
const MOCK_WORKOUTS = ['서킷', '하체', '상체', '코어', '전신', '스트레칭', '러닝', '케틀벨', '데드리프트', '힙·브릿지']
function checkVisual(b) {
  if (b.workout && !MOCK_WORKOUTS.includes(b.workout)) fail(400, 'INVALID_INPUT', `운동 종류는 ${MOCK_WORKOUTS.join(', ')} 중에서 선택해 주세요.`)
  if (b.imageUrl && !b.imageUrl.startsWith('data:image/')) fail(400, 'INVALID_INPUT', '수업 사진은 이미지 파일만 올릴 수 있습니다.')
  if (b.imageUrl && b.imageUrl.length > 1500000) fail(400, 'INVALID_INPUT', '수업 사진 용량이 너무 큽니다.')
}

// 키오스크 입장 완료 화면용 정보 (백엔드 AttendanceService.buildXxx와 같은 규칙)
function kioskExtras(uid, reservationId) {
  const t = todayS()
  const day = s => Math.round((pd(s) - pd(t)) / 86400000)
  const m = db.memberships.filter(x => x.userId === uid && x.startDate <= t && t <= x.endDate).sort((a, c) => a.endDate.localeCompare(c.endDate))[0]
  const membership = m ? { name: db.membershipProducts.find(x => x.id === m.productId)?.name, daysLeft: day(m.endDate), totalDays: Math.round((pd(m.endDate) - pd(m.startDate)) / 86400000) + 1, endDate: m.endDate } : null
  const r = reservationId ? db.reservations.find(x => x.id === reservationId) : null
  const pass = r ? db.ptPasses.find(x => x.id === r.ptPassId) : db.ptPasses.filter(x => x.userId === uid && x.startDate <= t && t <= x.endDate).sort((a, c) => a.endDate.localeCompare(c.endDate))[0]
  const pv = pass ? passView(pass) : null
  const pt = pv ? { name: pv.productName, remaining: pv.remainingCount, reserved: pv.reservedCount, available: pv.availableCount, total: pv.totalCount } : null
  const month = t.slice(0, 7)
  const monthAttendance = new Set(db.attendances.filter(a => a.userId === uid && a.checkedInAt.startsWith(month)).map(a => a.checkedInAt.slice(0, 10))).size
  const n = now()
  const nx = db.reservations.filter(x => x.userId === uid && x.status === '예약' && dt(x.lessonDate, x.startTime) >= n).sort((a, c) => (a.lessonDate + a.startTime).localeCompare(c.lessonDate + c.startTime))[0]
  const W = ['일', '월', '화', '수', '목', '금', '토']
  const nextLesson = nx ? {
    date: `${Number(nx.lessonDate.slice(5, 7))}월 ${Number(nx.lessonDate.slice(8, 10))}일 (${W[pd(nx.lessonDate).getDay()]})`,
    time: nx.startTime,
    title: nx.groupClassId ? db.groupClasses.find(g => g.id === nx.groupClassId)?.name : '개인 PT',
    trainer: userById(nx.trainerId)?.name
  } : null
  return { membership, pt, monthAttendance, nextLesson }
}

// 회원권 + PT 이용권 (내 이용권, 관리자 회원 상세 공용)
function passesOf(uid) {
  const t = todayS()
  const memberships = db.memberships.filter(m => m.userId === uid).sort((a, c) => a.startDate.localeCompare(c.startDate) || a.id - c.id).map(m => {
    const status = membershipStatus(m)
    const product = db.membershipProducts.find(x => x.id === m.productId)
    return { membershipId: m.id, productId: m.productId, productName: product?.name, startDate: m.startDate, endDate: m.endDate, status, price: m.price ?? product?.price ?? null, remainingDays: status === '이용중' ? Math.round((pd(m.endDate) - pd(t)) / 86400000) : 0 }
  })
  const ptPasses = db.ptPasses.filter(x => x.userId === uid).sort((a, c) => a.startDate.localeCompare(c.startDate) || a.id - c.id).map(passView)
  return { memberships, ptPasses }
}

const routes = [
  ['POST', /^\/auth\/login$/, null, (p, q, b) => {
    const u = db.users.find(x => x.loginId === b.loginId)
    if (!u || u.password !== b.password) fail(401, 'INVALID_CREDENTIALS', '아이디 또는 비밀번호가 올바르지 않습니다.')
    store.set(u.id)
    return ok(authUser(u), '로그인되었습니다.')
  }],
  ['POST', /^\/auth\/logout$/, null, () => {
    store.set(null)
    return ok(null, '로그아웃되었습니다.')
  }],
  ['GET', /^\/auth\/me$/, null, (p, q, b, ctx) => ok(authUser(ctx.user), '로그인 상태입니다.')],
  ['GET', /^\/auth\/check-login-id$/, null, (p, q) => {
    if (!/^[A-Za-z0-9_]{4,20}$/.test(q.loginId || '')) fail(400, 'INVALID_INPUT', '아이디는 영문, 숫자, 밑줄 4~20자로 입력해 주세요.')
    return ok({ loginId: q.loginId, available: !db.users.some(u => u.loginId === q.loginId) }, '아이디 확인이 완료되었습니다.')
  }],
  ['POST', /^\/auth\/register$/, null, (p, q, b) => {
    if (!/^[A-Za-z0-9_]{4,20}$/.test(b.loginId || '')) fail(400, 'INVALID_INPUT', '아이디는 영문, 숫자, 밑줄 4~20자입니다.')
    if (!b.password || b.password.length < 8 || b.password.length > 50) fail(400, 'INVALID_INPUT', '비밀번호는 8~50자입니다.')
    if (!b.name) fail(400, 'INVALID_INPUT', '입력값을 확인해 주세요.')
    if (!/^[0-9]{10,11}$/.test(b.phone || '')) fail(400, 'INVALID_INPUT', '전화번호는 숫자만 10~11자리입니다.')
    if (!['MEMBER', 'TRAINER'].includes(b.role)) fail(400, 'INVALID_INPUT', '입력값을 확인해 주세요.')
    if (db.users.some(u => u.loginId === b.loginId)) fail(409, 'DUPLICATE_LOGIN_ID', '이미 사용 중인 아이디입니다.')
    if (db.users.some(u => u.phone === b.phone)) fail(409, 'DUPLICATE_PHONE', '이미 등록된 전화번호입니다.')
    const u = { id: nextId(db.users), loginId: b.loginId, password: b.password, role: b.role, approval: b.role === 'TRAINER' ? 'PENDING' : 'APPROVED', name: b.name, phone: b.phone, birthDate: b.birthDate || null, gender: b.gender || null, career: null, introduction: null, mustChange: false, createdAt: isoDT(now()) }
    db.users.push(u)
    return ok(authUser(u), '가입이 완료되었습니다.', 201)
  }],
  ['PATCH', /^\/auth\/password$/, null, (p, q, b, ctx) => {
    if (ctx.user.password !== b.currentPassword) fail(400, 'INCORRECT_PASSWORD', '현재 비밀번호가 올바르지 않습니다.')
    if (!b.newPassword || b.newPassword.length < 8) fail(400, 'INVALID_INPUT', '비밀번호는 8~50자입니다.')
    ctx.user.password = b.newPassword
    ctx.user.mustChange = false
    return ok(null, '비밀번호가 변경되었습니다.')
  }],
  ['POST', /^\/admin\/users\/(\d+)\/reset-password$/, ['ADMIN'], (p, q, b, ctx) => {
    const id = Number(p[0])
    if (id === ctx.user.id) fail(400, 'INVALID_INPUT', '본인 계정은 초기화할 수 없습니다.')
    const u = userById(id)
    if (!u) fail(404, 'USER_NOT_FOUND', '해당 사용자를 찾을 수 없습니다.')
    const temp = 'tmp' + Math.random().toString(36).slice(2, 8) + '9'
    u.password = temp
    u.mustChange = true
    return ok({ userId: id, temporaryPassword: temp }, '비밀번호가 초기화되었습니다.')
  }],

  ['GET', /^\/users\/me$/, null, (p, q, b, ctx) => ok(userResponse(ctx.user), '내 정보를 조회했습니다.')],
  ['PATCH', /^\/users\/me$/, null, (p, q, b, ctx) => ok(userResponse(updateProfile(ctx.user, b)), '내 정보가 수정되었습니다.')],
  ['GET', /^\/admin\/members$/, ['ADMIN'], (p, q) => {
    const kw = (q.keyword || '').trim()
    const list = db.users.filter(u => u.role === 'MEMBER' && (!kw || u.name.includes(kw) || u.loginId.includes(kw) || u.phone.includes(kw.replace(/\D/g, '') || '@@'))).sort((a, b) => b.id - a.id)
    const pg = page(list, q.page ?? 0, q.size, 0)
    return ok({ members: pg.items.map(u => ({ userId: u.id, loginId: u.loginId, name: u.name, phone: u.phone, gender: u.gender, createdAt: u.createdAt })), page: pg.page, size: pg.size, totalCount: pg.total, totalPages: pg.totalPages }, '회원 목록을 조회했습니다.')
  }],
  ['GET', /^\/admin\/members\/(\d+)$/, ['ADMIN'], p => {
    const u = userById(Number(p[0]))
    if (!u || u.role !== 'MEMBER') fail(404, 'USER_NOT_FOUND', '해당 사용자를 찾을 수 없습니다.')
    return ok({ ...userResponse(u), ...passesOf(u.id) }, '회원 정보를 조회했습니다.')
  }],
  ['PATCH', /^\/admin\/members\/(\d+)$/, ['ADMIN'], (p, q, b) => {
    const u = userById(Number(p[0]))
    if (!u || u.role !== 'MEMBER') fail(404, 'USER_NOT_FOUND', '해당 사용자를 찾을 수 없습니다.')
    return ok({ ...userResponse(updateProfile(u, b)), ...passesOf(u.id) }, '회원 정보가 수정되었습니다.')
  }],

  ['GET', /^\/products$/, ['MEMBER'], () => ok({
    membershipProducts: db.membershipProducts.filter(x => x.active).map(x => ({ productId: x.id, name: x.name, months: x.months, price: x.price, active: x.active })),
    ptProducts: db.ptProducts.filter(x => x.active).map(x => ({ productId: x.id, ptType: x.ptType, name: x.name, totalCount: x.totalCount, validWeeks: x.validWeeks, price: x.price, active: x.active }))
  }, '상품 목록을 조회했습니다.')],
  ['GET', /^\/admin\/products$/, ['ADMIN'], () => ok({
    membershipProducts: db.membershipProducts.map(x => ({ productId: x.id, name: x.name, months: x.months, price: x.price, active: x.active })),
    ptProducts: db.ptProducts.map(x => ({ productId: x.id, ptType: x.ptType, name: x.name, totalCount: x.totalCount, validWeeks: x.validWeeks, price: x.price, active: x.active }))
  }, '상품 목록을 조회했습니다.')],
  ['POST', /^\/admin\/membership-products$/, ['ADMIN'], (p, q, b) => {
    if (!b.name || !(b.months > 0) || !(b.price >= 0)) fail(400, 'INVALID_INPUT', '입력값을 확인해 주세요.')
    const x = { id: nextId(db.membershipProducts), name: b.name, months: b.months, price: b.price, active: true }
    db.membershipProducts.push(x)
    return ok({ productId: x.id, name: x.name, months: x.months, price: x.price, active: true }, '회원권 상품이 등록되었습니다.', 201)
  }],
  ['PATCH', /^\/admin\/membership-products\/(\d+)$/, ['ADMIN'], (p, q, b) => {
    const x = db.membershipProducts.find(m => m.id === Number(p[0]))
    if (!x) fail(404, 'PRODUCT_NOT_FOUND', '해당 상품을 찾을 수 없습니다.')
    for (const k of ['name', 'months', 'price', 'active']) if (b[k] !== undefined && b[k] !== null) x[k] = b[k]
    return ok({ productId: x.id, name: x.name, months: x.months, price: x.price, active: x.active }, '회원권 상품이 수정되었습니다.')
  }],
  ['POST', /^\/admin\/pt-products$/, ['ADMIN'], (p, q, b) => {
    if (!['PERSONAL', 'GROUP'].includes(b.ptType) || !b.name || !(b.totalCount > 0) || !(b.validWeeks > 0) || !(b.price >= 0)) fail(400, 'INVALID_INPUT', '입력값을 확인해 주세요.')
    const x = { id: nextId(db.ptProducts), ptType: b.ptType, name: b.name, totalCount: b.totalCount, validWeeks: b.validWeeks, price: b.price, active: true }
    db.ptProducts.push(x)
    return ok({ productId: x.id, ...x, id: undefined }, 'PT 상품이 등록되었습니다.', 201)
  }],
  ['PATCH', /^\/admin\/pt-products\/(\d+)$/, ['ADMIN'], (p, q, b) => {
    const x = db.ptProducts.find(m => m.id === Number(p[0]))
    if (!x) fail(404, 'PRODUCT_NOT_FOUND', '해당 상품을 찾을 수 없습니다.')
    for (const k of ['name', 'totalCount', 'validWeeks', 'price', 'active']) if (b[k] !== undefined && b[k] !== null) x[k] = b[k]
    return ok({ productId: x.id, ptType: x.ptType, name: x.name, totalCount: x.totalCount, validWeeks: x.validWeeks, price: x.price, active: x.active }, 'PT 상품이 수정되었습니다.')
  }],

  ['POST', /^\/payments\/simulate$/, ['MEMBER'], (p, q, b, ctx) => {
    const uid = ctx.uid
    const isM = b.productType === '회원권'
    if (!['회원권', 'PT'].includes(b.productType) || !b.productId || !b.startDate || !['성공', '실패'].includes(b.simulationResult)) fail(400, 'INVALID_INPUT', '결제 정보를 확인해 주세요.')
    const prod = isM ? db.membershipProducts.find(x => x.id === b.productId) : db.ptProducts.find(x => x.id === b.productId)
    if (!prod) fail(404, 'PRODUCT_NOT_FOUND', '해당 상품을 찾을 수 없습니다.')
    if (!prod.active) fail(409, 'PRODUCT_INACTIVE', '현재 판매하지 않는 상품입니다.')
    const base = isM ? membershipDefaultStart(uid) : todayS()
    if (b.startDate < base || b.startDate > addD(base, setting('PASS_START_MAX_DAYS'))) fail(400, 'INVALID_START_DATE', '선택할 수 없는 이용권 시작일입니다.')
    let endDate
    if (isM) {
      endDate = endByMonths(b.startDate, prod.months)
      if (db.memberships.some(m => m.userId === uid && m.startDate <= endDate && b.startDate <= m.endDate)) fail(409, 'MEMBERSHIP_OVERLAP', '기존 회원권과 이용 기간이 겹칩니다.')
    } else {
      endDate = addD(b.startDate, prod.validWeeks * 7 - 1)
      if (prod.ptType === 'PERSONAL') {
        const t = userById(b.trainerId)
        if (!t || t.role !== 'TRAINER' || t.approval !== 'APPROVED') fail(400, 'INVALID_TRAINER', '선택할 수 없는 트레이너입니다.')
      }
    }
    const pay = { id: nextId(db.payments), userId: uid, amount: prod.price, status: b.simulationResult, paidAt: isoDT(now()) }
    db.payments.push(pay)
    if (b.simulationResult === '실패') return ok({ paymentId: pay.id, status: '실패', amount: pay.amount, paidAt: pay.paidAt, pass: null }, '모의결제에 실패했습니다.', 201)
    let pass
    if (isM) {
      const m = { id: nextId(db.memberships), userId: uid, productId: prod.id, paymentId: pay.id, startDate: b.startDate, endDate, price: prod.price }
      db.memberships.push(m)
      pass = { passType: '회원권', passId: m.id, startDate: m.startDate, endDate: m.endDate }
    } else {
      const ptType = prod.ptType === 'PERSONAL' ? '개인' : '그룹'
      const x = { id: nextId(db.ptPasses), userId: uid, ptType, trainerId: ptType === '개인' ? b.trainerId : null, productId: prod.id, paymentId: pay.id, startDate: b.startDate, endDate, totalCount: prod.totalCount, remainingCount: prod.totalCount, price: prod.price }
      db.ptPasses.push(x)
      pass = { passType: 'PT', passId: x.id, ptType, trainerId: x.trainerId, startDate: x.startDate, endDate: x.endDate }
    }
    return ok({ paymentId: pay.id, status: '성공', amount: pay.amount, paidAt: pay.paidAt, pass }, '결제가 완료되었습니다.', 201)
  }],
  ['GET', /^\/payments\/me$/, ['MEMBER'], (p, q, b, ctx) => {
    const list = db.payments.filter(x => x.userId === ctx.uid).sort((a, c) => c.paidAt.localeCompare(a.paidAt) || c.id - a.id)
    const pg = page(list, q.page, q.size)
    return ok({ payments: pg.items.map(x => ({ paymentId: x.id, amount: x.amount, status: x.status, paidAt: x.paidAt, ...paymentProduct(x) })), page: pg.page, size: pg.size, totalCount: pg.total, totalPages: pg.totalPages }, '결제 내역을 조회했습니다.')
  }],
  ['GET', /^\/admin\/payments$/, ['ADMIN'], (p, q) => {
    if (q.startDate && q.endDate && q.startDate > q.endDate) fail(400, 'INVALID_INPUT', '올바른 검색 조건을 입력해 주세요.')
    const list = db.payments.filter(x => (!q.status || q.status === '전체' || x.status === q.status) && (!q.userId || x.userId === Number(q.userId)) && inRange(x.paidAt, q.startDate, q.endDate)).sort((a, c) => c.paidAt.localeCompare(a.paidAt) || c.id - a.id)
    const pg = page(list, q.page, q.size)
    return ok({ payments: pg.items.map(x => ({ paymentId: x.id, userId: x.userId, userName: userById(x.userId)?.name, amount: x.amount, status: x.status, paidAt: x.paidAt, ...paymentProduct(x) })), page: pg.page, size: pg.size, totalCount: pg.total, totalPages: pg.totalPages }, '전체 결제 내역을 조회했습니다.')
  }],

  ['GET', /^\/passes\/me$/, ['MEMBER'], (p, q, b, ctx) => ok(passesOf(ctx.uid), '내 이용권을 조회했습니다.')],
  ['GET', /^\/trainers\/me\/clients$/, ['TRAINER'], (p, q, b, ctx) => {
    const passes = db.ptPasses.filter(x => x.trainerId === ctx.uid && x.ptType === '개인' && passStatus(x) !== '만료')
    const map = new Map()
    for (const x of passes) {
      if (!map.has(x.userId)) {
        const u = userById(x.userId)
        map.set(x.userId, { userId: u.id, name: u.name, phone: u.phone, ptPasses: [] })
      }
      const v = passView(x)
      map.get(x.userId).ptPasses.push({ ptPassId: v.ptPassId, productName: v.productName, startDate: v.startDate, endDate: v.endDate, status: v.status, totalCount: v.totalCount, remainingCount: v.remainingCount, reservedCount: v.reservedCount, availableCount: v.availableCount, usedCount: v.usedCount })
    }
    const clients = [...map.values()]
    return ok({ clients, totalClients: clients.length }, '담당 회원 목록을 조회했습니다.')
  }],

  ['POST', /^\/kiosk\/checkins$/, null, (p, q, b) => {
    if (!/^010[0-9]{8}$/.test(b.phone || '')) fail(400, 'INVALID_INPUT', '전화번호 8자리를 정확히 입력해 주세요.')
    const u = db.users.find(x => x.phone === b.phone && x.role === 'MEMBER')
    if (!u) fail(403, 'ENTRY_NOT_ALLOWED', '현재 입장 가능한 회원권 또는 PT 예약이 없습니다.')
    const n = now()
    const last = db.attendances.filter(a => a.userId === u.id).sort((a, c) => c.checkedInAt.localeCompare(a.checkedInAt))[0]
    if (last && (n - new Date(last.checkedInAt)) / 60000 < setting('RECHECKIN_MINUTES')) {
      return ok({ attendanceId: last.id, name: u.name, entryType: last.entryType, reservationId: last.reservationId, checkedInAt: last.checkedInAt, duplicate: true, ...kioskExtras(u.id, last.reservationId) }, '이미 체크인되었습니다.')
    }
    const t = todayS()
    const hasM = db.memberships.some(m => m.userId === u.id && m.startDate <= t && t <= m.endDate)
    let entryType = '회원권'
    let reservationId = null
    if (!hasM) {
      const before = setting('PT_ENTRY_BEFORE_MINUTES')
      const after = setting('PT_ENTRY_AFTER_MINUTES')
      const todays = db.reservations.filter(r => r.userId === u.id && r.lessonDate === t && r.status !== '취소')
      const okR = todays.find(r => {
        const st = dt(r.lessonDate, r.startTime)
        return n >= st - before * 60000 && n <= st.getTime() + after * 60000
      })
      if (!okR) {
        const future = todays.find(r => dt(r.lessonDate, r.startTime) - before * 60000 > n)
        if (future) {
          const open = new Date(dt(future.lessonDate, future.startTime) - before * 60000)
          fail(403, 'ENTRY_TIME_RESTRICTED', `${pad(open.getHours())}:${pad(open.getMinutes())}부터 입장할 수 있습니다.`)
        }
        fail(403, 'ENTRY_NOT_ALLOWED', '현재 입장 가능한 회원권 또는 PT 예약이 없습니다.')
      }
      entryType = 'PT'
      reservationId = okR.id
    }
    const a = { id: nextId(db.attendances), userId: u.id, checkedInAt: isoDT(n), entryType, reservationId }
    db.attendances.push(a)
    return ok({ attendanceId: a.id, name: u.name, entryType, reservationId, checkedInAt: a.checkedInAt, duplicate: false, ...kioskExtras(u.id, reservationId) }, '체크인되었습니다.', 201)
  }],
  ['GET', /^\/attendances\/me$/, ['MEMBER'], (p, q, b, ctx) => {
    if (q.startDate && q.endDate && q.startDate > q.endDate) fail(400, 'INVALID_INPUT', '출석 이력 조회 조건을 확인해 주세요.')
    const list = db.attendances.filter(a => a.userId === ctx.uid && inRange(a.checkedInAt, q.startDate, q.endDate) && (!q.entryType || q.entryType === '전체' || a.entryType === q.entryType)).sort((a, c) => c.checkedInAt.localeCompare(a.checkedInAt) || c.id - a.id)
    const pg = page(list, q.page, q.size)
    return ok({ attendances: pg.items.map(a => ({ attendanceId: a.id, checkedInAt: a.checkedInAt, entryType: a.entryType, reservationId: a.reservationId })), page: pg.page, size: pg.size, totalCount: pg.total, totalPages: pg.totalPages }, '내 출석 이력을 조회했습니다.')
  }],
  ['GET', /^\/admin\/attendances$/, ['ADMIN'], (p, q) => {
    if (q.startDate && q.endDate && q.startDate > q.endDate) fail(400, 'INVALID_INPUT', '올바른 출석 조회 조건을 입력해 주세요.')
    const list = db.attendances.filter(a => (!q.userId || a.userId === Number(q.userId)) && inRange(a.checkedInAt, q.startDate, q.endDate) && (!q.entryType || q.entryType === '전체' || a.entryType === q.entryType)).sort((a, c) => c.checkedInAt.localeCompare(a.checkedInAt) || c.id - a.id)
    const pg = page(list, q.page, q.size)
    return ok({ attendances: pg.items.map(a => ({ attendanceId: a.id, userId: a.userId, userName: userById(a.userId)?.name, phone: userById(a.userId)?.phone, checkedInAt: a.checkedInAt, entryType: a.entryType, reservationId: a.reservationId })), page: pg.page, size: pg.size, totalCount: pg.total, totalPages: pg.totalPages }, '전체 출석 목록을 조회했습니다.')
  }],

  ['GET', /^\/admin\/dashboard$/, ['ADMIN'], () => {
    const t = todayS()
    const ym = t.slice(0, 7)
    const succ = db.payments.filter(x => x.status === '성공')
    const members = db.users.filter(u => u.role === 'MEMBER')
    const min = setting('GROUP_MIN_PARTICIPANTS')
    return ok({
      todayRevenue: succ.filter(x => x.paidAt.startsWith(t)).reduce((s, x) => s + x.amount, 0),
      monthlyRevenue: succ.filter(x => x.paidAt.startsWith(ym)).reduce((s, x) => s + x.amount, 0),
      totalMembers: members.length,
      activeMembershipMembers: new Set(db.memberships.filter(m => m.startDate <= t && t <= m.endDate).map(m => m.userId)).size,
      approvedTrainers: db.users.filter(u => u.role === 'TRAINER' && u.approval === 'APPROVED').length,
      pendingTrainers: db.users.filter(u => u.role === 'TRAINER' && u.approval === 'PENDING').length,
      todayReservations: db.reservations.filter(r => r.lessonDate === t && r.status !== '취소').length,
      todayCheckins: db.attendances.filter(a => a.checkedInAt.startsWith(t)).length,
      pendingResultCount: db.reservations.filter(r => r.status === '결과대기').length,
      closingSoonClassCount: db.groupClasses.filter(g => g.status === '개설' && db.reservations.filter(r => r.groupClassId === g.id && r.status !== '취소').length < min).length,
      asOf: isoDT(now())
    }, '관리자 대시보드를 조회했습니다.')
  }],
  ['GET', /^\/admin\/statistics$/, ['ADMIN'], (p, q) => {
    const t = todayS()
    const start = q.startDate || `${t.slice(0, 7)}-01`
    const end = q.endDate || endByMonths(`${t.slice(0, 7)}-01`, 1)
    if (start > end) fail(400, 'INVALID_DATE_RANGE', '통계 조회 기간을 확인해 주세요.')
    const months = new Map()
    for (const x of db.payments.filter(x => x.status === '성공' && inRange(x.paidAt, start, end))) {
      const k = x.paidAt.slice(0, 7)
      const v = months.get(k) || { month: k, amount: 0, successCount: 0 }
      v.amount += x.amount
      v.successCount += 1
      months.set(k, v)
    }
    const trainers = db.users.filter(u => u.role === 'TRAINER' && u.approval === 'APPROVED' && (!q.trainerId || u.id === Number(q.trainerId)))
    const trainerPerformance = trainers.map(tr => {
      const rs = db.reservations.filter(r => r.trainerId === tr.id && inRange(r.lessonDate, start, end) && ['출석', '결석'].includes(r.status))
      const ia = rs.filter(r => !r.groupClassId && r.status === '출석').length
      const ib = rs.filter(r => !r.groupClassId && r.status === '결석').length
      const ga = rs.filter(r => r.groupClassId && r.status === '출석').length
      const gb = rs.filter(r => r.groupClassId && r.status === '결석').length
      return { trainerId: tr.id, trainerName: tr.name, individualAttendanceCount: ia, individualAbsenceCount: ib, groupAttendanceCount: ga, groupAbsenceCount: gb, groupClassesHeld: new Set(rs.filter(r => r.groupClassId).map(r => r.groupClassId)).size, groupParticipantsProcessed: ga + gb, totalProcessedCount: ia + ib + ga + gb }
    })
    const hourly = Array.from({ length: 24 }, (_, h) => ({ hour: h, count: db.attendances.filter(a => inRange(a.checkedInAt, start, end) && Number(a.checkedInAt.slice(11, 13)) === h).length }))
    return ok({ period: { startDate: start, endDate: end }, monthlyRevenue: [...months.values()].sort((a, c) => a.month.localeCompare(c.month)), trainerPerformance, hourlyCheckins: hourly }, '관리자 통계를 조회했습니다.')
  }],
  ['GET', /^\/admin\/settings$/, ['ADMIN'], () => ok({ settings: [...db.settings].sort((a, b) => a.settingKey.localeCompare(b.settingKey)) }, '운영 설정을 조회했습니다.')],
  ['PATCH', /^\/admin\/settings$/, ['ADMIN'], (p, q, b) => {
    const list = b?.settings
    if (!Array.isArray(list) || !list.length) fail(400, 'INVALID_INPUT', '운영 설정값을 확인해 주세요.')
    const MIN = { CANCEL_DEADLINE_HOURS: 1, FREE_CANCEL_MINUTES: 0, BOOKING_RANGE_DAYS: 1, BOOKING_CLOSE_HOURS: 0, GROUP_OPEN_MIN_HOURS: 1, GROUP_OPEN_MAX_DAYS: 1, GROUP_MIN_PARTICIPANTS: 2, GROUP_CONFIRM_HOURS: 1, PT_ENTRY_BEFORE_MINUTES: 0, PT_ENTRY_AFTER_MINUTES: 0, PASS_START_MAX_DAYS: 0, RECHECKIN_MINUTES: 1 }
    const keys = new Set()
    const next = Object.fromEntries(db.settings.map(s => [s.settingKey, s.settingValue]))
    for (const s of list) {
      if (!(s.settingKey in MIN)) fail(400, 'INVALID_SETTING_KEY', '변경할 수 없는 설정 항목입니다.')
      if (keys.has(s.settingKey)) fail(400, 'INVALID_INPUT', '운영 설정값을 확인해 주세요.')
      keys.add(s.settingKey)
      if (!Number.isInteger(s.settingValue) || s.settingValue < MIN[s.settingKey]) fail(400, 'INVALID_INPUT', '운영 설정값을 확인해 주세요.')
      next[s.settingKey] = s.settingValue
    }
    if (next.GROUP_MIN_PARTICIPANTS > 4 || next.GROUP_OPEN_MIN_HOURS <= next.GROUP_CONFIRM_HOURS || next.GROUP_OPEN_MAX_DAYS * 24 <= next.GROUP_OPEN_MIN_HOURS) fail(409, 'SETTING_CONFLICT', '설정값 간의 조건이 맞지 않습니다.')
    for (const s of list) db.settings.find(x => x.settingKey === s.settingKey).settingValue = s.settingValue
    return ok({ updatedSettings: list.map(s => ({ settingKey: s.settingKey, settingValue: s.settingValue })) }, '운영 설정이 저장되었습니다.')
  }],

  ['GET', /^\/trainers$/, ['MEMBER'], () => ok({
    trainers: db.users.filter(u => u.role === 'TRAINER' && u.approval === 'APPROVED').sort((a, b) => a.id - b.id).map(u => ({ trainerId: u.id, name: u.name, career: u.career, introduction: u.introduction, schedules: workTimes(u.id) }))
  }, '트레이너 목록을 조회했습니다.')],
  ['GET', /^\/admin\/trainers$/, ['ADMIN'], (p, q) => {
    const f = q.approvalStatus && q.approvalStatus !== '전체' ? (q.approvalStatus === '승인' ? 'APPROVED' : 'PENDING') : null
    const list = db.users.filter(u => u.role === 'TRAINER' && (!f || u.approval === f)).sort((a, b) => (a.approval === 'APPROVED') - (b.approval === 'APPROVED') || (a.approval === 'APPROVED' ? b.createdAt.localeCompare(a.createdAt) : a.createdAt.localeCompare(b.createdAt)))
    const pg = page(list, q.page, q.size)
    return ok({
      trainers: pg.items.map(u => ({ trainerId: u.id, loginId: u.loginId, name: u.name, phone: u.phone, approvalStatus: u.approval === 'APPROVED' ? '승인' : '승인대기', createdAt: u.createdAt, clientCount: new Set(db.ptPasses.filter(x => x.trainerId === u.id && passStatus(x) !== '만료').map(x => x.userId)).size, schedules: workTimes(u.id) })),
      page: pg.page, size: pg.size, totalCount: pg.total, totalPages: pg.totalPages
    }, '트레이너 목록을 조회했습니다.')
  }],
  ['POST', /^\/admin\/trainers\/(\d+)\/approve$/, ['ADMIN'], p => {
    const u = userById(Number(p[0]))
    if (!u || u.role !== 'TRAINER') fail(404, 'TRAINER_NOT_FOUND', '해당 트레이너를 찾을 수 없습니다.')
    if (u.approval === 'APPROVED') fail(409, 'ALREADY_APPROVED', '이미 승인된 트레이너입니다.')
    u.approval = 'APPROVED'
    return ok({ trainerId: u.id, approvalStatus: '승인' }, '트레이너 가입이 승인되었습니다.')
  }],
  ['POST', /^\/admin\/trainers\/(\d+)\/reject$/, ['ADMIN'], p => {
    const u = userById(Number(p[0]))
    if (!u || u.role !== 'TRAINER') fail(404, 'TRAINER_NOT_FOUND', '해당 트레이너를 찾을 수 없습니다.')
    if (u.approval === 'APPROVED') fail(409, 'ALREADY_APPROVED', '이미 승인된 트레이너입니다.')
    db.users = db.users.filter(x => x.id !== u.id)
    return ok({ trainerId: u.id, deleted: true }, '트레이너 가입이 거절되었습니다.')
  }],
  ['PATCH', /^\/admin\/trainers\/(\d+)$/, ['ADMIN'], (p, q, b) => {
    const u = userById(Number(p[0]))
    if (!u || u.role !== 'TRAINER') fail(404, 'TRAINER_NOT_FOUND', '해당 트레이너를 찾을 수 없습니다.')
    if (!b || !Object.keys(b).length) fail(400, 'INVALID_INPUT', '수정할 항목을 하나 이상 입력해 주세요.')
    for (const k of Object.keys(b)) if (!['name', 'phone', 'birthDate', 'gender', 'career', 'introduction'].includes(k)) fail(400, 'INVALID_INPUT', `${k}은(는) 수정할 수 없습니다.`)
    if (b.gender !== undefined && b.gender !== null && !['남', '여'].includes(b.gender)) fail(400, 'INVALID_INPUT', '성별은 남 또는 여입니다.')
    if (b.phone && db.users.some(x => x.phone === b.phone && x.id !== u.id)) fail(409, 'DUPLICATE_PHONE', '이미 등록된 전화번호입니다.')
    if ('name' in b) u.name = b.name
    if ('phone' in b) u.phone = b.phone
    if ('birthDate' in b) u.birthDate = b.birthDate
    if ('gender' in b) u.gender = b.gender === '남' ? 'MALE' : b.gender === '여' ? 'FEMALE' : null
    if ('career' in b) u.career = b.career
    if ('introduction' in b) u.introduction = b.introduction
    return ok({ trainerId: u.id, loginId: u.loginId, name: u.name, phone: u.phone, birthDate: u.birthDate, gender: GENDER_LABEL[u.gender] || null, career: u.career, introduction: u.introduction, approvalStatus: u.approval === 'APPROVED' ? '승인' : '승인대기' }, '트레이너 정보가 수정되었습니다.')
  }],
  ['GET', /^\/trainers\/(\d+)\/schedules$/, ['TRAINER', 'ADMIN'], (p, q, b, ctx) => {
    const t = trainerOfPath(p, ctx)
    return ok({ trainerId: t.id, schedules: db.schedules.filter(s => s.trainerId === t.id).sort((a, c) => a.dayOfWeek - c.dayOfWeek || a.startTime.localeCompare(c.startTime)).map(s => ({ scheduleId: s.id, dayOfWeek: s.dayOfWeek, startTime: s.startTime, endTime: s.endTime })), daysOff: db.daysOff.filter(d => d.trainerId === t.id).sort((a, c) => a.offDate.localeCompare(c.offDate)).map(d => ({ dayOffId: d.id, offDate: d.offDate })) }, '근무 일정을 조회했습니다.')
  }],
  ['POST', /^\/trainers\/(\d+)\/schedules$/, ['TRAINER', 'ADMIN'], (p, q, b, ctx) => {
    const t = trainerOfPath(p, ctx)
    if (t.approval !== 'APPROVED') fail(404, 'TRAINER_NOT_FOUND', '해당 트레이너를 찾을 수 없습니다.')
    if (!(b.dayOfWeek >= 1 && b.dayOfWeek <= 7) || !b.startTime || !b.endTime || b.endTime <= b.startTime) fail(400, 'INVALID_INPUT', '입력값을 확인해 주세요.')
    if (db.schedules.some(s => s.trainerId === t.id && s.dayOfWeek === b.dayOfWeek && s.startTime < b.endTime && s.endTime > b.startTime)) fail(409, 'SCHEDULE_CONFLICT', '이미 등록된 근무 시간과 겹칩니다.')
    const s = { id: nextId(db.schedules), trainerId: t.id, dayOfWeek: b.dayOfWeek, startTime: b.startTime, endTime: b.endTime }
    db.schedules.push(s)
    return ok({ scheduleId: s.id, trainerId: t.id, dayOfWeek: s.dayOfWeek, startTime: s.startTime, endTime: s.endTime }, '근무 일정이 등록되었습니다.', 201)
  }],
  ['DELETE', /^\/trainers\/(\d+)\/schedules\/(\d+)$/, ['TRAINER', 'ADMIN'], (p, q, b, ctx) => {
    const t = trainerOfPath(p, ctx)
    const s = db.schedules.find(x => x.id === Number(p[1]) && x.trainerId === t.id)
    if (!s) fail(404, 'SCHEDULE_NOT_FOUND', '해당 근무 일정을 찾을 수 없습니다.')
    const t0 = todayS()
    const busy = db.reservations.some(r => r.trainerId === t.id && r.status === '예약' && r.lessonDate >= t0 && dow(r.lessonDate) === s.dayOfWeek && r.startTime >= s.startTime && r.startTime < s.endTime)
      || db.groupClasses.some(g => g.trainerId === t.id && ['개설', '확정'].includes(g.status) && g.classDate >= t0 && dow(g.classDate) === s.dayOfWeek && g.startTime >= s.startTime && g.startTime < s.endTime)
    if (busy) fail(409, 'SCHEDULE_HAS_BOOKINGS', '예약된 PT 또는 그룹 수업이 있어 근무 일정을 삭제할 수 없습니다.')
    db.schedules = db.schedules.filter(x => x !== s)
    return ok(null, '근무 일정이 삭제되었습니다.')
  }],
  ['POST', /^\/trainers\/(\d+)\/days-off$/, ['TRAINER', 'ADMIN'], (p, q, b, ctx) => {
    const t = trainerOfPath(p, ctx)
    if (!b.offDate) fail(400, 'INVALID_INPUT', '입력값을 확인해 주세요.')
    if (db.daysOff.some(d => d.trainerId === t.id && d.offDate === b.offDate)) fail(409, 'DAY_OFF_ALREADY_EXISTS', '이미 휴무로 등록된 날짜입니다.')
    if (db.reservations.some(r => r.trainerId === t.id && r.lessonDate === b.offDate && r.status === '예약') || db.groupClasses.some(g => g.trainerId === t.id && g.classDate === b.offDate && ['개설', '확정'].includes(g.status))) fail(409, 'DAY_OFF_CONFLICT', '해당 날짜에 예약된 수업이 있어 휴무를 등록할 수 없습니다.')
    const d = { id: nextId(db.daysOff), trainerId: t.id, offDate: b.offDate }
    db.daysOff.push(d)
    return ok({ dayOffId: d.id, trainerId: t.id, offDate: d.offDate }, '휴무일이 등록되었습니다.', 201)
  }],
  ['DELETE', /^\/trainers\/(\d+)\/days-off\/(\d+)$/, ['TRAINER', 'ADMIN'], (p, q, b, ctx) => {
    const t = trainerOfPath(p, ctx)
    const d = db.daysOff.find(x => x.id === Number(p[1]) && x.trainerId === t.id)
    if (!d) fail(404, 'DAY_OFF_NOT_FOUND', '해당 휴무일을 찾을 수 없습니다.')
    db.daysOff = db.daysOff.filter(x => x !== d)
    return ok(null, '휴무일이 삭제되었습니다.')
  }],

  ['GET', /^\/group-classes$/, ['MEMBER'], (p, q, b, ctx) => {
    const start = q.startDate || todayS()
    const end = q.endDate || addD(todayS(), setting('BOOKING_RANGE_DAYS'))
    const list = db.groupClasses.filter(g => ['개설', '확정'].includes(g.status) && g.classDate >= start && g.classDate <= end && dt(g.classDate, g.startTime) > now()).sort((a, c) => (a.classDate + a.startTime).localeCompare(c.classDate + c.startTime))
    return ok({
      classes: list.map(g => {
        const cur = db.reservations.filter(r => r.groupClassId === g.id && r.status !== '취소').length
        const reason = groupReason(ctx.uid, g)
        return { classId: g.id, name: g.name, description: g.description, workout: g.workout || null, imageUrl: g.imageUrl || null, trainerId: g.trainerId, trainerName: userById(g.trainerId)?.name, classDate: g.classDate, startTime: g.startTime, endTime: g.endTime, capacity: g.capacity, currentParticipants: cur, remainingSeats: g.capacity - cur, status: g.status, canReserve: !reason, unavailableReason: reason }
      })
    }, '그룹 수업 목록을 조회했습니다.')
  }],
  ['GET', /^\/trainers\/me\/group-classes$/, ['TRAINER'], (p, q, b, ctx) => {
    let start = q.startDate
    let end = q.endDate
    if (start && !end) end = addD(start, 30)
    const list = db.groupClasses.filter(g => g.trainerId === ctx.uid && (!q.status || g.status === q.status) && (!start || g.classDate >= start) && (!end || g.classDate <= end)).sort((a, c) => (a.classDate + a.startTime).localeCompare(c.classDate + c.startTime))
    return ok({
      classes: list.map(g => {
        const parts = db.reservations.filter(r => r.groupClassId === g.id && r.status !== '취소').map(r => ({ reservationId: r.id, userId: r.userId, name: userById(r.userId)?.name, reservationStatus: r.status }))
        return { classId: g.id, name: g.name, description: g.description, workout: g.workout || null, imageUrl: g.imageUrl || null, classDate: g.classDate, startTime: g.startTime, endTime: g.endTime, capacity: g.capacity, currentParticipants: parts.length, status: g.status, participants: parts }
      })
    }, '내 그룹 수업 목록을 조회했습니다.')
  }],
  ['GET', /^\/trainers\/me\/group-class-available-slots$/, ['TRAINER'], (p, q, b, ctx) => {
    if (!q.classDate) fail(400, 'INVALID_INPUT', '입력값을 확인해 주세요.')
    return ok({ classDate: q.classDate, slots: trainerSlotsForGroup(ctx.uid, q.classDate) }, '그룹 수업 개설 가능 시간을 조회했습니다.')
  }],
  ['POST', /^\/group-classes$/, ['TRAINER'], (p, q, b, ctx) => {
    if (!b.name || !b.classDate || !b.startTime || ![4, 5, 6].includes(b.capacity)) fail(400, 'INVALID_INPUT', '입력값을 확인해 주세요.')
    if ((b.description || '').length > 200) fail(400, 'INVALID_INPUT', '설명은 200자 이하로 입력해 주세요.')
    checkVisual(b)
    const slot = trainerSlotsForGroup(ctx.uid, b.classDate).find(s => s.startTime === b.startTime)
    if (!slot) fail(409, 'TRAINER_UNAVAILABLE', '해당 시간에는 그룹 수업을 개설할 수 없습니다.')
    if (slot.unavailableReason === 'OUTSIDE_OPENING_RANGE') fail(400, 'INVALID_CLASS_DATE', '그룹 수업을 개설할 수 없는 날짜입니다.')
    if (slot.unavailableReason === 'DAY_OFF') fail(409, 'TRAINER_UNAVAILABLE', '해당 시간에는 그룹 수업을 개설할 수 없습니다.')
    if (slot.unavailableReason) fail(409, 'CLASS_TIME_CONFLICT', '해당 시간에 다른 PT 예약 또는 그룹 수업이 있습니다.')
    const g = { id: nextId(db.groupClasses), trainerId: ctx.uid, name: b.name, description: b.description || null, classDate: b.classDate, startTime: b.startTime, endTime: hh(toH(b.startTime) + 1), capacity: b.capacity, status: '개설', cancelReason: null, canceledAt: null, createdAt: isoDT(now()) }
    g.workout = b.workout || null
    g.imageUrl = b.imageUrl || null
    db.groupClasses.push(g)
    return ok({ classId: g.id, trainerId: g.trainerId, name: g.name, description: g.description, workout: g.workout || null, imageUrl: g.imageUrl || null, classDate: g.classDate, startTime: g.startTime, endTime: g.endTime, capacity: g.capacity, currentParticipants: 0, status: g.status }, '그룹 수업이 개설되었습니다.', 201)
  }],
  ['PATCH', /^\/group-classes\/(\d+)$/, ['TRAINER'], (p, q, b, ctx) => {
    const g = db.groupClasses.find(x => x.id === Number(p[0]) && x.trainerId === ctx.uid)
    if (!g) fail(404, 'GROUP_CLASS_NOT_FOUND', '해당 그룹 수업을 찾을 수 없습니다.')
    if (db.reservations.some(r => r.groupClassId === g.id && r.status !== '취소')) fail(409, 'CLASS_HAS_PARTICIPANTS', '신청한 회원이 있어 그룹 수업을 수정할 수 없습니다.')
    if (b.capacity !== undefined && b.capacity !== null && ![4, 5, 6].includes(b.capacity)) fail(400, 'INVALID_INPUT', '정원은 4, 5, 6명 중에서 고를 수 있습니다.')
    if ((b.classDate && b.classDate !== g.classDate) || (b.startTime && b.startTime !== g.startTime)) {
      const date = b.classDate || g.classDate
      const st = b.startTime || g.startTime
      const prev = g.status
      g.status = '취소'
      const slot = trainerSlotsForGroup(ctx.uid, date).find(s => s.startTime === st)
      g.status = prev
      if (!slot || slot.unavailableReason) fail(409, 'CLASS_TIME_CONFLICT', '해당 시간에 다른 PT 예약 또는 그룹 수업이 있습니다.')
      g.classDate = date
      g.startTime = st
      g.endTime = hh(toH(st) + 1)
    }
    if (b.name) g.name = b.name
    if (b.description !== undefined && b.description !== null) g.description = b.description === '' ? null : b.description
    if (b.capacity) g.capacity = b.capacity
    checkVisual(b)
    if (b.workout !== undefined && b.workout !== null) g.workout = b.workout || null      // '' = 지우기
    if (b.imageUrl !== undefined && b.imageUrl !== null) g.imageUrl = b.imageUrl || null
    return ok({ classId: g.id, trainerId: g.trainerId, name: g.name, description: g.description, workout: g.workout || null, imageUrl: g.imageUrl || null, classDate: g.classDate, startTime: g.startTime, endTime: g.endTime, capacity: g.capacity, currentParticipants: 0, status: g.status }, '그룹 수업이 수정되었습니다.')
  }],
  ['DELETE', /^\/group-classes\/(\d+)$/, ['TRAINER'], (p, q, b, ctx) => {
    const g = db.groupClasses.find(x => x.id === Number(p[0]) && x.trainerId === ctx.uid)
    if (!g) fail(404, 'GROUP_CLASS_NOT_FOUND', '해당 그룹 수업을 찾을 수 없습니다.')
    if (db.reservations.some(r => r.groupClassId === g.id)) fail(409, 'CLASS_HAS_RESERVATIONS', '예약 이력이 있는 그룹 수업은 삭제할 수 없습니다. 수업 취소 기능을 이용해 주세요.')
    db.groupClasses = db.groupClasses.filter(x => x !== g)
    return ok(null, '그룹 수업이 삭제되었습니다.')
  }],
  ['POST', /^\/group-classes\/(\d+)\/cancel$/, ['TRAINER'], (p, q, b, ctx) => {
    const g = db.groupClasses.find(x => x.id === Number(p[0]) && x.trainerId === ctx.uid)
    if (!g) fail(404, 'GROUP_CLASS_NOT_FOUND', '해당 그룹 수업을 찾을 수 없습니다.')
    if (!['개설', '확정'].includes(g.status) || dt(g.classDate, g.startTime) <= now()) fail(409, 'CLASS_NOT_CANCELABLE', '이미 취소되었거나 시작된 그룹 수업은 취소할 수 없습니다.')
    if (!b?.cancelReason?.trim()) fail(400, 'CANCEL_REASON_REQUIRED', '취소 사유를 입력해 주세요.')
    if (b.cancelReason.length > 200) fail(400, 'INVALID_INPUT', '취소 사유는 200자 이하로 입력해 주세요.')
    const ts = isoDT(now())
    const parts = db.reservations.filter(r => r.groupClassId === g.id && r.status === '예약')
    parts.forEach(r => Object.assign(r, { status: '취소', canceledBy: '트레이너', cancelReason: b.cancelReason, canceledAt: ts }))
    Object.assign(g, { status: '취소', cancelReason: b.cancelReason, canceledAt: ts })
    return ok({ classId: g.id, status: '취소', cancelReason: g.cancelReason, canceledAt: ts, canceledReservations: parts.length }, '그룹 수업이 취소되었습니다.')
  }],

  ['GET', /^\/pt-reservations\/available-slots$/, ['MEMBER'], (p, q, b, ctx) => {
    const tid = Number(q.trainerId)
    const t = userById(tid)
    if (!t || t.role !== 'TRAINER' || t.approval !== 'APPROVED') fail(404, 'TRAINER_NOT_FOUND', '해당 트레이너를 찾을 수 없습니다.')
    const date = q.date
    if (!date || date < todayS() || date > addD(todayS(), setting('BOOKING_RANGE_DAYS'))) fail(400, 'INVALID_DATE_RANGE', '조회 가능한 날짜 범위를 확인해 주세요.')
    const off = db.daysOff.some(d => d.trainerId === tid && d.offDate === date)
    const slots = []
    if (!off) {
      for (const s of db.schedules.filter(x => x.trainerId === tid && x.dayOfWeek === dow(date))) {
        for (let h = toH(s.startTime); h < toH(s.endTime); h++) {
          const reason = individualReason(ctx.uid, tid, date, hh(h))
          slots.push({ startTime: hh(h), endTime: hh(h + 1), available: !reason, unavailableReason: reason })
        }
      }
    }
    return ok({ trainerId: tid, trainerName: t.name, date, slots: slots.sort((a, c) => a.startTime.localeCompare(c.startTime)) }, '개인 PT 예약 가능 시간을 조회했습니다.')
  }],
  ['POST', /^\/pt-reservations\/individual$/, ['MEMBER'], (p, q, b, ctx) => {
    const tid = Number(b.trainerId)
    if (!tid || !b.lessonDate || !b.startTime) fail(400, 'INVALID_INPUT', '입력값을 확인해 주세요.')
    const inSchedule = db.schedules.some(s => s.trainerId === tid && s.dayOfWeek === dow(b.lessonDate) && s.startTime <= b.startTime && b.startTime < s.endTime) && !db.daysOff.some(d => d.trainerId === tid && d.offDate === b.lessonDate)
    if (!inSchedule) fail(409, 'SLOT_UNAVAILABLE', '선택한 시간에는 예약할 수 없습니다.')
    const reason = individualReason(ctx.uid, tid, b.lessonDate, b.startTime)
    if (reason) failReason(reason)
    const pass = usablePass(ctx.uid, '개인', b.lessonDate, tid).pass
    const r = { id: nextId(db.reservations), ptPassId: pass.id, userId: ctx.uid, trainerId: tid, groupClassId: null, lessonDate: b.lessonDate, startTime: b.startTime, endTime: hh(toH(b.startTime) + 1), status: '예약', canceledBy: null, cancelReason: null, canceledAt: null, isDeducted: false, reservedAt: isoDT(now()) }
    db.reservations.push(r)
    const v = resView(r)
    return ok({ reservationId: v.reservationId, ptPassId: v.ptPassId, ptType: '개인', trainerId: v.trainerId, trainerName: v.trainerName, lessonDate: v.lessonDate, startTime: v.startTime, endTime: v.endTime, status: v.status, isDeducted: false, reservedAt: v.reservedAt }, '개인 PT 예약이 완료되었습니다.', 201)
  }],
  ['POST', /^\/pt-reservations\/group$/, ['MEMBER'], (p, q, b, ctx) => {
    const g = db.groupClasses.find(x => x.id === Number(b.classId))
    if (!g || !['개설', '확정'].includes(g.status) || g.classDate > addD(todayS(), setting('BOOKING_RANGE_DAYS'))) fail(404, 'GROUP_CLASS_NOT_FOUND', '해당 그룹 수업을 찾을 수 없습니다.')
    const reason = groupReason(ctx.uid, g)
    if (reason) failReason(reason)
    const pass = usablePass(ctx.uid, '그룹', g.classDate).pass
    const r = { id: nextId(db.reservations), ptPassId: pass.id, userId: ctx.uid, trainerId: g.trainerId, groupClassId: g.id, lessonDate: g.classDate, startTime: g.startTime, endTime: g.endTime, status: '예약', canceledBy: null, cancelReason: null, canceledAt: null, isDeducted: false, reservedAt: isoDT(now()) }
    db.reservations.push(r)
    const cur = db.reservations.filter(x => x.groupClassId === g.id && x.status !== '취소').length
    return ok({ reservationId: r.id, ptPassId: r.ptPassId, ptType: '그룹', groupClassId: g.id, className: g.name, trainerId: g.trainerId, trainerName: userById(g.trainerId)?.name, lessonDate: r.lessonDate, startTime: r.startTime, endTime: r.endTime, status: '예약', isDeducted: false, reservedAt: r.reservedAt, currentParticipants: cur, capacity: g.capacity }, '그룹 PT 수업 신청이 완료되었습니다.', 201)
  }],
  ['GET', /^\/pt-reservations\/me$/, ['MEMBER'], (p, q, b, ctx) => {
    const list = db.reservations.filter(r => r.userId === ctx.uid && (!q.status || r.status === q.status)).map(resView).filter(v => !q.ptType || v.ptType === q.ptType).sort((a, c) => (c.lessonDate + c.startTime).localeCompare(a.lessonDate + a.startTime))
    return ok({ reservations: list }, '내 PT 예약 목록을 조회했습니다.')
  }],
  ['GET', /^\/pt-reservations\/(\d+)\/cancellation-preview$/, ['MEMBER'], (p, q, b, ctx) => {
    const r = db.reservations.find(x => x.id === Number(p[0]) && x.userId === ctx.uid)
    if (!r) fail(404, 'RESERVATION_NOT_FOUND', '해당 예약을 찾을 수 없습니다.')
    return ok(previewOf(r), '예약 취소 조건을 확인했습니다.')
  }],
  ['POST', /^\/pt-reservations\/(\d+)\/cancel$/, ['MEMBER'], (p, q, b, ctx) => {
    const r = db.reservations.find(x => x.id === Number(p[0]) && x.userId === ctx.uid)
    if (!r) fail(404, 'RESERVATION_NOT_FOUND', '해당 예약을 찾을 수 없습니다.')
    const pv = previewOf(r)
    if (!pv.canCancel) fail(409, 'RESERVATION_NOT_CANCELABLE', '이미 취소되었거나 수업이 시작된 예약은 취소할 수 없습니다.')
    if (pv.willDeduct && !b.acceptDeduction) fail(409, 'DEDUCTION_CONFIRMATION_REQUIRED', '취소 시 PT 1회가 차감됩니다. 확인 후 다시 요청해 주세요.')
    const ts = isoDT(now())
    Object.assign(r, { status: '취소', canceledBy: '회원', canceledAt: ts, isDeducted: pv.willDeduct })
    if (pv.willDeduct) db.ptPasses.find(x => x.id === r.ptPassId).remainingCount -= 1
    return { status: 200, data: { reservationId: r.id, status: '취소', canceledBy: '회원', isDeducted: r.isDeducted, deductionCount: r.isDeducted ? 1 : 0, canceledAt: ts }, message: r.isDeducted ? 'PT 1회가 차감되고 예약이 취소되었습니다.' : 'PT 예약이 차감 없이 취소되었습니다.' }
  }],
  ['GET', /^\/trainers\/me\/lessons$/, ['TRAINER'], (p, q, b, ctx) => {
    let start = q.startDate
    let end = q.endDate
    if (start && !end) end = start
    if (end && !start) start = end
    if (!start && !(q.status === '결과대기')) {
      start = todayS()
      end = addD(start, 6)
    }
    const rs = db.reservations.filter(r => r.trainerId === ctx.uid && r.status !== '취소' && (!q.status || r.status === q.status) && (!start || (r.lessonDate >= start && r.lessonDate <= end)))
    const groups = new Map()
    for (const r of rs) {
      const key = r.groupClassId ? `g${r.groupClassId}` : `r${r.id}`
      if (!groups.has(key)) groups.set(key, [])
      groups.get(key).push(r)
    }
    const lessons = [...groups.values()].map(list => {
      const f = list[0]
      const g = f.groupClassId ? db.groupClasses.find(x => x.id === f.groupClassId) : null
      return { ptType: g ? '그룹' : '개인', groupClassId: g?.id ?? null, className: g?.name ?? null, lessonDate: f.lessonDate, startTime: f.startTime, endTime: f.endTime, participants: list.map(r => ({ reservationId: r.id, userId: r.userId, name: userById(r.userId)?.name, phone: userById(r.userId)?.phone, status: r.status, isDeducted: r.isDeducted })) }
    }).sort((a, c) => (a.lessonDate + a.startTime).localeCompare(c.lessonDate + c.startTime))
    return ok({ lessons }, '수업 일정을 조회했습니다.')
  }],
  ['PATCH', /^\/pt-reservations\/(\d+)\/result$/, ['TRAINER'], (p, q, b, ctx) => {
    const target = b?.result === '출석' ? '출석' : b?.result === '결석' ? '결석' : null
    if (!target) fail(400, 'INVALID_INPUT', '출석 또는 결석을 선택해 주세요.')
    const r = db.reservations.find(x => x.id === Number(p[0]))
    if (!r) fail(404, 'RESERVATION_NOT_FOUND', '해당 예약을 찾을 수 없습니다.')
    if (r.trainerId !== ctx.uid) fail(403, 'FORBIDDEN', '담당 트레이너만 출석·결석을 처리할 수 있습니다.')
    if (r.status === '취소') fail(409, 'INVALID_RESERVATION_STATE', '해당 예약은 출석·결석 처리할 수 없습니다.')
    if (dt(r.lessonDate, r.startTime) > now()) fail(409, 'LESSON_NOT_STARTED', '수업 시작 후 출석·결석을 처리할 수 있습니다.')
    const first = r.status === '예약' || r.status === '결과대기'
    const same = r.status === target
    let deduction = 0
    if (!r.isDeducted) {
      r.isDeducted = true
      deduction = 1
      db.ptPasses.find(x => x.id === r.ptPassId).remainingCount -= 1
    }
    r.status = target
    const remaining = db.ptPasses.find(x => x.id === r.ptPassId).remainingCount
    return { status: 200, data: { reservationId: r.id, status: target, isDeducted: true, deductionCount: deduction, remainingCount: remaining }, message: first || same ? `${target} 처리가 완료되었습니다.` : `${target}으로 정정되었습니다.` }
  }],
  ['POST', /^\/pt-reservations\/(\d+)\/trainer-cancel$/, ['TRAINER'], (p, q, b, ctx) => {
    const r = db.reservations.find(x => x.id === Number(p[0]))
    if (!r || r.trainerId !== ctx.uid) fail(404, 'RESERVATION_NOT_FOUND', '해당 예약을 찾을 수 없습니다.')
    if (r.groupClassId) fail(409, 'INVALID_RESERVATION_TYPE', '그룹 PT 수업은 그룹 수업 취소 기능을 이용해 주세요.')
    return staffCancel(r, b, '트레이너')
  }],
  ['GET', /^\/admin\/pt-reservations$/, ['ADMIN'], (p, q) => {
    const list = db.reservations.map(r => ({ r, v: resView(r) })).filter(({ r, v }) =>
      (!q.userId || r.userId === Number(q.userId)) && (!q.trainerId || r.trainerId === Number(q.trainerId)) && (!q.ptType || q.ptType === '전체' || v.ptType === q.ptType) && (!q.status || q.status === '전체' || r.status === q.status) && inRange(r.lessonDate, q.startDate, q.endDate)
    ).sort((a, c) => (c.r.lessonDate + c.r.startTime).localeCompare(a.r.lessonDate + a.r.startTime))
    const pg = page(list, q.page, q.size)
    return ok({ reservations: pg.items.map(({ r, v }) => ({ reservationId: r.id, userId: r.userId, userName: userById(r.userId)?.name, trainerId: r.trainerId, trainerName: v.trainerName, ptType: v.ptType, ptPassId: r.ptPassId, groupClassId: v.groupClassId, className: v.className, lessonDate: r.lessonDate, startTime: r.startTime, endTime: r.endTime, status: r.status, isDeducted: r.isDeducted, reservedAt: r.reservedAt, canceledBy: r.canceledBy, cancelReason: r.cancelReason, canceledAt: r.canceledAt })), page: pg.page, size: pg.size, totalCount: pg.total, totalPages: pg.totalPages }, '전체 PT 예약 목록을 조회했습니다.')
  }],
  ['POST', /^\/admin\/pt-reservations\/(\d+)\/cancel$/, ['ADMIN'], (p, q, b) => {
    const r = db.reservations.find(x => x.id === Number(p[0]))
    if (!r) fail(404, 'RESERVATION_NOT_FOUND', '해당 예약을 찾을 수 없습니다.')
    return staffCancel(r, b, '관리자')
  }]
]

function staffCancel(r, b, by) {
  if (r.status !== '예약' || dt(r.lessonDate, r.startTime) <= now()) fail(409, 'RESERVATION_NOT_CANCELABLE', '이미 취소되었거나 수업이 시작된 예약은 취소할 수 없습니다.')
  const reason = b?.cancelReason?.trim()
  if (!reason) fail(400, 'CANCEL_REASON_REQUIRED', '취소 사유를 입력해 주세요.')
  if (reason.length > 200) fail(400, 'INVALID_INPUT', '취소 사유는 200자 이하로 입력해 주세요.')
  const ts = isoDT(now())
  Object.assign(r, { status: '취소', canceledBy: by, cancelReason: reason, canceledAt: ts })
  return ok({ reservationId: r.id, status: '취소', canceledBy: by, cancelReason: reason, isDeducted: r.isDeducted, deductionCount: 0, canceledAt: ts }, by === '관리자' ? 'PT 예약이 관리자에 의해 취소되었습니다.' : 'PT 예약이 취소되었습니다.')
}

function updateProfile(u, b) {
  if (b.phone !== undefined && b.phone !== null) {
    if (!/^[0-9]{10,11}$/.test(b.phone)) fail(400, 'INVALID_INPUT', '전화번호는 숫자만 10~11자리입니다.')
    if (db.users.some(x => x.phone === b.phone && x.id !== u.id)) fail(409, 'DUPLICATE_PHONE', '이미 등록된 전화번호입니다.')
    u.phone = b.phone
  }
  if (b.gender !== undefined && b.gender !== null && !['MALE', 'FEMALE'].includes(b.gender)) fail(400, 'INVALID_INPUT', '입력값을 확인해 주세요.')
  for (const k of ['name', 'birthDate', 'gender', 'career', 'introduction']) if (b[k] !== undefined && b[k] !== null) u[k] = b[k]
  return u
}

export async function mockHandle(method, path, query, body) {
  await new Promise(r => setTimeout(r, 120))
  tick()
  saveDb()
  const route = routes.find(([m, re]) => m === method && re.test(path))
  if (!route) return { status: 404, json: { success: false, data: null, code: 'NOT_FOUND', message: '요청한 데이터를 찾을 수 없습니다.' } }
  const [, re, roles, fn] = route
  const params = path.match(re).slice(1)
  try {
    const uid = MODE === 'all' ? store.get() : externalUid
    const user = uid ? userById(uid) : null
    if (MODE === 'all' && !PUBLIC.includes(path)) {
      if (!user) fail(401, 'UNAUTHORIZED', '로그인이 필요합니다.')
      if (user.role === 'TRAINER' && user.approval !== 'APPROVED' && !['/auth/me', '/auth/logout'].includes(path)) fail(403, 'TRAINER_NOT_APPROVED', '관리자 승인 대기 중입니다.')
      if (user.mustChange && !['/auth/me', '/auth/logout', '/auth/password'].includes(path)) fail(403, 'PASSWORD_CHANGE_REQUIRED', '비밀번호를 변경해 주세요.')
      if (roles && !roles.includes(user.role)) fail(403, 'FORBIDDEN', '접근 권한이 없습니다.')
    }
    const r = fn(params, query, body || {}, { uid, user })
    if (method !== 'GET') saveDb()
    return { status: r.status, json: { success: true, data: r.data ?? null, message: r.message } }
  } catch (e) {
    if (e instanceof MockError) return { status: e.status, json: { success: false, data: null, code: e.code, message: e.message } }
    console.error(e)
    return { status: 500, json: { success: false, data: null, code: 'INTERNAL_ERROR', message: '서버 오류가 발생했습니다.' } }
  }
}

export const mockAccounts = [
  ['member1', '회원 이서준'],
  ['member2', '회원 최하은'],
  ['trainer1', '트레이너 김도윤'],
  ['trainer2', '승인 대기 트레이너'],
  ['admin', '관리자']
]
export const mockRoleLabel = ROLE_LABEL
