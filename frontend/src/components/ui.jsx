import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { toneOf, statusText } from '../lib/format'

export function PageTitle({ title, sub, children }) {
  return (
    <div className="ptitle">
      <div>
        <h1>{title}</h1>
        {sub && <p>{sub}</p>}
      </div>
      {children && <div className="row">{children}</div>}
    </div>
  )
}

export function Badge({ s, tone, children }) {
  const t = tone ?? toneOf(s)
  return <span className={`badge ${t}`}>{children ?? statusText(s)}</span>
}

export function Loading({ text = '불러오는 중이에요…' }) {
  return <div className="loading">{text}</div>
}

export function Empty({ children }) {
  return <div className="empty">{children}</div>
}

export function ErrorBox({ error, onRetry }) {
  if (!error) return null
  return (
    <div className="alert bad row between">
      <span>{error.message || '문제가 생겼어요.'}</span>
      {onRetry && <button className="btn sm" onClick={onRetry}>다시 시도</button>}
    </div>
  )
}

export function Async({ state, children, empty, isEmpty }) {
  if (state.loading && !state.data) return <Loading />
  if (state.error) return <ErrorBox error={state.error} onRetry={state.reload} />
  if (!state.data) return null
  if (isEmpty && isEmpty(state.data)) return <Empty>{empty || '내용이 없어요.'}</Empty>
  return children(state.data)
}

export function Field({ label, hint, error, ok, children }) {
  return (
    <label className="field">
      {label}
      {children}
      {error ? <span className="err">{error}</span> : ok ? <span className="okmsg">{ok}</span> : hint ? <span className="hint">{hint}</span> : null}
    </label>
  )
}

export function Seg({ value, options, onChange }) {
  return (
    <div className="seg" role="radiogroup">
      {options.map(([v, l]) => (
        <button type="button" key={String(v)} className={value === v ? 'on' : ''} aria-pressed={value === v} onClick={() => onChange(v)}>{l}</button>
      ))}
    </div>
  )
}

export function Tabs({ value, options, onChange }) {
  return (
    <div className="tabs" role="tablist">
      {options.map(([v, l]) => (
        <button key={v} role="tab" aria-selected={value === v} className={value === v ? 'on' : ''} onClick={() => onChange(v)}>{l}</button>
      ))}
    </div>
  )
}

export function Modal({ title, onClose, children, wide }) {
  useEffect(() => {
    const h = e => e.key === 'Escape' && onClose?.()
    window.addEventListener('keydown', h)
    return () => window.removeEventListener('keydown', h)
  }, [onClose])
  return (
    <div className="overlay" onMouseDown={e => e.target === e.currentTarget && onClose?.()}>
      <div className={`modal ${wide ? 'wide' : ''}`} role="dialog" aria-modal="true" aria-label={title}>
        {title && <h2>{title}</h2>}
        {children}
      </div>
    </div>
  )
}

export function Pager({ page, totalPages, onChange, base = 1 }) {
  if (!totalPages || totalPages <= 1) return null
  const shown = page - base + 1
  return (
    <div className="pager">
      <button className="btn sm" disabled={shown <= 1} onClick={() => onChange(page - 1)}>이전</button>
      <span>{shown} / {totalPages}</span>
      <button className="btn sm" disabled={shown >= totalPages} onClick={() => onChange(page + 1)}>다음</button>
    </div>
  )
}

const ToastCtx = createContext(() => {})
export function ToastProvider({ children }) {
  const [t, setT] = useState(null)
  const show = useCallback((msg, bad = false) => {
    setT({ msg, bad, id: Date.now() })
  }, [])
  useEffect(() => {
    if (!t) return
    const id = setTimeout(() => setT(null), 2800)
    return () => clearTimeout(id)
  }, [t])
  return (
    <ToastCtx.Provider value={show}>
      {children}
      {t && <div className={`toast ${t.bad ? 'bad' : ''}`} role="status">{t.msg}</div>}
    </ToastCtx.Provider>
  )
}
export const useToast = () => useContext(ToastCtx)

export function Confirm({ title, message, confirmText = '확인', danger, busy, error, onConfirm, onClose, children }) {
  return (
    <Modal title={title} onClose={onClose}>
      {message && <p className="muted" style={{ margin: 0, lineHeight: 1.6 }}>{message}</p>}
      {children}
      {error && <div className="alert bad">{error}</div>}
      <div className="row end">
        <button className="btn" onClick={onClose} disabled={busy}>닫기</button>
        <button className={`btn ${danger ? 'danger' : 'primary'}`} onClick={onConfirm} disabled={busy}>{busy ? '처리 중…' : confirmText}</button>
      </div>
    </Modal>
  )
}
