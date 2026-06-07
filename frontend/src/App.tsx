import { Routes, Route, Navigate } from 'react-router-dom'
import { AppLayout } from '@/components/layout/AppLayout'
import { OrdersPage } from '@/pages/orders/OrdersPage'
import { OrderDetailPage } from '@/pages/orders/OrderDetailPage'
import { CustomersPage } from '@/pages/customers/CustomersPage'
import { ShortagesPage } from '@/pages/shortages/ShortagesPage'
import { UsersPage } from '@/pages/users/UsersPage'
import { NotificationsPage } from '@/pages/notifications/NotificationsPage'

export default function App() {
  return (
    <AppLayout>
      <Routes>
        <Route path="/orders" element={<OrdersPage />} />
        <Route path="/orders/:id" element={<OrderDetailPage />} />
        <Route path="/customers" element={<CustomersPage />} />
        <Route path="/shortages" element={<ShortagesPage />} />
        <Route path="/users" element={<UsersPage />} />
        <Route path="/notifications" element={<NotificationsPage />} />
        <Route path="*" element={<Navigate to="/orders" replace />} />
      </Routes>
    </AppLayout>
  )
}
