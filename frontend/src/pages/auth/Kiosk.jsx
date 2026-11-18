import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { attendance } from '../../api/endpoints'
import { Brand } from '../../components/Layout'
import { fmtTime } from '../../lib/format'

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
      const d = await attendance.kiosk('010' + digits)
      setResult({ ok: true, dup: d.duplicate, msg: d.duplicate ? `${d.name} 님, 이미 체크인됐어요.` : `${d.name} 님, 체크인됐어요!`, sub: `${d.entryType === 'PT' ? 'PT 예약' : '회원권'}으로 입장 · ${fmtTime(d.checkedInAt)}` })
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
    const id = setTimeout(() => setResult(null), 5000)
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
        {result && (
          <div className={`alert ${result.ok ? 'info' : 'bad'}`} role="status" style={{ textAlign: 'center', fontSize: 17 }}>
            <b>{result.msg}</b>
            {result.sub && <div className="muted small" style={{ marginTop: 4 }}>{result.sub}</div>}
          </div>
        )}
      </div>
      <p className="muted small" style={{ margin: 0, textAlign: 'center' }}>회원권이 없는 PT 회원은 수업 시작 30분 전부터 30분 후까지 입장할 수 있어요.</p>
    </div>
  )
}
