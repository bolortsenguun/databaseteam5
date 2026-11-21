import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { trainers as trainersApi, reservations, groups, passes } from '../../api/endpoints'
import { useAsync, useSafeAsync } from '../../lib/hooks'
import { Async, Badge, Confirm, Empty, ErrorBox, Loading, PageTitle, Tabs, useToast } from '../../components/ui'
import { addDays, fmtMD, fmtMDShort, reasonText, REASON_LONG, today } from '../../lib/format'
import { useSettings } from '../../lib/settings'


export default function MemberBook() {
  const [sp, setSp] = useSearchParams()
  const tab = sp.get('tab') === 'group' ? 'group' : 'personal'
  const myPasses = useSafeAsync(() => passes.mine(), null, [])
  const S = useSettings()
  return (
    <main className="page">
      <PageTitle title="PT 예약" sub={tab === 'personal' ? `개인 PT는 오늘부터 ${S.BOOKING_RANGE_DAYS}일 안, 정시 단위로 예약해요.` : '열려 있는 수업을 골라 신청하세요. 그룹 PT 이용권이 필요해요.'}>
        <Tabs value={tab} options={[['personal', '개인 PT'], ['group', '그룹 PT']]} onChange={v => setSp(v === 'group' ? { tab: 'group' } : {})} />
      </PageTitle>
      {tab === 'personal' ? <Personal myPasses={myPasses} initialTrainer={sp.get('trainer')} /> : <Group myPasses={myPasses} />}
    </main>
  )
}

