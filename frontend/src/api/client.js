import { isMocked, mockHandle } from './mock'

export class ApiError extends Error {
  constructor(status, code, message, data = null) {
    super(message)
    this.status = status
    this.code = code
    this.data = data
  }
}

const listeners = new Set()
export function onAuthEvent(fn) {
  listeners.add(fn)
  return () => listeners.delete(fn)
}
function emit(code) {
  listeners.forEach(fn => fn(code))
}

const AUTH_CODES = ['UNAUTHORIZED', 'TRAINER_NOT_APPROVED', 'PASSWORD_CHANGE_REQUIRED']
const SILENT_PATHS = ['/auth/login', '/auth/me', '/auth/logout', '/kiosk/checkins']

function buildQuery(query) {
  if (!query) return ''
  const entries = Object.entries(query).filter(([, v]) => v !== undefined && v !== null && v !== '')
  if (!entries.length) return ''
  return '?' + new URLSearchParams(entries).toString()
}

function fallbackMessage(status) {
  if (status === 401) return '로그인이 필요합니다.'
  if (status === 403) return '접근 권한이 없습니다.'
  if (status === 404) return '요청한 데이터를 찾을 수 없습니다.'
  if (status === 409) return '요청을 처리할 수 없는 상태입니다.'
  if (status >= 500) return '서버 오류가 발생했습니다.'
  return '요청을 처리하지 못했어요.'
}

function fallbackCode(status) {
  if (status === 400) return 'INVALID_INPUT'
  if (status === 401) return 'UNAUTHORIZED'
  if (status === 403) return 'FORBIDDEN'
  if (status === 404) return 'NOT_FOUND'
  if (status === 409) return 'CONFLICT'
  return 'INTERNAL_ERROR'
}

export async function requestRaw(method, path, { query, body } = {}) {
  let status
  let json
  if (isMocked(method, path)) {
    const r = await mockHandle(method, path, query || {}, body)
    status = r.status
    json = r.json
  } else {
    let res
    try {
      res = await fetch('/api/v1' + path + buildQuery(query), {
        method,
        credentials: 'include',
        headers: body !== undefined ? { 'Content-Type': 'application/json' } : undefined,
        body: body !== undefined ? JSON.stringify(body) : undefined
      })
    } catch {
      throw new ApiError(0, 'NETWORK', '서버에 연결할 수 없어요. 백엔드가 켜져 있는지 확인해 주세요.')
    }
    status = res.status
    const text = await res.text()
    try {
      json = text ? JSON.parse(text) : null
    } catch {
      json = null
    }
  }
  if (status < 400 && json && json.success !== false) {
    return { data: json.data, message: json.message }
  }
  const code = json?.code || fallbackCode(status)
  const message = json?.message || (status >= 500 && !json ? '서버에 연결할 수 없어요. 백엔드가 켜져 있는지 확인해 주세요.' : fallbackMessage(status))
  if (AUTH_CODES.includes(code) && !SILENT_PATHS.some(p => path.startsWith(p))) emit(code)
  throw new ApiError(status, code, message)
}

export async function request(method, path, opts) {
  const r = await requestRaw(method, path, opts)
  return r.data
}

export const get = (path, query) => request('GET', path, { query })
export const post = (path, body) => request('POST', path, { body: body ?? {} })
export const patch = (path, body) => request('PATCH', path, { body })
export const del = path => request('DELETE', path)
