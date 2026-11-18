import { useAuth } from '../../lib/auth'
import { PageTitle } from '../../components/ui'
import ScheduleManager from '../../components/ScheduleManager'

export default function TrainerSchedule() {
  const { me } = useAuth()
  return (
    <main className="page">
      <PageTitle title="근무 시간" sub="회원은 근무 시간 안에서만 개인 PT를 예약할 수 있어요. 휴무일에는 예약과 그룹 수업이 막혀요." />
      <ScheduleManager trainerId={me.userId} />
    </main>
  )
}
