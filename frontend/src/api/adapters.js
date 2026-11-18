import { diffDays, today } from '../lib/format'

const PASS_STATUS = { BEFORE_START: '이용예정', ACTIVE: '이용중', EXHAUSTED: '소진', EXPIRED: '만료' }
const PAY_STATUS = { SUCCESS: '성공', FAIL: '실패' }
const PT_TYPE = { PERSONAL: '개인', GROUP: '그룹' }
const PURCHASE = { MEMBERSHIP: '회원권', PT: 'PT' }

const passStatus = s => PASS_STATUS[s] || s
export const payStatus = s => PAY_STATUS[s] || s
const ptType = s => PT_TYPE[s] || s

function statusByDates(startDate, endDate) {
  const t = today()
  if (t < startDate) return '이용예정'
  if (t > endDate) return '만료'
  return '이용중'
}

export function normalizePasses(d) {
  const memberships = (d?.memberships || []).map(m => {
    const status = m.status ? passStatus(m.status) : statusByDates(m.startDate, m.endDate)
    return { ...m, status, remainingDays: m.remainingDays ?? (status === '이용중' ? diffDays(today(), m.endDate) : null) }
  }).sort((a, b) => a.startDate.localeCompare(b.startDate) || a.membershipId - b.membershipId)
  const ptPasses = (d?.ptPasses || []).map(p => {
    const status = p.status ? passStatus(p.status) : statusByDates(p.startDate, p.endDate)
    const reserved = Number(p.reservedCount ?? 0)
    const available = status === '만료' ? 0 : Number(p.availableCount ?? Math.max(0, p.remainingCount - reserved))
    return { ...p, ptType: ptType(p.ptType), status, reservedCount: reserved, availableCount: available, usedCount: p.usedCount ?? p.totalCount - p.remainingCount }
  }).sort((a, b) => a.startDate.localeCompare(b.startDate) || a.ptPassId - b.ptPassId)
  return { memberships, ptPasses }
}

export function normalizeClients(d) {
  const clients = (d?.clients || []).map(c => ({
    ...c,
    ptPasses: (c.ptPasses || c.passes || []).map(p => ({
      ...p,
      status: p.status ? passStatus(p.status) : statusByDates(p.startDate, p.endDate),
      usedCount: p.usedCount ?? p.totalCount - p.remainingCount
    }))
  }))
  return { clients, totalClients: d?.totalClients ?? clients.length }
}

function paymentItem(x) {
  const productType = x.productType ?? (x.purchaseType ? PURCHASE[x.purchaseType] || x.purchaseType : null)
  return { ...x, status: payStatus(x.status), productType, productName: x.productName ?? null }
}

export function normalizePaymentPage(d, oneBased) {
  const page = Number(d?.page ?? 0)
  return {
    payments: (d?.payments || []).map(paymentItem),
    page: oneBased ? page : page + 1,
    size: d?.size,
    totalCount: d?.totalCount ?? d?.totalElements ?? 0,
    totalPages: d?.totalPages ?? 1
  }
}

export function normalizePaymentResult(d) {
  return {
    paymentId: d.paymentId,
    status: payStatus(d.status),
    amount: d.amount,
    paidAt: d.paidAt,
    productName: d.productName ?? null,
    startDate: d.startDate ?? d.pass?.startDate ?? null,
    endDate: d.endDate ?? d.pass?.endDate ?? null
  }
}

const ENTRY = { MEMBERSHIP: '회원권', PT: 'PT' }
const entryType = s => ENTRY[s] || s

export function normalizeKiosk(d, status) {
  return {
    name: d.name ?? d.userName,
    entryType: entryType(d.entryType),
    checkedInAt: d.checkedInAt,
    duplicate: d.duplicate ?? (d.recorded === false || status === 200),
    message: d.message
  }
}

export function normalizeAttendancePage(d, oneBased) {
  const page = Number(d?.page ?? 0)
  const list = (d?.attendances || []).map(a => ({ ...a, entryType: entryType(a.entryType), userName: a.userName ?? a.name }))
  return {
    attendances: list,
    page: d?.page === undefined ? 1 : oneBased ? page : page + 1,
    size: d?.size ?? list.length,
    totalCount: d?.totalCount ?? d?.totalElements ?? list.length,
    totalPages: d?.totalPages ?? 1
  }
}

export function normalizeDashboard(d, extra = {}) {
  return {
    todayRevenue: d.todayRevenue ?? 0,
    monthlyRevenue: d.monthlyRevenue ?? extra.monthlyRevenue ?? null,
    totalMembers: d.totalMembers ?? extra.totalMembers ?? null,
    activeMembershipMembers: d.activeMembershipMembers ?? d.activeMemberCount ?? 0,
    approvedTrainers: d.approvedTrainers ?? extra.approvedTrainers ?? null,
    pendingTrainers: d.pendingTrainers ?? extra.pendingTrainers ?? null,
    todayReservations: d.todayReservations ?? d.todayPtCount ?? 0,
    todayCheckins: d.todayCheckins ?? d.todayEntryCount ?? 0,
    pendingResultCount: d.pendingResultCount ?? 0,
    closingSoonClassCount: d.closingSoonClassCount ?? 0,
    asOf: d.asOf ?? null
  }
}

export function normalizeStatistics(d) {
  const monthly = (d.monthlyRevenue || []).map(m => ({ month: m.month, amount: m.amount ?? m.revenue ?? 0, successCount: m.successCount ?? m.paymentCount ?? 0 }))
  const split = (d.trainerPerformance || []).some(t => t.individualAttendanceCount !== undefined)
  const trainerColumns = split
    ? [['individualAttendanceCount', '개인 출석'], ['individualAbsenceCount', '개인 결석'], ['groupClassesHeld', '그룹 수업'], ['groupAttendanceCount', '그룹 출석'], ['groupAbsenceCount', '그룹 결석'], ['totalProcessedCount', '합계']]
    : [['attendedCount', '출석'], ['absentCount', '결석'], ['canceledCount', '취소'], ['upcomingCount', '예정']]
  return {
    period: d.period ?? { startDate: d.from, endDate: d.to },
    totalRevenue: d.totalRevenue ?? monthly.reduce((s, m) => s + m.amount, 0),
    monthlyRevenue: monthly,
    trainerPerformance: d.trainerPerformance || [],
    trainerColumns,
    hourlyCheckins: d.hourlyCheckins ?? d.hourlyEntries ?? []
  }
}

export function normalizeSettings(d) {
  return { settings: (d?.settings || []).map(s => ({ settingKey: s.settingKey ?? s.key, settingValue: s.settingValue ?? s.value, description: s.description ?? null })) }
}
