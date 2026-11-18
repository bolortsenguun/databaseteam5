import { Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider, useAuth, landingFor } from './lib/auth'
import { ToastProvider, Loading } from './components/ui'
import { RoleLayout } from './components/Layout'
import Login from './pages/auth/Login'
import SignupType from './pages/auth/SignupType'
import SignupForm from './pages/auth/SignupForm'
import Pending from './pages/auth/Pending'
import ForcePassword from './pages/auth/ForcePassword'
import Kiosk from './pages/auth/Kiosk'
import MemberHome from './pages/member/Home'
import MemberBook from './pages/member/Book'
import MemberReservations from './pages/member/Reservations'
import MemberPasses from './pages/member/Passes'
import MemberShop from './pages/member/Shop'
import MemberTrainers from './pages/member/Trainers'
import MemberHistory from './pages/member/History'
import MemberProfile from './pages/member/Profile'
import TrainerLessons from './pages/trainer/Lessons'
import TrainerGroups from './pages/trainer/Groups'
import TrainerClients from './pages/trainer/Clients'
import TrainerSchedule from './pages/trainer/Schedule'
import TrainerProfile from './pages/trainer/Profile'
import AdminDashboard from './pages/admin/Dashboard'
import AdminStats from './pages/admin/Stats'
import AdminMembers from './pages/admin/Members'
import AdminMemberDetail from './pages/admin/MemberDetail'
import AdminTrainers from './pages/admin/Trainers'
import AdminProducts from './pages/admin/Products'
import AdminPayments from './pages/admin/Payments'
import AdminReservations from './pages/admin/Reservations'
import AdminAttendance from './pages/admin/Attendance'
import AdminSettings from './pages/admin/Settings'

function Root() {
  const { me } = useAuth()
  if (me === undefined) return <Loading />
  return <Navigate to={landingFor(me)} replace />
}

export default function App() {
  return (
    <ToastProvider>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<Root />} />
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<SignupType />} />
          <Route path="/signup/member" element={<SignupForm role="MEMBER" />} />
          <Route path="/signup/trainer" element={<SignupForm role="TRAINER" />} />
          <Route path="/pending" element={<Pending />} />
          <Route path="/password-change" element={<ForcePassword />} />
          <Route path="/kiosk" element={<Kiosk />} />
          <Route path="/member" element={<RoleLayout role="MEMBER" />}>
            <Route index element={<MemberHome />} />
            <Route path="book" element={<MemberBook />} />
            <Route path="reservations" element={<MemberReservations />} />
            <Route path="passes" element={<MemberPasses />} />
            <Route path="shop" element={<MemberShop />} />
            <Route path="trainers" element={<MemberTrainers />} />
            <Route path="history" element={<MemberHistory />} />
            <Route path="profile" element={<MemberProfile />} />
          </Route>
          <Route path="/trainer" element={<RoleLayout role="TRAINER" />}>
            <Route index element={<TrainerLessons />} />
            <Route path="groups" element={<TrainerGroups />} />
            <Route path="clients" element={<TrainerClients />} />
            <Route path="schedule" element={<TrainerSchedule />} />
            <Route path="profile" element={<TrainerProfile />} />
          </Route>
          <Route path="/admin" element={<RoleLayout role="ADMIN" />}>
            <Route index element={<AdminDashboard />} />
            <Route path="stats" element={<AdminStats />} />
            <Route path="members" element={<AdminMembers />} />
            <Route path="members/:id" element={<AdminMemberDetail />} />
            <Route path="trainers" element={<AdminTrainers />} />
            <Route path="products" element={<AdminProducts />} />
            <Route path="payments" element={<AdminPayments />} />
            <Route path="reservations" element={<AdminReservations />} />
            <Route path="attendance" element={<AdminAttendance />} />
            <Route path="settings" element={<AdminSettings />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </ToastProvider>
  )
}
