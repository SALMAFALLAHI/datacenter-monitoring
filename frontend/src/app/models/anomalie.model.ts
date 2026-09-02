
export interface Anomalie {
  idAnomalie: number;
  typeAnomalie: string;           // 'CPU' | 'RAM' | 'DISK' | 'NETWORK'
  niveau: string;                 // 'CRITIQUE' | 'HAUTE' | 'MAJEUR' | 'MINEUR'
  score: number;
  description: string;
  dateDetection: string;
  
  
  statut: 'NON_TRAITEE' | 'EN_COURS' | 'TRAITEE' | 'IGNOREE';
  dateStatut?: string;
  commentaireTraitement?: string;
  
  idMetrique?: number;
  idEquipement?: number;
  nomEquipement?: string;
  adresseIPEquipement?: string;
}

export interface AnomalieStats {
  nonTraitees: number;
  enCours: number;
  traitees: number;
  ignorees: number;
}