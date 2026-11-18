import { Link } from 'react-router-dom'
import { passes, attendance, reservations } from '../../api/endpoints'
import { useAsync } from '../../lib/hooks'
import { Badge, ErrorBox, Loading } from '../../components/ui'
import { diffDays, fmtDate, fmtMD, today, nowLocalISO } from '../../lib/format'

export default function MemberHome() {
  const p = useAsync(() => passes.mine(), [])
  const monthStart = today().slice(0, 8) + '01'
  const att = useAsync(() => attendance.mine({ startDate: monthStart, endDate: today(), page: 1, size: 1 }), [])
  const res = useAsync(() => reservations.mine({ status: '예약' }), [])

  const now = nowLocalISO()
  const upcoming = (res.data?.reservations || [])
    .filter(r => `${r.lessonDate}T${r.startTime}:00` > now)
    .sort((a, b) => (a.lessonDate + a.startTime).localeCompare(b.lessonDate + b.startTime))
  const next = upcoming[0]

  const ms = p.data?.memberships || []
  const current = ms.find(m => m.status === '이용중')
  const nextM = ms.find(m => m.status === '이용예정')
  const pts = (p.data?.ptPasses || []).filter(x => x.status !== '만료')

  return (
    <main className="page">
      <section className="grid" style={{ gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))' }}>
        <div className="card irid" style={{ minHeight: 300, justifyContent: 'space-between', gridColumn: '1 / -1' }}>
          {p.loading && !p.data ? <Loading /> : p.error ? <ErrorBox error={p.error} onRetry={p.reload} /> : current ? (
            <>
              <div className="stack" style={{ gap: 8 }}>
                <span className="muted">회원권 {current.productName}</span>
                <div className="row" style={{ alignItems: 'baseline', gap: 12 }}>
                  <span className="big">{current.remainingDays ?? diffDays(today(), current.endDate)}</span>
                  <span style={{ fontWeight: 900, fontSize: 28 }}>일 남음</span>
                </div>
                <div style={{ maxWidth: 420 }}>
                  <div className="bar"><i style={{ width: `${Math.min(100, Math.max(2, (diffDays(current.startDate, today()) / Math.max(1, diffDays(current.startDate, current.endDate))) * 100))}%` }} /></div>
                </div>
                <span className="muted small">{fmtDate(current.startDate)}부터 {fmtDate(current.endDate)}까지{nextM ? ` · 다음 회원권 ${nextM.productName} ${fmtDate(nextM.startDate)} 시작` : ''}</span>
              </div>
              <div className="row between">
                <span className="muted">입구 키오스크에서 전화번호를 누르면 체크인돼요.</span>
                <span>이번 달 <b className="num" style={{ fontSize: 20 }}>{att.data?.totalCount ?? '-'}</b>번 출석했어요.</span>
              </div>
            </>
          ) : (
            <div className="stack">
              <span className="muted">회원권</span>
              <h2 style={{ margin: 0, fontSize: 26, fontWeight: 900 }}>이용 중인 회원권이 없어요</h2>
              {nextM && <span className="muted">{nextM.productName}이 {fmtDate(nextM.startDate)}부터 시작해요.</span>}
              <div><Link to="/member/shop" className="btn primary">회원권 구매</Link></div>
            </div>
          )}
        </div>

        <div className="card">
          <div className="row between">
            <h3>PT 이용권</h3>
            <Link to="/member/book" className="btn sm">수업 예약</Link>
          </div>
          {p.loading && !p.data ? <Loading /> : pts.length === 0 ? (
            <div className="stack">
              <span className="muted">사용할 수 있는 PT 이용권이 없어요.</span>
              <Link to="/member/shop" className="btn sm" style={{ alignSelf: 'flex-start' }}>PT 이용권 구매</Link>
            </div>
          ) : pts.map(x => (
            <div key={x.ptPassId} className="stack" style={{ gap: 6 }}>
              <div className="row between">
                <b>{x.productName}</b>
                <Badge s={x.status} />
              </div>
              <span className="muted small">{x.trainerName ? `${x.trainerName} 트레이너, ` : ''}{fmtDate(x.endDate)}까지</span>
              <div className="row" style={{ alignItems: 'baseline', gap: 6 }}>
                <span className="stat">{x.availableCount}</span><span className="muted">/ {x.totalCount}회 예약 가능</span>
              </div>
              <span className="muted small">예약 중 {x.reservedCount}회 · 사용 {x.usedCount}회</span>
            </div>
          ))}
        </div>

        <div className="card">
          <div className="row between">
            <h3>다음 수업</h3>
            <Link to="/member/reservations" className="btn sm">내 예약 보기</Link>
          </div>
          {res.loading && !res.data ? <Loading /> : next ? (
            <div className="stack" style={{ gap: 6 }}>
              <span style={{ fontWeight: 900, fontSize: 20 }}>{fmtMD(next.lessonDate)}</span>
              <span className="stat">{next.startTime}</span>
              <span className="muted">{next.ptType === '그룹' ? `그룹 · ${next.className}` : '개인 PT'} · {next.trainerName} 트레이너 · 60분</span>
            </div>
          ) : (
            <div className="stack">
              <span className="muted">예정된 수업이 없어요.</span>
              <Link to="/member/book" className="btn sm" style={{ alignSelf: 'flex-start' }}>PT 예약하기</Link>
            </div>
          )}
        </div>
      </section>
    </main>
  )
}
