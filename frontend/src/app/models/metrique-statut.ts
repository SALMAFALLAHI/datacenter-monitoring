import { Metrique } from './metrique';

export interface AnomalieInfo {
  type: string;
  niveau: string;
  description: string;
  dateDetection: string;
}

export interface MetriqueStatutResponse {
  metrique: Metrique;
  status: string;                    // "NORMAL" | "HAUTE" | "CRITIQUE"
  typesAnomalie: string[];           // ["RAM", "DISQUE", "CPU"...]
  anomalies: AnomalieInfo[];         // Détails de chaque anomalie
}