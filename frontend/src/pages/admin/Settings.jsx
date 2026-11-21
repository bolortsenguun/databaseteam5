import { useEffect, useState } from 'react'
import { settings } from '../../api/endpoints'
import { useAsync } from '../../lib/hooks'
import { Async, PageTitle, useToast } from '../../components/ui'
import { refreshSettings } from '../../lib/settings'

const META = {
  CANCEL_DEADLINE_HOURS: ['예약 취소', '차감 없이 취소할 수 있는 기한', '시간 전', 1],
  FREE_CANCEL_MINUTES: ['예약 취소', '예약 직후 무료 취소 시간', '분', 0],
  BOOKING_RANGE_DAYS: ['예약', '예약 가능 범위', '일 후까지', 1],
  BOOKING_CLOSE_HOURS: ['예약', '예약 마감', '시간 전', 0],
  SLOT_MINUTES: ['예약', '수업 단위', '분 (고정)', 60],
  PT_ENTRY_BEFORE_MINUTES: ['입장', 'PT 회원 입장 (시작 전)', '분 전부터', 0],
  PT_ENTRY_AFTER_MINUTES: ['입장', 'PT 회원 입장 (시작 후)', '분 후까지', 0],
  RECHECKIN_MINUTES: ['입장', '중복 체크인 판정', '분', 1],
  GROUP_MIN_PARTICIPANTS: ['그룹 수업', '최소 인원', '명 (2~4)', 2],
  GROUP_CONFIRM_HOURS: ['그룹 수업', '확정·폐강 판정', '시간 전', 1],
  GROUP_OPEN_MIN_HOURS: ['그룹 수업', '개설 마감', '시간 전까지', 1],
  GROUP_OPEN_MAX_DAYS: ['그룹 수업', '개설 가능 범위', '일 후까지', 1],
  PASS_START_MAX_DAYS: ['이용권', '시작일 선택 범위', '일', 0]
}
const GROUPS = ['예약', '예약 취소', '입장', '그룹 수업', '이용권']

export default function AdminSettings() {
  const toast = useToast()
  const s = useAsync(() => settings.get(), [])
  const [vals, setVals] = useState({})
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    if (s.data) setVals(Object.fromEntries(s.data.settings.map(x => [x.settingKey, String(x.settingValue)])))
  }, [s.data])

  const orig = Object.fromEntries((s.data?.settings || []).map(x => [x.settingKey, x.settingValue]))
  const changed = Object.keys(vals).filter(k => k !== 'SLOT_MINUTES' && String(orig[k]) !== vals[k])

  const save = async () => {
    for (const k of changed) {
      const v = vals[k]
      if (!/^\d+$/.test(v)) return setErr(`${META[k]?.[1] || k}: 0 이상의 정수를 입력해 주세요.`)
      if (META[k] && Number(v) < META[k][3]) return setErr(`${META[k][1]}: ${META[k][3]} 이상이어야 해요.`)
    }
    const n = k => Number(vals[k])
    if (n('GROUP_MIN_PARTICIPANTS') > 4) return setErr('그룹 최소 인원은 4명을 넘을 수 없어요. (정원 최솟값이 4명)')
    if (n('GROUP_OPEN_MIN_HOURS') <= n('GROUP_CONFIRM_HOURS')) return setErr('그룹 개설 마감은 확정·폐강 판정 시점보다 길어야 해요.')
    if (n('GROUP_OPEN_MAX_DAYS') * 24 <= n('GROUP_OPEN_MIN_HOURS')) return setErr('그룹 개설 가능 범위는 개설 마감보다 길어야 해요.')
    setBusy(true)
    setErr('')
    try {
      await settings.save(changed.map(k => ({ settingKey: k, settingValue: Number(vals[k]) })))
      toast('운영 설정을 저장했어요.')
      s.reload()
      refreshSettings()   // 다른 화면 안내 문구도 새 값으로
    } catch (e) {
      setErr(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="page narrow">
      <PageTitle title="운영 설정" sub="규칙에 쓰이는 값이에요. 저장하면 바로 그 뒤의 모든 새 요청(예약·취소·체크인)에 적용돼요. 이미 끝난 결제·차감·체크인 기록은 바뀌지 않아요." />
      <Async state={s}>
        {d => {
          const keys = d.settings.map(x => x.settingKey)
          return (
            <>
              {GROUPS.map(g => {
                const ks = keys.filter(k => META[k]?.[0] === g)
                if (!ks.length) return null
                return (
                  <section key={g} className="card">
                    <h3>{g}</h3>
                    {ks.map(k => (
                      <div key={k} className="row between">
                        <div className="stack" style={{ gap: 2 }}>
                          <b>{META[k][1]}</b>
                          <span className="dim small">{d.settings.find(x => x.settingKey === k)?.description}</span>
                        </div>
                        <div className="row" style={{ gap: 8 }}>
                          <input className="input" style={{ width: 96, textAlign: 'right' }} inputMode="numeric" value={vals[k] ?? ''} disabled={k === 'SLOT_MINUTES'} onChange={e => setVals({ ...vals, [k]: e.target.value.replace(/\D/g, '') })} />
                          <span className="muted small" style={{ width: 84 }}>{META[k][2]}</span>
                        </div>
                      </div>
                    ))}
                  </section>
                )
              })}
              {keys.filter(k => !META[k]).length > 0 && (
                <section className="card">
                  <h3>기타</h3>
                  {keys.filter(k => !META[k]).map(k => (
                    <div key={k} className="row between">
                      <b>{k}</b>
                      <input className="input" style={{ width: 96 }} value={vals[k] ?? ''} onChange={e => setVals({ ...vals, [k]: e.target.value.replace(/\D/g, '') })} />
                    </div>
                  ))}
                </section>
              )}
              {err && <div className="alert bad">{err}</div>}
              <div className="row end" style={{ position: 'sticky', bottom: 16 }}>
                {changed.length > 0 && <span className="muted small">{changed.length}개 바뀜</span>}
                <button className="btn" disabled={!changed.length || busy} onClick={() => setVals(Object.fromEntries(d.settings.map(x => [x.settingKey, String(x.settingValue)])))}>되돌리기</button>
                <button className="btn primary" disabled={!changed.length || busy} onClick={save}>{busy ? '저장 중…' : '저장'}</button>
              </div>
            </>
          )
        }}
      </Async>
    </main>
  )
}
