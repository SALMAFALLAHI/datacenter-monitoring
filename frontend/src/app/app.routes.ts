import { Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { CentresListComponent } from './components/centres-list/centres-list.component';
import { CentreEquipementsDetailComponent } from './components/centre-equipements-detail/centre-equipements-detail.component';
import { MetriquesComponent } from './components/metriques/metriques.component';
import { LoginComponent } from './components/login/login.component';
import { AuthGuard } from './guards/auth.guard';
import { AdminRequiredGuard } from './guards/admin-required.guard';
import { DashboardAccessGuard } from './guards/dashboard-access.guard';
import { LoggedInGuard } from './guards/logged-in.guard'; // <-- AJOUT
import { DashboardModule } from './models/dashboard-access.model';

export const routes: Routes = [
  // Public — redirige si déjà connecté
  {
    path: 'login',
    component: LoginComponent,
    canActivate: [LoggedInGuard] // <-- AJOUT
  },

  // Redirections
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'equipements', redirectTo: '/centres', pathMatch: 'full' },

  // Dashboard (tout le monde authentifié)
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [AuthGuard]
  },

  // Centres
  {
    path: 'centres',
    component: CentresListComponent,
    canActivate: [AuthGuard, DashboardAccessGuard],
    data: { requiredModule: DashboardModule.EQUIPEMENTS }
  },
  {
    path: 'centres/:id/equipements',
    component: CentreEquipementsDetailComponent,
    canActivate: [AuthGuard, DashboardAccessGuard],
    data: { requiredModule: DashboardModule.EQUIPEMENTS }
  },

  // Métriques
  {
    path: 'metriques',
    component: MetriquesComponent,
    canActivate: [AuthGuard, DashboardAccessGuard],
    data: { requiredModule: DashboardModule.METRIQUES }
  },

  // Anomalies
  {
    path: 'anomalies',
    loadComponent: () => import('./components/anomalie-list/anomalie-list.component')
      .then(m => m.AnomalieListComponent),
    canActivate: [AuthGuard, DashboardAccessGuard],
    data: { requiredModule: DashboardModule.ANOMALIES }
  },

  // Admin - Gestion des utilisateurs
  {
    path: 'admin/utilisateurs',
    loadComponent: () => import('./components/admin/user-management.component')
      .then(m => m.UserManagementComponent),
    canActivate: [AuthGuard, AdminRequiredGuard]
  },

  // Fallback
  { path: '**', redirectTo: '/dashboard' }
];