function Personal({ myPasses, initialTrainer }) {
  const toast = useToast()
  const tr = useAsync(() => trainersApi.list(), [])
  const personalPasses = (myPasses.data?.ptPasses || []).filter(p => p.ptType === '개인' && p.status !== '만료')
  const [trainerId, setTrainerId] = useState(initialTrainer ? Number(initialTrainer) : null)
  const [date, setDate] = useState(today())
  const [pick, setPick] = useState(null)
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')

  useEffect(() => {
    if (trainerId || !tr.data) return
    const mine = personalPasses.find(p => p.availableCount > 0) || personalPasses[0]
    const id = mine?.trainerId || tr.data.trainers[0]?.trainerId
    if (id) setTrainerId(id)
  }, [tr.data, myPasses.data])

  const slots = useAsync(() => (trainerId ? reservations.slots(trainerId, date) : Promise.resolve(null)), [trainerId, date])
  const S = useSettings()
  const range = S.BOOKING_RANGE_DAYS
  const days = useMemo(() => Array.from({ length: range + 1 }, (_, i) => addDays(today(), i)), [range])
  const passOfTrainer = personalPasses.filter(p => p.trainerId === trainerId)
  const available = passOfTrainer.reduce((s, p) => s + p.availableCount, 0)
  const trainerName = tr.data?.trainers.find(t => t.trainerId === trainerId)?.name

  const reserve = async () => {
    setBusy(true)
    setErr('')
    try {
      await reservations.reserveIndividual({ trainerId, lessonDate: date, startTime: pick.startTime })
      toast('개인 PT 예약이 완료됐어요.')
      setPick(null)
      slots.reload()
      myPasses.reload()
    } catch (e) {
      setErr(e.message)
      slots.reload()
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <div className="card">
        <div className="row between">
          <div className="row">
            <label className="field" style={{ minWidth: 220 }}>
              담당 트레이너
              <select className="select" value={trainerId || ''} onChange={e => setTrainerId(Number(e.target.value))}>
                {(tr.data?.trainers || []).map(t => (
                  <option key={t.trainerId} value={t.trainerId}>{t.name}{personalPasses.some(p => p.trainerId === t.trainerId) ? ' (내 담당)' : ''}</option>
                ))}
              </select>
            </label>
          </div>
          {myPasses.data && (
            <div className="stack" style={{ gap: 2, alignItems: 'flex-end' }}>
              <span className="muted small">예약 가능</span>
              <span className="stat">{available}<small>회</small></span>
            </div>
          )}
        </div>
        {tr.error && <ErrorBox error={tr.error} onRetry={tr.reload} />}
        {myPasses.data && trainerId && passOfTrainer.length === 0 && (
          <div className="alert warn small row between">
            <span>{trainerName || '이'} 트레이너의 개인 PT 이용권이 없어서 예약할 수 없어요.</span>
            <Link to={`/member/shop?type=personal&trainer=${trainerId}`} className="btn sm">개인 PT 구매</Link>
          </div>
        )}
        <div className="days" role="listbox" aria-label="날짜">
          {days.map(d => {
            const s = fmtMDShort(d)
            return (
              <button key={d} className={`day ${d === date ? 'on' : ''}`} onClick={() => setDate(d)} aria-selected={d === date}>
                <span>{s.w}</span>
                <span className="d">{s.md}</span>
              </button>
            )
          })}
        </div>
      </div>

      <div className="card">
        <div className="row between">
          <h3>{fmtMD(date)}{trainerName ? ` · ${trainerName} 트레이너` : ''}</h3>
          <button className="btn sm" onClick={slots.reload}>새로고침</button>
        </div>
        {slots.loading && !slots.data ? <Loading /> : slots.error ? <ErrorBox error={slots.error} onRetry={slots.reload} /> : !slots.data ? null : slots.data.slots.length === 0 ? (
          <Empty>이날은 트레이너 근무 시간이 없어요. 다른 날짜를 골라 주세요.</Empty>
        ) : (
          <div className="slots">
            {slots.data.slots.map(s => (
              <button key={s.startTime} className={`slot ${s.unavailableReason === 'MY_RESERVATION' ? 'mine' : ''}`} disabled={!s.available} onClick={() => { setErr(''); setPick(s) }} title={s.unavailableReason ? REASON_LONG[s.unavailableReason] || '' : '예약 가능'}>
                <span className="t">{s.startTime}</span>
                <span className="r">{s.available ? '예약 가능' : reasonText(s.unavailableReason)}</span>
              </button>
            ))}
          </div>
        )}
        <div className="alert small">
          원하는 시간을 누르면 예약 확인 창이 열려요. 예약한 뒤 {S.FREE_CANCEL_MINUTES}분 안에, 또는 수업 {S.CANCEL_DEADLINE_HOURS}시간 전까지 취소하면 횟수가 차감되지 않아요.
        </div>
      </div>

      {pick && (
        <Confirm title="이 시간으로 예약할까요?" confirmText="예약하기" busy={busy} error={err} onClose={() => setPick(null)} onConfirm={reserve}>
          <dl className="kv">
            <dt>일시</dt><dd>{fmtMD(date)} {pick.startTime}~{pick.endTime}</dd>
            <dt>구분</dt><dd>개인 PT · 60분</dd>
            <dt>트레이너</dt><dd>{trainerName}</dd>
          </dl>
          <p className="muted small" style={{ margin: 0 }}>예약하면 이용권 1회가 확보되고, 출석·결석 처리될 때 차감돼요.</p>
        </Confirm>
      )}
    </>
  )
}

function Group({ myPasses }) {
  const toast = useToast()
  const S = useSettings()
  const range = S.BOOKING_RANGE_DAYS
  const list = useAsync(() => groups.memberList({ startDate: today(), endDate: addDays(today(), range) }), [range])
  const [pick, setPick] = useState(null)
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')
  const gp = (myPasses.data?.ptPasses || []).filter(p => p.ptType === '그룹' && p.status !== '만료')
  const avail = gp.reduce((s, p) => s + p.availableCount, 0)

  const apply = async () => {
    setBusy(true)
    setErr('')
    try {
      await reservations.reserveGroup(pick.classId)
      toast('그룹 수업 신청이 완료됐어요.')
      setPick(null)
      list.reload()
      myPasses.reload()
    } catch (e) {
      setErr(e.message)
      list.reload()
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      {myPasses.data && (
        <div className="alert small row between">
          <span>그룹 PT 잔여 <b className="num">{avail}</b>회 · 수업이 폐강되면 횟수는 차감되지 않아요.</span>
          {gp.length === 0 && <Link to="/member/shop?type=group" className="btn sm">그룹 PT 구매</Link>}
        </div>
      )}
      <Async state={list} isEmpty={d => d.classes.length === 0} empty="지금 신청할 수 있는 그룹 수업이 없어요.">
        {d => {
          const byDate = d.classes.reduce((m, c) => ((m[c.classDate] ||= []).push(c), m), {})
          return Object.entries(byDate).map(([date, cs]) => (
            <section key={date} className="stack">
              <h2 className="sec">{fmtMD(date)}</h2>
              <div className="grid g2">
                {cs.map(c => (
                  <div key={c.classId} className="card">
                    {c.imageUrl && <img className="gimg" src={c.imageUrl} alt={`${c.name} 수업 사진`} loading="lazy" />}
                    <div className="row between">
                      <div className="stack" style={{ gap: 4 }}>
                        <b style={{ fontSize: 18 }}>{c.name}</b>
                        <span className="muted small">{c.trainerName} 트레이너</span>
                      </div>
                      <Badge s={c.status} />
                    </div>
                    {c.workout && <span className="muted small">오늘 할 운동 <Badge tone="info">{c.workout}</Badge></span>}
                    <div className="row between">
                      <span className="stat">{c.startTime}</span>
                      <span className="muted">신청 <b className="num" style={{ color: 'var(--text)' }}>{c.currentParticipants} / {c.capacity}</b>명</span>
                    </div>
                    {c.description && <p className="muted" style={{ margin: 0, lineHeight: 1.6 }}>{c.description}</p>}
                    <span className="dim small">최소 {S.GROUP_MIN_PARTICIPANTS}명 모집 · 수업 {S.GROUP_CONFIRM_HOURS}시간 전까지 미달이면 폐강돼요.</span>
                    {c.canReserve ? (
                      <button className="btn primary" onClick={() => { setErr(''); setPick(c) }}>신청하기</button>
                    ) : (
                      <button className="btn" disabled title={REASON_LONG[c.unavailableReason] || ''}>{reasonText(c.unavailableReason)}</button>
                    )}
                  </div>
                ))}
              </div>
            </section>
          ))
        }}
      </Async>
      {pick && (
        <Confirm title="이 수업을 신청할까요?" confirmText="신청하기" busy={busy} error={err} onClose={() => setPick(null)} onConfirm={apply}>
          <dl className="kv">
            <dt>수업</dt><dd>{pick.name}</dd>
            {pick.workout && <><dt>운동</dt><dd>{pick.workout}</dd></>}
            <dt>일시</dt><dd>{fmtMD(pick.classDate)} {pick.startTime}~{pick.endTime}</dd>
            <dt>트레이너</dt><dd>{pick.trainerName}</dd>
            <dt>인원</dt><dd>{pick.currentParticipants + 1} / {pick.capacity}명 (신청 후)</dd>
          </dl>
        </Confirm>
      )}
    </>
  )
}
