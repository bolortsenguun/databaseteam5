import { Navigate, NavLink, Outlet, Link } from 'react-router-dom'
import { useAuth, landingFor, HOME } from '../lib/auth'
import { Loading } from './ui'
import { mockMode } from '../api/mock'

const NAV = {
  MEMBER: [
    ['/member', '홈', true],
    ['/member/book', 'PT 예약'],
    ['/member/reservations', '내 예약'],
    ['/member/passes', '내 이용권'],
    ['/member/shop', '이용권 구매'],
    ['/member/trainers', '트레이너'],
    ['/member/history', '이용 내역'],
    ['/member/profile', '내 정보']
  ],
  TRAINER: [
    ['/trainer', '수업 일정', true],
    ['/trainer/groups', '그룹 수업'],
    ['/trainer/clients', '담당 회원'],
    ['/trainer/schedule', '근무 시간'],
    ['/trainer/profile', '내 정보']
  ],
  ADMIN: [
    ['/admin', '현황', true],
    ['/admin/stats', '통계'],
    ['/admin/members', '회원'],
    ['/admin/trainers', '트레이너'],
    ['/admin/products', '상품'],
    ['/admin/payments', '결제'],
    ['/admin/reservations', '예약'],
    ['/admin/attendance', '출석'],
    ['/admin/settings', '운영 설정']
  ]
}

export function Brand({ to = '/' }) {
  return (
    <Link to={to} className="brand" aria-label="fatenexx 홈">
      <span className="orb" aria-hidden="true" />
      <b>fatenexx</b>
    </Link>
  )
}

export function MockBadge() {
  if (mockMode === 'off') return null
  return (
    <span className="badge warn" title={mockMode === 'all' ? '모든 API를 목업으로 사용 중' : '백엔드에 아직 없는 API만 목업으로 사용 중'}>
      {mockMode === 'all' ? '목업 모드' : '일부 목업'}
    </span>
  )
}

export function RoleLayout({ role }) {
  const { me, logout } = useAuth()
  if (me === undefined) return <Loading />
  if (!me) return <Navigate to="/login" replace />
  const landing = landingFor(me)
  if (landing !== HOME[me.role] || me.role !== role) return <Navigate to={landing} replace />
  return (
    <>
      <header className="hdr">
        <Brand to={HOME[role]} />
        <nav className="nav" aria-label="주 메뉴">
          {NAV[role].map(([to, label, end]) => (
            <NavLink key={to} to={to} end={end} className={({ isActive }) => (isActive ? 'active' : '')}>{label}</NavLink>
          ))}
        </nav>
        <div className="who">
          <MockBadge />
          <b>{role === 'ADMIN' ? '관리자' : `${me.name} 님`}</b>
          <button className="linkbtn" onClick={logout}>로그아웃</button>
        </div>
      </header>
      <Outlet />
    </>
  )
}
