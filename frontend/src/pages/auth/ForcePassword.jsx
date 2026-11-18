import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth, landingFor } from '../../lib/auth'
import { auth } from '../../api/endpoints'
import { Field, Loading, useToast } from '../../components/ui'
import { passwordError } from '../../components/inputs'
import { Brand } from '../../components/Layout'

export default function ForcePassword() {
  const { me, refresh, logout } = useAuth()
  const nav = useNavigate()
  const toast = useToast()
  const [f, setF] = useState({ cur: '', next: '', next2: '' })
  const [errs, setErrs] = useState({})
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)

  if (me === undefined) return <Loading />
  if (!me) return <Navigate to="/login" replace />
  if (!me.mustChangePassword) return <Navigate to={landingFor(me)} replace />

  const submit = async e => {
    e.preventDefault()
    const v = {
      cur: f.cur ? '' : '지금 쓰는 비밀번호(임시 비밀번호)를 입력해 주세요.',
      next: passwordError(f.next) || (f.next === f.cur ? '새 비밀번호는 지금 비밀번호와 달라야 해요.' : ''),
      next2: f.next2 !== f.next ? '비밀번호가 서로 달라요.' : ''
    }
    setErrs(v)
    if (Object.values(v).some(Boolean)) return
    setBusy(true)
    setErr('')
    try {
      await auth.changePassword(f.cur, f.next)
      const u = await refresh()
      toast('비밀번호를 바꿨어요.')
      nav(landingFor(u), { replace: true })
    } catch (e2) {
      if (e2.code === 'INCORRECT_PASSWORD') setErrs(s => ({ ...s, cur: e2.message }))
      else setErr(e2.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="kiosk">
      <Brand to="/login" />
      <form className="auth-card" style={{ maxWidth: 520, width: '100%' }} onSubmit={submit} noValidate>
        <h2>비밀번호를 바꿔 주세요</h2>
        <p className="muted" style={{ margin: 0, lineHeight: 1.6 }}>
          {me.role === 'ADMIN' ? '처음 로그인할 때는 비밀번호를 꼭 바꿔야 해요. 바꾼 뒤에 관리자 화면으로 이동해요.' : '비밀번호가 초기화됐어요. 새 비밀번호로 바꾼 뒤에 쓸 수 있어요.'}
        </p>
        <Field label="현재 비밀번호" error={errs.cur}>
          <input className="input" type="password" autoComplete="current-password" value={f.cur} onChange={e => setF({ ...f, cur: e.target.value })} />
        </Field>
        <Field label="새 비밀번호" error={errs.next} hint="8자 이상">
          <input className="input" type="password" autoComplete="new-password" value={f.next} onChange={e => setF({ ...f, next: e.target.value })} />
        </Field>
        <Field label="새 비밀번호 확인" error={errs.next2}>
          <input className="input" type="password" autoComplete="new-password" value={f.next2} onChange={e => setF({ ...f, next2: e.target.value })} />
        </Field>
        {err && <div className="alert bad">{err}</div>}
        <button className="btn primary lg block" disabled={busy}>{busy ? '바꾸는 중…' : '비밀번호 바꾸기'}</button>
        <button type="button" className="linkbtn" onClick={logout}>로그아웃</button>
      </form>
    </div>
  )
}
