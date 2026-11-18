import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth, landingFor } from '../../lib/auth'
import { Field } from '../../components/ui'
import { MockBadge } from '../../components/Layout'
import { mockMode, mockAccounts, resetMockDb } from '../../api/mock'
import heroMarkup from '../../heroMarkup'

export function Hero() {
  return <div className="auth-hero" dangerouslySetInnerHTML={{ __html: heroMarkup }} />
}

export default function Login() {
  const { me, login } = useAuth()
  const nav = useNavigate()
  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)

  if (me) return <Navigate to={landingFor(me)} replace />

  const submit = async e => {
    e.preventDefault()
    if (!loginId.trim() || !password) {
      setErr('아이디와 비밀번호를 입력해 주세요.')
      return
    }
    setBusy(true)
    setErr('')
    try {
      const u = await login(loginId.trim(), password)
      nav(landingFor(u), { replace: true })
    } catch (e2) {
      setErr(e2.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="auth-wrap">
      <Hero />
      <section className="auth-side">
        <form className="auth-card" onSubmit={submit} noValidate>
          <div className="row between">
            <h2>로그인</h2>
            <MockBadge />
          </div>
          <Field label="아이디">
            <input className="input" autoComplete="username" placeholder="아이디를 입력하세요" value={loginId} onChange={e => setLoginId(e.target.value)} />
          </Field>
          <Field label="비밀번호">
            <input className="input" type="password" autoComplete="current-password" placeholder="비밀번호를 입력하세요" value={password} onChange={e => setPassword(e.target.value)} />
          </Field>
          {err && <div className="alert bad" role="alert">{err}</div>}
          <button className="btn primary lg block" disabled={busy}>{busy ? '로그인 중…' : '로그인'}</button>
          <div className="row">
            <span className="muted small">처음이세요?</span>
            <Link to="/signup" style={{ color: 'var(--text)', fontWeight: 700, fontSize: 14, textDecoration: 'none' }}>회원가입</Link>
          </div>
        </form>
        <div style={{ padding: '0 4px' }}>
          <Link to="/kiosk" style={{ color: 'var(--text)', fontWeight: 700, fontSize: 14, textDecoration: 'none' }}>입구 키오스크 열기</Link>
        </div>
        {mockMode === 'all' && (
          <div className="alert info small">
            목업 계정 (비밀번호 모두 test1234)
            <div className="row" style={{ marginTop: 8 }}>
              {mockAccounts.map(([id, label]) => (
                <button key={id} type="button" className="btn sm" onClick={() => { setLoginId(id); setPassword('test1234') }}>{label}</button>
              ))}
              <button type="button" className="btn sm danger" onClick={() => { resetMockDb(); setErr('') }}>목업 데이터 초기화</button>
            </div>
          </div>
        )}
      </section>
    </div>
  )
}
