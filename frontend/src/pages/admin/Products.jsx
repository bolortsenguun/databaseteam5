import { useState } from 'react'
import { products } from '../../api/endpoints'
import { useAsync } from '../../lib/hooks'
import { Async, Badge, Field, Modal, PageTitle, Seg, useToast } from '../../components/ui'
import { fmtMoney, ptTypeKey, ptTypeLabel } from '../../lib/format'

export default function AdminProducts() {
  const toast = useToast()
  const list = useAsync(() => products.adminList(), [])
  const [form, setForm] = useState(null)
  const [busyId, setBusyId] = useState(null)

  const toggle = async (kind, p) => {
    setBusyId(kind + p.productId)
    try {
      if (kind === 'm') await products.updateMembership(p.productId, { active: !p.active })
      else await products.updatePt(p.productId, { active: !p.active })
      toast(p.active ? '판매를 멈췄어요.' : '다시 판매해요.')
      list.reload()
    } catch (e) {
      toast(e.message, true)
    } finally {
      setBusyId(null)
    }
  }

  return (
    <main className="page">
      <PageTitle title="상품" sub="가격을 바꿔도 이미 산 이용권의 가격은 그대로예요. 상품은 지울 수 없고 판매를 멈출 수만 있어요." />
      <Async state={list}>
        {d => (
          <>
            <section className="card">
              <div className="row between">
                <h3>회원권 상품</h3>
                <button className="btn sm primary" onClick={() => setForm({ kind: 'm' })}>추가</button>
              </div>
              <div className="tblwrap">
                <table className="tbl">
                  <thead><tr><th>상품</th><th>기간</th><th>가격</th><th>상태</th><th /></tr></thead>
                  <tbody>
                    {d.membershipProducts.map(p => (
                      <tr key={p.productId}>
                        <td><b>{p.name}</b></td>
                        <td>{p.months}개월</td>
                        <td className="num">{fmtMoney(p.price)}</td>
                        <td><Badge s={p.active ? '판매 중' : '판매 중지'} /></td>
                        <td style={{ textAlign: 'right' }}>
                          <div className="row end" style={{ gap: 6 }}>
                            <button className="btn sm" onClick={() => setForm({ kind: 'm', p })}>수정</button>
                            <button className="btn sm" disabled={busyId === 'm' + p.productId} onClick={() => toggle('m', p)}>{p.active ? '판매 중지' : '판매 재개'}</button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
            <section className="card">
              <div className="row between">
                <h3>PT 상품</h3>
                <button className="btn sm primary" onClick={() => setForm({ kind: 'p' })}>추가</button>
              </div>
              <div className="tblwrap">
                <table className="tbl">
                  <thead><tr><th>상품</th><th>구분</th><th>횟수</th><th>유효기간</th><th>가격</th><th>상태</th><th /></tr></thead>
                  <tbody>
                    {d.ptProducts.map(p => (
                      <tr key={p.productId}>
                        <td><b>{p.name}</b></td>
                        <td>{ptTypeLabel(p.ptType)}</td>
                        <td className="num">{p.totalCount}회</td>
                        <td>{p.validWeeks}주</td>
                        <td className="num">{fmtMoney(p.price)}</td>
                        <td><Badge s={p.active ? '판매 중' : '판매 중지'} /></td>
                        <td style={{ textAlign: 'right' }}>
                          <div className="row end" style={{ gap: 6 }}>
                            <button className="btn sm" onClick={() => setForm({ kind: 'p', p })}>수정</button>
                            <button className="btn sm" disabled={busyId === 'p' + p.productId} onClick={() => toggle('p', p)}>{p.active ? '판매 중지' : '판매 재개'}</button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </section>
          </>
        )}
      </Async>
      {form && <ProductForm {...form} onClose={() => setForm(null)} onSaved={() => { setForm(null); list.reload() }} />}
    </main>
  )
}

function ProductForm({ kind, p, onClose, onSaved }) {
  const toast = useToast()
  const edit = !!p
  const m = kind === 'm'
  const [f, setF] = useState({
    name: p?.name || '',
    months: p?.months ?? 1,
    ptType: ptTypeKey(p?.ptType) || 'PERSONAL',
    totalCount: p?.totalCount ?? 10,
    validWeeks: p?.validWeeks ?? 12,
    price: p?.price ?? 0
  })
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const num = v => (v === '' ? '' : Number(v))

  const save = async () => {
    if (!f.name.trim()) return setErr('상품 이름을 입력해 주세요.')
    if (f.name.trim().length > 50) return setErr('상품 이름은 50자 이하로 입력해 주세요.')
    if (!(f.price >= 0)) return setErr('가격은 0원 이상이어야 해요.')
    if (m && !(f.months > 0)) return setErr('기간은 1개월 이상이어야 해요.')
    if (!m && (!(f.totalCount > 0) || !(f.validWeeks > 0))) return setErr('횟수와 유효기간은 1 이상이어야 해요.')
    setBusy(true)
    setErr('')
    try {
      if (m) {
        const body = { name: f.name.trim(), months: f.months, price: f.price }
        edit ? await products.updateMembership(p.productId, body) : await products.createMembership(body)
      } else {
        const body = { name: f.name.trim(), totalCount: f.totalCount, validWeeks: f.validWeeks, price: f.price }
        edit ? await products.updatePt(p.productId, body) : await products.createPt({ ...body, ptType: f.ptType })
      }
      toast(edit ? '상품을 수정했어요.' : '상품을 추가했어요.')
      onSaved()
    } catch (e) {
      setErr(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Modal title={`${m ? '회원권' : 'PT'} 상품 ${edit ? '수정' : '추가'}`} onClose={onClose}>
      {!m && (
        <div className="field">
          구분 {edit && <span className="hint">구분은 바꿀 수 없어요.</span>}
          {edit ? <input className="input" value={ptTypeLabel(f.ptType)} disabled /> : <Seg value={f.ptType} options={[['PERSONAL', '개인'], ['GROUP', '그룹']]} onChange={v => setF({ ...f, ptType: v })} />}
        </div>
      )}
      <Field label="상품 이름"><input className="input" value={f.name} maxLength={50} onChange={e => setF({ ...f, name: e.target.value })} placeholder={m ? '예: 1개월권' : '예: 개인 PT 10회'} /></Field>
      {m ? (
        <Field label="기간 (개월)"><input className="input" type="number" min={1} value={f.months} onChange={e => setF({ ...f, months: num(e.target.value) })} /></Field>
      ) : (
        <div className="grid g2" style={{ gap: 16 }}>
          <Field label="횟수"><input className="input" type="number" min={1} value={f.totalCount} onChange={e => setF({ ...f, totalCount: num(e.target.value) })} /></Field>
          <Field label="유효기간 (주)"><input className="input" type="number" min={1} value={f.validWeeks} onChange={e => setF({ ...f, validWeeks: num(e.target.value) })} /></Field>
        </div>
      )}
      <Field label="가격 (원)" hint={edit ? '바꿔도 이미 산 이용권 가격은 그대로예요.' : ''}><input className="input" type="number" min={0} step={1000} value={f.price} onChange={e => setF({ ...f, price: num(e.target.value) })} /></Field>
      {err && <div className="alert bad">{err}</div>}
      <div className="row end">
        <button className="btn" onClick={onClose} disabled={busy}>취소</button>
        <button className="btn primary" onClick={save} disabled={busy}>{busy ? '저장 중…' : '저장'}</button>
      </div>
    </Modal>
  )
}
