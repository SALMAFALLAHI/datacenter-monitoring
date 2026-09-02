import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { DashboardModule, User } from '../models/dashboard-access.model';

interface JwtPayload {
  sub: string;
  role: string;
  userId: number;
  fullName: string;
  prenom: string;
  nom: string;
  dashboardAccess: string[];
  centreIds?: number[];        // AJOUTÉ : centres assignés depuis le token
  centreNoms?: string[];       // AJOUTÉ : noms des centres depuis le token
  exp: number;
}

interface LoginResponse {
  token?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKeys = ['nouvelair_token', 'auth_token', 'token', 'access_token'];
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadUserFromToken();
  }

  login(email: string, motDePasse: string): Observable<LoginResponse> {
    const normalizedEmail = email.trim().toLowerCase();
    return this.http.post<LoginResponse>('/api/auth/login', { email: normalizedEmail, motDePasse })
      .pipe(tap((res) => {
        const token = res.token;
        if (token) {
          this.setToken(token);
        }
      }));
  }

  logout(): void {
    this.tokenKeys.forEach(key => localStorage.removeItem(key));
    this.currentUserSubject.next(null);
  }

  get token(): string | null {
    for (const key of this.tokenKeys) {
      const raw = localStorage.getItem(key);
      if (raw) return this.sanitizeToken(raw);
    }
    return null;
  }

  get isAuthenticated(): boolean {
    return !!this.token;
  }

  /** Vérifie si l'utilisateur est ADMIN */
  isAdmin(): boolean {
    return this.currentUserSubject.value?.role === 'ADMIN';
  }

  /** Vérifie si l'utilisateur est OPERATEUR */
  isOperateur(): boolean {
    return this.currentUserSubject.value?.role === 'OPERATEUR';
  }

  /** Vérifie si l'utilisateur est OBSERVATEUR */
  isObservateur(): boolean {
    return this.currentUserSubject.value?.role === 'OBSERVATEUR';
  }

  /** Vérifie l'accès à un module du dashboard */
  hasAccess(module: DashboardModule): boolean {
    const user = this.currentUserSubject.value;
    if (!user) return false;
    if (user.role === 'ADMIN') return true;
    return user.dashboardAccess?.includes(module) ?? false;
  }

  /** Vérifie l'accès à un centre spécifique (par ID) */
  hasAccessToCentre(centreId: number): boolean {
    const user = this.currentUserSubject.value;
    if (!user) return false;
    if (user.role === 'ADMIN') return true;
    return user.centreIds?.includes(centreId) ?? false;
  }

  /** Retourne la liste des modules accessibles */
  getUserDashboardAccess(): DashboardModule[] {
    const user = this.currentUserSubject.value;
    if (!user) return [];
    if (user.role === 'ADMIN') return Object.values(DashboardModule);
    return user.dashboardAccess || [];
  }

  /** Retourne la liste des IDs des centres assignés */
  getUserCentreIds(): number[] {
    const user = this.currentUserSubject.value;
    if (!user) return [];
    if (user.role === 'ADMIN') return [];
    return user.centreIds || [];
  }

  getAuthHeaders(): { Authorization?: string } {
    const token = this.token;
    return token ? { Authorization: `Bearer ${token}` } : {};
  }

  get displayName(): string {
    return this.currentUserSubject.value?.fullName || 'Utilisateur';
  }

  get initials(): string {
    const user = this.currentUserSubject.value;
    if (!user) return 'U';
    const parts = [user.prenom, user.nom].filter(Boolean);
    if (parts.length >= 2) {
      return `${parts[0].charAt(0)}${parts[1].charAt(0)}`.toUpperCase();
    }
    return user.nom?.slice(0, 2).toUpperCase() || 'U';
  }

  private setToken(token: string): void {
    const cleaned = this.sanitizeToken(token);
    if (!cleaned) { this.logout(); return; }
    this.tokenKeys.forEach(key => localStorage.setItem(key, cleaned));
    this.loadUserFromToken();
  }

  private sanitizeToken(token: string | null): string | null {
    if (!token) return null;
    const trimmed = token.trim();
    return trimmed ? trimmed.replace(/^Bearer\s+/i, '') : null;
  }

  private loadUserFromToken(): void {
    const token = this.token;
    if (!token) { this.currentUserSubject.next(null); return; }

    try {
      const decoded = jwtDecode<JwtPayload>(token);
      if (decoded.exp * 1000 < Date.now()) { this.logout(); return; }

      const user: User = {
        idAdmin: decoded.userId,
        email: decoded.sub,
        nom: decoded.nom || '',
        prenom: decoded.prenom || '',
        fullName: decoded.fullName || '',
        role: decoded.role,
        dashboardAccess: (decoded.dashboardAccess || []) as DashboardModule[],
        actif: true,
        createdByName: '',
        centreIds: decoded.centreIds || [],      // CHARGÉ depuis le token
        centreNoms: decoded.centreNoms || []     // CHARGÉ depuis le token
      };
      this.currentUserSubject.next(user);
    } catch (e) {
      this.logout();
    }
  }
}