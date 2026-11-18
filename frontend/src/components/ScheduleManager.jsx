import { useState } from 'react'
import { trainers } from '../api/endpoints'
import { useAsync } from '../lib/hooks'
import { Async, Confirm, Empty, Field, useToast } from './ui'
import { DOW, fmtMD, hourStr, today } from '../lib/format'

const HOURS = Array.from({ length: 19 }, (_, i) => i + 6)

export default function ScheduleManager({ trainerId, compact }) {
  const toast = useToast()
  const s = useAsync(() => trainers.schedules(trainerId), [trainerId])
  const [f, setF] = useState({ days: [1], start: 10, end: 18 })
  const [off, setOff] = useState('')
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')
  const [offErr, setOffErr] = useState('')
  const [del, setDel] = useState(null)

  const toggleDay = d => setF(x => ({ ...x, days: x.days.includes(d) ? x.days.filter(y => y !== d) : [...x.days, d].sort() }))

  const add = async () => {
    if (!f.days.length) return setErr('요일을 하나 이상 골라 주세요.')
    if (f.end <= f.start) return setErr('끝나는 시간은 시작 시간보다 늦어야 해요.')
    setBusy(true)
    setErr('')
    const fails = []
    for (const d of f.days) {
      try {
        await trainers.addSchedule(trainerId, { dayOfWeek: d, startTime: hourStr(f.start), endTime: hourStr(f.end) })
      } catch (e) {
        fails.push(`${DOW[d]}: ${e.message}`)
      }
    }
    setBusy(false)
    if (fails.length) setErr(fails.join(' / '))
    else toast('근무 시간을 등록했어요.')
    s.reload()
  }

  const addOff = async () => {
    if (!off) return setOffErr('날짜를 골라 주세요.')
    setOffErr('')
    try {
      await trainers.addDayOff(trainerId, off)
      toast('휴무일을 등록했어요.')
      setOff('')
      s.reload()
    } catch (e) {
      setOffErr(e.message)
    }
  }

  const doDelete = async () => {
    setBusy(true)
    try {
      if (del.type === 'schedule') await trainers.deleteSchedule(trainerId, del.id)
      else await trainers.deleteDayOff(trainerId, del.id)
      toast(del.type === 'schedule' ? '근무 시간을 지웠어요.' : '휴무일을 지웠어요.')
      setDel(null)
      s.reload()
    } catch (e) {
      setDel(d => ({ ...d, err: e.message }))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Async state={s}>
      {d => (
        <div className={compact ? 'stack' : 'grid g2'} style={{ alignItems: 'start' }}>
          <section className={compact ? 'stack' : 'card'}>
            <h3>주간 근무 시간</h3>
            {d.schedules.length === 0 ? <Empty>등록된 근무 시간이 없어요. 근무 시간이 있어야 회원이 예약할 수 있어요.</Empty> : (
              <table className="tbl">
                <thead><tr><th>요일</th><th>시간</th><th /></tr></thead>
                <tbody>
                  {d.schedules.map(x => (
                    <tr key={x.scheduleId}>
                      <td><b>{DOW[x.dayOfWeek]}요일</b></td>
                      <td className="num">{x.startTime} ~ {x.endTime}</td>
                      <td style={{ textAlign: 'right' }}><button className="btn sm" onClick={() => setDel({ type: 'schedule', id: x.scheduleId, label: `${DOW[x.dayOfWeek]}요일 ${x.startTime}~${x.endTime}` })}>삭제</button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            <div className="card flat" style={{ padding: 20 }}>
              <b>근무 시간 추가</b>
              <div className="field">
                요일
                <div className="seg">
                  {[1, 2, 3, 4, 5, 6, 7].map(dd => (
                    <button key={dd} type="button" className={f.days.includes(dd) ? 'on' : ''} style={{ minWidth: 48, padding: 0 }} onClick={() => toggleDay(dd)}>{DOW[dd]}</button>
                  ))}
                </div>
              </div>
              <div className="grid g2" style={{ gap: 12 }}>
                <Field label="시작">
                  <select className="select" value={f.start} onChange={e => setF({ ...f, start: Number(e.target.value) })}>
                    {HOURS.map(h => <option key={h} value={h}>{hourStr(h)}</option>)}
                  </select>
                </Field>
                <Field label="끝">
                  <select className="select" value={f.end} onChange={e => setF({ ...f, end: Number(e.target.value) })}>
                    {HOURS.map(h => <option key={h} value={h}>{hourStr(h)}</option>)}
                  </select>
                </Field>
              </div>
              {err && <div className="alert bad small">{err}</div>}
              <button className="btn primary" onClick={add} disabled={busy}>{busy ? '등록 중…' : '추가'}</button>
            </div>
          </section>
          <section className={compact ? 'stack' : 'card'}>
            <h3>휴무일</h3>
            {d.daysOff.filter(x => x.offDate >= today()).length === 0 ? <Empty>예정된 휴무일이 없어요.</Empty> : (
              <table className="tbl">
                <tbody>
                  {d.daysOff.filter(x => x.offDate >= today()).map(x => (
                    <tr key={x.dayOffId}>
                      <td><b>{fmtMD(x.offDate)}</b></td>
                      <td style={{ textAlign: 'right' }}><button className="btn sm" onClick={() => setDel({ type: 'off', id: x.dayOffId, label: fmtMD(x.offDate) })}>삭제</button></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
            <div className="card flat" style={{ padding: 20 }}>
              <b>휴무일 추가</b>
              <span className="muted small">예약이나 그룹 수업이 있는 날은 휴무로 정할 수 없어요.</span>
              <div className="inline">
                <input className="input" type="date" min={today()} value={off} onChange={e => setOff(e.target.value)} />
                <button className="btn primary" onClick={addOff}>추가</button>
              </div>
              {offErr && <div className="alert bad small">{offErr}</div>}
            </div>
          </section>
          {del && <Confirm title="삭제할까요?" message={`${del.label}을(를) 지워요.`} confirmText="삭제" danger busy={busy} error={del.err} onClose={() => setDel(null)} onConfirm={doDelete} />}
        </div>
      )}
    </Async>
  )
}
