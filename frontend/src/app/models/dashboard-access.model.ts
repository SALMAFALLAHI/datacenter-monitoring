export enum DashboardModule {
  ANOMALIES = 'ANOMALIES',
  EQUIPEMENTS = 'EQUIPEMENTS',
  METRIQUES = 'METRIQUES',
  RAPPORTS = 'RAPPORTS',
  CONFIGURATION = 'CONFIGURATION',
  UTILISATEURS = 'UTILISATEURS'
}

export interface DashboardModuleInfo {
  key: DashboardModule;
  label: string;
  route: string;
  icon: string;
}

export const DASHBOARD_MODULES: DashboardModuleInfo[] = [
  { key: DashboardModule.ANOMALIES, label: 'Anomalies', route: '/anomalies', icon: 'alert-triangle' },
  { key: DashboardModule.EQUIPEMENTS, label: 'Equipements', route: '/equipements', icon: 'server' },
  { key: DashboardModule.METRIQUES, label: 'Métriques', route: '/metriques', icon: 'activity' },
  { key: DashboardModule.RAPPORTS, label: 'Rapports', route: '/rapports', icon: 'file-text' },
  { key: DashboardModule.CONFIGURATION, label: 'Configuration', route: '/configuration', icon: 'settings' },
  { key: DashboardModule.UTILISATEURS, label: 'Utilisateurs', route: '/admin/utilisateurs', icon: 'users' }
];

export enum Role {
  ADMIN = 'ADMIN',
  OPERATEUR = 'OPERATEUR',
  OBSERVATEUR = 'OBSERVATEUR'
}

export interface User {
  idAdmin: number;
  email: string;
  nom: string;
  prenom: string;
  fullName: string;
  role: string;
  dashboardAccess: DashboardModule[];
  actif: boolean;
  createdByName: string;
  centreIds: number[];
  centreNoms: string[];
}

export interface CreateUserRequest {
  nom: string;
  prenom: string;
  email: string;
  password: string;
  role: Role;
  dashboardAccess: DashboardModule[];
  centreIds: number[];
  actif: boolean;
}