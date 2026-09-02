import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export interface EquipementDetecte {
  nom?: string;
  adresseIP?: string;
  ip?: string;
  type?: string;
  systeme?: string;
  etat?: string;
}

type ScannerItem =
  | EquipementDetecte
  | {
      nomPropose?: string;
      typeDetecte?: string;
      systemeDetecte?: string;
      adresseIP?: string;
      ip?: string;
      etat?: string;
      joignable?: boolean;
    };

type ScannerResponse =
  | ScannerItem[]
  | {
      detectes?: ScannerItem[];
      value?: ScannerItem[];
      items?: ScannerItem[];
    };

@Injectable({
  providedIn: 'root'
})
export class DecouverteService {
  private apiUrl = '/api/decouverte';

  constructor(private http: HttpClient) {}

  scanner(debut: string, fin: string): Observable<EquipementDetecte[]> {
    return this.http
      .get<ScannerResponse>(`${this.apiUrl}/scanner`, {
        params: { debut, fin }
      })
      .pipe(map((response) => this.normalizeScannerResponse(response)));
  }

  scannerEtAjouter(debut: string, fin: string, idCentre: number): Observable<unknown> {
    return this.http.post(`${this.apiUrl}/scanner-ajouter`, null, {
      params: { debut, fin, idCentre }
    });
  }

  testerIp(ip: string): Observable<unknown> {
    return this.http.get(`${this.apiUrl}/tester/${encodeURIComponent(ip)}`);
  }

  private normalizeScannerResponse(response: ScannerResponse | null | undefined): EquipementDetecte[] {
    const source = Array.isArray(response)
      ? response
      : response?.detectes ?? response?.value ?? response?.items ?? [];

    if (!Array.isArray(source)) {
      return [];
    }

    const normalizedItems = source.map((item) => {
      const normalized = item as {
        nom?: string;
        nomPropose?: string;
        adresseIP?: string;
        ip?: string;
        type?: string;
        typeDetecte?: string;
        systeme?: string;
        systemeDetecte?: string;
        etat?: string;
      };

      return {
        nom: normalized.nom ?? normalized.nomPropose,
        adresseIP: normalized.adresseIP,
        ip: normalized.ip,
        type: normalized.type ?? normalized.typeDetecte,
        systeme: normalized.systeme ?? normalized.systemeDetecte,
        etat: normalized.etat ?? 'Actif'
      };
    });

    const uniqueByKey = new Map<string, EquipementDetecte>();
    for (const item of normalizedItems) {
      const ip = (item.adresseIP ?? item.ip ?? '').trim().toLowerCase();
      const nom = (item.nom ?? '').trim().toLowerCase();
      const key = `${ip}-${nom}`;

      if (!uniqueByKey.has(key)) {
        uniqueByKey.set(key, item);
      }
    }

    return Array.from(uniqueByKey.values());
  }
}
