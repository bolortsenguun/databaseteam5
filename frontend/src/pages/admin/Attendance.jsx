import { useState } from 'react'
import { attendance } from '../../api/endpoints'
import { useAsync } from '../../lib/hooks'
import { Async, Field, PageTitle, Pager, Seg } from '../../components/ui'
import { addDays, fmtDateTime, fmtMD, fmtPhone, today } from '../../lib/format'

export default function AdminAttendance() {
  const [date, setDate] = useState(today())
  const [entryType, setEntryType] = useState('전체')
  const [page, setPage] = useState(1)
  const list = useAsync(() => attendance.adminList({ date, entryType, page, size: 20 }), [date, entryType, page])
  const go = d => { setDate(d); setPage(1) }
  return (
    <main className="page">
      <PageTitle title="출석" sub="키오스크 체크인 기록을 날짜별로 봐요. 10분 안에 다시 누른 체크인은 한 번만 남아요." />
      <div className="card">
        <div className="row" style={{ alignItems: 'flex-end' }}>
          <button className="btn sm" onClick={() => go(addDays(date, -1))}>← 이전 날</button>
          <Field label="날짜"><input className="input" type="date" value={date} max={today()} onChange={e => e.target.value && go(e.target.value)} /></Field>
          <button className="btn sm" onClick={() => go(addDays(date, 1))} disabled={date >= today()}>다음 날 →</button>
          <button className="btn sm" onClick={() => go(today())}>오늘</button>
          <div className="field">입장 구분<Seg value={entryType} options={[['전체', '전체'], ['회원권', '회원권'], ['PT', 'PT']]} onChange={v => { setEntryType(v); setPage(1) }} /></div>
        </div>
      </div>
      <Async state={list} isEmpty={d => d.attendances.length === 0} empty={`${fmtMD(date)}에는 체크인 기록이 없어요.`}>
        {d => (
          <div className="card">
            <span className="muted small">{fmtMD(date)} · {d.totalCount}건</span>
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
            <Pager page={d.page} totalPages={d.totalPages} onChange={setPage} />
          </div>
        )}
      </Async>
    </main>
  )
}
