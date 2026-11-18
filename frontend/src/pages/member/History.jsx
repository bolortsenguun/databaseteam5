import { useState } from 'react'
import { attendance, payments } from '../../api/endpoints'
import { useAsync } from '../../lib/hooks'
import { Async, Badge, PageTitle, Pager, Seg } from '../../components/ui'
import { fmtDateTime, fmtMoney } from '../../lib/format'

export default function MemberHistory() {
  const [aPage, setAPage] = useState(1)
  const [entry, setEntry] = useState('전체')
  const [pPage, setPPage] = useState(1)
  const att = useAsync(() => attendance.mine({ entryType: entry, page: aPage, size: 10 }), [aPage, entry])
  const pay = useAsync(() => payments.mine({ page: pPage, size: 10 }), [pPage])
  return (
    <main className="page">
      <PageTitle title="이용 내역" sub="출석과 결제 기록을 모아서 봐요." />
      <div className="grid g2" style={{ alignItems: 'start' }}>
        <section className="card">
          <div className="row between">
            <h3>출석 기록</h3>
            <Seg value={entry} options={[['전체', '전체'], ['회원권', '회원권'], ['PT', 'PT']]} onChange={v => { setEntry(v); setAPage(1) }} />
          </div>
          <Async state={att} isEmpty={d => d.attendances.length === 0} empty="아직 체크인 기록이 없어요.">
            {d => (
              <>
                <table className="tbl">
                  <thead><tr><th>체크인 시각</th><th>입장 구분</th></tr></thead>
                  <tbody>
                    {d.attendances.map(a => (
                      <tr key={a.attendanceId}><td>{fmtDateTime(a.checkedInAt)}</td><td>{a.entryType === 'PT' ? 'PT 예약' : '회원권'}</td></tr>
                    ))}
                  </tbody>
                </table>
                <Pager page={d.page} totalPages={d.totalPages} onChange={setAPage} />
              </>
            )}
          </Async>
        </section>
        <section className="card">
          <h3>결제 내역</h3>
          <Async state={pay} isEmpty={d => d.payments.length === 0} empty="아직 결제 기록이 없어요.">
            {d => (
              <>
                <table className="tbl">
                  <thead><tr><th>일시</th><th>상품</th><th>금액</th><th>결과</th></tr></thead>
                  <tbody>
                    {d.payments.map(x => (
                      <tr key={x.paymentId}>
                        <td>{fmtDateTime(x.paidAt)}</td>
                        <td>{x.productName || '-'}</td>
                        <td className="num">{fmtMoney(x.amount)}</td>
                        <td><Badge s={x.status} /></td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                <span className="dim small">실패한 결제는 상품 정보가 남지 않아 '-'로 보여요.</span>
                <Pager page={d.page} totalPages={d.totalPages} onChange={setPPage} />
              </>
            )}
          </Async>
        </section>
      </div>
    </main>
  )
}
