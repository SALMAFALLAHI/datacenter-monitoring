import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { DashboardModule } from '../models/dashboard-access.model';

@Injectable({ providedIn: 'root' })
export class DashboardAccessGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {}

  canActivate(route: any): boolean | UrlTree {
    const requiredModule = route.data?.['requiredModule'] as DashboardModule;

    if (!this.authService.isAuthenticated) {
      return this.router.createUrlTree(['/login']);
    }

    if (!requiredModule) return true;

    if (this.authService.hasAccess(requiredModule)) {
      return true;
    }

    return this.router.createUrlTree(['/acces-refuse']);
  }
}