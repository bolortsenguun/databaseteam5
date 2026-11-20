import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { members } from '../../api/endpoints'
import { useAsync } from '../../lib/hooks'
import { Async, PageTitle, Pager } from '../../components/ui'
import { fmtDate, fmtPhone, genderLabel } from '../../lib/format'

export default function AdminMembers() {
  const nav = useNavigate()
  const [kw, setKw] = useState('')
  const [q, setQ] = useState({ keyword: '', page: 0 })
  const list = useAsync(() => members.list({ keyword: q.keyword, page: q.page, size: 20 }), [q.keyword, q.page])
  return (
    <main className="page">
      <PageTitle title="회원" sub="이름을 누르면 회원 상세를 볼 수 있어요." />
      <form className="inline" onSubmit={e => { e.preventDefault(); setQ({ keyword: kw.trim(), page: 0 }) }} style={{ maxWidth: 560 }}>
        <input className="input" placeholder="이름, 아이디, 전화번호로 검색" value={kw} onChange={e => setKw(e.target.value)} />
        <button className="btn primary">검색</button>
        {q.keyword && <button type="button" className="btn" onClick={() => { setKw(''); setQ({ keyword: '', page: 0 }) }}>초기화</button>}
      </form>
      <Async state={list} isEmpty={d => d.members.length === 0} empty={q.keyword ? '검색 결과가 없어요.' : '가입한 회원이 없어요.'}>
        {d => (
          <div className="card">
            <span className="muted small">전체 {d.totalElements}명</span>
            <div className="tblwrap">
              <table className="tbl">
                <thead><tr><th>이름</th><th>아이디</th><th>전화번호</th><th>성별</th><th>가입일</th></tr></thead>
                <tbody>
                  {d.members.map(m => (
                    <tr key={m.userId} className="click" onClick={() => nav(`/admin/members/${m.userId}`)}>
                      <td><b>{m.name}</b></td>
                      <td>{m.loginId}</td>
                      <td>{fmtPhone(m.phone)}</td>
                      <td>{genderLabel(m.gender)}</td>
                      <td>{fmtDate(m.createdAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pager page={d.page} totalPages={d.totalPages} base={0} onChange={p => setQ({ ...q, page: p })} />
          </div>
        )}
      </Async>
    </main>
  )
}
