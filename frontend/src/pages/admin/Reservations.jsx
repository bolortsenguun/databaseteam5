import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { reservations } from '../../api/endpoints'
import { useAsync } from '../../lib/hooks'
import { Async, Badge, Field, PageTitle, Pager, useToast } from '../../components/ui'
import { fmtMD, nowLocalISO } from '../../lib/format'
import { ReasonCancel } from '../trainer/Lessons'

const STATUSES = ['전체', '예약', '결과대기', '출석', '결석', '취소']

export default function AdminReservations() {
  const toast = useToast()
  const [sp] = useSearchParams()
  const init = { status: sp.get('status') || '전체', ptType: '전체', startDate: '', endDate: '', userId: sp.get('userId') || '' }
  const [f, setF] = useState(init)
  const [q, setQ] = useState({ ...init, page: 1 })
  const [err, setErr] = useState('')
  const [target, setTarget] = useState(null)
  const list = useAsync(() => reservations.adminList({
    status: q.status === '전체' ? undefined : q.status,
    ptType: q.ptType === '전체' ? undefined : q.ptType,
    startDate: q.startDate, endDate: q.endDate, userId: q.userId, page: q.page, size: 20
  }), [q.status, q.ptType, q.startDate, q.endDate, q.userId, q.page])
  const now = nowLocalISO()

  const apply = () => {
    if (f.startDate && f.endDate && f.startDate > f.endDate) return setErr('시작일은 종료일보다 늦을 수 없어요.')
    setErr('')
    setQ({ ...f, page: 1 })
  }

  return (
    <main className="page">
      <PageTitle title="예약" sub="모든 수업 예약을 한눈에 봐요. 수업 시작 전 예약은 관리자가 취소할 수 있어요." />
      <div className="card">
        <div className="row" style={{ alignItems: 'flex-end' }}>
          <Field label="상태">
            <select className="select" value={f.status} onChange={e => setF({ ...f, status: e.target.value })}>
              {STATUSES.map(s => <option key={s} value={s}>{s === '결과대기' ? '결과 대기' : s}</option>)}
            </select>
          </Field>
          <Field label="구분">
            <select className="select" value={f.ptType} onChange={e => setF({ ...f, ptType: e.target.value })}>
              {['전체', '개인', '그룹'].map(s => <option key={s} value={s}>{s}</option>)}
            </select>
          </Field>
          <Field label="시작일"><input className="input" type="date" value={f.startDate} onChange={e => setF({ ...f, startDate: e.target.value })} /></Field>
          <Field label="종료일"><input className="input" type="date" value={f.endDate} onChange={e => setF({ ...f, endDate: e.target.value })} /></Field>
          <button className="btn primary" onClick={apply}>조회</button>
          {q.userId && <button className="btn sm" onClick={() => { const v = { ...f, userId: '' }; setF(v); setQ({ ...v, page: 1 }) }}>회원 필터 해제</button>}
        </div>
        {err && <div className="alert bad small">{err}</div>}
      </div>
      <Async state={list} isEmpty={d => d.reservations.length === 0} empty="조건에 맞는 예약이 없어요.">
        {d => (
          <div className="card">
            <span className="muted small">{d.totalCount}건</span>
            <div className="tblwrap">
              <table className="tbl">
                <thead><tr><th>수업일</th><th>구분</th><th>회원</th><th>트레이너</th><th>상태</th><th /></tr></thead>
                <tbody>
                  {d.reservations.map(r => {
                    const cancelable = r.status === '예약' && `${r.lessonDate}T${r.startTime}:00` > now
                    return (
                      <tr key={r.reservationId}>
                        <td>{fmtMD(r.lessonDate)} <span className="num">{r.startTime}</span></td>
                        <td>{r.ptType === '그룹' ? `그룹 · ${r.className}` : '개인 PT'}</td>
                        <td>{r.userName}</td>
                        <td>{r.trainerName}</td>
                        <td>
                          <Badge s={r.status} />
                          {r.status === '취소' && r.canceledBy && <span className="muted small"> {r.canceledBy}{r.cancelReason ? ` · ${r.cancelReason}` : ''}</span>}
                          {r.isDeducted && <span className="dim small"> · 차감</span>}
                        </td>
                        <td style={{ textAlign: 'right' }}>{cancelable && <button className="btn sm danger" onClick={() => setTarget(r)}>예약 취소</button>}</td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
            <Pager page={d.page} totalPages={d.totalPages} onChange={p => setQ({ ...q, page: p })} />
          </div>
        )}
      </Async>
      {target && <ReasonCancel title="예약을 취소할까요? (관리자)" desc={`${fmtMD(target.lessonDate)} ${target.startTime} · ${target.userName} 회원, ${target.ptType === '그룹' ? `그룹 ${target.className}` : '개인 PT'}, ${target.trainerName} 트레이너. 관리자가 취소하면 회원의 횟수는 차감되지 않아요.`} onClose={() => setTarget(null)} onSubmit={async reason => {
        await reservations.adminCancel(target.reservationId, reason)
        toast('예약을 취소했어요.')
        setTarget(null)
        list.reload()
      }} />}
    </main>
  )
}
