import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { auth } from '../api/endpoints'
import { onAuthEvent } from '../api/client'
import { setMockUser } from '../api/mock'
import { roleKey, isApproved } from './format'

const AuthCtx = createContext(null)

export const HOME = { MEMBER: '/member', TRAINER: '/trainer', ADMIN: '/admin' }

function normalize(me) {
  if (!me) return null
  return { ...me, role: roleKey(me.role), approved: isApproved(me.approvalStatus) }
}

export function landingFor(me) {
  if (!me) return '/login'
  if (me.mustChangePassword) return '/password-change'
  if (me.role === 'TRAINER' && !me.approved) return '/pending'
  return HOME[me.role] || '/login'
}

export function AuthProvider({ children }) {
  const [me, setMeRaw] = useState(undefined)
  const navigate = useNavigate()

  const setMe = useCallback(v => {
    const n = normalize(v)
    setMockUser(n?.userId ?? null)
    setMeRaw(n)
    return n
  }, [])

  const refresh = useCallback(async () => {
    try {
      return setMe(await auth.me())
    } catch {
      return setMe(null)
    }
  }, [setMe])

  useEffect(() => {
    refresh()
  }, [refresh])

  useEffect(() => onAuthEvent(code => {
    if (code === 'UNAUTHORIZED') {
      setMe(null)
      navigate('/login', { replace: true })
    } else if (code === 'TRAINER_NOT_APPROVED') {
      navigate('/pending', { replace: true })
    } else if (code === 'PASSWORD_CHANGE_REQUIRED') {
      refresh().then(() => navigate('/password-change', { replace: true }))
    }
  }), [navigate, refresh, setMe])

  const login = useCallback(async (loginId, password) => setMe(await auth.login(loginId, password)), [setMe])

  const logout = useCallback(async () => {
    try { await auth.logout() } catch {}
    setMe(null)
    navigate('/login', { replace: true })
  }, [navigate, setMe])

  return <AuthCtx.Provider value={{ me, setMe, refresh, login, logout }}>{children}</AuthCtx.Provider>
}

export const useAuth = () => useContext(AuthCtx)
