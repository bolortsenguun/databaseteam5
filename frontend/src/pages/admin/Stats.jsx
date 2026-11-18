import { useState } from 'react'
import { stats } from '../../api/endpoints'
import { useAsync } from '../../lib/hooks'
import { Async, Empty, Field, PageTitle } from '../../components/ui'
import { addDays, addMonths, fmtMoney, today } from '../../lib/format'

function Bars({ items, fmt }) {
  const max = Math.max(1, ...items.map(i => i.value))
  return (
    <div className="chart">
      {items.map(i => (
        <div key={i.label} className="col" title={`${i.label}: ${fmt ? fmt(i.value) : i.value}`}>
          <b>{i.value ? (fmt ? fmt(i.value) : i.value) : ''}</b>
          <i style={{ height: `${(i.value / max) * 100}%`, opacity: i.value ? 1 : 0.25 }} />
          <span>{i.label}</span>
        </div>
      ))}
    </div>
  )
}

export default function AdminStats() {
  const first = today().slice(0, 8) + '01'
  const [range, setRange] = useState({ startDate: first, endDate: addDays(addMonths(first, 1), -1) })
  const [q, setQ] = useState(range)
  const [err, setErr] = useState('')
  const s = useAsync(() => stats.statistics(q), [q.startDate, q.endDate])

  const apply = () => {
    if (range.startDate > range.endDate) return setErr('시작일은 종료일보다 늦을 수 없어요.')
    setErr('')
    setQ(range)
  }
  const preset = months => {
    const st = addMonths(first, -(months - 1))
    const r = { startDate: st, endDate: addDays(addMonths(first, 1), -1) }
    setRange(r)
    setQ(r)
  }

  return (
    <main className="page">
      <PageTitle title="통계" sub="성공한 결제만 매출로 계산해요. 개인 PT와 그룹 PT는 따로 집계해요." />
      <div className="card">
        <div className="row" style={{ alignItems: 'flex-end' }}>
          <Field label="시작일"><input className="input" type="date" value={range.startDate} onChange={e => setRange({ ...range, startDate: e.target.value })} /></Field>
          <Field label="종료일"><input className="input" type="date" value={range.endDate} onChange={e => setRange({ ...range, endDate: e.target.value })} /></Field>
          <button className="btn primary" onClick={apply}>조회</button>
          <button className="btn sm" onClick={() => preset(1)}>이번 달</button>
          <button className="btn sm" onClick={() => preset(3)}>최근 3개월</button>
          <button className="btn sm" onClick={() => preset(6)}>최근 6개월</button>
        </div>
        {err && <div className="alert bad small">{err}</div>}
      </div>
      <Async state={s}>
        {d => {
          const total = d.monthlyRevenue.reduce((a, m) => a + m.amount, 0)
          return (
            <>
              <section className="card">
                <div className="row between">
                  <h3>월별 매출</h3>
                  <span className="muted">합계 <b className="num" style={{ color: 'var(--text)' }}>{fmtMoney(total)}</b> · {d.monthlyRevenue.reduce((a, m) => a + m.successCount, 0)}건</span>
                </div>
                {d.monthlyRevenue.length === 0 ? <Empty>이 기간에 성공한 결제가 없어요.</Empty> : (
                  <Bars items={d.monthlyRevenue.map(m => ({ label: `${Number(m.month.slice(5))}월`, value: m.amount }))} fmt={v => `${Math.round(v / 10000).toLocaleString()}만`} />
                )}
              </section>
              <section className="card">
                <h3>트레이너 PT 실적</h3>
                {d.trainerPerformance.length === 0 ? <Empty>실적이 없어요.</Empty> : (
                  <div className="tblwrap">
                    <table className="tbl">
                      <thead><tr><th>트레이너</th><th>개인 출석</th><th>개인 결석</th><th>그룹 수업</th><th>그룹 출석</th><th>그룹 결석</th><th>합계</th></tr></thead>
                      <tbody>
                        {d.trainerPerformance.map(t => (
                          <tr key={t.trainerId}>
                            <td><b>{t.trainerName}</b></td>
                            <td className="num">{t.individualAttendanceCount}</td>
                            <td className="num">{t.individualAbsenceCount}</td>
                            <td className="num">{t.groupClassesHeld}</td>
                            <td className="num">{t.groupAttendanceCount}</td>
                            <td className="num">{t.groupAbsenceCount}</td>
                            <td className="num"><b>{t.totalProcessedCount}</b></td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
                <span className="dim small">회원이 차감 취소한 수업은 실적에 들어가지 않아요.</span>
              </section>
              <section className="card">
                <h3>시간대별 입장</h3>
                <Bars items={d.hourlyCheckins.filter(h => h.hour >= 5).map(h => ({ label: `${h.hour}시`, value: h.count }))} />
                <span className="dim small">체크인 기록 기준 · 0~4시는 생략</span>
              </section>
            </>
          )
        }}
      </Async>
    </main>
  )
}
