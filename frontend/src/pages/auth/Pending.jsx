import { useEffect, useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth, landingFor } from '../../lib/auth'
import { Brand } from '../../components/Layout'
import { Loading } from '../../components/ui'

export default function Pending() {
  const { me, refresh, logout } = useAuth()
  const loc = useLocation()
  const nav = useNavigate()
  const [checking, setChecking] = useState(false)
  const justSignedUp = loc.state?.justSignedUp

  useEffect(() => {
    if (me && me.role === 'TRAINER' && !me.approved) {
      const id = setInterval(refresh, 30000)
      return () => clearInterval(id)
    }
  }, [me, refresh])

  if (me === undefined) return <Loading />
  if (me && !(me.role === 'TRAINER' && !me.approved)) return <Navigate to={landingFor(me)} replace />
  if (!me && !justSignedUp) return <Navigate to="/login" replace />

  const name = me?.name || loc.state?.name || ''

  const recheck = async () => {
    setChecking(true)
    const u = await refresh()
    setChecking(false)
    if (u && u.approved) nav(landingFor(u), { replace: true })
  }

  return (
    <div className="kiosk">
      <Brand to="/login" />
      <div className="card irid" style={{ maxWidth: 560, width: '100%' }}>
        <span className="badge warn" style={{ alignSelf: 'flex-start' }}>승인 대기</span>
        <h2 style={{ margin: 0, fontSize: 26, fontWeight: 900 }}>{name ? `${name} 님, ` : ''}승인을 기다리고 있어요</h2>
        <p className="muted" style={{ margin: 0, lineHeight: 1.7 }}>
          가입 신청이 접수됐어요. 관리자가 승인하면 로그인해서 쓸 수 있어요. 그때까지는 이 안내만 볼 수 있어요.
        </p>
        <div className="alert info small">승인이 끝난 뒤에는 로그아웃하고 다시 로그인해야 트레이너 화면이 열려요.</div>
        <div className="row">
          {me ? (
            <>
              <button className="btn" onClick={recheck} disabled={checking}>{checking ? '확인 중…' : '승인 상태 확인'}</button>
              <button className="btn primary" onClick={logout}>로그아웃</button>
            </>
          ) : (
            <button className="btn primary" onClick={() => nav('/login')}>로그인 화면으로</button>
          )}
        </div>
      </div>
    </div>
  )
}
