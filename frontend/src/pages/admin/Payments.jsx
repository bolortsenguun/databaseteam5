import { useState } from 'react'
import { payments } from '../../api/endpoints'
import { useAsync } from '../../lib/hooks'
import { Async, Badge, Field, PageTitle, Pager, Seg } from '../../components/ui'
import { fmtDateTime, fmtMoney } from '../../lib/format'

export default function AdminPayments() {
  const [f, setF] = useState({ status: '전체', startDate: '', endDate: '' })
  const [q, setQ] = useState({ ...f, page: 1 })
  const [err, setErr] = useState('')
  const list = useAsync(() => payments.adminList({ ...q, size: 20 }), [q.status, q.startDate, q.endDate, q.page])
  const apply = next => {
    const v = { ...f, ...next }
    if (v.startDate && v.endDate && v.startDate > v.endDate) return setErr('시작일은 종료일보다 늦을 수 없어요.')
    setErr('')
    setF(v)
    setQ({ ...v, page: 1 })
  }
  return (
    <main className="page">
      <PageTitle title="결제" sub="결제 내역은 읽기만 할 수 있어요. 실패한 결제는 매출에 들어가지 않아요." />
      <div className="card">
        <div className="row" style={{ alignItems: 'flex-end' }}>
          <div className="field">결과<Seg value={f.status} options={[['전체', '전체'], ['성공', '성공'], ['실패', '실패']]} onChange={v => apply({ status: v })} /></div>
          <Field label="시작일"><input className="input" type="date" value={f.startDate} onChange={e => setF({ ...f, startDate: e.target.value })} /></Field>
          <Field label="종료일"><input className="input" type="date" value={f.endDate} onChange={e => setF({ ...f, endDate: e.target.value })} /></Field>
          <button className="btn primary" onClick={() => apply({})}>조회</button>
        </div>
        {err && <div className="alert bad small">{err}</div>}
      </div>
      <Async state={list} isEmpty={d => d.payments.length === 0} empty="조건에 맞는 결제가 없어요.">
        {d => (
          <div className="card">
            <span className="muted small">{d.totalCount}건{d.clientFiltered ? ' · 백엔드에 검색 조건이 아직 없어서 최근 100건 안에서만 걸러 보여요.' : ''}</span>
            <div className="tblwrap">
              <table className="tbl">
                <thead><tr><th>일시</th><th>회원</th><th>구분</th><th>상품</th><th>금액</th><th>결과</th></tr></thead>
                <tbody>
                  {d.payments.map(x => (
                    <tr key={x.paymentId}>
                      <td>{fmtDateTime(x.paidAt)}</td>
                      <td>{x.userName}</td>
                      <td>{x.productType || '-'}</td>
                      <td>{x.productName || '-'}</td>
                      <td className="num">{fmtMoney(x.amount)}</td>
                      <td><Badge s={x.status} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pager page={d.page} totalPages={d.totalPages} onChange={p => setQ({ ...q, page: p })} />
          </div>
        )}
      </Async>
    </main>
  )
}
