import { Component, AfterViewInit, OnDestroy, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Subscription, interval, startWith, switchMap, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import Chart from 'chart.js/auto';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';

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

interface Anomalie {
  idAnomalie: number;
  typeAnomalie: string;
  niveau: string;
  dateDetection: string;
  idEquipement: number;
  nomEquipement: string;
}

@Component({
  selector: 'app-metriques',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './metriques.html',
  styleUrls: ['./metriques.css']
})
export class MetriquesComponent implements AfterViewInit, OnDestroy {

  @ViewChild('serverCpuChart') serverCpuChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('latencyChart') latencyChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('storageChart') storageChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('incidentChart') incidentChartRef!: ElementRef<HTMLCanvasElement>;

  private charts: Chart[] = [];
  private refreshSubscription?: Subscription;

  private apiUrl = 'http://localhost:8080/api/metriques';
  private anomaliesUrl = 'http://localhost:8080/api/anomalies';

  private readonly HISTORIQUE_MINUTES = 2880;

  // ===== NOUVEAU : centres et sélection =====
  centres: Centre[] = [];
  centreSelectionne: number | null = null;

  constructor(
    private http: HttpClient,
    public auth: AuthService
  ) {}

  ngAfterViewInit(): void {
    Chart.defaults.color = '#8aa4c8';
    Chart.defaults.borderColor = 'rgba(255,255,255,0.06)';
    Chart.defaults.font.family = "'Inter','Roboto',sans-serif";

    this.initCharts();
    this.loadCentres(); // NOUVEAU
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
        console.log('[Metriques] Centres charges:', this.centres);

        const saved = localStorage.getItem('selectedCentre');
        if (saved) {
          const savedId = Number(saved);
          if (this.centres.some(c => c.idCentre === savedId)) {
            this.centreSelectionne = savedId;
            this.startRefresh();
          }
        } else if (this.centres.length === 1) {
          // Auto-select si un seul centre
          this.centreSelectionne = this.centres[0].idCentre;
          this.startRefresh();
        }
      },
      error: (err) => console.error('[Metriques] Erreur chargement centres:', err)
    });
  }

  // ===== NOUVEAU : changement de centre =====
  onCentreChange(): void {
    if (this.centreSelectionne) {
      localStorage.setItem('selectedCentre', this.centreSelectionne.toString());
      this.refreshSubscription?.unsubscribe();
      this.startRefresh();
    } else {
      localStorage.removeItem('selectedCentre');
      this.refreshSubscription?.unsubscribe();
    }
  }

  startRefresh(): void {
    if (!this.centreSelectionne) return;

    this.refreshSubscription = interval(30000)
      .pipe(
        startWith(0),
        switchMap(() => {
          const params = new HttpParams().set('centreId', this.centreSelectionne!.toString());
          const histParams = new HttpParams()
            .set('minutes', this.HISTORIQUE_MINUTES.toString())
            .set('centreId', this.centreSelectionne!.toString());

          return forkJoin({
            latest: this.http.get<MetriqueLatest[]>(this.apiUrl, { params }).pipe(
              catchError(err => { console.error('[Metriques] Erreur latest:', err); return of([] as MetriqueLatest[]); })
            ),
            historique: this.http.get<MetriqueSeriePoint[]>(
              `${this.apiUrl}/historique`, { params: histParams }
            ).pipe(
              catchError(err => { console.error('[Metriques] Erreur historique:', err); return of([] as MetriqueSeriePoint[]); })
            ),
            anomalies: this.http.get<Anomalie[]>(
              `${this.anomaliesUrl}?centreId=${this.centreSelectionne}`
            ).pipe(
              catchError(err => { console.error('[Metriques] Erreur anomalies:', err); return of([] as Anomalie[]); })
            )
          });
        })
      )
      .subscribe({
        next: ({ latest, historique, anomalies }) => {
          console.log('Metriques recues :', { latest, historique, anomalies });
          this.updateSnapshotCharts(latest);
          this.updateHistoriqueCharts(historique, anomalies);
        },
        error: (error) => console.error('Erreur globale:', error)
      });
  }

  initCharts(): void {
    const sCpuCtx = this.serverCpuChartRef.nativeElement.getContext('2d');
    if (sCpuCtx) {
      this.charts.push(new Chart(sCpuCtx, {
        type: 'bar',
        data: {
          labels: [],
          datasets: [
            { label: 'CPU %', data: [], backgroundColor: '#3b82f6', borderRadius: 6 },
            { label: 'Memoire %', data: [], backgroundColor: '#22c55e', borderRadius: 6 }
          ]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { position: 'top', align: 'end' } },
          scales: { y: { beginAtZero: true, max: 100 } }
        }
      }));
    }

    const latCtx = this.latencyChartRef.nativeElement.getContext('2d');
    if (latCtx) {
      this.charts.push(new Chart(latCtx, {
        type: 'line',
        data: {
          labels: [],
          datasets: [{
            label: 'Reseau (Mo/s)',
            data: [],
            borderColor: '#f59e0b',
            backgroundColor: 'rgba(245,158,11,0.1)',
            fill: true,
            tension: 0.4,
            pointRadius: 3
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false },
            zoom: {
              pan: { enabled: true, mode: 'x', threshold: 5 },
              zoom: { wheel: { enabled: true }, pinch: { enabled: true }, drag: { enabled: false }, mode: 'x' }
            }
          },
          scales: { y: { beginAtZero: true } }
        }
      }));
    }

    const storCtx = this.storageChartRef.nativeElement.getContext('2d');
    if (storCtx) {
      this.charts.push(new Chart(storCtx, {
        type: 'bar',
        data: {
          labels: [],
          datasets: [{ label: 'Utilise (GB)', data: [], backgroundColor: '#8b5cf6', borderRadius: 6 }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: { legend: { display: false } },
          scales: { y: { beginAtZero: true } }
        }
      }));
    }

    const incCtx = this.incidentChartRef.nativeElement.getContext('2d');
    if (incCtx) {
      this.charts.push(new Chart(incCtx, {
        type: 'line',
        data: {
          labels: [],
          datasets: [{
            label: 'Incidents',
            data: [],
            borderColor: '#ef4444',
            backgroundColor: 'rgba(239,68,68,0.1)',
            fill: true,
            tension: 0.3,
            pointRadius: 3
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { display: false },
            zoom: {
              pan: { enabled: true, mode: 'x', threshold: 5 },
              zoom: { wheel: { enabled: true }, pinch: { enabled: true }, drag: { enabled: false }, mode: 'x' }
            }
          },
          scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } }
        }
      }));
    }
  }

  private updateSnapshotCharts(data: MetriqueLatest[]): void {
    if (!data || data.length === 0) return;

    const cpuChart = this.charts[0];
    if (cpuChart) {
      cpuChart.data.labels = data.map(item => item.nomEquipement);
      cpuChart.data.datasets[0].data = data.map(item => item.cpu);
      cpuChart.data.datasets[1].data = data.map(item => item.ramPct);
      cpuChart.update();
    }

    const storageChart = this.charts[2];
    if (storageChart) {
      storageChart.data.labels = data.map(item => item.nomEquipement);
      storageChart.data.datasets[0].data = data.map(item => item.usedGb);
      storageChart.update();
    }
  }

  private updateHistoriqueCharts(historique: MetriqueSeriePoint[], anomalies: Anomalie[]): void {
    if (!historique || historique.length === 0) return;

    const labels = historique.map(p => new Date(p.dateCollecte).toLocaleTimeString());

    const latencyChart = this.charts[1];
    if (latencyChart) {
      latencyChart.data.labels = labels;
      latencyChart.data.datasets[0].data = historique.map(p => p.reseauMoyen);
      latencyChart.update();
    }

    const incidentChart = this.charts[3];
    if (incidentChart) {
      const counts = this.countAnomaliesPerBucket(historique, anomalies);
      incidentChart.data.labels = labels;
      incidentChart.data.datasets[0].data = counts;
      incidentChart.update();
    }
  }

  private countAnomaliesPerBucket(historique: MetriqueSeriePoint[], anomalies: Anomalie[]): number[] {
    return historique.map((point, index) => {
      const bucketEnd = new Date(point.dateCollecte).getTime();
      const bucketStart = index > 0 ? new Date(historique[index - 1].dateCollecte).getTime() : -Infinity;
      return anomalies.filter(a => {
        const t = new Date(a.dateDetection).getTime();
        return t > bucketStart && t <= bucketEnd;
      }).length;
    });
  }
}