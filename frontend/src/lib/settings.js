import { useEffect, useState } from 'react'
import { settings as settingsApi } from '../api/endpoints'

// 운영 설정 기본값 (db/schema.sql 초기값과 같음). 서버 값을 받기 전이나 못 받았을 때 씀
export const SETTING_DEFAULTS = {
  CANCEL_DEADLINE_HOURS: 24,
  FREE_CANCEL_MINUTES: 10,
  BOOKING_RANGE_DAYS: 14,
  BOOKING_CLOSE_HOURS: 1,
  SLOT_MINUTES: 60,
  GROUP_OPEN_MIN_HOURS: 48,
  GROUP_OPEN_MAX_DAYS: 30,
  GROUP_MIN_PARTICIPANTS: 2,
  GROUP_CONFIRM_HOURS: 24,
  PT_ENTRY_BEFORE_MINUTES: 30,
  PT_ENTRY_AFTER_MINUTES: 30,
  RECHECKIN_MINUTES: 10,
  PASS_START_MAX_DAYS: 30
}

// 한 번 받아 온 값을 화면 전체가 같이 씀 (관리자가 저장하면 다시 받음)
let current = SETTING_DEFAULTS
let loading = null
const listeners = new Set()

function load() {
  if (!loading) {
    loading = settingsApi.public()
      .then(d => {
        current = { ...SETTING_DEFAULTS, ...(d?.settings || {}) }
        listeners.forEach(fn => fn(current))
      })
      .catch(() => { loading = null })   // 실패하면 기본값 그대로, 다음 화면에서 다시 시도
  }
  return loading
}

// 관리자 운영 설정 저장 뒤 호출
export function refreshSettings() {
  loading = null
  return load()
}

// 로그인한 화면에서 쓰는 운영 설정 값. 처음엔 기본값, 서버 값이 오면 바뀜
export function useSettings() {
  const [s, setS] = useState(current)
  useEffect(() => {
    listeners.add(setS)
    load()
    return () => listeners.delete(setS)
  }, [])
  return s
}
