import { Equipement } from './equipement';

export interface Metrique {
  idMetrique: number;
  cpu: number;
  ram: number;
  
  disque: number;
  reseau: number;
  temperature: number | null;
  dateCollecte: string;
  equipement: Equipement;
  status?: string;           // ← AJOUTÉ par le backend via /latest-with-status
}