import { Component, OnInit, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import * as L from 'leaflet';
import { Centre } from '../../models/centre.model';
import { CentreService } from '../../services/centre.service';
import { EquipementService } from '../../services/equipement.services';
import { AuthService } from '../../services/auth.service'; // <-- AJOUT

@Component({
  selector: 'app-centres-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './centres-list.component.html',
  styleUrls: ['./centres-list.component.scss']
})
export class CentresListComponent implements OnInit, AfterViewInit {
  centres: Centre[] = [];
  equipementsCountByCentre: Record<number, number> = {};

  loading = false;
  saving = false;
  deletingId: number | null = null;

  showForm = false;
  editingId: number | null = null;

  message: string | null = null;
  errorMessage: string | null = null;

  // Toggle type
  type: 'PHYSIQUE' | 'CLOUD' = 'PHYSIQUE';

  // <-- AJOUT : propriété pour le template
  isAdmin = false;

  fournisseurs = [
    { value: 'AWS', label: 'AWS', color: '#FF9900' },
    { value: 'AZURE', label: 'Azure', color: '#0078D4' },
    { value: 'GCP', label: 'GCP', color: '#4285F4' }
  ];

  regions = [
    'eu-west-1', 'eu-west-2', 'eu-west-3',
    'us-east-1', 'us-east-2', 'us-west-1', 'us-west-2',
    'ap-south-1', 'ap-northeast-1', 'ap-southeast-1'
  ];

  centreForm;

  private mapInitialized = false;
  private mainMap: L.Map | null = null;
  private formMap: L.Map | null = null;
  private formMarker: L.Marker | null = null;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private centreService: CentreService,
    private equipementService: EquipementService,
    private authService: AuthService // <-- AJOUT
  ) {
    this.centreForm = this.fb.group({
      nom: ['', [Validators.required]],
      localisation: [''],
      latitude: this.fb.control<number | null>(null),
      longitude: this.fb.control<number | null>(null),
      region: [''],
      fournisseur: [''],
      apiEndpoint: ['']
    });
  }

  ngOnInit(): void {
    this.isAdmin = this.authService.isAdmin(); // <-- AJOUT
    this.loadCentres();
  }

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    this.mainMap?.remove();
    this.formMap?.remove();
  }

  // ========== PUBLIC METHODS ==========

  onTypeChange(newType: 'PHYSIQUE' | 'CLOUD'): void {
    this.type = newType;
    this.centreForm.patchValue({
      localisation: '',
      latitude: null,
      longitude: null,
      region: '',
      fournisseur: '',
      apiEndpoint: ''
    });
    this.applyValidators();
    if (this.type === 'PHYSIQUE') {
      setTimeout(() => this.initFormMap(), 100);
    } else {
      this.destroyFormMap();
    }
  }

  loadCentres(): void {
    this.loading = true;
    this.errorMessage = '';
    this.centreService
      .getMesCentres()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (data: unknown) => {
          this.centres = this.normalizeCentres(data);
          this.loadCounts();
          setTimeout(() => this.initMainMap(), 100);
        },
        error: (error) => {
          this.centres = [];
          this.equipementsCountByCentre = {};
          this.errorMessage = this.buildErrorMessage('Chargement des centres impossible.', error);
        }
      });
  }

  loadCounts(): void {
    const ids = this.centres
      .map((centre) => this.getCentreId(centre))
      .filter((id): id is number => !!id);
    const requests = ids.map((id) => this.equipementService.getEquipementsByCentre(id).pipe(catchError(() => of([]))));
    if (requests.length === 0) {
      this.equipementsCountByCentre = {};
      return;
    }
    forkJoin(requests).subscribe((results) => {
      const counts: Record<number, number> = {};
      results.forEach((equipements, index) => {
        counts[ids[index]] = equipements.length;
      });
      this.equipementsCountByCentre = counts;
    });
  }

  openAddForm(): void {
    if (!this.isAdmin) return; // <-- AJOUT : sécurité supplémentaire
    this.showForm = true;
    this.editingId = null;
    this.type = 'PHYSIQUE';
    this.centreForm.reset({
      nom: '',
      localisation: '',
      latitude: null,
      longitude: null,
      region: '',
      fournisseur: '',
      apiEndpoint: ''
    });
    this.applyValidators();
    this.message = null;
    this.errorMessage = null;
    setTimeout(() => this.initFormMap(), 100);
  }

  openEditForm(centre: Centre, event: Event): void {
    if (!this.isAdmin) return; // <-- AJOUT
    event.stopPropagation();
    const id = this.getCentreId(centre);
    if (!id) return;
    this.showForm = true;
    this.editingId = id;
    this.type = (centre.type as any) === 'CLOUD' ? 'CLOUD' : 'PHYSIQUE';
    this.centreForm.reset({
      nom: centre.nom,
      localisation: centre.localisation ?? '',
      latitude: centre.latitude ?? null,
      longitude: centre.longitude ?? null,
      region: centre.region ?? '',
      fournisseur: centre.fournisseur ?? '',
      apiEndpoint: centre.apiEndpoint ?? ''
    });
    this.applyValidators();
    setTimeout(() => {
      if (this.type === 'PHYSIQUE') {
        this.initFormMap(centre.latitude, centre.longitude);
      }
    }, 100);
  }

  closeForm(): void {
    this.showForm = false;
    this.editingId = null;
    this.centreForm.reset();
    this.destroyFormMap();
  }

  submitCentre(): void {
    if (!this.isAdmin) { // <-- AJOUT
      this.errorMessage = 'Action reservee aux administrateurs.';
      return;
    }
    if (this.saving || this.centreForm.invalid) {
      this.centreForm.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.message = null;
    this.errorMessage = null;

    const raw = this.centreForm.getRawValue();
    const payload: any = {
      nom: raw.nom,
      type: this.type
    };

    if (this.type === 'PHYSIQUE') {
      payload.localisation = raw.localisation;
      payload.latitude = raw.latitude;
      payload.longitude = raw.longitude;
    } else {
      payload.region = raw.region;
      payload.fournisseur = raw.fournisseur;
      payload.apiEndpoint = raw.apiEndpoint || null;
      payload.latitude = null;
      payload.longitude = null;
      payload.localisation = raw.region;
    }

    const request$ = this.editingId
      ? this.centreService.updateCentre(this.editingId, payload)
      : this.centreService.createCentre(payload);

    request$.pipe(finalize(() => (this.saving = false))).subscribe({
      next: () => {
        this.message = this.editingId ? 'Centre modifie avec succes.' : 'Centre ajoute avec succes.';
        this.closeForm();
        this.loadCentres();
      },
      error: (error) => {
        this.errorMessage = this.buildErrorMessage('Enregistrement du centre impossible.', error);
      }
    });
  }

  deleteCentre(centre: Centre, event: Event): void {
    if (!this.isAdmin) return; // <-- AJOUT
    event.stopPropagation();
    const id = this.getCentreId(centre);
    if (!id || !window.confirm('Supprimer ce centre ?')) return;
    this.deletingId = id;
    this.message = null;
    this.errorMessage = null;
    this.centreService
      .deleteCentre(id)
      .pipe(finalize(() => (this.deletingId = null)))
      .subscribe({
        next: () => {
          this.message = 'Centre supprime avec succes.';
          this.loadCentres();
        },
        error: (error) => {
          this.errorMessage = this.buildErrorMessage('Suppression du centre impossible.', error);
        }
      });
  }

  goToDetail(centre: Centre): void {
    const id = this.getCentreId(centre);
    if (!id) return;
    this.router.navigate(['/centres', id, 'equipements']);
  }

  getCount(centre: Centre): number | null {
    const id = this.getCentreId(centre);
    if (!id || this.equipementsCountByCentre[id] === undefined) return null;
    return this.equipementsCountByCentre[id];
  }

  getDatabases(centre: Centre): string[] {
    const raw = centre.basesDeDonnees ?? centre.basesDonnees ?? centre.databases ?? [];
    return raw
      .map((item) => typeof item === 'string' ? item : (item.nom ?? item.name ?? item.type ?? ''))
      .map((name) => name.trim())
      .filter((name) => !!name);
  }

  hasDatabases(centre: Centre): boolean {
    return this.getDatabases(centre).length > 0;
  }

  trackByCentre(index: number, centre: Centre): number {
    return centre.idCentre ?? centre.id ?? index;
  }

  getCentreId(centre: Centre): number | undefined {
    return centre.idCentre ?? centre.id;
  }

  // ========== PRIVATE METHODS ==========

  private applyValidators(): void {
    const localisation = this.centreForm.get('localisation');
    const latitude = this.centreForm.get('latitude');
    const longitude = this.centreForm.get('longitude');
    const region = this.centreForm.get('region');
    const fournisseur = this.centreForm.get('fournisseur');

    if (this.type === 'PHYSIQUE') {
      localisation?.setValidators([Validators.required]);
      latitude?.setValidators([Validators.required]);
      longitude?.setValidators([Validators.required]);
      region?.clearValidators();
      fournisseur?.clearValidators();
    } else {
      localisation?.clearValidators();
      latitude?.clearValidators();
      longitude?.clearValidators();
      region?.setValidators([Validators.required]);
      fournisseur?.setValidators([Validators.required]);
    }

    localisation?.updateValueAndValidity();
    latitude?.updateValueAndValidity();
    longitude?.updateValueAndValidity();
    region?.updateValueAndValidity();
    fournisseur?.updateValueAndValidity();
  }

  private destroyFormMap(): void {
    if (this.formMap) {
      this.formMap.remove();
      this.formMap = null;
      this.formMarker = null;
    }
  }

  private buildErrorMessage(prefix: string, error: { status?: number }): string {
    if (error?.status === 401) return `${prefix} Session expiree (401).`;
    if (error?.status === 403) return `${prefix} Acces refuse (403).`;
    if (error?.status === 404) return `${prefix} Endpoint introuvable (404).`;
    return `${prefix} Veuillez reessayer.`;
  }

  private normalizeCentres(payload: unknown): Centre[] {
    if (Array.isArray(payload)) return payload;
    if (!payload || typeof payload !== 'object') return [];
    const wrapped = payload as Record<string, unknown>;
    const candidates = [
      wrapped['content'], wrapped['data'], wrapped['centres'], wrapped['items'],
      wrapped['result'], wrapped['results'], wrapped['payload'],
      wrapped['value'], wrapped['$values'],
      (wrapped['_embedded'] as Record<string, unknown>)?.['centres']
    ];
    for (const candidate of candidates) {
      if (Array.isArray(candidate)) return candidate as Centre[];
      if (candidate && typeof candidate === 'object') return [candidate as Centre];
    }
    return [];
  }

  private initMainMap(): void {
    if (this.mapInitialized) return;
    const container = document.getElementById('centresMap');
    if (!container) return;
    this.mainMap = L.map('centresMap').setView([35.5, 10.5], 6);
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/attributions">CARTO</a>',
      subdomains: 'abcd',
      maxZoom: 19
    }).addTo(this.mainMap);

    this.centres.forEach(centre => {
      if (centre.latitude == null || centre.longitude == null) return;
      const id = this.getCentreId(centre);
      const count = id ? this.getCount(centre) ?? 0 : 0;
      const color = count > 0 ? '#22c55e' : '#f59e0b';
      L.circleMarker([centre.latitude, centre.longitude], {
        radius: 14, fillColor: color, color, weight: 2, opacity: 1, fillOpacity: 0.8
      }).addTo(this.mainMap!)
        .bindPopup(`<b>${centre.nom}</b><br>${centre.localisation}<br>Equipements: ${count}`);
    });
    this.mapInitialized = true;
  }

  private initFormMap(lat?: number, lng?: number): void {
    if (this.formMap) {
      this.formMap.remove();
      this.formMap = null;
      this.formMarker = null;
    }
    const container = document.getElementById('formMap');
    if (!container) return;
    const startLat = lat ?? 34.0;
    const startLng = lng ?? 9.5;
    const startZoom = lat ? 12 : 6;
    this.formMap = L.map('formMap').setView([startLat, startLng], startZoom);
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      attribution: '&copy; OpenStreetMap contributors &copy; CARTO',
      subdomains: 'abcd',
      maxZoom: 19
    }).addTo(this.formMap);

    if (lat != null && lng != null) {
      this.formMarker = L.marker([lat, lng]).addTo(this.formMap);
    }
    this.formMap.on('click', (e: L.LeafletMouseEvent) => {
      const { lat, lng } = e.latlng;
      if (this.formMarker) {
        this.formMarker.setLatLng([lat, lng]);
      } else {
        this.formMarker = L.marker([lat, lng]).addTo(this.formMap!);
      }
      this.centreForm.patchValue({ latitude: lat, longitude: lng });
    });
  }
}