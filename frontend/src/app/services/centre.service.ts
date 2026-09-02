import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Centre } from '../models/centre.model';

export interface CentrePayload {
  nom: string;
  localisation: string;
}

type CentresResponse =
  | string
  | Centre[]
  | {
      content?: Centre[];
      data?: Centre[];
      centres?: Centre[];
      items?: Centre[];
      result?: Centre[];
      results?: Centre[];
      payload?: Centre[];
      value?: Centre[];
      $values?: Centre[];
      _embedded?: { centres?: Centre[] };
    };

@Injectable({
  providedIn: 'root'
})
export class CentreService {
  private apiUrl = '/api/centres';

  constructor(private http: HttpClient) {}

  getCentres(): Observable<Centre[]> {
    return this.http.get<Centre[]>(this.apiUrl);
  }

  getMesCentres(): Observable<Centre[]> {
    return this.http.get<CentresResponse>(`${this.apiUrl}/mes-centres`).pipe(map((response) => this.extractCentres(response)));
  }

  getCentreById(id: number): Observable<Centre> {
    return this.http.get<Centre>(`${this.apiUrl}/${id}`);
  }

  createCentre(payload: CentrePayload): Observable<Centre> {
    return this.http.post<Centre>(this.apiUrl, payload);
  }

  updateCentre(id: number, payload: CentrePayload): Observable<Centre> {
    return this.http.put<Centre>(`${this.apiUrl}/${id}`, payload);
  }

  deleteCentre(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  private extractCentres(response: CentresResponse | null | undefined): Centre[] {
    if (typeof response === 'string') {
      try {
        return this.extractCentres(JSON.parse(response) as CentresResponse);
      } catch {
        return [];
      }
    }

    if (Array.isArray(response)) {
      return response;
    }

    if (!response || typeof response !== 'object') {
      return [];
    }

    const candidates = [
      response?.content,
      response?.data,
      response?.centres,
      response?.items,
      response?.result,
      response?.results,
      response?.payload,
      response?.value,
      response?.$values,
      response?._embedded?.centres
    ];
    for (const candidate of candidates) {
      if (Array.isArray(candidate)) {
        return candidate;
      }
      if (candidate && typeof candidate === 'object') {
        return [candidate as Centre];
      }
    }

    return [];
  }
}
