import { useState } from 'react'
import { Link } from 'react-router-dom'
import { reservations } from '../../api/endpoints'
import { useAsync } from '../../lib/hooks'
import { Async, Badge, Field, Modal, PageTitle, Tabs, useToast } from '../../components/ui'
import { addDays, fmtMD, fmtPhone, nowLocalISO, today } from '../../lib/format'

export default function TrainerLessons() {
  const toast = useToast()
  const [mode, setMode] = useState('week')
  const [start, setStart] = useState(today())
  const end = addDays(start, 6)
  const list = useAsync(() => (mode === 'pending' ? reservations.lessons({ status: '결과대기' }) : reservations.lessons({ startDate: start, endDate: end })), [mode, start])
  const [busyId, setBusyId] = useState(null)
  const [cancelTarget, setCancelTarget] = useState(null)
  const now = nowLocalISO()

  const mark = async (p, result) => {
    setBusyId(p.reservationId)
    try {
      const r = await reservations.result(p.reservationId, result)
      toast(r.message || `${result} 처리했어요.`)
      list.reload()
    } catch (e) {
      toast(e.message, true)
    } finally {
      setBusyId(null)
    }
  }

  return (
    <main className="page">
      <PageTitle title="수업 일정" sub="수업이 끝나면 출석·결석을 처리해 주세요. 처리하면 회원의 이용권 1회가 차감돼요.">
        <Tabs value={mode} options={[['week', '주간 일정'], ['pending', '결과 대기']]} onChange={setMode} />
      </PageTitle>
      {mode === 'week' && (
        <div className="row between">
          <div className="row">
            <button className="btn sm" onClick={() => setStart(addDays(start, -7))}>← 이전 주</button>
            <button className="btn sm" onClick={() => setStart(today())}>오늘</button>
            <button className="btn sm" onClick={() => setStart(addDays(start, 7))}>다음 주 →</button>
          </div>
          <b>{fmtMD(start)} ~ {fmtMD(end)}</b>
        </div>
      )}
      <div className="row between">
        <span className="muted small">수업이 끝난 뒤 1분 안에 '결과 대기'로 바뀌어요.</span>
        <button className="btn sm" onClick={list.reload}>새로고침</button>
      </div>
      <Async state={list} isEmpty={d => d.lessons.length === 0} empty={mode === 'pending' ? '처리할 수업이 없어요.' : '이 기간에 예정된 수업이 없어요.'}>
        {d => {
          const byDate = d.lessons.reduce((m, l) => ((m[l.lessonDate] ||= []).push(l), m), {})
          return Object.entries(byDate).sort(([a], [b]) => a.localeCompare(b)).map(([date, ls]) => (
            <section key={date} className="stack">
              <h2 className="sec">{fmtMD(date)}{date === today() ? ' · 오늘' : ''}</h2>
              {ls.map(l => {
                const started = `${l.lessonDate}T${l.startTime}:00` <= now
                return (
                  <div key={(l.groupClassId ? 'g' : 'r') + (l.groupClassId || l.participants[0]?.reservationId)} className="card">
                    <div className="row between">
                      <div className="row">
                        <span className="stat" style={{ fontSize: 26 }}>{l.startTime}</span>
                        <b>{l.ptType === '그룹' ? `그룹 · ${l.className}` : '개인 PT'}</b>
                      </div>
                      {l.ptType === '그룹' && <Link to="/trainer/groups" className="btn sm">그룹 수업 관리</Link>}
                    </div>
                    <div className="tblwrap">
                      <table className="tbl">
                        <thead><tr><th>회원</th><th>전화번호</th><th>상태</th><th style={{ textAlign: 'right' }}>처리</th></tr></thead>
                        <tbody>
                          {l.participants.map(p => (
                            <tr key={p.reservationId}>
                              <td><b>{p.name}</b></td>
                              <td className="muted">{fmtPhone(p.phone)}</td>
                              <td><Badge s={p.status} />{p.isDeducted && <span className="dim small"> · 차감됨</span>}</td>
                              <td style={{ textAlign: 'right' }}>
                                <div className="row end" style={{ gap: 6 }}>
                                  {started && p.status !== '취소' && (
                                    <>
                                      <button className="btn sm" disabled={busyId === p.reservationId || p.status === '출석'} onClick={() => mark(p, '출석')}>{p.status === '결석' ? '출석으로 정정' : '출석'}</button>
                                      <button className="btn sm" disabled={busyId === p.reservationId || p.status === '결석'} onClick={() => mark(p, '결석')}>{p.status === '출석' ? '결석으로 정정' : '결석'}</button>
                                    </>
                                  )}
                                  {!started && p.status === '예약' && l.ptType === '개인' && (
                                    <button className="btn sm danger" onClick={() => setCancelTarget({ l, p })}>예약 취소</button>
                                  )}
                                  {!started && p.status === '예약' && l.ptType === '그룹' && <span className="dim small">수업 시작 후 처리</span>}
                                </div>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                )
              })}
            </section>
          ))
        }}
      </Async>
      {cancelTarget && <ReasonCancel title="개인 PT 예약을 취소할까요?" desc={`${fmtMD(cancelTarget.l.lessonDate)} ${cancelTarget.l.startTime} · ${cancelTarget.p.name} 회원. 트레이너가 취소하면 회원의 횟수는 차감되지 않아요.`} onClose={() => setCancelTarget(null)} onSubmit={async reason => {
        await reservations.trainerCancel(cancelTarget.p.reservationId, reason)
        toast('예약을 취소했어요.')
        setCancelTarget(null)
        list.reload()
      }} />}
    </main>
  )
}

export function ReasonCancel({ title, desc, confirmText = '취소하기', onClose, onSubmit }) {
  const [reason, setReason] = useState('')
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const go = async () => {
    const r = reason.trim()
    if (!r) return setErr('취소 사유를 입력해 주세요.')
    if (r.length > 200) return setErr('취소 사유는 200자 이하로 입력해 주세요.')
    setBusy(true)
    setErr('')
    try {
      await onSubmit(r)
    } catch (e) {
      setErr(e.message)
      setBusy(false)
    }
  }
  return (
    <Modal title={title} onClose={onClose}>
      {desc && <p className="muted" style={{ margin: 0, lineHeight: 1.6 }}>{desc}</p>}
      <Field label="취소 사유" hint={`회원에게 보여요. ${reason.length}/200`}>
        <textarea className="textarea" value={reason} onChange={e => setReason(e.target.value)} maxLength={200} />
      </Field>
      {err && <div className="alert bad">{err}</div>}
      <div className="row end">
        <button className="btn" onClick={onClose} disabled={busy}>닫기</button>
        <button className="btn danger" onClick={go} disabled={busy}>{busy ? '처리 중…' : confirmText}</button>
      </div>
    </Modal>
  )
}
