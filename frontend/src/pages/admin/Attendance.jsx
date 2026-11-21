import { useState } from 'react'
import { attendance } from '../../api/endpoints'
import { useAsync } from '../../lib/hooks'
import { Async, Field, PageTitle, Pager, Seg } from '../../components/ui'
import { addDays, fmtDateTime, fmtPhone, today } from '../../lib/format'
import { useSettings } from '../../lib/settings'

export default function AdminAttendance() {
  const S = useSettings()
  const [f, setF] = useState({ startDate: today(), endDate: today(), entryType: '전체' })
  const [q, setQ] = useState({ ...f, page: 1 })
  const [err, setErr] = useState('')
  const list = useAsync(() => attendance.adminList({ ...q, size: 20 }), [q.startDate, q.endDate, q.entryType, q.page])
  const apply = next => {
    const v = { ...f, ...next }
    if (v.startDate && v.endDate && v.startDate > v.endDate) return setErr('시작일은 종료일보다 늦을 수 없어요.')
    setErr('')
    setF(v)
    setQ({ ...v, page: 1 })
  }
  return (
    <main className="page">
      <PageTitle title="출석" sub={`키오스크 체크인 기록이에요. ${S.RECHECKIN_MINUTES}분 안에 다시 누른 체크인은 한 번만 남아요.`} />
      <div className="card">
        <div className="row" style={{ alignItems: 'flex-end' }}>
          <Field label="시작일"><input className="input" type="date" value={f.startDate} onChange={e => setF({ ...f, startDate: e.target.value })} /></Field>
          <Field label="종료일"><input className="input" type="date" value={f.endDate} onChange={e => setF({ ...f, endDate: e.target.value })} /></Field>
          <button className="btn primary" onClick={() => apply({})}>조회</button>
          <button className="btn sm" onClick={() => apply({ startDate: today(), endDate: today() })}>오늘</button>
          <button className="btn sm" onClick={() => apply({ startDate: addDays(today(), -6), endDate: today() })}>최근 7일</button>
          <div className="field">입장 구분<Seg value={f.entryType} options={[['전체', '전체'], ['회원권', '회원권'], ['PT', 'PT']]} onChange={v => apply({ entryType: v })} /></div>
        </div>
        {err && <div className="alert bad small">{err}</div>}
      </div>
      <Async state={list} isEmpty={d => d.attendances.length === 0} empty="이 기간에 체크인 기록이 없어요.">
        {d => (
          <div className="card">
            <span className="muted small">{d.totalCount}건</span>
            <div className="tblwrap">
              <table className="tbl">
                <thead><tr><th>체크인 시각</th><th>이름</th><th>전화번호</th><th>입장 구분</th></tr></thead>
                <tbody>
                  {d.attendances.map(a => (
                    <tr key={a.attendanceId}>
                      <td>{fmtDateTime(a.checkedInAt)}</td>
                      <td><b>{a.userName}</b></td>
                      <td className="muted">{fmtPhone(a.phone)}</td>
                      <td>{a.entryType === 'PT' ? 'PT 예약' : '회원권'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pager page={d.page} totalPages={d.totalPages} onChange={p => setQ({ ...q, page: p })} />
          </div>
        )}
      </Async>
    </main>
  )
}
