export interface Centre {
  idCentre?: number;
  id?: number;
  nom: string;
  localisation?: string;      // plus optionnel (cloud n'en a pas)
  longitude?: number;
  latitude?: number;
  
  // ===== NOUVEAU =====
  type?: 'PHYSIQUE' | 'CLOUD';
  region?: string;
  fournisseur?: string;
  apiEndpoint?: string;
  // ===================
  
  basesDeDonnees?: Array<string | { nom?: string; name?: string; type?: string }>;
  basesDonnees?: Array<string | { nom?: string; name?: string; type?: string }>;
  databases?: Array<string | { nom?: string; name?: string; type?: string }>;
}