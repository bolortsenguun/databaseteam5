import { useEffect, useState } from 'react'
import { trainers } from '../../api/endpoints'
import { useAsync } from '../../lib/hooks'
import { Async, Confirm, Field, Modal, PageTitle, Seg, Tabs, useToast } from '../../components/ui'
import { PhoneInput, phoneError } from '../../components/inputs'
import ScheduleManager from '../../components/ScheduleManager'
import { ResetPassword } from './MemberDetail'
import { workText } from '../member/Trainers'
import { fmtDate, fmtPhone, genderKey, today } from '../../lib/format'

export default function AdminTrainers() {
  const toast = useToast()
  const pending = useAsync(() => trainers.adminList({ approvalStatus: '승인대기', page: 1, size: 50 }), [])
  const approved = useAsync(() => trainers.adminList({ approvalStatus: '승인', page: 1, size: 100 }), [])
  const [review, setReview] = useState(null)
  const [edit, setEdit] = useState(null)
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')

  const decide = async action => {
    setBusy(true)
    setErr('')
    try {
      if (action === 'approve') await trainers.approve(review.trainerId)
      else await trainers.reject(review.trainerId)
      toast(action === 'approve' ? `${review.name} 트레이너를 승인했어요.` : `${review.name} 님의 가입을 거절했어요.`)
      setReview(null)
      pending.reload()
      approved.reload()
    } catch (e) {
      setErr(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="page">
      <PageTitle title="트레이너" sub="승인이 끝난 트레이너만 근무 시간을 정하고 수업을 열 수 있어요." />
      <section className="card irid">
        <h3>승인 대기 {pending.data ? `${pending.data.totalCount}명` : ''}</h3>
        <Async state={pending} isEmpty={d => d.trainers.length === 0} empty="승인을 기다리는 트레이너가 없어요.">
          {d => (
            <table className="tbl">
              <tbody>
                {d.trainers.map(t => (
                  <tr key={t.trainerId}>
                    <td><b>{t.name}</b> <span className="muted small">{t.loginId}, {fmtPhone(t.phone)}, 가입일 {fmtDate(t.createdAt)}</span></td>
                    <td style={{ textAlign: 'right' }}>
                      <div className="row end" style={{ gap: 6 }}>
                        <button className="btn sm primary" onClick={() => { setErr(''); setReview({ ...t, action: 'approve' }) }}>승인</button>
                        <button className="btn sm" onClick={() => { setErr(''); setReview({ ...t, action: 'reject' }) }}>거절</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </Async>
      </section>
      <section className="card">
        <div className="row between">
          <h3>승인된 트레이너</h3>
          <span className="muted small">행을 누르면 정보를 수정할 수 있어요</span>
        </div>
        <Async state={approved} isEmpty={d => d.trainers.length === 0} empty="승인된 트레이너가 없어요.">
          {d => (
            <div className="tblwrap">
              <table className="tbl">
                <thead><tr><th>이름</th><th>아이디</th><th>전화번호</th><th>담당 회원</th><th>근무</th></tr></thead>
                <tbody>
                  {d.trainers.map(t => (
                    <tr key={t.trainerId} className="click" onClick={() => setEdit(t)}>
                      <td><b>{t.name}</b></td>
                      <td>{t.loginId}</td>
                      <td>{fmtPhone(t.phone)}</td>
                      <td className="num">{t.clientCount}명</td>
                      <td className="small">{workText(t.schedules)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Async>
      </section>
      {review && (
        <Confirm
          title={review.action === 'approve' ? `${review.name} 트레이너를 승인할까요?` : `${review.name} 님의 가입을 거절할까요?`}
          message={review.action === 'approve' ? '승인하면 다시 로그인한 뒤부터 트레이너 화면을 쓸 수 있어요.' : '거절하면 계정이 삭제되고, 같은 아이디와 전화번호로 다시 가입할 수 있어요.'}
          confirmText={review.action === 'approve' ? '승인하기' : '거절하기'}
          danger={review.action === 'reject'}
          busy={busy}
          error={err}
          onClose={() => setReview(null)}
          onConfirm={() => decide(review.action)}
        />
      )}
      {edit && <EditTrainer t={edit} onClose={() => setEdit(null)} onSaved={() => { setEdit(null); approved.reload() }} onScheduleChanged={approved.reload} />}
    </main>
  )
}

function EditTrainer({ t, onClose, onSaved, onScheduleChanged }) {
  const toast = useToast()
  const [tab, setTab] = useState('info')
  const [f, setF] = useState({ name: t.name, phone: t.phone, birthDate: '', gender: null, career: '', introduction: '' })
  const [touched, setTouched] = useState({})
  const [errs, setErrs] = useState({})
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const [reset, setReset] = useState(false)
  // 현재 값 불러오기: 'loading' → 성공하면 원래 값 객체, 실패하면 null (입력한 항목만 바꾸는 방식으로 동작)
  const [orig, setOrig] = useState('loading')
  useEffect(() => {
    let alive = true
    trainers.adminDetail(t.trainerId)
      .then(d => {
        if (!alive) return
        const o = { birthDate: d.birthDate || '', gender: genderKey(d.gender), career: d.career || '', introduction: d.introduction || '' }
        setOrig(o)
        setF(s => ({ ...s, ...o }))
      })
      .catch(() => { if (alive) setOrig(null) })
    return () => { alive = false }
  }, [t.trainerId])
  const loaded = orig && orig !== 'loading'
  const set = (k, v) => { setF(s => ({ ...s, [k]: v })); setTouched(s => ({ ...s, [k]: true })); setErrs(e => ({ ...e, [k]: '' })) }

  const save = async () => {
    const v = { name: f.name.trim() ? '' : '이름을 입력해 주세요.', phone: phoneError(f.phone), birthDate: f.birthDate && f.birthDate > today() ? '미래 날짜는 고를 수 없어요.' : '', career: f.career.length > 200 ? '200자 이하로 입력해 주세요.' : '', introduction: f.introduction.length > 1000 ? '1000자 이하로 입력해 주세요.' : '' }
    setErrs(v)
    if (Object.values(v).some(Boolean)) return
    const body = {}
    if (f.name.trim() !== t.name) body.name = f.name.trim()
    if (f.phone !== t.phone) body.phone = f.phone
    if (loaded) {
      // 원래 값과 달라진 항목만 보낸다. 비우면 null → 서버에서 지움
      if (f.birthDate !== orig.birthDate) body.birthDate = f.birthDate || null
      if (f.gender && f.gender !== orig.gender) body.gender = f.gender === 'MALE' ? '남' : '여'
      if (f.career.trim() !== orig.career) body.career = f.career.trim() || null
      if (f.introduction.trim() !== orig.introduction) body.introduction = f.introduction.trim() || null
    } else {
      if (touched.birthDate && f.birthDate) body.birthDate = f.birthDate
      if (touched.gender && f.gender) body.gender = f.gender === 'MALE' ? '남' : '여'
      if (touched.career) body.career = f.career.trim() || null
      if (touched.introduction) body.introduction = f.introduction.trim() || null
    }
    if (!Object.keys(body).length) return onClose()
    setBusy(true)
    setErr('')
    try {
      await trainers.adminUpdate(t.trainerId, body)
      toast('트레이너 정보를 저장했어요.')
      onSaved()
    } catch (e) {
      if (e.code === 'DUPLICATE_PHONE') setErrs(s => ({ ...s, phone: e.message }))
      else setErr(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal title={`${t.name} 트레이너`} onClose={() => { onScheduleChanged(); onClose() }} wide>
      <Tabs value={tab} options={[['info', '정보 수정'], ['schedule', '근무 시간']]} onChange={setTab} />
      {tab === 'info' ? (orig === 'loading' ? <p className="muted small">트레이너 정보를 불러오는 중…</p> : (
        <>
          <Field label="아이디"><input className="input" value={t.loginId} disabled /></Field>
          <div className="grid g2" style={{ gap: 16 }}>
            <Field label="이름" error={errs.name}><input className="input" value={f.name} onChange={e => set('name', e.target.value)} /></Field>
            <Field label="전화번호" error={errs.phone}><PhoneInput value={f.phone} onChange={v => set('phone', v)} /></Field>
            <Field label="생년월일" error={errs.birthDate} hint={loaded ? '' : '바꿀 때만 입력'}><input className="input" type="date" max={today()} value={f.birthDate} onChange={e => set('birthDate', e.target.value)} /></Field>
            <div className="field">성별 {!loaded && <span className="hint">바꿀 때만 선택</span>}<Seg value={f.gender} options={[['MALE', '남'], ['FEMALE', '여']]} onChange={v => set('gender', v)} /></div>
          </div>
          <Field label="경력 (선택)" error={errs.career} hint="비워서 저장하면 지워져요."><input className="input" value={f.career} onChange={e => set('career', e.target.value)} placeholder={loaded || touched.career ? '' : '바꿀 때만 입력'} /></Field>
          <Field label="소개글 (선택)" error={errs.introduction}><textarea className="textarea" value={f.introduction} onChange={e => set('introduction', e.target.value)} placeholder={loaded || touched.introduction ? '' : '바꿀 때만 입력'} /></Field>
          {!loaded && <div className="alert small">트레이너 정보를 불러오지 못해서 생년월일·성별·경력·소개의 현재 값은 보여 줄 수 없어요. 입력한 항목만 바뀌어요.</div>}
          {err && <div className="alert bad">{err}</div>}
          <div className="row between">
            <button className="btn" onClick={() => setReset(true)}>비밀번호 초기화</button>
            <div className="row">
              <button className="btn" onClick={onClose} disabled={busy}>닫기</button>
              <button className="btn primary" onClick={save} disabled={busy}>{busy ? '저장 중…' : '저장'}</button>
            </div>
          </div>
        </>
      )) : (
        <ScheduleManager trainerId={t.trainerId} compact />
      )}
      {reset && <ResetPassword userId={t.trainerId} name={t.name} onClose={() => setReset(false)} />}
    </Modal>
  )
}
