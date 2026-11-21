import { useState } from 'react'
import { groups } from '../../api/endpoints'
import { useAsync } from '../../lib/hooks'
import { Async, Badge, Confirm, Field, Loading, Modal, PageTitle, Seg, Tabs, useToast } from '../../components/ui'
import { addDays, fmtMD, GROUP_WORKOUTS, nowLocalISO, reasonText, today } from '../../lib/format'
import { resizeImage } from '../../lib/image'
import { ReasonCancel } from './Lessons'
import { useSettings } from '../../lib/settings'

export default function TrainerGroups() {
  const toast = useToast()
  const [view, setView] = useState('upcoming')
  const list = useAsync(() => (view === 'upcoming' ? groups.mine({ startDate: today(), endDate: addDays(today(), 60) }) : groups.mine({ startDate: addDays(today(), -60), endDate: addDays(today(), -1) })), [view])
  const [form, setForm] = useState(null)
  const [del, setDel] = useState(null)
  const [cancel, setCancel] = useState(null)
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState('')
  const now = nowLocalISO()
  const S = useSettings()

  const remove = async () => {
    setBusy(true)
    setErr('')
    try {
      await groups.remove(del.classId)
      toast('그룹 수업을 삭제했어요.')
      setDel(null)
      list.reload()
    } catch (e) {
      setErr(e.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <main className="page">
      <PageTitle title="그룹 수업" sub={`지금부터 ${S.GROUP_OPEN_MIN_HOURS}시간 뒤 ~ ${S.GROUP_OPEN_MAX_DAYS}일 후 사이의 수업을 열 수 있어요. 시작 ${S.GROUP_CONFIRM_HOURS}시간 전에 최소 인원(${S.GROUP_MIN_PARTICIPANTS}명)이 안 되면 자동으로 폐강돼요.`}>
        <Tabs value={view} options={[['upcoming', '다가오는 수업'], ['past', '지난 수업']]} onChange={setView} />
        <button className="btn primary" onClick={() => setForm({})}>수업 열기</button>
      </PageTitle>
      <Async state={list} isEmpty={d => d.classes.length === 0} empty={view === 'upcoming' ? '열어 둔 그룹 수업이 없어요.' : '지난 그룹 수업이 없어요.'}>
        {d => (
          <div className="grid g2">
            {(view === 'past' ? [...d.classes].reverse() : d.classes).map(c => {
              const future = `${c.classDate}T${c.startTime}:00` > now
              const live = ['개설', '확정'].includes(c.status)
              return (
                <div key={c.classId} className="card">
                  {c.imageUrl && <img className="gimg" src={c.imageUrl} alt={`${c.name} 수업 사진`} />}
                  <div className="row between">
                    <div className="stack" style={{ gap: 4 }}>
                      <b style={{ fontSize: 18 }}>{c.name}</b>
                      <span className="muted">{fmtMD(c.classDate)} <span className="num">{c.startTime}~{c.endTime}</span></span>
                    </div>
                    <Badge s={c.status} />
                  </div>
                  {c.workout && <span className="muted small">오늘 할 운동 <Badge tone="info">{c.workout}</Badge></span>}
                  {c.description && <p className="muted" style={{ margin: 0, lineHeight: 1.6 }}>{c.description}</p>}
                  <span>신청 <b className="num">{c.currentParticipants} / {c.capacity}</b>명</span>
                  {c.participants.length > 0 && (
                    <div className="row" style={{ gap: 6 }}>
                      {c.participants.map(p => <Badge key={p.reservationId} tone="">{p.name} · {p.reservationStatus}</Badge>)}
                    </div>
                  )}
                  {live && future && (
                    <div className="row">
                      {c.currentParticipants === 0 && <button className="btn sm" onClick={() => setForm(c)}>수정</button>}
                      {c.currentParticipants === 0 && <button className="btn sm" onClick={() => { setErr(''); setDel(c) }}>삭제</button>}
                      <button className="btn sm danger" onClick={() => setCancel(c)}>수업 취소</button>
                    </div>
                  )}
                  {live && future && c.currentParticipants > 0 && <span className="dim small">신청한 회원이 있어서 수정·삭제는 못 하고 취소만 할 수 있어요.</span>}
                </div>
              )
            })}
          </div>
        )}
      </Async>
      {form && <ClassForm initial={form.classId ? form : null} onClose={() => setForm(null)} onSaved={() => { setForm(null); list.reload() }} />}
      {del && <Confirm title="그룹 수업을 삭제할까요?" message={`${fmtMD(del.classDate)} ${del.startTime} '${del.name}' 수업을 지워요. 예약 이력이 있으면 삭제 대신 취소해야 해요.`} confirmText="삭제" danger busy={busy} error={err} onClose={() => setDel(null)} onConfirm={remove} />}
      {cancel && <ReasonCancel title="그룹 수업을 취소할까요?" desc={`${fmtMD(cancel.classDate)} ${cancel.startTime} '${cancel.name}'. 신청한 회원 ${cancel.currentParticipants}명의 예약도 함께 취소되고, 횟수는 차감되지 않아요.`} confirmText="수업 취소" onClose={() => setCancel(null)} onSubmit={async reason => {
        const r = await groups.cancel(cancel.classId, reason)
        toast(`수업을 취소했어요. 예약 ${r?.canceledReservations ?? 0}건도 함께 취소됐어요.`)
        setCancel(null)
        list.reload()
      }} />}
    </main>
  )
}

function ClassForm({ initial, onClose, onSaved }) {
  const toast = useToast()
  const edit = !!initial
  const [f, setF] = useState({
    name: initial?.name || '',
    description: initial?.description || '',
    capacity: initial?.capacity || 4,
    classDate: initial?.classDate || addDays(today(), 3),
    startTime: initial?.startTime || '',
    workout: initial?.workout || '',
    imageUrl: initial?.imageUrl || ''
  })
  const [imgBusy, setImgBusy] = useState(false)
  const [err, setErr] = useState('')
  const [busy, setBusy] = useState(false)
  const slots = useAsync(() => (f.classDate ? groups.availableSlots(f.classDate) : Promise.resolve(null)), [f.classDate])
  const set = (k, v) => setF(s => ({ ...s, [k]: v }))
  const S = useSettings()
  const isOriginal = edit && f.classDate === initial.classDate && f.startTime === initial.startTime

  const save = async () => {
    if (!f.name.trim()) return setErr('수업 이름을 입력해 주세요.')
    if (f.name.trim().length > 100) return setErr('수업 이름은 100자 이하로 입력해 주세요.')
    if (f.description.length > 200) return setErr('설명은 200자 이하로 입력해 주세요.')
    if (!f.startTime) return setErr('시작 시간을 골라 주세요.')
    setBusy(true)
    setErr('')
    try {
      if (edit) {
        const body = {}
        if (f.name.trim() !== initial.name) body.name = f.name.trim()
        if (f.description !== (initial.description || '')) body.description = f.description
        if (f.capacity !== initial.capacity) body.capacity = f.capacity
        if (f.classDate !== initial.classDate) body.classDate = f.classDate
        if (f.startTime !== initial.startTime) body.startTime = f.startTime
        if (f.workout !== (initial.workout || '')) body.workout = f.workout          // '' = 지우기
        if (f.imageUrl !== (initial.imageUrl || '')) body.imageUrl = f.imageUrl     // '' = 지우기
        if (Object.keys(body).length) await groups.update(initial.classId, body)
        toast('수업을 수정했어요.')
      } else {
        await groups.create({ name: f.name.trim(), description: f.description || null, classDate: f.classDate, startTime: f.startTime, capacity: f.capacity, workout: f.workout || null, imageUrl: f.imageUrl || null })
        toast('그룹 수업을 열었어요.')
      }
      onSaved()
    } catch (e) {
      setErr(e.message)
      slots.reload()
    } finally {
      setBusy(false)
    }
  }

  const pickImage = async e => {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    setImgBusy(true)
    setErr('')
    try {
      set('imageUrl', await resizeImage(file))
    } catch (x) {
      setErr(x.message)
    } finally {
      setImgBusy(false)
    }
  }

  return (
    <Modal title={edit ? '그룹 수업 수정' : '그룹 수업 열기'} onClose={onClose} wide>
      <Field label="수업 이름">
        <input className="input" value={f.name} maxLength={100} onChange={e => set('name', e.target.value)} placeholder="예: 모닝 서킷" />
      </Field>
      <Field label="설명 (선택)" hint={`${f.description.length}/200`}>
        <textarea className="textarea" value={f.description} maxLength={200} onChange={e => set('description', e.target.value)} />
      </Field>
      <div className="grid g2" style={{ gap: 16 }}>
        <Field label="오늘 할 운동 (선택)">
          <select className="select" value={f.workout} onChange={e => set('workout', e.target.value)}>
            <option value="">선택 안 함</option>
            {GROUP_WORKOUTS.map(w => <option key={w} value={w}>{w}</option>)}
          </select>
        </Field>
        <div className="field">
          수업 사진 (선택)
          <div className="gimg-pick">
            {f.imageUrl && <img className="gimg" src={f.imageUrl} alt="수업 사진 미리보기" />}
            <label className="btn sm" style={{ cursor: imgBusy ? 'wait' : 'pointer' }}>
              {imgBusy ? '불러오는 중…' : f.imageUrl ? '사진 바꾸기' : '사진 고르기'}
              <input type="file" accept="image/*" hidden disabled={imgBusy} onChange={pickImage} />
            </label>
            {f.imageUrl && <button type="button" className="btn sm" onClick={() => set('imageUrl', '')}>사진 빼기</button>}
          </div>
        </div>
      </div>
      <div className="grid g2" style={{ gap: 16 }}>
        <Field label="날짜" hint={`${S.GROUP_OPEN_MIN_HOURS}시간 뒤부터 ${S.GROUP_OPEN_MAX_DAYS}일 안`}>
          <input className="input" type="date" min={today()} max={addDays(today(), S.GROUP_OPEN_MAX_DAYS)} value={f.classDate} onChange={e => setF(s => ({ ...s, classDate: e.target.value, startTime: edit && e.target.value === initial.classDate ? initial.startTime : '' }))} />
        </Field>
        <div className="field">
          정원
          <Seg value={f.capacity} options={[[4, '4명'], [5, '5명'], [6, '6명']]} onChange={v => set('capacity', v)} />
        </div>
      </div>
      <div className="field">
        시작 시간 ({S.SLOT_MINUTES}분 수업)
        {slots.loading ? <Loading /> : slots.error ? <div className="alert bad">{slots.error.message}</div> : !slots.data?.slots.length ? (
          <div className="empty">이날은 근무 시간이 없어요. 근무 시간 메뉴에서 먼저 등록해 주세요.</div>
        ) : (
          <div className="slots">
            {slots.data.slots.map(s => {
              const mine = edit && f.classDate === initial.classDate && s.startTime === initial.startTime
              const can = s.available || mine
              return (
                <button type="button" key={s.startTime} className={`slot ${f.startTime === s.startTime ? 'mine' : ''}`} disabled={!can} onClick={() => set('startTime', s.startTime)}>
                  <span className="t">{s.startTime}</span>
                  <span className="r">{mine ? '현재 시간' : s.available ? '열 수 있음' : reasonText(s.unavailableReason)}</span>
                </button>
              )
            })}
          </div>
        )}
      </div>
      {isOriginal && <span className="dim small">날짜와 시간은 그대로 두고 다른 항목만 바꿀 수 있어요.</span>}
      {err && <div className="alert bad">{err}</div>}
      <div className="row end">
        <button className="btn" onClick={onClose} disabled={busy}>취소</button>
        <button className="btn primary" onClick={save} disabled={busy || imgBusy}>{busy ? '저장 중…' : edit ? '수정 저장' : '수업 열기'}</button>
      </div>
    </Modal>
  )
}
