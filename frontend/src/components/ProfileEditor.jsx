import { useState } from 'react'
import { users, auth } from '../api/endpoints'
import { useAsync } from '../lib/hooks'
import { useAuth } from '../lib/auth'
import { Async, Field, Modal, PageTitle, Seg, useToast } from './ui'
import { PhoneInput, phoneError, passwordError } from './inputs'
import { fmtDate, fmtPhone, genderKey, genderLabel, today } from '../lib/format'

export default function ProfileEditor({ trainer }) {
  const me = useAsync(() => users.me(), [])
  const [edit, setEdit] = useState(false)
  const [pw, setPw] = useState(false)
  return (
    <main className="page narrow">
      <PageTitle title="내 정보" sub="아이디는 바꿀 수 없어요." />
      <Async state={me}>
        {u => (
          <>
            <section className="card">
              <div className="row between">
                <h3>기본 정보</h3>
                <button className="btn sm" onClick={() => setEdit(true)}>개인정보 변경</button>
              </div>
              <dl className="kv">
                <dt>이름</dt><dd>{u.name}</dd>
                <dt>아이디</dt><dd>{u.loginId}</dd>
                <dt>전화번호</dt><dd>{fmtPhone(u.phone)}</dd>
                <dt>생년월일</dt><dd>{u.birthDate ? fmtDate(u.birthDate) : '-'}</dd>
                <dt>성별</dt><dd>{genderLabel(u.gender)}</dd>
                {trainer && <><dt>경력</dt><dd style={{ fontWeight: 500 }}>{u.career || <span className="muted">등록된 정보가 없어요</span>}</dd></>}
                {trainer && <><dt>소개</dt><dd style={{ fontWeight: 500, lineHeight: 1.6 }}>{u.introduction || <span className="muted">등록된 정보가 없어요</span>}</dd></>}
                <dt>가입일</dt><dd>{fmtDate(u.createdAt)}</dd>
              </dl>
            </section>
            <section className="card">
              <div className="row between">
                <div>
                  <h3>비밀번호</h3>
                  <span className="muted small">현재 비밀번호를 확인한 뒤에 바꿀 수 있어요.</span>
                </div>
                <button className="btn sm" onClick={() => setPw(true)}>비밀번호 변경</button>
              </div>
            </section>
            {edit && <EditModal u={u} trainer={trainer} onClose={() => setEdit(false)} onSaved={v => { me.setData(v); setEdit(false) }} />}
            {pw && <PasswordModal onClose={() => setPw(false)} />}
          </>
        )}
      </Async>
    </main>
  )
}

