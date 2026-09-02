import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Equipement } from '../models/equipement';

export interface CreateEquipementPayload {
  nom: string;
  type: string;
  etat: string;
  adresseIP: string;
  systeme: string;
  idCentre?: number;
}

export interface UpdateEquipementPayload {
  nom: string;
  type: string;
  etat: string;
  adresseIP: string;
  systeme: string;
}

@Injectable({
  providedIn: 'root'
})
export class EquipementService {
  private apiUrl = '/api/equipements';

  constructor(private http: HttpClient) {}

  getEquipements(): Observable<Equipement[]> {
    return this.http.get<Equipement[]>(this.apiUrl);
  }

  getEquipement(id: number): Observable<Equipement> {
    return this.http.get<Equipement>(`${this.apiUrl}/${id}`);
  }

  getEquipementsByCentre(idCentre: number): Observable<Equipement[]> {
    return this.http.get<Equipement[]>(`${this.apiUrl}/centre/${idCentre}`);
  }

  createEquipement(payload: CreateEquipementPayload): Observable<Equipement> {
    return this.http.post<Equipement>(this.apiUrl, payload);
  }

  updateEquipement(id: number, payload: UpdateEquipementPayload): Observable<Equipement> {
    return this.http.put<Equipement>(`${this.apiUrl}/${id}`, payload);
  }

  deleteEquipement(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}