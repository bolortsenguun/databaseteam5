import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { products, passes, payments, trainers as trainersApi } from '../../api/endpoints'
import { useAsync, useSafeAsync } from '../../lib/hooks'
import { Async, Field, Modal, PageTitle } from '../../components/ui'
import { addDays, addMonths, fmtDate, fmtMoney, ptTypeKey, today } from '../../lib/format'

const START_MAX_DAYS = 30

function membershipBase(p) {
  if (!p) return today()
  const ends = p.memberships.filter(m => m.status !== '만료').map(m => m.endDate).sort()
  return ends.length ? addDays(ends[ends.length - 1], 1) : today()
}

export default function MemberShop() {
  const [sp] = useSearchParams()
  const prods = useAsync(() => products.list(), [])
  const myPasses = useSafeAsync(() => passes.mine(), null, [])
  const [buy, setBuy] = useState(null)

  const base = membershipBase(myPasses.data)
  const hasActive = myPasses.data?.memberships.some(m => m.status !== '만료')

  useEffect(() => {
    if (!prods.data || buy) return
    const t = sp.get('type')
    if (t === 'personal' || t === 'group') {
      document.getElementById(`sec-${t}`)?.scrollIntoView({ behavior: 'smooth' })
    }
  }, [prods.data])

  return (
    <main className="page">
      <PageTitle title="이용권 구매" sub="결제는 모의 결제예요. 환불과 일시정지는 지원하지 않아요." />
      {hasActive && <div className="alert info">이용 중인 회원권이 있어서, 새 회원권은 <b>{fmtDate(base)}</b>부터 시작할 수 있어요.</div>}
      <Async state={prods}>
        {d => {
          const personal = d.ptProducts.filter(x => ptTypeKey(x.ptType) === 'PERSONAL')
          const group = d.ptProducts.filter(x => ptTypeKey(x.ptType) === 'GROUP')
          return (
            <>
              <Section id="sec-membership" title="회원권" sub="체육관을 자유롭게 이용해요." items={d.membershipProducts} render={x => (
                <>
                  <b style={{ fontSize: 18 }}>{x.name}</b>
                  <span className="muted small">{x.months}개월 동안 자유 입장</span>
                  <span className="stat">{x.price.toLocaleString()}<small>원</small></span>
                  <span className="dim small">월 {Math.round(x.price / x.months).toLocaleString()}원</span>
                </>
              )} onBuy={x => setBuy({ kind: 'membership', product: x })} />
              <Section id="sec-personal" title="개인 PT" sub="담당 트레이너와 1대1로 해요." items={personal} render={x => (
                <>
                  <b style={{ fontSize: 18 }}>{x.name}</b>
                  <span className="muted small">{x.validWeeks}주 안에 사용</span>
                  <span className="stat">{x.price.toLocaleString()}<small>원</small></span>
                  <span className="dim small">회당 {Math.round(x.price / x.totalCount).toLocaleString()}원</span>
                </>
              )} onBuy={x => setBuy({ kind: 'personal', product: x })} />
              <Section id="sec-group" title="그룹 PT" sub="열린 그룹 수업에 자유롭게 신청해요." items={group} render={x => (
                <>
                  <b style={{ fontSize: 18 }}>{x.name}</b>
                  <span className="muted small">{x.validWeeks}주 안에 사용</span>
                  <span className="stat">{x.price.toLocaleString()}<small>원</small></span>
                  <span className="dim small">회당 {Math.round(x.price / x.totalCount).toLocaleString()}원</span>
                </>
              )} onBuy={x => setBuy({ kind: 'group', product: x })} />
            </>
          )
        }}
      </Async>
      {buy && <BuyFlow buy={buy} base={buy.kind === 'membership' ? base : today()} initialTrainer={sp.get('trainer')} onClose={() => setBuy(null)} onPaid={() => myPasses.reload()} />}
    </main>
  )
}

