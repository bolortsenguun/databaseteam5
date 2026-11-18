const pad = n => String(n).padStart(2, '0')
const iso = d => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
const shift = n => {
  const d = new Date()
  d.setDate(d.getDate() + n)
  return iso(d)
}
const shiftMonths = (s, m) => {
  const [y, mo, d] = s.split('-').map(Number)
  const x = new Date(y, mo - 1 + m, d)
  x.setDate(x.getDate() - 1)
  return iso(x)
}
const at = (date, time) => `${date}T${time}:00`
const dowOf = s => {
  const [y, m, d] = s.split('-').map(Number)
  const w = new Date(y, m - 1, d).getDay()
  return w === 0 ? 7 : w
}
const nextDow = (from, dow) => {
  for (let i = from; i < from + 8; i++) if (dowOf(shift(i)) === dow) return i
  return from
}

export function seed() {
  const users = [
    { id: 1, loginId: 'member1', password: 'test1234', role: 'MEMBER', approval: 'APPROVED', name: '이서준', phone: '01012345678', birthDate: '1999-02-18', gender: 'MALE', career: null, introduction: null, mustChange: false, createdAt: at(shift(-40), '10:12') },
    { id: 2, loginId: 'trainer1', password: 'test1234', role: 'TRAINER', approval: 'APPROVED', name: '김도윤', phone: '01011112222', birthDate: '1992-05-03', gender: 'MALE', career: '생활체육지도사 2급 · 경력 6년', introduction: '자세 교정과 체중 감량을 함께 잡아 드려요. 처음 운동하는 분도 편하게 시작할 수 있어요.', mustChange: false, createdAt: at(shift(-90), '09:00') },
    { id: 3, loginId: 'admin', password: 'test1234', role: 'ADMIN', approval: 'APPROVED', name: '관리자', phone: '01000000000', birthDate: null, gender: null, career: null, introduction: null, mustChange: false, createdAt: at(shift(-120), '09:00') },
    { id: 4, loginId: 'trainer2', password: 'test1234', role: 'TRAINER', approval: 'PENDING', name: '박지민', phone: '01033334444', birthDate: '1996-08-21', gender: 'FEMALE', career: null, introduction: null, mustChange: false, createdAt: at(shift(-2), '15:20') },
    { id: 5, loginId: 'member2', password: 'test1234', role: 'MEMBER', approval: 'APPROVED', name: '최하은', phone: '01055556666', birthDate: '2001-11-02', gender: 'FEMALE', career: null, introduction: null, mustChange: false, createdAt: at(shift(-30), '13:40') },
    { id: 6, loginId: 'trainer3', password: 'test1234', role: 'TRAINER', approval: 'APPROVED', name: '박서연', phone: '01077778888', birthDate: '1994-01-15', gender: 'FEMALE', career: null, introduction: null, mustChange: false, createdAt: at(shift(-60), '11:00') },
    { id: 7, loginId: 'member3', password: 'test1234', role: 'MEMBER', approval: 'APPROVED', name: '강민준', phone: '01045678901', birthDate: '1997-07-07', gender: 'MALE', career: null, introduction: null, mustChange: false, createdAt: at(shift(-15), '18:02') },
    { id: 8, loginId: 'member4', password: 'test1234', role: 'MEMBER', approval: 'APPROVED', name: '윤채원', phone: '01056789012', birthDate: null, gender: 'FEMALE', career: null, introduction: null, mustChange: false, createdAt: at(shift(-8), '09:40') }
  ]

  const settings = [
    ['BOOKING_CLOSE_HOURS', 1, '예약 마감(수업 시작 N시간 전)'],
    ['BOOKING_RANGE_DAYS', 14, '예약 가능 범위(일), 회원 목록 표시 범위'],
    ['CANCEL_DEADLINE_HOURS', 24, '취소 기한(시간). 이내 회원 취소 시 1회 차감'],
    ['FREE_CANCEL_MINUTES', 10, '예약 후 무료 취소 가능 시간(분)'],
    ['GROUP_CONFIRM_HOURS', 24, '그룹 최소 인원 판정 시점(시작 N시간 전)'],
    ['GROUP_MIN_PARTICIPANTS', 2, '그룹 수업 최소 인원'],
    ['GROUP_OPEN_MAX_DAYS', 30, '그룹 수업 개설 가능 범위(오늘부터 N일 후)'],
    ['GROUP_OPEN_MIN_HOURS', 48, '그룹 수업 개설 마감(시작 N시간 전까지)'],
    ['PASS_START_MAX_DAYS', 30, '이용권 시작일 선택 가능 범위(일)'],
    ['PT_ENTRY_AFTER_MINUTES', 30, 'PT 회원 입장 가능(시작 N분 후까지)'],
    ['PT_ENTRY_BEFORE_MINUTES', 30, 'PT 회원 입장 가능(시작 N분 전부터)'],
    ['RECHECKIN_MINUTES', 10, '재체크인 중복 판정(분)'],
    ['SLOT_MINUTES', 60, '슬롯 단위(분)']
  ].map(([settingKey, settingValue, description]) => ({ settingKey, settingValue, description }))

  const membershipProducts = [
    { id: 1, name: '1개월권', months: 1, price: 50000, active: true },
    { id: 2, name: '3개월권', months: 3, price: 130000, active: true },
    { id: 3, name: '6개월권', months: 6, price: 240000, active: true },
    { id: 4, name: '12개월권', months: 12, price: 440000, active: true }
  ]
  const ptProducts = [
    { id: 1, ptType: 'PERSONAL', name: '개인 PT 10회', totalCount: 10, validWeeks: 12, price: 500000, active: true },
    { id: 2, ptType: 'PERSONAL', name: '개인 PT 20회', totalCount: 20, validWeeks: 24, price: 950000, active: true },
    { id: 3, ptType: 'PERSONAL', name: '개인 PT 30회', totalCount: 30, validWeeks: 36, price: 1350000, active: true },
    { id: 4, ptType: 'GROUP', name: '그룹 PT 10회', totalCount: 10, validWeeks: 12, price: 300000, active: true },
    { id: 5, ptType: 'GROUP', name: '그룹 PT 20회', totalCount: 20, validWeeks: 24, price: 570000, active: true },
    { id: 6, ptType: 'GROUP', name: '그룹 PT 30회', totalCount: 30, validWeeks: 36, price: 810000, active: true }
  ]

  const mStart = shift(-26)
  const payments = [
    { id: 1, userId: 1, amount: 50000, status: '실패', paidAt: at(shift(-27), '20:41') },
    { id: 2, userId: 1, amount: 130000, status: '성공', paidAt: at(shift(-26), '09:15') },
    { id: 3, userId: 1, amount: 500000, status: '성공', paidAt: at(shift(-20), '11:30') },
    { id: 4, userId: 1, amount: 300000, status: '성공', paidAt: at(shift(-10), '19:02') },
    { id: 5, userId: 5, amount: 300000, status: '성공', paidAt: at(shift(-9), '18:20') },
    { id: 6, userId: 7, amount: 500000, status: '성공', paidAt: at(shift(-6), '18:02') },
    { id: 7, userId: 8, amount: 50000, status: '실패', paidAt: at(shift(-5), '09:40') },
    { id: 8, userId: 5, amount: 130000, status: '성공', paidAt: at(shift(-3), '14:20') },
    { id: 9, userId: 1, amount: 50000, status: '성공', paidAt: at(shift(-1), '21:10') }
  ]
  const memberships = [
    { id: 1, userId: 1, productId: 2, paymentId: 2, startDate: mStart, endDate: shiftMonths(mStart, 3), price: 130000 },
    { id: 2, userId: 5, productId: 2, paymentId: 8, startDate: shift(-3), endDate: shiftMonths(shift(-3), 3), price: 130000 }
  ]
  const m1End = memberships[0].endDate
  const [ey, em, ed] = m1End.split('-').map(Number)
  const nextStart = iso(new Date(ey, em - 1, ed + 1))
  memberships.push({ id: 3, userId: 1, productId: 1, paymentId: 9, startDate: nextStart, endDate: shiftMonths(nextStart, 1), price: 50000 })

  const ptPasses = [
    { id: 1, userId: 1, ptType: '개인', trainerId: 2, productId: 1, paymentId: 3, startDate: shift(-20), endDate: shift(-20 + 84 - 1), totalCount: 10, remainingCount: 7, price: 500000 },
    { id: 2, userId: 1, ptType: '그룹', trainerId: null, productId: 4, paymentId: 4, startDate: shift(-10), endDate: shift(-10 + 84 - 1), totalCount: 10, remainingCount: 9, price: 300000 },
    { id: 3, userId: 5, ptType: '그룹', trainerId: null, productId: 4, paymentId: 5, startDate: shift(-9), endDate: shift(-9 + 84 - 1), totalCount: 10, remainingCount: 10, price: 300000 },
    { id: 4, userId: 7, ptType: '개인', trainerId: 2, productId: 1, paymentId: 6, startDate: shift(-6), endDate: shift(-6 + 84 - 1), totalCount: 10, remainingCount: 10, price: 500000 }
  ]

  const schedules = [1, 2, 3, 4, 5].map((d, i) => ({ id: i + 1, trainerId: 2, dayOfWeek: d, startTime: '10:00', endTime: '18:00' }))
    .concat([2, 3, 4, 5, 6].map((d, i) => ({ id: i + 6, trainerId: 6, dayOfWeek: d, startTime: '11:00', endTime: '20:00' })))

  const g1 = nextDow(3, 1)
  const g2 = nextDow(4, 3)
  const g3 = nextDow(5, 6)
  const groupClasses = [
    { id: 1, trainerId: 2, name: '모닝 서킷', description: '전신 근력과 유산소를 번갈아 하는 서킷 수업이에요. 처음 오신 분도 쉬운 동작부터 따라 할 수 있어요.', classDate: shift(g1), startTime: '10:00', endTime: '11:00', capacity: 4, status: '개설', cancelReason: null, canceledAt: null, createdAt: at(shift(-4), '10:00') },
    { id: 2, trainerId: 2, name: '하체 집중', description: '스쿼트와 런지로 하체를 탄탄하게 만드는 수업이에요.', classDate: shift(g2), startTime: '11:00', endTime: '12:00', capacity: 5, status: '개설', cancelReason: null, canceledAt: null, createdAt: at(shift(-3), '10:00') },
    { id: 3, trainerId: 6, name: '코어 스트레칭', description: null, classDate: shift(g3), startTime: '14:00', endTime: '15:00', capacity: 6, status: '개설', cancelReason: null, canceledAt: null, createdAt: at(shift(-2), '10:00') },
    { id: 4, trainerId: 6, name: '스트레칭 기초', description: null, classDate: shift(-8), startTime: '19:00', endTime: '20:00', capacity: 4, status: '폐강', cancelReason: null, canceledAt: at(shift(-9), '19:00'), createdAt: at(shift(-14), '10:00') }
  ]

  const r = (id, o) => ({ id, ptPassId: 1, userId: 1, trainerId: 2, groupClassId: null, status: '예약', canceledBy: null, cancelReason: null, canceledAt: null, isDeducted: false, ...o })
  const p1 = nextDow(2, 2)
  const reservations = [
    r(1, { lessonDate: shift(-12), startTime: '10:00', endTime: '11:00', status: '출석', isDeducted: true, reservedAt: at(shift(-14), '09:00') }),
    r(2, { lessonDate: shift(-9), startTime: '11:00', endTime: '12:00', status: '결석', isDeducted: true, reservedAt: at(shift(-11), '09:00') }),
    r(3, { lessonDate: shift(-5), startTime: '15:00', endTime: '16:00', status: '출석', isDeducted: true, reservedAt: at(shift(-7), '09:00') }),
    r(4, { lessonDate: shift(-1), startTime: '15:00', endTime: '16:00', status: '결과대기', reservedAt: at(shift(-3), '09:00') }),
    r(5, { lessonDate: shift(p1), startTime: '14:00', endTime: '15:00', reservedAt: at(shift(-1), '20:00') }),
    r(6, { ptPassId: 2, groupClassId: 1, lessonDate: shift(g1), startTime: '10:00', endTime: '11:00', reservedAt: at(shift(-2), '20:00') }),
    r(7, { ptPassId: 3, userId: 5, groupClassId: 1, lessonDate: shift(g1), startTime: '10:00', endTime: '11:00', reservedAt: at(shift(-2), '21:00') }),
    r(8, { ptPassId: 2, trainerId: 6, groupClassId: 4, lessonDate: shift(-8), startTime: '19:00', endTime: '20:00', status: '취소', canceledBy: '폐강', canceledAt: at(shift(-9), '19:00'), reservedAt: at(shift(-12), '10:00') }),
    r(9, { ptPassId: 4, userId: 7, lessonDate: shift(nextDow(1, 4)), startTime: '16:00', endTime: '17:00', reservedAt: at(shift(-1), '10:00') })
  ]

  const attendances = []
  let aid = 1
  for (const [d, t, uid] of [[-12, '09:42', 1], [-9, '10:48', 1], [-7, '19:05', 1], [-6, '19:12', 1], [-5, '14:40', 1], [-4, '07:15', 5], [-3, '18:30', 5], [-3, '19:05', 1], [-2, '12:20', 5], [-1, '07:02', 1], [-1, '18:52', 5], [-1, '16:31', 7], [0, '07:10', 5]]) {
    attendances.push({ id: aid++, userId: uid, checkedInAt: at(shift(d), t), entryType: uid === 7 ? 'PT' : '회원권', reservationId: uid === 7 ? 9 : null })
  }

  return { users, settings, membershipProducts, ptProducts, payments, memberships, ptPasses, schedules, daysOff: [], groupClasses, reservations, attendances }
}
