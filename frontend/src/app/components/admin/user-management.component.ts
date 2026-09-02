import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { UserManagementService } from '../../services/user-management.service';
import { AuthService } from '../../services/auth.service';
import { CreateUserRequest, DashboardModule, DASHBOARD_MODULES, Role, User } from '../../models/dashboard-access.model';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user-management.component.html',
  styleUrls: ['./user-management.component.scss']
})
export class UserManagementComponent implements OnInit {
  users: User[] = [];
  userForm: FormGroup;
  roles = Object.values(Role);
  modules = DASHBOARD_MODULES;
  selectedModules: DashboardModule[] = [];
  selectedCentreIds: number[] = [];
  loading = false;
  error: string | null = null;
  successMessage: string | null = null;
  showForm = false;

  availableCentres: { id: number; nom: string }[] = [];

  constructor(
    private fb: FormBuilder,
    private userService: UserManagementService,
    private authService: AuthService
  ) {
    this.userForm = this.fb.group({
      nom: ['', Validators.required],
      prenom: [''],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      role: [Role.OPERATEUR, Validators.required],
      actif: [true]
    });
  }

  ngOnInit(): void {
    this.loadUsers();
    this.loadCentres();
    this.onRoleChange();
  }

  loadUsers(): void {
    this.loading = true;
    this.userService.getAllUsers().subscribe({
      next: (users) => { this.users = users; this.loading = false; },
      error: () => { this.error = 'Erreur chargement utilisateurs'; this.loading = false; }
    });
  }

  loadCentres(): void {
    this.userService.getAvailableCentres().subscribe({
      next: (centres) => {
        console.log('Centres bruts depuis API:', centres);
        this.availableCentres = (centres as any[])
          .map(c => ({
            id: c.idCentre ?? c.id ?? 0,
            nom: c.nom ?? c.name ?? c.nomCentre ?? 'Sans nom'
          }))
          .filter(c => c.id !== 0 && c.nom && c.nom.trim() !== '');
        console.log('Centres filtres et mappes:', this.availableCentres);
      },
      error: (err) => {
        console.error('Erreur chargement centres:', err);
        this.availableCentres = [];
      }
    });
  }

  onRoleChange(): void {
    const role = this.userForm.get('role')?.value;
    if (role === Role.ADMIN) {
      this.selectedModules = this.modules.map(m => m.key);
    } else if (role === Role.OPERATEUR) {
      this.selectedModules = [DashboardModule.ANOMALIES, DashboardModule.EQUIPEMENTS, DashboardModule.METRIQUES];
    } else {
      this.selectedModules = [DashboardModule.ANOMALIES, DashboardModule.RAPPORTS];
    }
  }

  toggleModule(module: DashboardModule): void {
    if (this.userForm.get('role')?.value === Role.ADMIN) return;
    const index = this.selectedModules.indexOf(module);
    if (index > -1) {
      this.selectedModules.splice(index, 1);
    } else {
      this.selectedModules.push(module);
    }
  }

  isModuleSelected(module: DashboardModule): boolean {
    return this.selectedModules.includes(module);
  }

  toggleCentre(centreId: number): void {
    const index = this.selectedCentreIds.indexOf(centreId);
    if (index > -1) {
      this.selectedCentreIds.splice(index, 1);
    } else {
      this.selectedCentreIds.push(centreId);
    }
  }

  isCentreSelected(centreId: number): boolean {
    return this.selectedCentreIds.includes(centreId);
  }

  onSubmit(): void {
    console.log('=== SUBMIT ===');
    console.log('Form valid?', this.userForm.valid);
    console.log('Form value:', this.userForm.value);
    console.log('Selected modules:', this.selectedModules);
    console.log('Selected centres:', this.selectedCentreIds);

    if (this.userForm.invalid) {
      this.error = 'Veuillez corriger les erreurs du formulaire';
      return;
    }

    this.error = null;
    this.successMessage = null;
    this.loading = true;

    const request: CreateUserRequest = {
      ...this.userForm.value,
      dashboardAccess: this.selectedModules,
      centreIds: this.selectedCentreIds
    };

    console.log('Request envoye:', request);

    this.userService.createUser(request).subscribe({
      next: (user) => {
        console.log('User cree:', user);
        this.successMessage = `Utilisateur ${user.fullName} cree avec succes`;
        this.users.unshift(user);
        this.resetForm();
        this.showForm = false;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur creation:', err);
        this.loading = false;
        this.error = err.error?.message || 'Erreur lors de la creation';
      }
    });
  }

  toggleUserStatus(user: User): void {
    this.userService.toggleStatus(user.idAdmin).subscribe({
      next: () => { user.actif = !user.actif; },
      error: () => { this.error = 'Erreur changement statut'; }
    });
  }

  deleteUser(user: User): void {
    if (!confirm(`Supprimer ${user.fullName} ?`)) return;
    this.userService.deleteUser(user.idAdmin).subscribe({
      next: () => { this.users = this.users.filter(u => u.idAdmin !== user.idAdmin); },
      error: () => { this.error = 'Erreur suppression'; }
    });
  }

  resetForm(): void {
    this.userForm.reset({ role: Role.OPERATEUR, actif: true });
    this.selectedModules = [];
    this.selectedCentreIds = [];
    this.onRoleChange();
  }

  getModuleLabel(key: DashboardModule): string {
    return this.modules.find(m => m.key === key)?.label || key;
  }
}