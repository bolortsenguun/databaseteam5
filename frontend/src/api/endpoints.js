import { get, post, patch, del, requestRaw } from './client'
import { normalizeAttendancePage, normalizeClients, normalizeDashboard, normalizeKiosk, normalizePasses, normalizePaymentPage, normalizePaymentResult, normalizeSettings, normalizeStatistics } from './adapters'

export const auth = {
  me: () => get('/auth/me'),
  login: (loginId, password) => post('/auth/login', { loginId, password }),
  logout: () => post('/auth/logout'),
  checkLoginId: loginId => get('/auth/check-login-id', { loginId }),
  register: body => post('/auth/register', body),
  changePassword: (currentPassword, newPassword) => patch('/auth/password', { currentPassword, newPassword }),
  resetPassword: userId => post(`/admin/users/${userId}/reset-password`)
}

export const users = {
  me: () => get('/users/me'),
  updateMe: body => patch('/users/me', body)
}

export const products = {
  list: () => get('/products'),
  adminList: () => get('/admin/products'),
  createMembership: body => post('/admin/membership-products', body),
  updateMembership: (id, body) => patch(`/admin/membership-products/${id}`, body),
  createPt: body => post('/admin/pt-products', body),
  updatePt: (id, body) => patch(`/admin/pt-products/${id}`, body)
}

const zeroBased = q => ({ ...q, page: Math.max(0, Number(q?.page ?? 1) - 1) })

export const payments = {
  simulate: async ({ kind, productId, trainerId, startDate, success }) => {
    const body = { purchaseType: kind === 'membership' ? 'MEMBERSHIP' : 'PT', productId, startDate, simulateFail: !success }
    if (kind === 'personal') body.trainerId = trainerId
    const r = await requestRaw('POST', '/payments/simulate', { body })
    return normalizePaymentResult(r.data)
  },
  mine: async query => normalizePaymentPage(await get('/payments/me', zeroBased(query))),
  adminList: async query => {
    const { status, userId, startDate, endDate, ...rest } = query || {}
    const filtered = (status && status !== '전체') || userId || startDate || endDate
    if (!filtered) return normalizePaymentPage(await get('/admin/payments', zeroBased(rest)))
    const all = normalizePaymentPage(await get('/admin/payments', { page: 0, size: 100 }))
    const list = all.payments.filter(x =>
      (!status || status === '전체' || x.status === status) &&
      (!userId || x.userId === Number(userId)) &&
      (!startDate || x.paidAt.slice(0, 10) >= startDate) &&
      (!endDate || x.paidAt.slice(0, 10) <= endDate))
    const size = Number(rest.size || 20)
    const page = Number(rest.page || 1)
    return { payments: list.slice((page - 1) * size, page * size), page, size, totalCount: list.length, totalPages: Math.max(1, Math.ceil(list.length / size)), clientFiltered: true }
  }
}

export const passes = {
  mine: async () => normalizePasses(await get('/passes/me')),
  clients: async () => normalizeClients(await get('/trainers/me/clients'))
}

export const attendance = {
  kiosk: async phone => {
    const r = await requestRaw('POST', '/kiosk/checkins', { body: { phone } })
    return normalizeKiosk(r.data)
  },
  mine: async query => {
    const { startDate, endDate, page, size } = query || {}
    const d = await get('/attendances/me', { from: startDate, to: endDate, startDate, endDate, page: Math.max(0, Number(page ?? 1) - 1), size })
    return normalizeAttendancePage(d)
  },
  adminList: async query => {
    const { date, startDate, endDate, entryType, userId, page = 1, size = 20 } = query || {}
    const day = date || (startDate && startDate === endDate ? startDate : startDate || endDate)
    const d = normalizeAttendancePage(await get('/admin/attendances', { date: day, startDate, endDate, entryType, userId, page, size }), true)
    if (d.totalPages > 1 || (d.size && d.size < d.totalCount)) return d
    const list = d.attendances.filter(a => (!entryType || entryType === '전체' || a.entryType === entryType) && (!userId || a.userId === Number(userId)))
    const s = Number(size)
    const p = Number(page)
    return { attendances: list.slice((p - 1) * s, p * s), page: p, size: s, totalCount: list.length, totalPages: Math.max(1, Math.ceil(list.length / s)), singleDay: true }
  }
}

