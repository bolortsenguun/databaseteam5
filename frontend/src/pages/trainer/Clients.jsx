import { passes } from '../../api/endpoints'
import { useAsync } from '../../lib/hooks'
import { Async, Badge, PageTitle } from '../../components/ui'
import { fmtDate, fmtPhone } from '../../lib/format'

export default function TrainerClients() {
  const c = useAsync(() => passes.clients(), [])
  return (
    <main className="page">
      <PageTitle title="담당 회원" sub="나를 담당 트레이너로 고른 개인 PT 회원이에요. 만료된 이용권은 빠져요." />
      <Async state={c} isEmpty={d => d.clients.length === 0} empty="아직 담당 회원이 없어요.">
        {d => (
          <>
            <span className="muted">담당 회원 <b className="num" style={{ color: 'var(--text)' }}>{d.totalClients}</b>명</span>
            <div className="card">
              <div className="tblwrap">
                <table className="tbl">
                  <thead><tr><th>회원</th><th>전화번호</th><th>이용권</th><th>기간</th><th>상태</th><th>예약 가능</th><th>예약 중</th><th>사용</th></tr></thead>
                  <tbody>
                    {d.clients.flatMap(cl => cl.ptPasses.map((p, i) => (
                      <tr key={p.ptPassId}>
                        <td>{i === 0 ? <b>{cl.name}</b> : ''}</td>
                        <td className="muted">{i === 0 ? fmtPhone(cl.phone) : ''}</td>
                        <td>{p.productName}</td>
                        <td className="small">{fmtDate(p.startDate)} ~ {fmtDate(p.endDate)}</td>
                        <td><Badge s={p.status} /></td>
                        <td className="num">{p.availableCount}</td>
                        <td className="num">{p.reservedCount}</td>
                        <td className="num">{p.usedCount} / {p.totalCount}</td>
                      </tr>
                    )))}
                  </tbody>
                </table>
              </div>
            </div>
          </>
        )}
      </Async>
    </main>
  )
}
