import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AuthService } from '../../services/auth.service';
import { SidebarService } from '../../services/sidebar.service';
@Component({
	selector: 'app-sidebar',
	standalone: true,
	imports: [CommonModule, RouterLink, RouterLinkActive, MatIconModule, MatListModule, MatTooltipModule],
	templateUrl: './sidebar.component.html',
	styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {
	readonly navItems = [
		{ label: 'Dashboard', icon: 'dashboard', link: '/dashboard' },
		{ label: 'Centres', icon: 'devices', link: '/centres' },
		{ label: 'Métriques', icon: 'timeline', link: '/metriques' },
		{ label: 'Anomalies', icon: 'warning', link: '/anomalies' }
	];

	constructor(
		private readonly sidebarService: SidebarService,
		public readonly auth: AuthService,
		private readonly router: Router
	) {}

	get collapsed() {
		return this.sidebarService.collapsed;
	}

	toggleSidebar(): void {
		this.sidebarService.toggle();
	}

	logout(): void {
		this.auth.logout();
		this.router.navigate(['/login']);
	}

	tooltipFor(label: string): string {
		return this.collapsed() ? label : '';
	}
}
