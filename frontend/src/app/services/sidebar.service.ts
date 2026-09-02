import { Injectable, effect, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class SidebarService {
  private readonly storageKey = 'nouvelair-sidebar-collapsed';
  private readonly collapsedState = signal(this.readInitialState());

  readonly collapsed = this.collapsedState.asReadonly();

  constructor() {
    effect(() => {
      const value = this.collapsedState();

      try {
        localStorage.setItem(this.storageKey, JSON.stringify(value));
      } catch {
        // Ignore storage failures.
      }
    });
  }

  toggle(): void {
    this.collapsedState.update((current) => !current);
  }

  setCollapsed(value: boolean): void {
    this.collapsedState.set(value);
  }

  private readInitialState(): boolean {
    try {
      return JSON.parse(localStorage.getItem(this.storageKey) ?? 'false') === true;
    } catch {
      return false;
    }
  }
}
