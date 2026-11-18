import { useState } from 'react'
import { reservations } from '../../api/endpoints'
import { useAsync } from '../../lib/hooks'
import { Async, Badge, Empty, Loading, Modal, PageTitle, useToast } from '../../components/ui'
import { fmtMD, nowLocalISO } from '../../lib/format'

export default function MemberReservations() {
  const list = useAsync(() => reservations.mine(), [])
  const [target, setTarget] = useState(null)
  const now = nowLocalISO()
  return (
    <main className="page">
      <PageTitle title="내 예약" sub="수업을 취소하기 전에 횟수가 차감되는지 먼저 알려 드려요." />
      <Async state={list}>
        {d => {
          const all = d.reservations
          const key = r => `${r.lessonDate}T${r.startTime}:00`
          const up = all.filter(r => r.status === '예약' && key(r) > now).sort((a, b) => key(a).localeCompare(key(b)))
          const past = all.filter(r => !up.includes(r)).sort((a, b) => key(b).localeCompare(key(a)))
          return (
            <>
              <section className="card">
                <h3>다가오는 수업</h3>
                {up.length === 0 ? <Empty>예정된 수업이 없어요.</Empty> : <ResTable rows={up} onCancel={setTarget} />}
              </section>
              <section className="card">
                <h3>지난 수업 · 취소된 수업</h3>
                {past.length === 0 ? <Empty>아직 기록이 없어요.</Empty> : <ResTable rows={past} />}
              </section>
            </>
          )
        }}
      </Async>
      {target && <CancelModal r={target} onClose={() => setTarget(null)} onDone={() => { setTarget(null); list.reload() }} />}
    </main>
  )
}

function ResTable({ rows, onCancel }) {
  return (
    <div className="tblwrap">
      <table className="tbl">
        <thead>
          <tr><th>일시</th><th>구분</th><th>트레이너</th><th>상태</th>{onCancel && <th />}</tr>
        </thead>
        <tbody>
          {rows.map(r => (
            <tr key={r.reservationId}>
              <td>{fmtMD(r.lessonDate)} <span className="num">{r.startTime}</span></td>
              <td>{r.ptType === '그룹' ? `그룹 · ${r.className}` : '개인 PT'}</td>
              <td>{r.trainerName}</td>
              <td>
                <Badge s={r.status}>{r.status === '취소' && r.canceledBy === '폐강' ? '폐강' : undefined}</Badge>
                {r.status === '취소' && r.canceledBy && r.canceledBy !== '회원' && r.canceledBy !== '폐강' && <span className="muted small"> {r.canceledBy} 취소{r.cancelReason ? ` · ${r.cancelReason}` : ''}</span>}
                {r.isDeducted && r.status === '취소' && <span className="muted small"> · 1회 차감</span>}
              </td>
              {onCancel && <td style={{ textAlign: 'right' }}><button className="btn sm" onClick={() => onCancel(r)}>예약 취소</button></td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function CancelModal({ r, onClose, onDone }) {
  const toast = useToast()
  const pv = useAsync(() => reservations.preview(r.reservationId), [r.reservationId])
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')
  const go = async () => {
    setBusy(true)
    setErr('')
    try {
      const res = await reservations.cancel(r.reservationId, !!pv.data?.willDeduct)
      toast(res.message || '예약을 취소했어요.')
      onDone()
    } catch (e) {
      setErr(e.message)
      pv.reload()
    } finally {
      setBusy(false)
    }
  }
  return (
    <Modal title="예약을 취소할까요?" onClose={onClose}>
      <dl className="kv">
        <dt>일시</dt><dd>{fmtMD(r.lessonDate)} {r.startTime}</dd>
        <dt>구분</dt><dd>{r.ptType === '그룹' ? `그룹 · ${r.className}` : '개인 PT'}</dd>
        <dt>트레이너</dt><dd>{r.trainerName}</dd>
      </dl>
      {pv.loading ? <Loading text="취소 조건을 확인하는 중이에요…" /> : pv.error ? <div className="alert bad">{pv.error.message}</div> : (
        <div className={`alert ${!pv.data.canCancel ? 'bad' : pv.data.willDeduct ? 'warn' : 'info'}`}>
          <b>{!pv.data.canCancel ? '취소할 수 없어요.' : pv.data.willDeduct ? `취소하면 PT ${pv.data.deductionCount}회가 차감돼요.` : '지금 취소하면 횟수가 차감되지 않아요.'}</b>
          <div className="small" style={{ marginTop: 4 }}>{pv.data.message}</div>
        </div>
      )}
      {err && <div className="alert bad">{err}</div>}
      <div className="row end">
        <button className="btn" onClick={onClose} disabled={busy}>닫기</button>
        {pv.data?.canCancel && (
          <button className={`btn ${pv.data.willDeduct ? 'danger' : 'primary'}`} onClick={go} disabled={busy}>
            {busy ? '취소하는 중…' : pv.data.willDeduct ? '차감하고 취소' : '예약 취소'}
          </button>
        )}
      </div>
    </Modal>
  )
}
