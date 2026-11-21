import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { attendance } from '../../api/endpoints'
import { Brand } from '../../components/Layout'
import { fmtDate, fmtTime } from '../../lib/format'

export default function Kiosk() {
  const [digits, setDigits] = useState('')
  const [busy, setBusy] = useState(false)
  const [result, setResult] = useState(null)

  const press = useCallback(k => {
    setResult(null)
    if (k === 'del') setDigits(d => d.slice(0, -1))
    else if (k === 'clear') setDigits('')
    else setDigits(d => (d.length < 8 ? d + k : d))
  }, [])

  const submit = useCallback(async () => {
    if (digits.length !== 8 || busy) {
      if (digits.length !== 8) setResult({ ok: false, msg: '전화번호 8자리를 정확히 입력해 주세요.' })
      return
    }
    setBusy(true)
    try {
      const r = await attendance.kiosk('010' + digits)
      const d = r.data
      setResult({ ok: true, data: d })
      setDigits('')
    } catch (e) {
      setResult({ ok: false, msg: e.message })
    } finally {
      setBusy(false)
    }
  }, [digits, busy])

  useEffect(() => {
    const h = e => {
      if (/^[0-9]$/.test(e.key)) press(e.key)
      else if (e.key === 'Backspace') press('del')
      else if (e.key === 'Enter') submit()
      else if (e.key === 'Escape') press('clear')
    }
    window.addEventListener('keydown', h)
    return () => window.removeEventListener('keydown', h)
  }, [press, submit])

  useEffect(() => {
    if (!result?.ok) return
    const id = setTimeout(() => setResult(null), 8000)   // 입장 완료 화면은 8초 뒤 자동으로 닫힘
    return () => clearTimeout(id)
  }, [result])

  const shown = `${digits.slice(0, 4).padEnd(4, '•')} - ${digits.slice(4).padEnd(4, '•')}`

  return (
    <div className="kiosk">
      <div className="row between" style={{ width: 'min(100%, 520px)' }}>
        <Brand to="/kiosk" />
        <Link to="/login" className="small">로그인 화면으로</Link>
      </div>
      <h1 style={{ margin: 0, fontSize: 28, fontWeight: 900, textAlign: 'center' }}>전화번호를 누르고 체크인하세요</h1>
      <div className="phone" aria-live="polite"><span className="muted">010-</span> {shown}</div>
      <div className="keypad">
        {['1', '2', '3', '4', '5', '6', '7', '8', '9'].map(k => <button key={k} onClick={() => press(k)}>{k}</button>)}
        <button className="fn" onClick={() => press('del')}>지우기</button>
        <button onClick={() => press('0')}>0</button>
        <button className="go" onClick={submit} disabled={busy}>{busy ? '…' : '체크인'}</button>
      </div>
      <div style={{ minHeight: 84, width: 'min(100%, 520px)' }}>
        {result && !result.ok && (
          <div className={`alert ${result.ok ? 'info' : 'bad'}`} role="status" style={{ textAlign: 'center', fontSize: 17 }}>
            <b>{result.msg}</b>
            {result.sub && <div className="muted small" style={{ marginTop: 4 }}>{result.sub}</div>}
          </div>
        )}
      </div>
      <p className="muted small" style={{ margin: 0, textAlign: 'center' }}>회원권이 없는 PT 회원은 수업 시작 30분 전부터 30분 후까지 입장할 수 있어요.</p>
      {result?.ok && <Welcome d={result.data} onClose={() => setResult(null)} />}
    </div>
  )
}

// 입장 완료 화면: 회원권 남은 일수, PT 남은 횟수, 다음 수업, 이번 달 출석
function Welcome({ d, onClose }) {
  const m = d.membership
  const pt = d.pt
  const next = d.nextLesson
  return (
    <div className="kiosk-done" role="dialog" aria-live="assertive" onClick={onClose}>
      <div className="kiosk-card" onClick={e => e.stopPropagation()}>
        <div className="stack" style={{ gap: 6, textAlign: 'center' }}>
          <span className="kiosk-check" aria-hidden="true">✓</span>
          <b style={{ fontSize: 28 }}>{d.duplicate ? `${d.name} 님, 이미 체크인됐어요` : `${d.name} 님, 환영해요!`}</b>
          <span className="muted">
            {d.entryType === 'PT' ? 'PT 예약' : '회원권'}으로 입장 · {fmtTime(d.checkedInAt)}
            {d.monthAttendance > 0 && ` · 이번 달 ${d.monthAttendance}일째 출석`}
          </span>
        </div>
        {(m || pt) && (
          <div className="kiosk-tiles">
            {m && (
              <div className="kiosk-tile">
                <span className="muted small">{m.name}</span>
                <div><span className="stat">{m.daysLeft}</span><span className="muted"> 일 남음</span></div>
                <div className="bar"><i style={{ width: `${Math.max(2, (m.daysLeft / Math.max(1, m.totalDays)) * 100)}%` }} /></div>
                <span className="dim small">{fmtDate(m.endDate)}까지</span>
              </div>
            )}
            {pt && (
              <div className="kiosk-tile">
                <span className="muted small">{pt.name}</span>
                <div><span className="stat">{pt.remaining}</span><span className="muted"> / {pt.total}회 남음</span></div>
                <div className="bar"><i style={{ width: `${Math.max(2, (pt.remaining / Math.max(1, pt.total)) * 100)}%` }} /></div>
                <span className="dim small">예약 가능 {pt.available}회{pt.reserved > 0 ? ` · 예약 중 ${pt.reserved}회` : ''}</span>
              </div>
            )}
          </div>
        )}
        {next && (
          <div className="kiosk-next">
            <span className="muted small">다음 수업</span>
            <b>{next.date} {next.time}</b>
            <span className="muted">{next.title} · {next.trainer} 트레이너</span>
          </div>
        )}
        <button className="btn primary" style={{ alignSelf: 'center', minWidth: 160 }} onClick={onClose}>확인</button>
      </div>
    </div>
  )
}
