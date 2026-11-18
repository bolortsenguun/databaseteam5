import { Link } from 'react-router-dom'
import { stats, attendance } from '../../api/endpoints'
import { useAsync } from '../../lib/hooks'
import { Async, PageTitle } from '../../components/ui'
import { fmtDateTime, fmtMoney, today } from '../../lib/format'

function Tile({ label, value, unit, to, hint, accent }) {
  const body = (
    <div className={`card ${accent ? 'irid' : ''}`} style={{ gap: 8, height: '100%' }}>
      <span className="muted small">{label}</span>
      <span className="stat">{value ?? '-'}<small>{unit}</small></span>
      {hint && <span className="dim small">{hint}</span>}
    </div>
  )
  return to ? <Link to={to} style={{ textDecoration: 'none', color: 'inherit' }}>{body}</Link> : body
}

export default function AdminDashboard() {
  const d = useAsync(() => stats.dashboard(), [])
  const att = useAsync(() => attendance.adminList({ startDate: today(), endDate: today(), page: 1, size: 6 }), [])
  return (
    <main className="page">
      <PageTitle title="현황" sub="오늘 처리할 일부터 확인해요.">
        <button className="btn sm" onClick={() => { d.reload(); att.reload() }}>새로고침</button>
      </PageTitle>
      <Async state={d}>
        {x => (
          <>
            <div className="grid g4">
              <Tile accent={x.pendingTrainers > 0} label="승인 대기 트레이너" value={x.pendingTrainers} unit="명" to="/admin/trainers" hint="눌러서 승인하기" />
              <Tile accent={x.pendingResultCount > 0} label="결과 대기 수업" value={x.pendingResultCount} unit="건" to="/admin/reservations?status=결과대기" hint="출석·결석 처리 필요" />
              <Tile label="오늘 입장" value={x.todayCheckins} unit="건" to="/admin/attendance" hint="체크인 기록 수" />
              <Tile label="이번 달 매출" value={x.monthlyRevenue?.toLocaleString()} unit="원" to="/admin/stats" hint={`오늘 ${fmtMoney(x.todayRevenue)}`} />
            </div>
            <div className="grid g2">
              <section className="card">
                <div className="row between">
                  <h3>오늘 출석</h3>
                  <Link to="/admin/attendance" className="btn sm">전체 보기</Link>
                </div>
                <Async state={att} isEmpty={a => a.attendances.length === 0} empty="오늘 체크인한 회원이 아직 없어요.">
                  {a => (
                    <table className="tbl">
                      <thead><tr><th>시각</th><th>이름</th><th>입장 구분</th></tr></thead>
                      <tbody>
                        {a.attendances.map(r => <tr key={r.attendanceId}><td className="num">{r.checkedInAt.slice(11, 16)}</td><td>{r.userName}</td><td>{r.entryType === 'PT' ? 'PT' : '회원권'}</td></tr>)}
                      </tbody>
                    </table>
                  )}
                </Async>
              </section>
              <section className="card">
                <h3>운영 요약</h3>
                <dl className="kv">
                  <dt>회원</dt><dd>{x.totalMembers}명 <span className="muted small">(회원권 이용 중 {x.activeMembershipMembers}명)</span></dd>
                  <dt>트레이너</dt><dd>{x.approvedTrainers}명 <span className="muted small">(승인 대기 {x.pendingTrainers}명)</span></dd>
                  <dt>오늘 예약</dt><dd>{x.todayReservations}건</dd>
                  <dt>폐강 위험</dt><dd>{x.closingSoonClassCount}건 <span className="muted small">(최소 인원 미달 그룹 수업)</span></dd>
                </dl>
                {x.asOf && <span className="dim small">{fmtDateTime(x.asOf)} 기준</span>}
                <Link to="/admin/settings" className="btn sm" style={{ alignSelf: 'flex-start' }}>운영 설정</Link>
              </section>
            </div>
          </>
        )}
      </Async>
    </main>
  )
}
