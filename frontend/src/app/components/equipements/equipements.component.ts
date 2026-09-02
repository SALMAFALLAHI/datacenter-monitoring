import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { CentreDonnees } from '../../models/centre-donnees';
import { Equipement } from '../../models/equipement';
import { CentreDonneesService } from '../../services/centre-donnees.service';
import { DecouverteService, EquipementDetecte } from '../../services/decouverte.service';
import {
  CreateEquipementPayload,
  EquipementService,
  UpdateEquipementPayload
} from '../../services/equipement.services';

@Component({
  selector: 'app-equipements',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './equipements.html',
  styleUrls: ['./equipements.css']
})
export class EquipementsComponent implements OnInit {
  private readonly ipPattern =
    /^(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d{2}|[1-9]?\d)){3}$/;

  centres: CentreDonnees[] = [];
  selectedCentre: CentreDonnees | null = null;
  equipements: Equipement[] = [];
  detectedEquipements: EquipementDetecte[] = [];

  loadingCentres = false;
  loadingEquipements = false;
  savingCentre = false;
  deletingCentreId: number | null = null;
  savingEquipement = false;
  deletingEquipementId: number | null = null;
  scanning = false;

  showCentreForm = false;
  editingCentreId: number | null = null;
  showEquipementForm = false;
  editingEquipementId: number | null = null;
  showScanForm = false;
  scanMode: 'preview' | 'scanAdd' = 'preview';

  message = '';
  errorMessage = '';

  centreForm;
  equipementForm;
  scanForm;

  constructor(
    private fb: FormBuilder,
    private centreService: CentreDonneesService,
    private equipementService: EquipementService,
    private decouverteService: DecouverteService
  ) {
    this.centreForm = this.fb.group({
      nom: ['', [Validators.required]],
      localisation: ['', [Validators.required]]
    });

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
    this.loadCentres();
  }

  loadCentres(): void {
    this.loadingCentres = true;
    this.errorMessage = '';

    const selectedId = this.selectedCentre ? this.getCentreId(this.selectedCentre) : undefined;

    this.centreService
      .getCentres()
      .pipe(finalize(() => (this.loadingCentres = false)))
      .subscribe({
        next: (data) => {
          this.centres = data ?? [];

          if (this.centres.length === 0) {
            this.selectedCentre = null;
            this.equipements = [];
            return;
          }

          const selected = selectedId
            ? this.centres.find((centre) => this.getCentreId(centre) === selectedId)
            : this.centres[0];

          this.selectCentre(selected ?? this.centres[0]);
        },
        error: (error) => {
          this.errorMessage = this.buildErrorMessage('Chargement des centres impossible.', error);
          this.centres = [];
          this.selectedCentre = null;
          this.equipements = [];
        }
      });
  }

  selectCentre(centre: CentreDonnees): void {
    this.selectedCentre = centre;
    this.detectedEquipements = [];
    this.showScanForm = false;
    this.showEquipementForm = false;
    this.loadEquipementsForCentre();
  }

  openCentreForm(): void {
    this.showCentreForm = true;
    this.editingCentreId = null;
    this.centreForm.reset({ nom: '', localisation: '' });
    this.message = '';
    this.errorMessage = '';
  }

  editCentre(centre: CentreDonnees): void {
    const centreId = this.getCentreId(centre);
    if (!centreId) {
      return;
    }

    this.showCentreForm = true;
    this.editingCentreId = centreId;
    this.centreForm.reset({
      nom: centre.nom,
      localisation: centre.localisation
    });
    this.message = '';
    this.errorMessage = '';
  }

  cancelCentreForm(): void {
    this.showCentreForm = false;
    this.editingCentreId = null;
  }

  submitCentre(): void {
    if (this.savingCentre || this.centreForm.invalid) {
      this.centreForm.markAllAsTouched();
      return;
    }

    this.savingCentre = true;
    this.message = '';
    this.errorMessage = '';

    const payload = this.centreForm.getRawValue() as { nom: string; localisation: string };
    const request$ = this.editingCentreId
      ? this.centreService.updateCentre(this.editingCentreId, payload)
      : this.centreService.createCentre(payload);

    request$.pipe(finalize(() => (this.savingCentre = false))).subscribe({
      next: () => {
        this.message = this.editingCentreId
          ? 'Centre modifie avec succes.'
          : 'Centre ajoute avec succes.';
        this.showCentreForm = false;
        this.editingCentreId = null;
        this.loadCentres();
      },
      error: (error) => {
        this.errorMessage = this.buildErrorMessage("L'enregistrement du centre a echoue.", error);
      }
    });
  }

  deleteCentre(centre: CentreDonnees): void {
    const centreId = this.getCentreId(centre);
    if (!centreId || !window.confirm('Supprimer ce centre ?')) {
      return;
    }

    this.deletingCentreId = centreId;
    this.message = '';
    this.errorMessage = '';

    this.centreService
      .deleteCentre(centreId)
      .pipe(finalize(() => (this.deletingCentreId = null)))
      .subscribe({
        next: () => {
          this.message = 'Centre supprime avec succes.';
          if (this.selectedCentre && this.getCentreId(this.selectedCentre) === centreId) {
            this.selectedCentre = null;
            this.equipements = [];
          }
          this.loadCentres();
        },
        error: (error) => {
          this.errorMessage = this.buildErrorMessage('La suppression du centre a echoue.', error);
        }
      });
  }

  loadEquipementsForCentre(): void {
    const centreId = this.selectedCentre ? this.getCentreId(this.selectedCentre) : undefined;
    if (!centreId) {
      this.equipements = [];
      return;
    }

    this.loadingEquipements = true;
    this.errorMessage = '';

    this.equipementService
      .getEquipementsByCentre(centreId)
      .pipe(finalize(() => (this.loadingEquipements = false)))
      .subscribe({
        next: (data) => {
          this.equipements = data ?? [];
        },
        error: (error) => {
          this.equipements = [];
          this.errorMessage = this.buildErrorMessage('Chargement des equipements impossible.', error);
        }
      });
  }

  openEquipementForm(): void {
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
    if (this.savingEquipement || this.equipementForm.invalid || !this.selectedCentre) {
      this.equipementForm.markAllAsTouched();
      return;
    }

    const centreId = this.getCentreId(this.selectedCentre);
    if (!centreId) {
      this.errorMessage = 'Centre selectionne invalide.';
      return;
    }

    this.savingEquipement = true;
    this.message = '';
    this.errorMessage = '';

    const formValue = this.equipementForm.getRawValue() as {
      nom: string;
      type: string;
      adresseIP: string;
      systeme: string;
      etat: string;
    };

    if (this.editingEquipementId) {
      const payload: UpdateEquipementPayload = {
        nom: formValue.nom,
        type: formValue.type,
        adresseIP: formValue.adresseIP,
        systeme: formValue.systeme,
        etat: formValue.etat
      };

      this.equipementService
        .updateEquipement(this.editingEquipementId, payload)
        .pipe(finalize(() => (this.savingEquipement = false)))
        .subscribe({
          next: () => {
            this.message = 'Equipement modifie avec succes.';
            this.showEquipementForm = false;
            this.editingEquipementId = null;
            this.loadEquipementsForCentre();
          },
          error: (error) => {
            this.errorMessage = this.buildErrorMessage("La modification de l'equipement a echoue.", error);
          }
        });
      return;
    }

    const createPayload: CreateEquipementPayload = {
      nom: formValue.nom,
      type: formValue.type,
      adresseIP: formValue.adresseIP,
      systeme: formValue.systeme,
      etat: formValue.etat,
      idCentre: centreId
    };

    this.equipementService
      .createEquipement(createPayload)
      .pipe(finalize(() => (this.savingEquipement = false)))
      .subscribe({
        next: () => {
          this.message = 'Equipement ajoute avec succes.';
          this.showEquipementForm = false;
          this.loadEquipementsForCentre();
        },
        error: (error) => {
          this.errorMessage = this.buildErrorMessage("L'ajout de l'equipement a echoue.", error);
        }
      });
  }

  deleteEquipement(item: Equipement): void {
    if (!window.confirm(`Supprimer l'equipement ${item.nom} ?`)) {
      return;
    }

    this.deletingEquipementId = item.idEquipement;
    this.message = '';
    this.errorMessage = '';

    this.equipementService
      .deleteEquipement(item.idEquipement)
      .pipe(finalize(() => (this.deletingEquipementId = null)))
      .subscribe({
        next: () => {
          this.message = 'Equipement supprime avec succes.';
          this.loadEquipementsForCentre();
        },
        error: (error) => {
          this.errorMessage = this.buildErrorMessage("La suppression de l'equipement a echoue.", error);
        }
      });
  }

  openScan(mode: 'preview' | 'scanAdd'): void {
    if (!this.selectedCentre) {
      return;
    }

    this.scanMode = mode;
    this.showScanForm = true;
    this.detectedEquipements = [];
    this.message = '';
    this.errorMessage = '';
  }

  runScan(): void {
    if (this.scanning || this.scanForm.invalid || !this.selectedCentre) {
      this.scanForm.markAllAsTouched();
      return;
    }

    const centreId = this.getCentreId(this.selectedCentre);
    if (!centreId) {
      this.errorMessage = 'Centre selectionne invalide.';
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
        .scannerEtAjouter(debut, fin, centreId)
        .pipe(finalize(() => (this.scanning = false)))
        .subscribe({
          next: () => {
            this.message = 'Scan et ajout termines.';
            this.showScanForm = false;
            this.detectedEquipements = [];
            this.loadEquipementsForCentre();
          },
          error: (error) => {
            this.errorMessage = this.buildErrorMessage('Le scan et ajout ont echoue.', error);
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
          this.message = `${this.detectedEquipements.length} equipement(s) detecte(s).`;
        },
        error: (error) => {
          this.detectedEquipements = [];
          this.errorMessage = this.buildErrorMessage('Le scan preview a echoue.', error);
        }
      });
  }

  addDetectedEquipement(item: EquipementDetecte): void {
    if (!this.selectedCentre || this.savingEquipement) {
      return;
    }

    const centreId = this.getCentreId(this.selectedCentre);
    if (!centreId) {
      this.errorMessage = 'Centre selectionne invalide.';
      return;
    }

    const payload: CreateEquipementPayload = {
      nom: item.nom || `Equipement-${item.adresseIP || item.ip || 'auto'}`,
      adresseIP: item.adresseIP || item.ip || '0.0.0.0',
      type: item.type || 'Inconnu',
      systeme: item.systeme || 'LINUX',
      etat: item.etat || 'Actif',
      idCentre: centreId
    };

    this.savingEquipement = true;
    this.equipementService
      .createEquipement(payload)
      .pipe(finalize(() => (this.savingEquipement = false)))
      .subscribe({
        next: () => {
          this.message = 'Equipement detecte ajoute.';
          this.loadEquipementsForCentre();
        },
        error: (error) => {
          this.errorMessage = this.buildErrorMessage("L'ajout de l'equipement detecte a echoue.", error);
        }
      });
  }

  trackByCentre(index: number, centre: CentreDonnees): number {
    return this.getCentreId(centre) ?? index;
  }

  trackByEquipement(_index: number, equipement: Equipement): number {
    return equipement.idEquipement;
  }

  trackByDetecte(_index: number, item: EquipementDetecte): string {
    return `${item.adresseIP || item.ip || 'unknown'}-${item.nom || 'equipement'}`;
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

  private getCentreId(centre: CentreDonnees): number | undefined {
    return centre.idCentre ?? centre.id;
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
    if (error?.status === 500) {
      return backendMessage ? `${prefix} Erreur serveur (500). ${backendMessage}` : `${prefix} Erreur serveur (500).`;
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
}
