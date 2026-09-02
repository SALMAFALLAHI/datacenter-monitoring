import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { AnomalieService, AnomalieGroupe, BatchDecisionRequest } from '../../services/anomalie.service';
import { AuthService } from '../../services/auth.service';

interface Centre {
  idCentre: number;
  nom: string;
}

@Component({
  selector: 'app-anomalie-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './anomalie-list.component.html',
  styleUrls: ['./anomalie-list.component.css']
})
export class AnomalieListComponent implements OnInit {
  groupes: (AnomalieGroupe & { selected?: boolean; expanded?: boolean })[] = [];
  loading = false;
  saving = false;

  centres: Centre[] = [];
  centreId: number | null = null;
  centreNom: string = '';
  statutFiltre: string = 'NON_TRAITEE';

  stats: Record<string, number> = {};

  showDecisionModal = false;
  decisionMode: 'single' | 'batch' = 'single';
  decisionTarget: AnomalieGroupe | null = null;
  selectedIds: number[] = [];
  nouveauStatut: string = 'TRAITEE';
  commentaire: string = '';

  readonly statuts = [
    { value: 'NON_TRAITEE', label: 'Non traitees', color: '#ef4444' },
    { value: 'EN_COURS',    label: 'En cours',     color: '#f59e0b' },
    { value: 'TRAITEE',     label: 'Traitees',     color: '#22c55e' },
    { value: 'IGNOREE',     label: 'Ignorees',     color: '#64748b' }
  ];

  get selectedCount(): number {
    return this.groupes.filter(g => g.selected).length;
  }

  get selectedTotalOccurrences(): number {
    return this.groupes.filter(g => g.selected).reduce((sum, g) => sum + g.nombreOccurrences, 0);
  }

  constructor(
    private anomalieService: AnomalieService,
    private http: HttpClient,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    this.loadCentres(); // NOUVEAU : charge d'abord les centres
  }

  // ===== NOUVEAU : charge les centres selon le role =====
  loadCentres(): void {
    const url = this.auth.isAdmin() ? '/api/centres' : '/api/centres/mes-centres';

    this.http.get<Centre[]>(url).subscribe({
      next: (data) => {
        this.centres = data;
        console.log('[Anomalies] Centres charges:', this.centres);

        const saved = localStorage.getItem('selectedCentre');
        const savedNom = localStorage.getItem('selectedCentreNom');

        if (saved && this.centres.some(c => c.idCentre === Number(saved))) {
          this.centreId = Number(saved);
          this.centreNom = savedNom || this.centres.find(c => c.idCentre === Number(saved))?.nom || '';
        } else if (this.centres.length > 0) {
          // Auto-select le premier centre si pas de sauvegarde
          this.centreId = this.centres[0].idCentre;
          this.centreNom = this.centres[0].nom;
          localStorage.setItem('selectedCentre', this.centreId.toString());
          localStorage.setItem('selectedCentreNom', this.centreNom);
        }

        if (this.centreId) {
          this.loadAnomalies();
          this.loadStats();
        }
      },
      error: (err) => console.error('[Anomalies] Erreur chargement centres:', err)
    });
  }

  // ===== NOUVEAU : changement de centre =====
  onCentreChange(): void {
    const centre = this.centres.find(c => c.idCentre === this.centreId);
    this.centreNom = centre?.nom || '';
    if (this.centreId) {
      localStorage.setItem('selectedCentre', this.centreId.toString());
      localStorage.setItem('selectedCentreNom', this.centreNom);
      this.loadAnomalies();
      this.loadStats();
    }
  }

  loadAnomalies(): void {
    if (!this.centreId) return;
    this.loading = true;

    this.anomalieService
      .getAnomalies(this.centreId, this.statutFiltre)
      .pipe(finalize(() => this.loading = false))
      .subscribe({
        next: (data) => {
          this.groupes = data.map(g => ({ ...g, selected: false, expanded: false }));
        },
        error: (err) => console.error('Erreur chargement anomalies', err)
      });
  }

