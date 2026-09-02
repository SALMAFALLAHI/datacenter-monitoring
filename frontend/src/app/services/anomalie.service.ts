import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Anomalie,AnomalieStats } from '../models/anomalie.model';

export interface Decision {
  idDecision: number;
  auteur: string;
  dateDecision: string;
  ancienStatut: string;
  nouveauStatut: string;
  commentaire: string;
}

export interface AnomalieGroupe {
  idGroupe: string;
  idEquipement: number;
  nomEquipement: string;
  typeAnomalie: string;
  niveau: string;
  scoreMax: number;
  dateDerniereDetection: string;
  nombreOccurrences: number;
  idsAnomalies: number[];
  statut: string;
  decisions: Decision[];
  idCentre: number;
}

export interface BatchDecisionRequest {
  idsAnomalies: number[];
  nouveauStatut: string;
  commentaire: string;
}

@Injectable({ providedIn: 'root' })
export class AnomalieService {
  private readonly api = 'http://localhost:8080/api/anomalies';

  constructor(private http: HttpClient) {}

  getAnomalies(centreId: number, statut?: string): Observable<AnomalieGroupe[]> {
    let params = new HttpParams().set('centreId', centreId.toString());
    if (statut) params = params.set('statut', statut);
    return this.http.get<AnomalieGroupe[]>(this.api, { params });
  }

  getStats(centreId: number): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(`${this.api}/stats`, {
      params: new HttpParams().set('centreId', centreId.toString())
    });
  }

  traiterBatch(request: BatchDecisionRequest): Observable<void> {
    return this.http.post<void>(`${this.api}/batch`, request);
  }


  getAnomaliesNonTraitees(centreId?: number): Observable<Anomalie[]> {
    let params = new HttpParams();
    if (centreId) {
      params = params.set('centreId', centreId.toString());
    }
    return this.http.get<Anomalie[]>(`${this.api}/non-traitees`, { params });
  }

  changerStatut(id: number, statut: string, commentaire?: string): Observable<Anomalie> {
    return this.http.patch<Anomalie>(
      `${this.api}/${id}/statut`,
      { statut, commentaire }
    );
  }
}