function Section({ id, title, sub, items, render, onBuy }) {
  return (
    <section id={id} className="stack">
      <div>
        <h2 className="sec">{title}</h2>
        <span className="muted small">{sub}</span>
      </div>
      {items.length === 0 ? <div className="empty">판매 중인 상품이 없어요.</div> : (
        <div className="grid g4">
          {items.map(x => (
            <div key={x.productId} className="card" style={{ gap: 8 }}>
              {render(x)}
              <button className="btn primary" style={{ marginTop: 8 }} onClick={() => onBuy(x)}>구매</button>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

function BuyFlow({ buy, base, initialTrainer, onClose, onPaid }) {
  const { kind, product } = buy
  const tr = useAsync(() => (kind === 'personal' ? trainersApi.list() : Promise.resolve(null)), [kind])
  const [step, setStep] = useState('form')
  const [startDate, setStartDate] = useState(base)
  const [trainerId, setTrainerId] = useState(initialTrainer ? Number(initialTrainer) : null)
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')
  const [result, setResult] = useState(null)

  useEffect(() => {
    if (kind === 'personal' && !trainerId && tr.data?.trainers?.length) setTrainerId(tr.data.trainers[0].trainerId)
  }, [tr.data])

  const maxDate = addDays(base, START_MAX_DAYS)
  const endDate = useMemo(() => {
    if (!startDate) return null
    return kind === 'membership' ? addDays(addMonths(startDate, product.months), -1) : addDays(startDate, product.validWeeks * 7 - 1)
  }, [startDate, kind, product])
  const trainerName = tr.data?.trainers.find(t => t.trainerId === trainerId)?.name
  const label = kind === 'membership' ? '회원권' : kind === 'personal' ? '개인 PT' : '그룹 PT'

  const next = () => {
    if (!startDate || startDate < base || startDate > maxDate) {
      setErr(`시작일은 ${fmtDate(base)}부터 ${fmtDate(maxDate)}까지 고를 수 있어요.`)
      return
    }
    if (kind === 'personal' && !trainerId) {
      setErr('담당 트레이너를 골라 주세요.')
      return
    }
    setErr('')
    setStep('pay')
  }

  const pay = async success => {
    setBusy(true)
    setErr('')
    try {
      const r = await payments.simulate({ kind, productId: product.productId, trainerId, startDate, success })
      setResult(r)
      setStep('done')
      if (r.status === '성공') onPaid()
    } catch (e) {
      setErr(e.message)
      setStep('form')
    } finally {
      setBusy(false)
    }
  }

  if (step === 'done' && result) {
    const okPay = result.status === '성공'
    return (
      <Modal title={okPay ? '결제가 완료됐어요' : '결제에 실패했어요'} onClose={onClose}>
        <div className={`alert ${okPay ? 'info' : 'bad'}`}>
          {okPay ? `${product.name}을(를) 샀어요. ${fmtDate(result.startDate)}부터 ${fmtDate(result.endDate)}까지 쓸 수 있어요.` : '모의 결제가 실패로 끝나서 이용권이 만들어지지 않았어요. 결제 기록은 이용 내역에 남아요.'}
        </div>
        <dl className="kv">
          <dt>상품</dt><dd>{product.name}</dd>
          <dt>금액</dt><dd>{fmtMoney(result.amount)}</dd>
          <dt>결과</dt><dd>{result.status}</dd>
        </dl>
        <div className="row end">
          {okPay ? <Link to={kind === 'membership' ? '/member/passes' : '/member/book' + (kind === 'group' ? '?tab=group' : '')} className="btn primary">{kind === 'membership' ? '내 이용권 보기' : '수업 예약하기'}</Link> : <button className="btn primary" onClick={() => { setResult(null); setStep('pay') }}>다시 결제하기</button>}
          <button className="btn" onClick={onClose}>닫기</button>
        </div>
      </Modal>
    )
  }

  if (step === 'pay') {
    return (
      <Modal title="모의 결제" onClose={busy ? undefined : onClose}>
        <dl className="kv">
          <dt>상품</dt><dd>{product.name} ({label})</dd>
          {kind === 'personal' && <><dt>트레이너</dt><dd>{trainerName}</dd></>}
          <dt>이용 기간</dt><dd>{fmtDate(startDate)} ~ {fmtDate(endDate)}</dd>
          <dt>결제 금액</dt><dd className="num" style={{ fontSize: 22 }}>{fmtMoney(product.price)}</dd>
        </dl>
        <div className="alert small">실제 돈이 나가지 않는 모의 결제예요. 결과를 직접 골라 주세요.</div>
        {err && <div className="alert bad">{err}</div>}
        <div className="row end">
          <button className="btn" onClick={() => setStep('form')} disabled={busy}>이전</button>
          <button className="btn danger" onClick={() => pay(false)} disabled={busy}>결제 실패</button>
          <button className="btn primary" onClick={() => pay(true)} disabled={busy}>{busy ? '결제 중…' : '결제 성공'}</button>
        </div>
      </Modal>
    )
  }

  return (
    <Modal title={`${label} 구매`} onClose={onClose}>
      <dl className="kv">
        <dt>상품</dt><dd>{product.name}</dd>
        <dt>가격</dt><dd>{fmtMoney(product.price)}</dd>
      </dl>
      {kind === 'personal' && (
        <Field label="담당 트레이너" hint="개인 PT는 고른 트레이너하고만 예약할 수 있어요.">
          <select className="select" value={trainerId || ''} onChange={e => setTrainerId(Number(e.target.value))}>
            {(tr.data?.trainers || []).map(t => <option key={t.trainerId} value={t.trainerId}>{t.name}</option>)}
          </select>
        </Field>
      )}
      <Field label="시작일" hint={`${fmtDate(base)}부터 ${fmtDate(maxDate)}까지 고를 수 있어요.`}>
        <input className="input" type="date" min={base} max={maxDate} value={startDate} onChange={e => setStartDate(e.target.value)} />
      </Field>
      {endDate && <span className="muted">이용 기간: <b style={{ color: 'var(--text)' }}>{fmtDate(startDate)} ~ {fmtDate(endDate)}</b></span>}
      {err && <div className="alert bad">{err}</div>}
      <div className="row end">
        <button className="btn" onClick={onClose}>닫기</button>
        <button className="btn primary" onClick={next}>결제하기</button>
      </div>
    </Modal>
  )
}