const safe = p => p.catch(() => null)

export const stats = {
  dashboard: async () => {
    const d = await get('/admin/dashboard')
    if (d.totalMembers !== undefined && d.monthlyRevenue !== undefined) return normalizeDashboard(d)
    const [pending, approved, mem, st] = await Promise.all([
      safe(get('/admin/trainers', { approvalStatus: '승인대기', page: 1, size: 1 })),
      safe(get('/admin/trainers', { approvalStatus: '승인', page: 1, size: 1 })),
      safe(get('/admin/members', { page: 0, size: 1 })),
      safe(get('/admin/statistics'))
    ])
    return normalizeDashboard(d, {
      pendingTrainers: pending?.totalCount ?? pending?.totalElements,
      approvedTrainers: approved?.totalCount ?? approved?.totalElements,
      totalMembers: mem?.totalElements ?? mem?.totalCount,
      monthlyRevenue: st ? normalizeStatistics(st).totalRevenue : null
    })
  },
  statistics: async query => {
    const { startDate, endDate, ...rest } = query || {}
    return normalizeStatistics(await get('/admin/statistics', { from: startDate, to: endDate, startDate, endDate, ...rest }))
  }
}

export const settings = {
  get: async () => normalizeSettings(await get('/admin/settings')),
  save: list => patch('/admin/settings', { settings: list.map(s => ({ key: s.settingKey, value: s.settingValue, settingKey: s.settingKey, settingValue: s.settingValue })) })
}

export const trainers = {
  list: () => get('/trainers'),
  adminList: query => get('/admin/trainers', query),
  adminDetail: id => get(`/admin/trainers/${id}`),
  approve: id => post(`/admin/trainers/${id}/approve`),
  reject: id => post(`/admin/trainers/${id}/reject`),
  adminUpdate: (id, body) => patch(`/admin/trainers/${id}`, body),
  schedules: id => get(`/trainers/${id}/schedules`),
  addSchedule: (id, body) => post(`/trainers/${id}/schedules`, body),
  deleteSchedule: (id, scheduleId) => del(`/trainers/${id}/schedules/${scheduleId}`),
  addDayOff: (id, offDate) => post(`/trainers/${id}/days-off`, { offDate }),
  deleteDayOff: (id, dayOffId) => del(`/trainers/${id}/days-off/${dayOffId}`)
}

export const groups = {
  memberList: query => get('/group-classes', query),
  mine: query => get('/trainers/me/group-classes', query),
  availableSlots: classDate => get('/trainers/me/group-class-available-slots', { classDate }),
  create: body => post('/group-classes', body),
  update: (id, body) => patch(`/group-classes/${id}`, body),
  remove: id => del(`/group-classes/${id}`),
  cancel: (id, cancelReason) => post(`/group-classes/${id}/cancel`, { cancelReason })
}

export const reservations = {
  slots: (trainerId, date) => get('/pt-reservations/available-slots', { trainerId, date }),
  reserveIndividual: body => post('/pt-reservations/individual', body),
  reserveGroup: classId => post('/pt-reservations/group', { classId }),
  mine: query => get('/pt-reservations/me', query),
  preview: id => get(`/pt-reservations/${id}/cancellation-preview`),
  cancel: (id, acceptDeduction) => requestRaw('POST', `/pt-reservations/${id}/cancel`, { body: { acceptDeduction } }),
  lessons: query => get('/trainers/me/lessons', query),
  result: (id, result) => requestRaw('PATCH', `/pt-reservations/${id}/result`, { body: { result } }),
  trainerCancel: (id, cancelReason) => post(`/pt-reservations/${id}/trainer-cancel`, { cancelReason }),
  adminList: query => get('/admin/pt-reservations', query),
  adminCancel: (id, cancelReason) => post(`/admin/pt-reservations/${id}/cancel`, { cancelReason })
}

export const members = {
  list: query => get('/admin/members', query),
  detail: id => get(`/admin/members/${id}`),
  update: (id, body) => patch(`/admin/members/${id}`, body)
}
