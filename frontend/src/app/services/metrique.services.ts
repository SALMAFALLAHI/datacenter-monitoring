import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Metrique } from '../models/metrique';
import { Equipement } from '../models/equipement';
import { MetriqueStatutResponse } from '../models/metrique-statut';

type ApiMetrique = {
  idMetrique?: number;
  cpu?: number;
  ram?: number;
  disque?: number;
  reseau?: number;
  temperature?: number | null;
  dateCollecte?: string;
  idEquipement?: number;
  nomEquipement?: string;
  adresseIP?: string;
  equipement?: Equipement;
  data?: unknown;
  value?: unknown;
  metrique?: unknown;
  latest?: unknown;
  item?: unknown;
};

type LatestMetriqueResponse =
  | ApiMetrique
  | ApiMetrique[]
  | {
      data?: ApiMetrique | ApiMetrique[];
      value?: ApiMetrique | ApiMetrique[];
      metrique?: ApiMetrique | ApiMetrique[];
      latest?: ApiMetrique | ApiMetrique[];
      item?: ApiMetrique | ApiMetrique[];
      content?: ApiMetrique[];
      items?: ApiMetrique[];
    };

type MetriquesListResponse =
  | ApiMetrique[]
  | {
      data?: ApiMetrique[];
      value?: ApiMetrique[];
      metriques?: ApiMetrique[];
      items?: ApiMetrique[];
      content?: ApiMetrique[];
      result?: ApiMetrique[];
    };

@Injectable({
  providedIn: 'root'
})
export class MetriqueService {
  private apiUrl = '/api/metriques';

  constructor(private http: HttpClient) {}

  getDernieresMetriques(): Observable<Metrique[]> {
    return this.http.get<Metrique[]>(`${this.apiUrl}/dernieres`);
  }

  getMetriquesParEquipement(id: number): Observable<Metrique[]> {
    return this.http
      .get<MetriquesListResponse>(`${this.apiUrl}/equipement/${id}`)
      .pipe(map((response) => this.normalizeListResponse(response)));
  }

  getDerniereMetriqueParEquipement(id: number): Observable<Metrique | null> {
    return this.http
      .get<LatestMetriqueResponse>(`${this.apiUrl}/equipement/${id}/latest`)
      .pipe(map((response) => this.normalizeLatestResponse(response)));
  }

  private normalizeLatestResponse(response: LatestMetriqueResponse | null | undefined): Metrique | null {
    if (!response) {
      return null;
    }

    if (Array.isArray(response)) {
      return this.mapApiMetrique(response[0]) ?? null;
    }

    const raw = response as ApiMetrique & {
      content?: unknown;
      items?: unknown;
    };

    if (typeof raw.idMetrique === 'number') {
      return this.mapApiMetrique(raw);
    }

    const candidates = [raw.data, raw.value, raw.metrique, raw.latest, raw.item, raw.content, raw.items];
    for (const candidate of candidates) {
      if (Array.isArray(candidate)) {
        return this.mapApiMetrique(candidate[0]) ?? null;
      }
      if (candidate && typeof candidate === 'object') {
        return this.mapApiMetrique(candidate as ApiMetrique);
      }
    }

    return null;
  }

  private normalizeListResponse(response: MetriquesListResponse | null | undefined): Metrique[] {
    if (Array.isArray(response)) {
      return response.map((item) => this.mapApiMetrique(item)).filter((item): item is Metrique => !!item);
    }

    if (!response || typeof response !== 'object') {
      return [];
    }

    const candidates = [response.data, response.value, response.metriques, response.items, response.content, response.result];
    for (const candidate of candidates) {
      if (Array.isArray(candidate)) {
        return candidate.map((item) => this.mapApiMetrique(item)).filter((item): item is Metrique => !!item);
      }
    }

    return [];
  }
  getDerniereMetriqueAvecStatut(idEquipement: number): Observable<MetriqueStatutResponse> {
  return this.http.get<MetriqueStatutResponse>(
    `${this.apiUrl}/equipement/${idEquipement}/latest-with-status`
  );
}

getHistoriqueParEquipement(idEquipement: number): Observable<Metrique[]> {
  return this.http.get<Metrique[]>(`${this.apiUrl}/equipement/${idEquipement}`);
}

  private mapApiMetrique(apiMetrique: ApiMetrique | null | undefined): Metrique | null {
    if (!apiMetrique) {
      return null;
    }

    const equipement: Equipement =
      apiMetrique.equipement ??
      ({
        idEquipement: apiMetrique.idEquipement ?? 0,
        nom: apiMetrique.nomEquipement ?? 'Equipement',
        type: '',
        etat: 'actif',
        adresseIP: apiMetrique.adresseIP ?? '',
        systeme: 'LINUX',
        dateAjout: apiMetrique.dateCollecte ?? new Date().toISOString()
      } as Equipement);

    return {
      idMetrique: apiMetrique.idMetrique ?? 0,
      cpu: apiMetrique.cpu ?? 0,
      ram: apiMetrique.ram ?? 0,
      disque: apiMetrique.disque ?? 0,
      reseau: apiMetrique.reseau ?? 0,
      temperature: apiMetrique.temperature ?? null,
      dateCollecte: apiMetrique.dateCollecte ?? new Date().toISOString(),
      equipement
    };
  }
}