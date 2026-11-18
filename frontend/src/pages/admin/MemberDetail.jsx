import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { members, reservations, payments, attendance, auth } from '../../api/endpoints'
import { useAsync, useSafeAsync } from '../../lib/hooks'
import { Async, Badge, Confirm, Empty, Field, Modal, Seg, useToast } from '../../components/ui'
import { PhoneInput, phoneError } from '../../components/inputs'
import { fmtDate, fmtDateTime, fmtMD, fmtMoney, fmtPhone, genderKey, genderLabel, today } from '../../lib/format'

export default function AdminMemberDetail() {
  const { id } = useParams()
  const uid = Number(id)
  const m = useAsync(() => members.detail(uid), [uid])
  const res = useAsync(() => reservations.adminList({ userId: uid, page: 1, size: 8 }), [uid])
  const pay = useSafeAsync(() => payments.adminList({ userId: uid, page: 1, size: 8 }), null, [uid])
  const att = useSafeAsync(() => attendance.adminList({ userId: uid, page: 1, size: 8 }), null, [uid])
  const [edit, setEdit] = useState(false)
  const [reset, setReset] = useState(false)

  return (
    <main className="page">
      <Link to="/admin/members" className="small" style={{ textDecoration: 'none' }}>← 회원 목록</Link>
      <Async state={m}>
        {u => (
          <>
            <div className="ptitle">
              <div>
                <h1>{u.name}</h1>
                <p>{u.loginId}, 가입일 {fmtDate(u.createdAt)}</p>
              </div>
              <div className="row">
                <button className="btn" onClick={() => setEdit(true)}>정보 수정</button>
                <button className="btn" onClick={() => setReset(true)}>비밀번호 초기화</button>
              </div>
            </div>
            <section className="card">
              <h3>기본 정보</h3>
              <dl className="kv">
                <dt>이름</dt><dd>{u.name}</dd>
                <dt>아이디</dt><dd>{u.loginId}</dd>
                <dt>전화번호</dt><dd>{fmtPhone(u.phone)}</dd>
                <dt>생년월일</dt><dd>{u.birthDate ? fmtDate(u.birthDate) : '-'}</dd>
                <dt>성별</dt><dd>{genderLabel(u.gender)}</dd>
              </dl>
            </section>
            {(u.memberships || u.ptPasses) && (
              <div className="grid g2" style={{ alignItems: 'start' }}>
                <section className="card">
                  <h3>회원권</h3>
                  {!u.memberships?.length ? <Empty>회원권이 없어요.</Empty> : (
                    <table className="tbl">
                      <thead><tr><th>상품</th><th>기간</th><th>상태</th></tr></thead>
                      <tbody>{u.memberships.map(x => <tr key={x.membershipId}><td>{x.productName}</td><td className="small">{fmtDate(x.startDate)} ~ {fmtDate(x.endDate)}</td><td><Badge s={x.status} /></td></tr>)}</tbody>
                    </table>
                  )}
                </section>
                <section className="card">
                  <h3>PT 이용권</h3>
                  {!u.ptPasses?.length ? <Empty>PT 이용권이 없어요.</Empty> : (
                    <table className="tbl">
                      <thead><tr><th>상품</th><th>담당</th><th>기간</th><th>잔여</th></tr></thead>
                      <tbody>{u.ptPasses.map(x => <tr key={x.ptPassId}><td>{x.productName}</td><td>{x.trainerName || '-'}</td><td className="small">{fmtDate(x.startDate)} ~ {fmtDate(x.endDate)}</td><td className="num">{x.remainingCount} / {x.totalCount}</td></tr>)}</tbody>
                    </table>
                  )}
                </section>
              </div>
            )}
            {!(u.memberships || u.ptPasses) && <div className="alert small">회원 상세 API에 이용권 정보가 아직 없어서(명세 User ④의 memberships, ptPasses) 예약·결제·출석 기록만 보여 줘요.</div>}
            {edit && <EditMember u={u} onClose={() => setEdit(false)} onSaved={v => { m.setData(v); setEdit(false) }} />}
            {reset && <ResetPassword userId={u.userId} name={u.name} onClose={() => setReset(false)} />}
          </>
        )}
      </Async>
      <section className="card">
        <div className="row between">
          <h3>최근 예약</h3>
          <Link to={`/admin/reservations?userId=${uid}`} className="btn sm">전체 보기</Link>
        </div>
        <Async state={res} isEmpty={d => d.reservations.length === 0} empty="예약 기록이 없어요.">
          {d => (
            <div className="tblwrap">
              <table className="tbl">
                <thead><tr><th>수업일</th><th>구분</th><th>트레이너</th><th>상태</th></tr></thead>
                <tbody>
                  {d.reservations.map(r => (
                    <tr key={r.reservationId}>
                      <td>{fmtMD(r.lessonDate)} <span className="num">{r.startTime}</span></td>
                      <td>{r.ptType === '그룹' ? `그룹 · ${r.className}` : '개인 PT'}</td>
                      <td>{r.trainerName}</td>
                      <td><Badge s={r.status} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Async>
      </section>
      <div className="grid g2" style={{ alignItems: 'start' }}>
        <section className="card">
          <h3>결제</h3>
          {!pay.data ? (pay.loading ? null : <Empty>결제 내역을 불러오지 못했어요.</Empty>) : pay.data.payments.length === 0 ? <Empty>결제 기록이 없어요.</Empty> : (
            <table className="tbl">
              <thead><tr><th>일시</th><th>상품</th><th>금액</th><th>결과</th></tr></thead>
              <tbody>
                {pay.data.payments.map(x => <tr key={x.paymentId}><td className="small">{fmtDateTime(x.paidAt)}</td><td>{x.productName || '-'}</td><td className="num">{fmtMoney(x.amount)}</td><td><Badge s={x.status} /></td></tr>)}
              </tbody>
            </table>
          )}
        </section>
        <section className="card">
          <h3>출석</h3>
          {!att.data ? (att.loading ? null : <Empty>출석 기록을 불러오지 못했어요.</Empty>) : att.data.attendances.length === 0 ? <Empty>출석 기록이 없어요.</Empty> : (
            <table className="tbl">
              <thead><tr><th>체크인 시각</th><th>입장 구분</th></tr></thead>
              <tbody>
                {att.data.attendances.map(a => <tr key={a.attendanceId}><td className="small">{fmtDateTime(a.checkedInAt)}</td><td>{a.entryType === 'PT' ? 'PT 예약' : '회원권'}</td></tr>)}
              </tbody>
            </table>
          )}
        </section>
      </div>
    </main>
  )
}

function EditMember({ u, onClose, onSaved }) {
  const toast = useToast()
  const [f, setF] = useState({ name: u.name, phone: u.phone, birthDate: u.birthDate || '', gender: genderKey(u.gender) })
  const [errs, setErrs] = useState({})
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const save = async () => {
    const v = { name: f.name.trim() ? '' : '이름을 입력해 주세요.', phone: phoneError(f.phone), birthDate: f.birthDate && f.birthDate > today() ? '미래 날짜는 고를 수 없어요.' : '' }
    setErrs(v)
    if (Object.values(v).some(Boolean)) return
    const body = {}
    if (f.name.trim() !== u.name) body.name = f.name.trim()
    if (f.phone !== u.phone) body.phone = f.phone
    if (f.birthDate && f.birthDate !== u.birthDate) body.birthDate = f.birthDate
    if (f.gender && f.gender !== genderKey(u.gender)) body.gender = f.gender
    if (!Object.keys(body).length) return onClose()
    setBusy(true)
    setErr('')
    try {
      const r = await members.update(u.userId, body)
      toast('회원 정보를 저장했어요.')
      onSaved(r)
    } catch (e) {
      if (e.code === 'DUPLICATE_PHONE') setErrs(s => ({ ...s, phone: e.message }))
      else setErr(e.message)
    } finally {
      setBusy(false)
    }
  }
  return (
    <Modal title="회원 정보 수정" onClose={onClose}>
      <Field label="아이디" hint="아이디는 바꿀 수 없어요."><input className="input" value={u.loginId} disabled /></Field>
      <Field label="이름" error={errs.name}><input className="input" value={f.name} onChange={e => setF({ ...f, name: e.target.value })} /></Field>
      <Field label="전화번호" error={errs.phone}><PhoneInput value={f.phone} onChange={v => setF({ ...f, phone: v })} /></Field>
      <div className="grid g2" style={{ gap: 16 }}>
        <Field label="생년월일" error={errs.birthDate}><input className="input" type="date" max={today()} value={f.birthDate} onChange={e => setF({ ...f, birthDate: e.target.value })} /></Field>
        <div className="field">성별<Seg value={f.gender} options={[['MALE', '남'], ['FEMALE', '여']]} onChange={v => setF({ ...f, gender: v })} /></div>
      </div>
      {err && <div className="alert bad">{err}</div>}
      <div className="row end">
        <button className="btn" onClick={onClose} disabled={busy}>취소</button>
        <button className="btn primary" onClick={save} disabled={busy}>{busy ? '저장 중…' : '저장'}</button>
      </div>
    </Modal>
  )
}

export function ResetPassword({ userId, name, onClose }) {
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')
  const [temp, setTemp] = useState(null)
  if (temp) {
    return (
      <Modal title="임시 비밀번호가 만들어졌어요" onClose={onClose}>
        <p className="muted" style={{ margin: 0, lineHeight: 1.6 }}>{name} 님에게 아래 임시 비밀번호를 알려 주세요. 다음 로그인 때 새 비밀번호로 바꿔야 해요. 이 창을 닫으면 다시 볼 수 없어요.</p>
        <div className="card flat" style={{ alignItems: 'center' }}>
          <span className="num" style={{ fontSize: 28, fontWeight: 700, userSelect: 'all' }}>{temp}</span>
        </div>
        <div className="row end">
          <button className="btn" onClick={() => navigator.clipboard?.writeText(temp)}>복사</button>
          <button className="btn primary" onClick={onClose}>확인</button>
        </div>
      </Modal>
    )
  }
  return (
    <Confirm title="비밀번호를 초기화할까요?" message={`${name} 님의 비밀번호를 임시 비밀번호로 바꿔요. 지금 쓰는 비밀번호로는 더 이상 로그인할 수 없어요.`} confirmText="초기화" danger busy={busy} error={err} onClose={onClose} onConfirm={async () => {
      setBusy(true)
      setErr('')
      try {
        const r = await auth.resetPassword(userId)
        setTemp(r.temporaryPassword)
      } catch (e) {
        setErr(e.message)
      } finally {
        setBusy(false)
      }
    }} />
  )
}
