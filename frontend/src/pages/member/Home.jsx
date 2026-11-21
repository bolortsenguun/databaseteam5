import { Link } from 'react-router-dom'
import { passes, attendance, reservations } from '../../api/endpoints'
import { useAsync } from '../../lib/hooks'
import { ErrorBox, Loading } from '../../components/ui'
import { diffDays, fmtDate, fmtMD, today, nowLocalISO } from '../../lib/format'

function Segments({ total, available, reserved }) {
  const cells = Array.from({ length: total }, (_, i) => (i < available ? 'on' : i < available + reserved ? 'mid' : ''))
  return (
    <div className="home-seg" role="img" aria-label={`${total}회 중 예약 가능 ${available}회, 예약 중 ${reserved}회, 사용 ${total - available - reserved}회`} style={{ gridTemplateColumns: `repeat(${Math.max(1, total)}, minmax(0, 1fr))` }}>
      {cells.map((c, i) => <span key={i} className={c} />)}
    </div>
  )
}

function Membership({ p, att }) {
  if (p.loading && !p.data) return <Loading />
  if (p.error) return <ErrorBox error={p.error} onRetry={p.reload} />
  const ms = p.data?.memberships || []
  const current = ms.find(m => m.status === '이용중')
  const nextM = ms.find(m => m.status === '이용예정')
  const count = att.data?.totalCount

  if (!current) {
    return (
      <div className="home-left">
        <div className="stack" style={{ gap: 10 }}>
          <span className="home-label">회원권</span>
          <h2 className="home-empty">이용 중인 회원권이 없어요</h2>
          {nextM && <span className="muted">{nextM.productName}이 {fmtDate(nextM.startDate)}부터 시작해요.</span>}
          <div><Link to="/member/shop" className="btn primary">회원권 구매</Link></div>
        </div>
      </div>
    )
  }

  const total = Math.max(1, diffDays(current.startDate, current.endDate))
  const pct = Math.min(100, Math.max(2, (diffDays(current.startDate, today()) / total) * 100))

  return (
    <div className="home-left">
      <div className="stack" style={{ gap: 10 }}>
        <span className="home-label">회원권 {current.productName}</span>
        <div className="row" style={{ alignItems: 'baseline', gap: 12 }}>
          <span className="home-days">{current.remainingDays ?? diffDays(today(), current.endDate)}</span>
          <span className="home-days-unit">일 남음</span>
        </div>
        <div className="bar home-bar" role="img" aria-label={`${Math.round(pct)}%`}><i style={{ width: `${pct}%` }} /></div>
        <span className="muted small">
          {fmtDate(current.startDate)}부터 {fmtDate(current.endDate)}까지
          {nextM ? ` · 다음 회원권 ${nextM.productName} ${fmtDate(nextM.startDate)} 시작` : ''}
        </span>
      </div>
      <div className="stack" style={{ gap: 6, maxWidth: 420 }}>
        <b style={{ fontSize: 18 }}>입구 키오스크에서 체크인</b>
        <span className="muted small" style={{ lineHeight: 1.55 }}>
          전화번호를 누르면 돼요.{count != null ? ` 이번 달에는 ${count}번 출석했어요.` : ''}
        </span>
      </div>
    </div>
  )
}

function PtCard({ p }) {
  const order = { '이용중': 0, '이용예정': 1, '소진': 2 }
  const pts = (p.data?.ptPasses || []).filter(x => x.status !== '만료')
    .sort((a, b) => (order[a.status] ?? 9) - (order[b.status] ?? 9) || a.endDate.localeCompare(b.endDate))   // 먼저 쓰게 될 이용권부터
  return (
    <div className="home-glass">
      <div className="row between">
        <h2>PT 이용권</h2>
        <Link to="/member/book" className="home-link">수업 예약</Link>
      </div>
      {p.loading && !p.data ? <Loading /> : pts.length === 0 ? (
        <div className="stack" style={{ gap: 10 }}>
          <span className="home-sub">사용할 수 있는 PT 이용권이 없어요.</span>
          <Link to="/member/shop" className="btn sm" style={{ alignSelf: 'flex-start' }}>PT 이용권 구매</Link>
        </div>
      ) : pts.map(x => (
        <div key={x.ptPassId} className="stack" style={{ gap: 14 }}>
          <div className="row between" style={{ alignItems: 'flex-end' }}>
            <div className="stack" style={{ gap: 2 }}>
              <span className="home-title">{x.productName}</span>
              <span className="home-sub">{x.trainerName ? `${x.trainerName} 트레이너, ` : ''}{fmtDate(x.endDate)}까지</span>
            </div>
            <div className="row" style={{ alignItems: 'baseline', gap: 4 }}>
              <span className="home-num" style={{ fontSize: 48, lineHeight: 1 }}>{x.availableCount}</span>
              <span className="home-sub" style={{ fontSize: 15 }}>/ {x.totalCount}회</span>
            </div>
          </div>
          <Segments total={x.totalCount} available={x.availableCount} reserved={x.reservedCount} />
          <span className="home-sub">예약 가능 {x.availableCount}회, 예약 중 {x.reservedCount}회, 사용 {x.usedCount}회</span>
        </div>
      ))}
    </div>
  )
}

function NextCard({ res }) {
  const now = nowLocalISO()
  const next = (res.data?.reservations || [])
    .filter(r => `${r.lessonDate}T${r.startTime}:00` > now)
    .sort((a, b) => (a.lessonDate + a.startTime).localeCompare(b.lessonDate + b.startTime))[0]
  return (
    <div className="home-glass">
      <div className="row between">
        <h2>다음 수업</h2>
        <Link to="/member/reservations" className="home-link">내 예약 보기</Link>
      </div>
      {res.loading && !res.data ? <Loading /> : next ? (
        <div className="row" style={{ gap: 16, flexWrap: 'nowrap' }}>
          <span className="orb home-orb" aria-hidden="true" />
          <div className="stack" style={{ flex: 1, gap: 2, minWidth: 0 }}>
            <span className="home-title">{fmtMD(next.lessonDate)}</span>
            <span className="home-sub">
              {next.ptType === '그룹' ? `그룹 ${next.className ? `· ${next.className}` : ''}, ` : ''}{next.trainerName} 트레이너, 60분
            </span>
          </div>
          <span className="home-num" style={{ fontSize: 38 }}>{next.startTime}</span>
        </div>
      ) : (
        <div className="stack" style={{ gap: 10 }}>
          <span className="home-sub">예정된 수업이 없어요.</span>
          <Link to="/member/book" className="btn sm" style={{ alignSelf: 'flex-start' }}>PT 예약하기</Link>
        </div>
      )}
    </div>
  )
}

export default function MemberHome() {
  const p = useAsync(() => passes.mine(), [])
  const monthStart = today().slice(0, 8) + '01'
  const att = useAsync(() => attendance.mine({ startDate: monthStart, endDate: today(), page: 1, size: 1 }), [])
  const res = useAsync(() => reservations.mine({ status: '예약' }), [])

  return (
    <main className="page">
      <section className="home-hero">
        <div className="home-glow" aria-hidden="true" />
        <div className="home-glow2" aria-hidden="true" />
        <Membership p={p} att={att} />
        <div className="home-right">
          <PtCard p={p} />
          <NextCard res={res} />
        </div>
      </section>
    </main>
  )
}
