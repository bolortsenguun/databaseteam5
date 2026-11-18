import { Link } from 'react-router-dom'
import { trainers, passes } from '../../api/endpoints'
import { useAsync, useSafeAsync } from '../../lib/hooks'
import { Async, Badge, PageTitle } from '../../components/ui'
import { DOW } from '../../lib/format'

export function workText(schedules) {
  if (!schedules?.length) return '근무 시간이 아직 없어요'
  const byTime = new Map()
  for (const s of schedules) {
    const k = `${s.startTime}~${s.endTime}`
    byTime.set(k, [...(byTime.get(k) || []), s.dayOfWeek])
  }
  return [...byTime.entries()].map(([t, days]) => {
    const sorted = [...new Set(days)].sort((a, b) => a - b)
    const contiguous = sorted.length > 2 && sorted.every((d, i) => i === 0 || d === sorted[i - 1] + 1)
    const ds = contiguous ? `${DOW[sorted[0]]}~${DOW[sorted[sorted.length - 1]]}` : sorted.map(d => DOW[d]).join('·')
    return `${ds} ${t}`
  }).join(', ')
}

export default function MemberTrainers() {
  const tr = useAsync(() => trainers.list(), [])
  const p = useSafeAsync(() => passes.mine(), null, [])
  const mine = (p.data?.ptPasses || []).filter(x => x.ptType === '개인' && x.status !== '만료')
  return (
    <main className="page">
      <PageTitle title="트레이너" sub="내 담당 트레이너를 확인하고 바로 예약해요." />
      <Async state={tr} isEmpty={d => d.trainers.length === 0} empty="등록된 트레이너가 없어요.">
        {d => (
          <div className="grid g2">
            {[...d.trainers].sort((a, b) => mine.some(x => x.trainerId === b.trainerId) - mine.some(x => x.trainerId === a.trainerId)).map(t => {
              const my = mine.filter(x => x.trainerId === t.trainerId)
              const avail = my.reduce((s, x) => s + x.availableCount, 0)
              return (
                <div key={t.trainerId} className={`card ${my.length ? 'irid' : ''}`}>
                  <div className="row between">
                    <b style={{ fontSize: 20 }}>{t.name} 트레이너</b>
                    {my.length > 0 && <Badge tone="info">내 담당 · 개인 PT {avail}회 예약 가능</Badge>}
                  </div>
                  <dl className="kv">
                    <dt>경력</dt><dd style={{ fontWeight: 500 }}>{t.career || <span className="muted">등록된 정보가 없어요</span>}</dd>
                    <dt>소개</dt><dd style={{ fontWeight: 500, lineHeight: 1.6 }}>{t.introduction || <span className="muted">등록된 정보가 없어요</span>}</dd>
                    <dt>근무</dt><dd style={{ fontWeight: 500 }}>{workText(t.schedules)}</dd>
                  </dl>
                  <div className="row">
                    <Link to={`/member/book?trainer=${t.trainerId}`} className="btn primary sm">PT 예약</Link>
                    <Link to={`/member/shop?type=personal&trainer=${t.trainerId}`} className="btn sm">개인 PT 구매</Link>
                  </div>
                </div>
              )
            })}
          </div>
        )}
      </Async>
    </main>
  )
}