  loadStats(): void {
    if (!this.centreId) return;
    this.anomalieService.getStats(this.centreId).subscribe({
      next: (data) => {
        const normalized: Record<string, number> = {};
        for (const [key, value] of Object.entries(data)) {
          const cleanKey = key.toUpperCase().replace(/-/g, '_').trim();
          normalized[cleanKey] = Number(value) || 0;
        }
        for (const s of this.statuts) {
          if (!(s.value in normalized)) normalized[s.value] = 0;
        }
        this.stats = normalized;
      },
      error: (err) => {
        console.error('Erreur stats', err);
        this.stats = { NON_TRAITEE: 0, EN_COURS: 0, TRAITEE: 0, IGNOREE: 0 };
      }
    });
  }

  onFiltreChange(): void {
    this.loadAnomalies();
  }

  toggleSelect(groupe: AnomalieGroupe & { selected?: boolean }): void {
    groupe.selected = !groupe.selected;
  }

  selectAll(): void {
    this.groupes.forEach(g => g.selected = true);
  }

  unselectAll(): void {
    this.groupes.forEach(g => g.selected = false);
  }

  openDecisionSingle(groupe: AnomalieGroupe, statut: string): void {
    this.decisionMode = 'single';
    this.decisionTarget = groupe;
    this.selectedIds = [...groupe.idsAnomalies];
    this.nouveauStatut = statut;
    this.commentaire = '';
    this.showDecisionModal = true;
  }

  openDecisionBatch(statut: string): void {
    const selected = this.groupes.filter(g => g.selected);
    if (selected.length === 0) return;

    this.decisionMode = 'batch';
    this.decisionTarget = null;
    this.selectedIds = selected.flatMap(g => g.idsAnomalies);
    this.nouveauStatut = statut;
    this.commentaire = '';
    this.showDecisionModal = true;
  }

  closeDecisionModal(): void {
    this.showDecisionModal = false;
    this.decisionTarget = null;
    this.selectedIds = [];
    this.commentaire = '';
  }

  submitDecision(): void {
    if (!this.commentaire.trim() || this.selectedIds.length === 0) return;

    this.saving = true;
    const ancienFiltre = this.statutFiltre;

    const request: BatchDecisionRequest = {
      idsAnomalies: this.selectedIds,
      nouveauStatut: this.nouveauStatut,
      commentaire: this.commentaire.trim()
    };

    this.anomalieService.traiterBatch(request)
      .pipe(finalize(() => this.saving = false))
      .subscribe({
        next: () => {
          this.closeDecisionModal();
          this.unselectAll();
          if (this.nouveauStatut !== ancienFiltre) {
            this.statutFiltre = this.nouveauStatut;
          }
          this.loadAnomalies();
          this.loadStats();
        },
        error: (err) => console.error('Erreur decision', err)
      });
  }

  getNiveauClass(niveau: string): string {
    const n = (niveau || '').toUpperCase();
    if (n === 'CRITIQUE') return 'critique';
    if (n === 'HAUTE' || n === 'MAJEUR') return 'haute';
    if (n === 'MOYENNE' || n === 'MEDIUM') return 'moyenne';
    if (n === 'MINEUR' || n === 'BASSE') return 'basse';
    return 'info';
  }

  getStatutClass(statut: string): string {
    const s = (statut || '').toUpperCase();
    if (s === 'NON_TRAITEE') return 'non-traitee';
    if (s === 'EN_COURS') return 'en-cours';
    if (s === 'TRAITEE') return 'traitee';
    if (s === 'IGNOREE') return 'ignoree';
    return '';
  }

  getTypeIcon(type: string): string {
    const t = (type || '').toUpperCase();
    if (t === 'CPU') return 'fa-microchip';
    if (t === 'RAM') return 'fa-memory';
    if (t === 'DISK') return 'fa-hdd';
    if (t === 'NETWORK') return 'fa-network-wired';
    return 'fa-exclamation-triangle';
  }

  getTimeAgo(date: string): string {
    const d = new Date(date);
    const diff = Date.now() - d.getTime();
    const min = Math.floor(diff / 60000);
    if (min < 1) return "A l'instant";
    if (min < 60) return `Il y a ${min} min`;
    const h = Math.floor(min / 60);
    if (h < 24) return `Il y a ${h}h`;
    const j = Math.floor(h / 24);
    return `Il y a ${j}j`;
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleString('fr-FR');
  }
}