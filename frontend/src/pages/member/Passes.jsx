import { Link } from 'react-router-dom'
import { passes } from '../../api/endpoints'
import { useAsync } from '../../lib/hooks'
import { Async, Badge, Empty, PageTitle } from '../../components/ui'
import { fmtDate } from '../../lib/format'

export default function MemberPasses() {
  const p = useAsync(() => passes.mine(), [])
  return (
    <main className="page">
      <PageTitle title="내 이용권" sub="회원권과 PT 이용권을 한곳에서 봐요.">
        <Link to="/member/shop" className="btn primary">이용권 구매</Link>
      </PageTitle>
      <Async state={p}>
        {d => {
          const ms = d.memberships.filter(m => m.status !== '만료')
          const pts = d.ptPasses.filter(x => x.status !== '만료')
          const expired = [...d.memberships.filter(m => m.status === '만료').map(m => ({ id: 'm' + m.membershipId, name: m.productName, start: m.startDate, end: m.endDate })), ...d.ptPasses.filter(x => x.status === '만료').map(x => ({ id: 'p' + x.ptPassId, name: x.productName, start: x.startDate, end: x.endDate }))]
          return (
            <>
              <section className="stack">
                <h2 className="sec">회원권</h2>
                {ms.length === 0 ? <Empty>이용 중이거나 이용 예정인 회원권이 없어요.</Empty> : (
                  <div className="grid g2">
                    {ms.map(m => (
                      <div key={m.membershipId} className={`card ${m.status === '이용중' ? 'irid' : ''}`}>
                        <div className="row between"><b style={{ fontSize: 18 }}>{m.productName}</b><Badge s={m.status} /></div>
                        {m.status === '이용중' ? (
                          <div className="row" style={{ alignItems: 'baseline', gap: 8 }}><span className="stat">{m.remainingDays}</span><span className="muted">일 남음</span></div>
                        ) : <span className="muted small">지금 회원권이 끝난 다음 날부터 시작해요.</span>}
                        <span className="muted">{fmtDate(m.startDate)} ~ {fmtDate(m.endDate)}</span>
                      </div>
                    ))}
                  </div>
                )}
              </section>
              <section className="stack">
                <div className="row between">
                  <h2 className="sec">PT 이용권</h2>
                  <Link to="/member/book" className="btn sm">수업 예약</Link>
                </div>
                {pts.length === 0 ? <Empty>사용할 수 있는 PT 이용권이 없어요.</Empty> : (
                  <div className="grid g2">
                    {pts.map(x => (
                      <div key={x.ptPassId} className="card">
                        <div className="row between"><b style={{ fontSize: 18 }}>{x.productName}</b><Badge s={x.status} /></div>
                        <span className="muted">{x.trainerName ? `${x.trainerName} 트레이너, ` : ''}{fmtDate(x.startDate)} ~ {fmtDate(x.endDate)}</span>
                        <div className="grid" style={{ gap: 12, gridTemplateColumns: 'repeat(3, 1fr)' }}>
                          <div><div className="stat">{x.availableCount}</div><span className="muted small">예약 가능</span></div>
                          <div><div className="stat">{x.reservedCount}</div><span className="muted small">예약 중</span></div>
                          <div><div className="stat">{x.usedCount}</div><span className="muted small">사용함</span></div>
                        </div>
                        <div className="bar"><i style={{ width: `${(x.usedCount / x.totalCount) * 100}%` }} /></div>
                        <span className="dim small">전체 {x.totalCount}회 · 남은 {x.remainingCount}회</span>
                      </div>
                    ))}
                  </div>
                )}
              </section>
              <section className="card">
                <h3>만료된 이용권</h3>
                {expired.length === 0 ? <Empty>만료된 이용권이 없어요.</Empty> : (
                  <div className="tblwrap">
                    <table className="tbl">
                      <thead><tr><th>이용권</th><th>기간</th><th>상태</th></tr></thead>
                      <tbody>
                        {expired.map(e => (
                          <tr key={e.id}><td>{e.name}</td><td>{fmtDate(e.start)} ~ {fmtDate(e.end)}</td><td><Badge s="만료" /></td></tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </section>
            </>
          )
        }}
      </Async>
    </main>
  )
}
