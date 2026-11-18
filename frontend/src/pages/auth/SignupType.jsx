import { Link } from 'react-router-dom'
import { Hero } from './Login'

export default function SignupType() {
  return (
    <div className="auth-wrap">
      <Hero />
      <section className="auth-side">
        <Link to="/login" className="small" style={{ textDecoration: 'none' }}>← 로그인으로 돌아가기</Link>
        <div>
          <h1 style={{ margin: 0, fontSize: 30, fontWeight: 900, letterSpacing: '-0.03em' }}>어떤 계정을 만들까요?</h1>
          <p className="muted" style={{ margin: '8px 0 0', lineHeight: 1.6 }}>회원은 가입하면 바로 쓸 수 있어요. 트레이너는 관리자가 승인한 뒤에 쓸 수 있어요.</p>
        </div>
        <div className="card irid">
          <h3>회원</h3>
          <p className="muted" style={{ margin: 0 }}>회원권을 사고 PT를 예약해요.</p>
          <Link to="/signup/member" className="btn primary">선택하기</Link>
        </div>
        <div className="card">
          <h3>트레이너</h3>
          <p className="muted" style={{ margin: 0 }}>수업과 근무 시간을 관리해요. 가입 뒤 승인이 필요해요.</p>
          <Link to="/signup/trainer" className="btn">선택하기</Link>
        </div>
      </section>
    </div>
  )
}