function EditModal({ u, trainer, onClose, onSaved }) {
  const toast = useToast()
  const { refresh } = useAuth()
  const [f, setF] = useState({ name: u.name, phone: u.phone, birthDate: u.birthDate || '', gender: genderKey(u.gender), career: u.career || '', introduction: u.introduction || '' })
  const [errs, setErrs] = useState({})
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (k, v) => { setF(s => ({ ...s, [k]: v })); setErrs(e => ({ ...e, [k]: '' })) }

  const save = async () => {
    const v = {
      name: f.name.trim() ? '' : '이름을 입력해 주세요.',
      phone: phoneError(f.phone),
      birthDate: f.birthDate && f.birthDate > today() ? '생년월일은 오늘 이후로 정할 수 없어요.' : '',
      career: f.career.length > 200 ? '경력은 200자 이하로 입력해 주세요.' : '',
      introduction: f.introduction.length > 1000 ? '소개는 1000자 이하로 입력해 주세요.' : ''
    }
    setErrs(v)
    if (Object.values(v).some(Boolean)) return
    const body = {}
    if (f.name.trim() !== u.name) body.name = f.name.trim()
    if (f.phone !== u.phone) body.phone = f.phone
    if ((f.birthDate || null) !== (u.birthDate || null) && f.birthDate) body.birthDate = f.birthDate
    if (f.gender && f.gender !== genderKey(u.gender)) body.gender = f.gender
    if (trainer && f.career !== (u.career || '')) body.career = f.career
    if (trainer && f.introduction !== (u.introduction || '')) body.introduction = f.introduction
    if (!Object.keys(body).length) {
      onClose()
      return
    }
    setBusy(true)
    setErr('')
    try {
      const r = await users.updateMe(body)
      toast('내 정보를 저장했어요.')
      refresh()
      onSaved(r)
    } catch (e) {
      if (e.code === 'DUPLICATE_PHONE') setErrs(s => ({ ...s, phone: e.message }))
      else setErr(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal title="개인정보 변경" onClose={onClose} wide={trainer}>
      <Field label="아이디" hint="아이디는 바꿀 수 없어요.">
        <input className="input" value={u.loginId} disabled />
      </Field>
      <Field label="이름" error={errs.name}>
        <input className="input" value={f.name} onChange={e => set('name', e.target.value)} />
      </Field>
      <Field label="전화번호" error={errs.phone}>
        <PhoneInput value={f.phone} onChange={v => set('phone', v)} />
      </Field>
      <div className="grid g2" style={{ gap: 16 }}>
        <Field label="생년월일" error={errs.birthDate}>
          <input className="input" type="date" max={today()} value={f.birthDate} onChange={e => set('birthDate', e.target.value)} />
        </Field>
        <div className="field">
          성별
          <Seg value={f.gender} options={[['MALE', '남'], ['FEMALE', '여']]} onChange={v => set('gender', v)} />
        </div>
      </div>
      {trainer && (
        <>
          <Field label="경력 (선택)" error={errs.career} hint={`${f.career.length}/200`}>
            <input className="input" value={f.career} onChange={e => set('career', e.target.value)} placeholder="예: 생활체육지도사 2급 · 경력 6년" />
          </Field>
          <Field label="소개 (선택)" error={errs.introduction} hint={`${f.introduction.length}/1000`}>
            <textarea className="textarea" value={f.introduction} onChange={e => set('introduction', e.target.value)} />
          </Field>
        </>
      )}
      {err && <div className="alert bad">{err}</div>}
      <div className="row end">
        <button className="btn" onClick={onClose} disabled={busy}>취소</button>
        <button className="btn primary" onClick={save} disabled={busy}>{busy ? '저장 중…' : '내 정보 저장'}</button>
      </div>
    </Modal>
  )
}

export function PasswordModal({ onClose }) {
  const toast = useToast()
  const [f, setF] = useState({ cur: '', next: '', next2: '' })
  const [errs, setErrs] = useState({})
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const save = async () => {
    const v = {
      cur: f.cur ? '' : '현재 비밀번호를 입력해 주세요.',
      next: passwordError(f.next),
      next2: f.next2 !== f.next ? '비밀번호가 서로 달라요.' : ''
    }
    setErrs(v)
    if (Object.values(v).some(Boolean)) return
    setBusy(true)
    setErr('')
    try {
      await auth.changePassword(f.cur, f.next)
      toast('비밀번호를 바꿨어요.')
      onClose()
    } catch (e) {
      if (e.code === 'INCORRECT_PASSWORD') setErrs(s => ({ ...s, cur: e.message }))
      else setErr(e.message)
    } finally {
      setBusy(false)
    }
  }
  return (
    <Modal title="비밀번호 변경" onClose={onClose}>
      <p className="muted" style={{ margin: 0 }}>현재 비밀번호를 확인한 뒤 새 비밀번호로 바꿔요.</p>
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
      <div className="row end">
        <button className="btn" onClick={onClose} disabled={busy}>취소</button>
        <button className="btn primary" onClick={save} disabled={busy}>{busy ? '바꾸는 중…' : '비밀번호 바꾸기'}</button>
      </div>
    </Modal>
  )
}
