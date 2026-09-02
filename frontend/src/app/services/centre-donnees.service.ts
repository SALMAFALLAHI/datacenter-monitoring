import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CentreDonnees } from '../models/centre-donnees';

export interface CentreDonneesPayload {
  nom: string;
  localisation: string;
}

@Injectable({
  providedIn: 'root'
})
export class CentreDonneesService {
  private apiUrl = '/api/centres';

  constructor(private http: HttpClient) {}

  getCentres(): Observable<CentreDonnees[]> {
    return this.http.get<CentreDonnees[]>(this.apiUrl);
  }

  createCentre(payload: CentreDonneesPayload): Observable<CentreDonnees> {
    return this.http.post<CentreDonnees>(this.apiUrl, payload);
  }

  updateCentre(idCentre: number, payload: CentreDonneesPayload): Observable<CentreDonnees> {
    return this.http.put<CentreDonnees>(`${this.apiUrl}/${idCentre}`, payload);
  }

  deleteCentre(idCentre: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${idCentre}`);
  }
}
