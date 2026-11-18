import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { auth } from '../../api/endpoints'
import { Field, Seg, useToast } from '../../components/ui'
import { PhoneInput, phoneError, passwordError, loginIdError } from '../../components/inputs'
import { today } from '../../lib/format'
import { Hero } from './Login'

export default function SignupForm({ role }) {
  const nav = useNavigate()
  const toast = useToast()
  const trainer = role === 'TRAINER'
  const [f, setF] = useState({ loginId: '', name: '', password: '', password2: '', phone: '', birthDate: '', gender: null })
  const [checked, setChecked] = useState(null)
  const [checking, setChecking] = useState(false)
  const [errs, setErrs] = useState({})
  const [busy, setBusy] = useState(false)
  const [formErr, setFormErr] = useState('')

  const set = (k, v) => {
    setF(s => ({ ...s, [k]: v }))
    setErrs(e => ({ ...e, [k]: '' }))
    if (k === 'loginId') setChecked(null)
  }

  const check = async () => {
    const e = loginIdError(f.loginId)
    if (e) {
      setErrs(s => ({ ...s, loginId: e }))
      return
    }
    setChecking(true)
    try {
      const r = await auth.checkLoginId(f.loginId)
      if (r.available) setChecked(f.loginId)
      else {
        setChecked(null)
        setErrs(s => ({ ...s, loginId: '이미 사용 중인 아이디예요.' }))
      }
    } catch (e2) {
      setErrs(s => ({ ...s, loginId: e2.message }))
    } finally {
      setChecking(false)
    }
  }

  const validate = () => {
    const e = {}
    e.loginId = loginIdError(f.loginId) || (checked !== f.loginId ? '아이디 중복 확인을 해 주세요.' : '')
    e.name = f.name.trim() ? (f.name.trim().length > 50 ? '이름은 50자 이하로 입력해 주세요.' : '') : '이름을 입력해 주세요.'
    e.password = passwordError(f.password)
    e.password2 = f.password2 !== f.password ? '비밀번호가 서로 달라요.' : ''
    e.phone = phoneError(f.phone)
    e.birthDate = f.birthDate && f.birthDate > today() ? '생년월일은 오늘 이후로 정할 수 없어요.' : ''
    setErrs(e)
    return !Object.values(e).some(Boolean)
  }

  const submit = async ev => {
    ev.preventDefault()
    setFormErr('')
    if (!validate()) return
    setBusy(true)
    try {
      await auth.register({
        role,
        loginId: f.loginId,
        password: f.password,
        name: f.name.trim(),
        phone: f.phone,
        birthDate: f.birthDate || null,
        gender: f.gender
      })
      if (trainer) nav('/pending', { replace: true, state: { name: f.name.trim(), justSignedUp: true } })
      else {
        toast('가입이 완료됐어요. 로그인해 주세요.')
        nav('/login', { replace: true })
      }
    } catch (e2) {
      if (e2.code === 'DUPLICATE_LOGIN_ID') {
        setChecked(null)
        setErrs(s => ({ ...s, loginId: e2.message }))
      } else if (e2.code === 'DUPLICATE_PHONE') setErrs(s => ({ ...s, phone: e2.message }))
      else setFormErr(e2.message)
    } finally {
      setBusy(false)
    }
  }

  const idOk = checked && checked === f.loginId

  return (
    <div className="auth-wrap">
      <Hero />
      <section className="auth-side">
        <Link to="/signup" className="small" style={{ textDecoration: 'none' }}>← 가입 유형 다시 고르기</Link>
        <form className="auth-card" onSubmit={submit} noValidate>
          <h2>{trainer ? '트레이너 가입' : '회원 가입'}</h2>
          <Field label="아이디" error={errs.loginId} ok={idOk ? '쓸 수 있는 아이디예요.' : ''} hint="영문, 숫자, 밑줄(_) 4~20자. 가입 뒤에는 바꿀 수 없어요.">
            <div className="inline">
              <input className={`input ${errs.loginId ? 'bad' : ''}`} autoComplete="username" value={f.loginId} onChange={e => set('loginId', e.target.value.trim())} />
              <button type="button" className="btn" onClick={check} disabled={checking || !f.loginId}>{checking ? '확인 중…' : '중복 확인'}</button>
            </div>
          </Field>
          <Field label="이름" error={errs.name}>
            <input className={`input ${errs.name ? 'bad' : ''}`} autoComplete="name" value={f.name} onChange={e => set('name', e.target.value)} />
          </Field>
          <Field label="비밀번호" error={errs.password} hint="8자 이상">
            <input className={`input ${errs.password ? 'bad' : ''}`} type="password" autoComplete="new-password" value={f.password} onChange={e => set('password', e.target.value)} />
          </Field>
          <Field label="비밀번호 확인" error={errs.password2}>
            <input className={`input ${errs.password2 ? 'bad' : ''}`} type="password" autoComplete="new-password" value={f.password2} onChange={e => set('password2', e.target.value)} />
          </Field>
          <Field label="전화번호" error={errs.phone} hint="키오스크 체크인에 써요.">
            <PhoneInput value={f.phone} onChange={v => set('phone', v)} />
          </Field>
          <div className="grid g2" style={{ gap: 16 }}>
            <Field label="생년월일 (선택)" error={errs.birthDate}>
              <input className="input" type="date" max={today()} value={f.birthDate} onChange={e => set('birthDate', e.target.value)} />
            </Field>
            <div className="field">
              성별 (선택)
              <Seg value={f.gender} options={[['MALE', '남'], ['FEMALE', '여']]} onChange={v => set('gender', f.gender === v ? null : v)} />
            </div>
          </div>
          {trainer && <div className="alert info small">가입 신청 뒤에는 관리자가 승인할 때까지 로그인해도 안내 화면만 보여요.</div>}
          {formErr && <div className="alert bad" role="alert">{formErr}</div>}
          <div className="row">
            <button className="btn primary lg" disabled={busy || !idOk}>{busy ? '처리 중…' : trainer ? '가입 신청하기' : '가입하기'}</button>
            <Link to="/signup" className="btn lg">이전으로</Link>
          </div>
          {!idOk && <span className="muted small">아이디 중복 확인을 마쳐야 가입할 수 있어요.</span>}
        </form>
      </section>
    </div>
  )
}
