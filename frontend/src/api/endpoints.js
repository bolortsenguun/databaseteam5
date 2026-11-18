import { get, post, patch, del, requestRaw } from './client'

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

export const payments = {
  simulate: body => requestRaw('POST', '/payments/simulate', { body }),
  mine: query => get('/payments/me', query),
  adminList: query => get('/admin/payments', query)
}

export const passes = {
  mine: () => get('/passes/me'),
  clients: () => get('/trainers/me/clients')
}

export const attendance = {
  kiosk: phone => requestRaw('POST', '/kiosk/checkins', { body: { phone } }),
  mine: query => get('/attendances/me', query),
  adminList: query => get('/admin/attendances', query)
}

export const stats = {
  dashboard: () => get('/admin/dashboard'),
  statistics: query => get('/admin/statistics', query)
}

export const settings = {
  get: () => get('/admin/settings'),
  save: list => patch('/admin/settings', { settings: list })
}

export const trainers = {
  list: () => get('/trainers'),
  adminList: query => get('/admin/trainers', query),
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
