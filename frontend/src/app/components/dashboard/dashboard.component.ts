import { Component, AfterViewInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Subscription, interval, startWith, switchMap, forkJoin, of, catchError } from 'rxjs';
import Chart from 'chart.js/auto';
import { AnomalieService } from '../../services/anomalie.service';
import { Anomalie } from '../../models/anomalie.model';
import zoomPlugin from 'chartjs-plugin-zoom';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

Chart.register(zoomPlugin);

interface Centre {
  idCentre: number;
  nom: string;
}

interface MetriqueLatest {
  idMetrique: number;
  cpu: number;
  ram: number;
  ramPct: number;
  usedGb: number;
  availableGb: number;
  disque: number;
  reseau: number;
  dateCollecte: string;
  idEquipement: number;
  nomEquipement: string;
}

interface MetriqueSeriePoint {
  dateCollecte: string;
  cpuMoyen: number;
  ramPctMoyen: number;
  reseauMoyen: number;
  diskPctMoyen: number;
  nombreEquipements: number;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export class DashboardComponent implements AfterViewInit, OnDestroy {
  @ViewChild('cpuChart') cpuChartRef!: ElementRef;
  @ViewChild('networkChart') networkChartRef!: ElementRef;
  @ViewChild('alertChart') alertChartRef!: ElementRef;

  private charts: Chart[] = [];
  private refreshSubscription?: Subscription;

  private metriquesApiUrl = '/api/metriques';
  private readonly HISTORIQUE_MINUTES = 2880;

  equipementsActifs = 0;
  cpuMoyen = 0;
  ramPctMoyen = 0;
  ramUtiliseeGb = 0;
  ramTotaleGb = 0;
  diskPctMoyen = 0;
  alertesActives = 0;
  alertesRecentes: Anomalie[] = [];
  uptimeGlobal: number | null = null;
  temperatureMoyenne: number | null = null;
  loadingDashboard = false;
  erreurHistorique: string | null = null;

  centres: Centre[] = [];
  centreSelectionne: number | null = null;

  readonly gaugeCircumference = 2 * Math.PI * 58;

  constructor(
    private http: HttpClient,
    private anomalieService: AnomalieService,
    public auth: AuthService
  ) {}

  ngAfterViewInit(): void {
    Chart.defaults.color = '#8aa4c8';
    Chart.defaults.borderColor = 'rgba(255,255,255,0.06)';
    Chart.defaults.font.family = "'Inter','Roboto',sans-serif";

    this.initCharts();
    this.loadCentres(); // NOUVEAU : charge selon le role

    this.refreshSubscription = interval(30000)
      .pipe(
        startWith(0),
        switchMap(() => {
          if (!this.centreSelectionne) return of(null);
          return this.fetchDashboardData().pipe(
            catchError(err => {
              console.error('[Dashboard] Erreur refresh auto:', err);
              this.loadingDashboard = false;
              return of(null);
            })
          );
        })
      )
      .subscribe();
  }

  ngOnDestroy(): void {
    this.refreshSubscription?.unsubscribe();
    this.charts.forEach(chart => chart.destroy());
    this.charts = [];
  }

  // ===== NOUVEAU : charge les centres selon le role =====
  loadCentres(): void {
    const url = this.auth.isAdmin() ? '/api/centres' : '/api/centres/mes-centres';

    this.http.get<Centre[]>(url).subscribe({
      next: (data) => {
        this.centres = data;
        console.log('[Dashboard] Centres charges:', this.centres);

        const saved = localStorage.getItem('selectedCentre');
        if (saved) {
          const savedId = Number(saved);
          const existe = this.centres.some(c => c.idCentre === savedId);
          if (existe) {
            this.centreSelectionne = savedId;
            this.fetchDashboardData().subscribe();
          } else {
            console.warn('[Dashboard] Centre sauvegarde invalide:', savedId);
            localStorage.removeItem('selectedCentre');
            this.centreSelectionne = null;
          }
        }
      },
      error: (err) => console.error('Erreur chargement centres', err)
    });
  }

  onCentreChange(value: any): void {
    let numericValue: number | null = null;
    if (value === null || value === undefined || value === '') {
      numericValue = null;
    } else if (typeof value === 'number') {
      numericValue = value;
    } else if (typeof value === 'string') {
      numericValue = Number(value);
    } else if (typeof value === 'object' && value !== null) {
      numericValue = value.idCentre ?? value.id ?? null;
    }
    if (numericValue !== null && isNaN(numericValue)) numericValue = null;

    this.centreSelectionne = numericValue;
    this.erreurHistorique = null;

    if (numericValue !== null) {
      const existe = this.centres.some(c => c.idCentre === numericValue);
      if (!existe) {
        console.error('[Dashboard] Centre non autorise:', numericValue);
        localStorage.removeItem('selectedCentre');
        return;
      }
      localStorage.setItem('selectedCentre', numericValue.toString());
      this.fetchDashboardData().subscribe({
        error: () => this.loadingDashboard = false
      });
    } else {
      localStorage.removeItem('selectedCentre');
    }
  }

  private fetchDashboardData() {
    this.loadingDashboard = true;
    this.erreurHistorique = null;

    const centreId = this.centreSelectionne;
    if (!centreId) {
      this.loadingDashboard = false;
      return of(null);
    }

    const paramsMetriques = new HttpParams().set('centreId', centreId.toString());
    const paramsHistorique = new HttpParams()
      .set('minutes', this.HISTORIQUE_MINUTES.toString())
      .set('centreId', centreId.toString());

    return forkJoin({
      metriques: this.http.get<MetriqueLatest[]>(this.metriquesApiUrl, { params: paramsMetriques }).pipe(
        catchError(err => { console.error('[Dashboard] Erreur metriques:', err.status); return of([]); })
      ),
      historique: this.http.get<MetriqueSeriePoint[]>(
        `${this.metriquesApiUrl}/historique`, { params: paramsHistorique }
      ).pipe(
        catchError((err: any) => {
          console.error('[Dashboard] Erreur historique:', err.status, err.url);
          if (err.status === 401) {
            this.erreurHistorique = 'Acces refuse a l historique (401). Verifiez vos droits cote backend.';
          } else {
            this.erreurHistorique = 'Erreur de chargement de l historique.';
          }
          return of([]);
        })
      ),
      anomalies: this.anomalieService.getAnomaliesNonTraitees(centreId).pipe(
        catchError(err => { console.error('[Dashboard] Erreur anomalies:', err); return of([]); })
      )
    }).pipe(
      switchMap(({ metriques, historique, anomalies }) => {
        this.applyMetriques(metriques ?? []);
        this.applyHistorique(historique ?? []);
        this.applyAnomalies(anomalies ?? []);
        this.loadingDashboard = false;
        return of(null);
      }),
      catchError(err => {
        console.error('[Dashboard] Erreur forkJoin:', err);
        this.loadingDashboard = false;
        return of(null);
      })
    );
  }

  refresh(): void {
    if (!this.centreSelectionne) return;
    this.erreurHistorique = null;
    this.fetchDashboardData().subscribe({ error: () => this.loadingDashboard = false });
  }

  private applyMetriques(data: MetriqueLatest[]): void {
    if (!data || data.length === 0) return;
    this.equipementsActifs = data.length;
    this.cpuMoyen = this.average(data.map(m => m.cpu ?? 0));
    this.ramPctMoyen = this.average(data.map(m => m.ramPct ?? 0));
    this.ramUtiliseeGb = this.average(data.map(m => m.usedGb ?? 0));
    this.ramTotaleGb = this.average(data.map(m => (m.usedGb ?? 0) + (m.availableGb ?? 0)));
    this.diskPctMoyen = this.average(data.map(m => m.disque ?? 0));
  }

  private applyHistorique(historique: MetriqueSeriePoint[]): void {
    if (!historique || historique.length === 0) return;
    this.erreurHistorique = null;
    const labels = historique.map(p => new Date(p.dateCollecte).toLocaleTimeString());
    const cpuChart = this.charts[0];
    if (cpuChart) {
      cpuChart.data.labels = labels;
      cpuChart.data.datasets[0].data = historique.map(p => p.cpuMoyen);
      cpuChart.update();
    }
    const netChart = this.charts[1];
    if (netChart) {
      netChart.data.labels = labels;
      netChart.data.datasets[0].data = historique.map(p => p.reseauMoyen);
      netChart.update();
    }
  }

  private applyAnomalies(data: Anomalie[]): void {
    this.alertesActives = data.length;
    this.alertesRecentes = data.slice(0, 5);
    this.updateAlertChart(data);
  }

  private average(values: number[]): number {
    if (!values.length) return 0;
    return values.reduce((sum, v) => sum + v, 0) / values.length;
  }

  getGaugeOffset(percent: number): number {
    const clamped = Math.max(0, Math.min(100, percent));
    return this.gaugeCircumference * (1 - clamped / 100);
  }

  initCharts(): void {
    const cpuCtx = this.cpuChartRef.nativeElement.getContext('2d');
    const cpuGradient = cpuCtx.createLinearGradient(0, 0, 0, 260);
    cpuGradient.addColorStop(0, 'rgba(59,130,246,0.3)');
    cpuGradient.addColorStop(1, 'rgba(59,130,246,0)');

    this.charts.push(new Chart(cpuCtx, {
      type: 'line',
      data: {
        labels: [],
        datasets: [{ label: 'CPU %', data: [], borderColor: '#3b82f6', backgroundColor: cpuGradient, fill: true, tension: 0.4, pointRadius: 3 }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: { y: { beginAtZero: true, max: 100 } }
      }
    }));

    const netCtx = this.networkChartRef.nativeElement.getContext('2d');
    this.charts.push(new Chart(netCtx, {
      type: 'line',
      data: {
        labels: [],
        datasets: [{ label: 'Reseau (Mo/s)', data: [], borderColor: '#22c55e', backgroundColor: 'rgba(34,197,94,0.1)', fill: true, tension: 0.4 }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { position: 'top', align: 'end', labels: { boxWidth: 10 } } },
        scales: { y: { beginAtZero: true } }
      }
    }));

    const alertCtx = this.alertChartRef.nativeElement.getContext('2d');
    this.charts.push(new Chart(alertCtx, {
      type: 'doughnut',
      data: {
        labels: ['CPU', 'RAM', 'DISK', 'NETWORK'],
        datasets: [{ data: [0, 0, 0, 0], backgroundColor: ['#3b82f6', '#22c55e', '#8b5cf6', '#f59e0b'], borderWidth: 0 }]
      },
      options: { responsive: true, maintainAspectRatio: false, cutout: '70%', plugins: { legend: { position: 'right', labels: { padding: 15 } } } }
    }));
  }

  private updateAlertChart(anomalies: Anomalie[]): void {
    const chart = this.charts[2];
    if (!chart) return;
    const counts: Record<string, number> = { CPU: 0, RAM: 0, DISK: 0, NETWORK: 0 };
    anomalies.forEach(a => {
      const type = (a.typeAnomalie || '').toUpperCase();
      if (counts[type] !== undefined) counts[type]++;
    });
    chart.data.datasets[0].data = [counts['CPU'], counts['RAM'], counts['DISK'], counts['NETWORK']];
    chart.update();
  }

  getNiveauClass(niveau: string): string {
    return (niveau || '').toUpperCase() === 'CRITIQUE' ? 'danger' : 'warning';
  }

  resetZoom(): void {
    this.charts.forEach(chart => chart.resetZoom());
  }

  getNiveauLabel(niveau: string): string {
    const value = (niveau || '').toUpperCase();
    if (value === 'CRITIQUE') return 'Critique';
    if (value === 'HAUTE') return 'Haute';
    if (value === 'MAJEUR') return 'Majeur';
    if (value === 'MINEUR') return 'Mineur';
    return niveau || 'Info';
  }

  getAlertIcon(typeAnomalie: string): string {
    const type = (typeAnomalie || '').toUpperCase();
    if (type === 'CPU') return 'fa-microchip';
    if (type === 'RAM') return 'fa-memory';
    if (type === 'DISK') return 'fa-hdd';
    if (type === 'NETWORK') return 'fa-network-wired';
    return 'fa-exclamation-triangle';
  }

  getTimeAgo(dateDetection: string): string {
    const date = new Date(dateDetection);
    const diffMs = Date.now() - date.getTime();
    const diffMin = Math.floor(diffMs / 60000);
    if (diffMin < 1) return "A l'instant";
    if (diffMin < 60) return `Il y a ${diffMin} minute${diffMin > 1 ? 's' : ''}`;
    const diffH = Math.floor(diffMin / 60);
    if (diffH < 24) return `Il y a ${diffH} heure${diffH > 1 ? 's' : ''}`;
    const diffJ = Math.floor(diffH / 24);
    return `Il y a ${diffJ} jour${diffJ > 1 ? 's' : ''}`;
  }
}