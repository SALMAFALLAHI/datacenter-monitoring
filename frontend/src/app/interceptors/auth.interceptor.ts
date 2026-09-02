import { Injectable } from '@angular/core';
import {
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  constructor(private authService: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const isApiRequest = req.url.includes('/api/');
    const isPublicAuthRequest = req.url.includes('/api/auth/login');

    // ===== LOGS DE DÉBOGAGE =====
    console.log('[AuthInterceptor] URL:', req.url);
    console.log('[AuthInterceptor] isApiRequest:', isApiRequest);
    console.log('[AuthInterceptor] token:', this.authService.token ? 'PRÉSENT' : 'ABSENT');
    // ==============================

    if (!isApiRequest) {
      console.log('[AuthInterceptor] Ignoré (pas /api/)');
      return next.handle(req);
    }

    const token = this.authService.token;
    let authReq = req;

    if (token && !isPublicAuthRequest) {
      authReq = req.clone({
        setHeaders: {
          Authorization: `Bearer ${token}`
        }
      });
      console.log('[AuthInterceptor] Header Authorization ajouté');
    } else {
      console.log('[AuthInterceptor] PAS de header ajouté');
    }

    return next.handle(authReq).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401 || error.status === 403) {
          console.warn(`[AuthInterceptor] ${error.status} sur`, req.url);
        }
        return throwError(() => error);
      })
    );
  }
}