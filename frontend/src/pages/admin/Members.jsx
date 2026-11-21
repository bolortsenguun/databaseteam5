import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { members } from '../../api/endpoints'
import { useAsync } from '../../lib/hooks'
import { Async, Badge, PageTitle, Pager, Seg } from '../../components/ui'
import { fmtDate, fmtPhone, genderLabel } from '../../lib/format'

export default function AdminMembers() {
  const nav = useNavigate()
  const [kw, setKw] = useState('')
  const [q, setQ] = useState({ keyword: '', status: '', page: 1 })
  const list = useAsync(() => members.list({ keyword: q.keyword, membershipStatus: q.status, page: q.page, size: 20 }), [q.keyword, q.status, q.page])
  return (
    <main className="page">
      <PageTitle title="회원" sub="이름을 누르면 회원 상세를 볼 수 있어요." />
      <form className="inline" onSubmit={e => { e.preventDefault(); setQ({ ...q, keyword: kw.trim(), page: 1 }) }} style={{ maxWidth: 560 }}>
        <input className="input" placeholder="이름, 아이디, 전화번호로 검색" value={kw} onChange={e => setKw(e.target.value)} />
        <button className="btn primary">검색</button>
        {q.keyword && <button type="button" className="btn" onClick={() => { setKw(''); setQ({ ...q, keyword: '', page: 1 }) }}>초기화</button>}
      </form>
      <Seg value={q.status} options={[['', '전체'], ['이용중', '이용 중'], ['미이용', '미이용']]} onChange={v => setQ({ ...q, status: v, page: 1 })} />
      <Async state={list} isEmpty={d => d.members.length === 0} empty={q.keyword || q.status ? '조건에 맞는 회원이 없어요.' : '가입한 회원이 없어요.'}>
        {d => (
          <div className="card">
            <span className="muted small">{q.status ? `${q.status === '이용중' ? '이용 중' : '미이용'} ` : '전체 '}{d.totalCount}명</span>
            <div className="tblwrap">
              <table className="tbl">
                <thead><tr><th>이름</th><th>아이디</th><th>전화번호</th><th>성별</th><th>가입일</th><th>회원권</th></tr></thead>
                <tbody>
                  {d.members.map(m => (
                    <tr key={m.userId} className="click" onClick={() => nav(`/admin/members/${m.userId}`)}>
                      <td><b>{m.name}</b></td>
                      <td>{m.loginId}</td>
                      <td>{fmtPhone(m.phone)}</td>
                      <td>{genderLabel(m.gender)}</td>
                      <td>{fmtDate(m.createdAt)}</td>
                      <td><Badge s={m.membershipStatus} tone={m.membershipStatus === '이용중' ? 'ok' : ''} /></td>
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
