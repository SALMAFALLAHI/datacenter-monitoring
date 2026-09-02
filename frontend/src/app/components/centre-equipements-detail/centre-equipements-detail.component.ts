import { Component, OnDestroy, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, finalize } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import Chart from 'chart.js/auto';
import { Centre } from '../../models/centre.model';
import { Equipement } from '../../models/equipement';
import { Metrique } from '../../models/metrique';
import { CentreService } from '../../services/centre.service';
import { DecouverteService, EquipementDetecte } from '../../services/decouverte.service';
import { MetriqueService } from '../../services/metrique.services';
import {
  CreateEquipementPayload,
  EquipementService,
  UpdateEquipementPayload
} from '../../services/equipement.services';

@Component({
  selector: 'app-centre-equipements-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './centre-equipements-detail.component.html',
  styleUrls: ['./centre-equipements-detail.component.scss']
})
export class CentreEquipementsDetailComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();
  private readonly ipPattern =
    /^(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)){3}$/;

  // ===== CHART VIEWCHILDS — un seul jeu, dans la modal =====
  @ViewChild('modalCpuChart') modalCpuChartRef?: ElementRef<HTMLCanvasElement>;
  @ViewChild('modalNetChart') modalNetChartRef?: ElementRef<HTMLCanvasElement>;

  centreId: number | null = null;
  centre: Centre | null = null;
  equipements: Equipement[] = [];
  detectedEquipements: EquipementDetecte[] = [];
  selectedEquipement: Equipement | null = null;
  latestMetric: Metrique | null = null;
  loadingMetriques = false;
  loadingHistorique = false;

  loadingCentre = false;
  loadingEquipements = false;
  scanning = false;
  savingEquipement = false;
  addingDetectedKey: string | null = null;
  deletingEquipementId: number | null = null;

  showScanForm = false;
  showEquipementForm = false;
  scanMode: 'preview' | 'scanAdd' = 'preview';
  editingEquipementId: number | null = null;

  message = '';
  errorMessage = '';

  equipementForm;
  scanForm;

  // charts de la modal, recrees a chaque ouverture
  private modalCpuChart: Chart | null = null;
  private modalNetChart: Chart | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fb: FormBuilder,
    private centreService: CentreService,
    private equipementService: EquipementService,
    private decouverteService: DecouverteService,
    private metriqueService: MetriqueService
  ) {
    this.equipementForm = this.fb.group({
      nom: ['', [Validators.required]],
      type: ['', [Validators.required]],
      adresseIP: ['', [Validators.required, Validators.pattern(this.ipPattern)]],
      systeme: ['LINUX', [Validators.required]],
      etat: ['Actif', [Validators.required]]
    });

    this.scanForm = this.fb.group({
      debut: ['', [Validators.required, Validators.pattern(this.ipPattern)]],
      fin: ['', [Validators.required, Validators.pattern(this.ipPattern)]]
    });
  }

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const idParam = params.get('id');
      const id = idParam ? Number(idParam) : NaN;
      if (!idParam || Number.isNaN(id)) {
        this.errorMessage = 'Identifiant du centre invalide.';
        this.router.navigate(['/centres']);
        return;
      }

      this.centreId = id;
      this.detectedEquipements = [];
      this.selectedEquipement = null;
      this.latestMetric = null;
      this.showScanForm = false;
      this.showEquipementForm = false;

      this.loadCentre();
      this.loadEquipements();
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.destroyModalCharts();
  }

  loadCentre(): void {
    if (!this.centreId) {
      return;
    }

    this.loadingCentre = true;
    this.errorMessage = '';

    this.centreService
      .getCentreById(this.centreId)
      .pipe(finalize(() => (this.loadingCentre = false)))
      .subscribe({
        next: (data) => {
          this.centre = data;
        },
        error: (error) => {
          this.centre = null;
          this.errorMessage = this.buildErrorMessage('Chargement du centre impossible.', error);
        }
      });
  }

  loadEquipements(): void {
    if (!this.centreId) {
      return;
    }

    this.loadingEquipements = true;
    this.errorMessage = '';

    this.equipementService
      .getEquipementsByCentre(this.centreId)
      .pipe(finalize(() => (this.loadingEquipements = false)))
      .subscribe({
        next: (data) => {
          this.equipements = data ?? [];
          this.filterDetectedEquipements();
          if (this.selectedEquipement) {
            this.loadEquipementMetriques(this.selectedEquipement);
            this.loadEquipementHistorique(this.selectedEquipement);
          }
        },
        error: (error) => {
          this.equipements = [];
          this.errorMessage = this.buildErrorMessage('Chargement des equipements impossible.', error);
        }
      });
  }

  selectEquipement(item: Equipement, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }

    this.selectedEquipement = item;
    this.loadEquipementMetriques(item);

    // laisser Angular rendre la modal (canvas) avant de construire les charts
    setTimeout(() => this.loadEquipementHistorique(item), 50);
  }

  loadEquipementMetriques(item: Equipement): void {
    if (!item?.idEquipement) {
      this.latestMetric = null;
      return;
    }

    this.loadingMetriques = true;
    this.metriqueService
      .getDerniereMetriqueParEquipement(item.idEquipement)
      .pipe(finalize(() => (this.loadingMetriques = false)))
      .subscribe({
        next: (data) => {
          this.latestMetric = data ?? null;
        },
        error: (error) => {
          this.latestMetric = null;
          this.errorMessage = this.buildErrorMessage('Chargement des metriques impossible.', error);
        }
      });
  }

  // ============================================================
  // HISTORIQUE + CHARTS DE LA MODAL
  // ============================================================
  loadEquipementHistorique(item: Equipement): void {
    if (!item?.idEquipement) {
      return;
    }

    this.loadingHistorique = true;
    this.metriqueService
      .getHistoriqueParEquipement(item.idEquipement)
      .pipe(finalize(() => (this.loadingHistorique = false)))
      .subscribe({
        next: (data) => {
          this.buildModalCharts(data ?? []);
        },
        error: (error) => {
          this.errorMessage = this.buildErrorMessage("Chargement de l'historique impossible.", error);
        }
      });
  }

  private buildModalCharts(historique: Metrique[]): void {
    this.destroyModalCharts();

    if (!this.modalCpuChartRef || !this.modalNetChartRef) {
      return; // canvas pas encore dans le DOM
    }

    const ordered = [...historique].reverse();
    const labels = ordered.map(m =>
      m.dateCollecte ? new Date(m.dateCollecte).toLocaleTimeString() : ''
    );

    const cpuCtx = this.modalCpuChartRef.nativeElement.getContext('2d');
    if (cpuCtx) {
      this.modalCpuChart = new Chart(cpuCtx, {
        type: 'line',
        data: {
          labels,
          datasets: [
            { label: 'CPU %', data: ordered.map(m => m.cpu ?? 0), borderColor: '#3b82f6', backgroundColor: 'rgba(59,130,246,0.1)', fill: true, tension: 0.4 },
            { label: 'Memoire %', data: ordered.map(m => m.ram ?? 0), borderColor: '#22c55e', backgroundColor: 'rgba(34,197,94,0.1)', fill: true, tension: 0.4 }
          ]
        },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'top', align: 'end' } }, scales: { y: { beginAtZero: true, max: 100 } } }
      });
    }

    const netCtx = this.modalNetChartRef.nativeElement.getContext('2d');
    if (netCtx) {
      this.modalNetChart = new Chart(netCtx, {
        type: 'line',
        data: {
          labels,
          datasets: [
            { label: 'Reseau (Mo/s)', data: ordered.map(m => m.reseau ?? 0), borderColor: '#f59e0b', backgroundColor: 'rgba(245,158,11,0.1)', fill: true, tension: 0.4 }
          ]
        },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: true } } }
      });
    }
  }

  private destroyModalCharts(): void {
    this.modalCpuChart?.destroy();
    this.modalNetChart?.destroy();
    this.modalCpuChart = null;
    this.modalNetChart = null;
  }

  closeEquipementMetrics(): void {
    this.selectedEquipement = null;
    this.latestMetric = null;
    this.loadingMetriques = false;
    this.destroyModalCharts();
  }

  openScan(mode: 'preview' | 'scanAdd'): void {
    this.scanMode = mode;
    this.showScanForm = true;
    this.detectedEquipements = [];
    this.message = '';
    this.errorMessage = '';
  }

  runScan(): void {
    if (!this.centreId || this.scanning || this.scanForm.invalid) {
      this.scanForm.markAllAsTouched();
      return;
    }

    const { debut, fin } = this.scanForm.getRawValue() as { debut: string; fin: string };

    if (this.isLoopbackRange(debut, fin)) {
      this.errorMessage =
        "La plage 127.x.x.x est reservee au loopback local. Elle peut remonter de faux equipements. Utilisez la plage reseau reelle (ex: 192.168.x.x).";
      return;
    }

    this.scanning = true;
    this.message = '';
    this.errorMessage = '';

    if (this.scanMode === 'scanAdd') {
      this.decouverteService
        .scannerEtAjouter(debut, fin, this.centreId)
        .pipe(finalize(() => (this.scanning = false)))
        .subscribe({
          next: () => {
            this.message = 'Scan et ajout termines.';
            this.showScanForm = false;
            this.detectedEquipements = [];
            this.loadEquipements();
          },
          error: (error) => {
            this.errorMessage = this.buildErrorMessage('Scan et ajout impossibles.', error);
          }
        });
      return;
    }

    this.decouverteService
      .scanner(debut, fin)
      .pipe(finalize(() => (this.scanning = false)))
      .subscribe({
        next: (data) => {
          this.detectedEquipements = data ?? [];
          this.filterDetectedEquipements();
          this.message = `${this.detectedEquipements.length} equipement(s) detecte(s).`;
        },
        error: (error) => {
          this.detectedEquipements = [];
          this.errorMessage = this.buildErrorMessage('Scan preview impossible.', error);
        }
      });
  }

  addDetectedEquipement(item: EquipementDetecte): void {
    if (!this.centreId || this.savingEquipement) {
      return;
    }

    const itemKey = this.detectedKey(item);
    this.addingDetectedKey = itemKey;

    const payload: CreateEquipementPayload = {
      nom: item.nom || `Equipement-${item.adresseIP || item.ip || 'auto'}`,
      adresseIP: item.adresseIP || item.ip || '0.0.0.0',
      type: item.type || 'Inconnu',
      systeme: item.systeme || 'LINUX',
      etat: item.etat || 'Actif',
      idCentre: this.centreId
    };

    this.savingEquipement = true;
    this.equipementService
      .createEquipement(payload)
      .pipe(
        finalize(() => {
          this.savingEquipement = false;
          this.addingDetectedKey = null;
        })
      )
      .subscribe({
        next: () => {
          this.message = 'Equipement detecte ajoute.';
          this.detectedEquipements = this.detectedEquipements.filter((detected) => this.detectedKey(detected) !== itemKey);
          this.loadEquipements();
        },
        error: (error) => {
          this.errorMessage = this.buildErrorMessage("Ajout de l'equipement detecte impossible.", error);
        }
      });
  }

  openManualAddForm(): void {
    this.showEquipementForm = true;
    this.editingEquipementId = null;
    this.equipementForm.reset({
      nom: '',
      type: '',
      adresseIP: '',
      systeme: 'LINUX',
      etat: 'Actif'
    });
  }

  editEquipement(item: Equipement): void {
    this.selectEquipement(item);
    this.showEquipementForm = true;
    this.editingEquipementId = item.idEquipement;
    this.equipementForm.reset({
      nom: item.nom,
      type: item.type,
      adresseIP: item.adresseIP,
      systeme: item.systeme,
      etat: item.etat
    });
  }

  cancelEquipementForm(): void {
    this.showEquipementForm = false;
    this.editingEquipementId = null;
  }

  submitEquipement(): void {
    if (!this.centreId || this.savingEquipement || this.equipementForm.invalid) {
      this.equipementForm.markAllAsTouched();
      return;
    }

    this.savingEquipement = true;
    this.message = '';
    this.errorMessage = '';

    const value = this.equipementForm.getRawValue() as {
      nom: string;
      type: string;
      adresseIP: string;
      systeme: string;
      etat: string;
    };

    if (this.editingEquipementId) {
      const updatePayload: UpdateEquipementPayload = {
        nom: value.nom,
        type: value.type,
        adresseIP: value.adresseIP,
        systeme: value.systeme,
        etat: value.etat
      };

      this.equipementService
        .updateEquipement(this.editingEquipementId, updatePayload)
        .pipe(finalize(() => (this.savingEquipement = false)))
        .subscribe({
          next: () => {
            this.message = 'Equipement modifie avec succes.';
            this.showEquipementForm = false;
            this.editingEquipementId = null;
            this.loadEquipements();
          },
          error: (error) => {
            this.errorMessage = this.buildErrorMessage('Modification impossible.', error);
          }
        });
      return;
    }

    const createPayload: CreateEquipementPayload = {
      nom: value.nom,
      type: value.type,
      adresseIP: value.adresseIP,
      systeme: value.systeme,
      etat: value.etat,
      idCentre: this.centreId
    };

    this.equipementService
      .createEquipement(createPayload)
      .pipe(finalize(() => (this.savingEquipement = false)))
      .subscribe({
        next: () => {
          this.message = 'Equipement ajoute avec succes.';
          this.showEquipementForm = false;
          this.loadEquipements();
        },
        error: (error) => {
          this.errorMessage = this.buildErrorMessage('Ajout impossible.', error);
        }
      });
  }

  deleteEquipement(item: Equipement): void {
    if (!window.confirm(`Supprimer l'equipement ${item.nom} ?`)) {
      return;
    }

    this.deletingEquipementId = item.idEquipement;
    this.equipementService
      .deleteEquipement(item.idEquipement)
      .pipe(finalize(() => (this.deletingEquipementId = null)))
      .subscribe({
        next: () => {
          this.message = 'Equipement supprime avec succes.';
          this.loadEquipements();
        },
        error: (error) => {
          this.errorMessage = this.buildErrorMessage('Suppression impossible.', error);
        }
      });
  }

  trackByEquipement(_index: number, item: Equipement): number {
    return item.idEquipement;
  }

  trackByDetecte(_index: number, item: EquipementDetecte): string {
    return `${item.adresseIP || item.ip || 'unknown'}-${item.nom || 'equipement'}`;
  }

  isAddingDetected(item: EquipementDetecte): boolean {
    return this.addingDetectedKey !== null && this.addingDetectedKey === this.detectedKey(item);
  }

  getEtatClass(etat: string): string {
    const value = (etat || '').toLowerCase();
    if (value.includes('actif')) {
      return 'etat-actif';
    }
    if (value.includes('maintenance')) {
      return 'etat-maintenance';
    }
    return 'etat-inactif';
  }

  getSelectedMetric(): Metrique | null {
    return this.latestMetric;
  }

  private buildErrorMessage(prefix: string, error: { status?: number; error?: unknown; message?: string }): string {
    const backendMessage = this.extractBackendMessage(error);

    if (error?.status === 401) {
      return backendMessage ? `${prefix} Session expiree (401). ${backendMessage}` : `${prefix} Session expiree (401).`;
    }
    if (error?.status === 403) {
      return backendMessage
        ? `${prefix} Acces refuse (403). ${backendMessage}`
        : `${prefix} Acces refuse (403). Verifiez les permissions SCAN/SCAN_ADD de votre compte.`;
    }
    if (error?.status === 404) {
      return backendMessage ? `${prefix} Endpoint introuvable (404). ${backendMessage}` : `${prefix} Endpoint introuvable (404).`;
    }
    return backendMessage ? `${prefix} ${backendMessage}` : `${prefix} Veuillez reessayer.`;
  }

  private isLoopbackRange(debut: string, fin: string): boolean {
    return this.isLoopbackIp(debut) || this.isLoopbackIp(fin);
  }

  private isLoopbackIp(ip: string): boolean {
    return ip.trim().startsWith('127.');
  }

  private extractBackendMessage(error: { error?: unknown; message?: string } | null | undefined): string {
    const payload = error?.error as { message?: unknown; error?: unknown; detail?: unknown } | undefined;

    const messageCandidates = [payload?.message, payload?.error, payload?.detail, error?.message];
    for (const candidate of messageCandidates) {
      if (typeof candidate === 'string' && candidate.trim()) {
        return candidate.trim();
      }
    }

    return '';
  }

  private detectedKey(item: EquipementDetecte): string {
    const ip = (item.adresseIP || item.ip || '').trim().toLowerCase();
    const nom = (item.nom || '').trim().toLowerCase();
    return `${ip}-${nom}`;
  }

  private filterDetectedEquipements(): void {
    if (!this.detectedEquipements.length || !this.equipements.length) {
      return;
    }

    const existingIps = new Set(
      this.equipements
        .map((equipement) => (equipement.adresseIP || '').trim().toLowerCase())
        .filter((ip) => !!ip)
    );

    this.detectedEquipements = this.detectedEquipements.filter((detected) => {
      const detectedIp = (detected.adresseIP || detected.ip || '').trim().toLowerCase();
      return !detectedIp || !existingIps.has(detectedIp);
    });
  }
}