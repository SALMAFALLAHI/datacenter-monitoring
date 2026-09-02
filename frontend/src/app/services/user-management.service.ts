import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CreateUserRequest, User } from '../models/dashboard-access.model';

export interface CentreOption {
  id: number;
  nom: string;
}

@Injectable({ providedIn: 'root' })
export class UserManagementService {
  private readonly API_URL = 'http://localhost:8080/api/admin/users';

  constructor(private http: HttpClient) {}

  /** Récupère tous les utilisateurs (admin uniquement) */
  getAllUsers(): Observable<User[]> {
    return this.http.get<User[]>(this.API_URL);
  }

  /** Crée un nouvel utilisateur (admin uniquement) */
  createUser(user: CreateUserRequest): Observable<User> {
    return this.http.post<User>(this.API_URL, user);
  }

  /** Met à jour les droits d'accès dashboard d'un utilisateur */
  updateAccess(userId: number, access: string[]): Observable<User> {
    return this.http.put<User>(`${this.API_URL}/${userId}/access`, { dashboardAccess: access });
  }

  /** Active/Désactive un utilisateur */
  toggleStatus(userId: number): Observable<void> {
    return this.http.patch<void>(`${this.API_URL}/${userId}/toggle-status`, {});
  }

  /** Supprime un utilisateur */
  deleteUser(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${userId}`);
  }

  /** Récupère la liste des centres depuis la BDD (admin uniquement) */
  getAvailableCentres(): Observable<CentreOption[]> {
    return this.http.get<CentreOption[]>('/api/centres');
  }
}