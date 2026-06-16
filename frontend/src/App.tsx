import { Routes, Route, Navigate } from 'react-router-dom'
import { AppLayout } from '@/components/layout/AppLayout'
import { OrdersPage } from '@/pages/orders/OrdersPage'
import { OrderDetailPage } from '@/pages/orders/OrderDetailPage'
import { CustomersPage } from '@/pages/customers/CustomersPage'
import { ShortagesPage } from '@/pages/shortages/ShortagesPage'
import { ShortageOrderDetailPage } from '@/pages/shortages/ShortageOrderDetailPage'
import { DistributorsPage } from '@/pages/distributors/DistributorsPage'
import { UsersPage } from '@/pages/users/UsersPage'
import { CompoundingsPage } from '@/pages/compoundings/CompoundingsPage'
import { CompoundingDetailPage } from '@/pages/compoundings/CompoundingDetailPage'
import { CompoundingPharmaciesPage } from '@/pages/compounding-pharmacies/CompoundingPharmaciesPage'
import { PrescriptionsPage } from '@/pages/prescriptions/PrescriptionsPage'
import { PrescriptionDetailPage } from '@/pages/prescriptions/PrescriptionDetailPage'

export default function App() {
  return (
    <AppLayout>
      <Routes>
        <Route path="/orders" element={<OrdersPage />} />
        <Route path="/orders/:id" element={<OrderDetailPage />} />
        <Route path="/customers" element={<CustomersPage />} />
        <Route path="/shortages" element={<ShortagesPage />} />
        <Route path="/shortage-orders/:id" element={<ShortageOrderDetailPage />} />
        <Route path="/distributors" element={<DistributorsPage />} />
        <Route path="/users" element={<UsersPage />} />
        <Route path="/compoundings" element={<CompoundingsPage />} />
        <Route path="/compoundings/:id" element={<CompoundingDetailPage />} />
        <Route path="/compounding-pharmacies" element={<CompoundingPharmaciesPage />} />
        <Route path="/prescriptions" element={<PrescriptionsPage />} />
        <Route path="/prescriptions/:id" element={<PrescriptionDetailPage />} />
        <Route path="*" element={<Navigate to="/orders" replace />} />
      </Routes>
    </AppLayout>
  )
}
