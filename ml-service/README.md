# RAM Stress Detector - Microservice Python

## Démarrage rapide

### Option 1: Python direct
```bash
cd ram-ml-service
pip install -r requirements.txt
python main.py
```
API disponible sur http://localhost:8000

### Option 2: Docker
```bash
cd ram-ml-service
docker-compose up --build -d
```

## Endpoints

| Endpoint | Méthode | Description |
|----------|---------|-------------|
| `/health` | GET | Vérifie que le service tourne |
| `/predict` | POST | Envoyer une mesure RAM, retourne alerte |
| `/predict/batch` | POST | Envoyer plusieurs mesures |
| `/reset/{machine_id}` | POST | Vider l'historique d'une machine |

## Exemple requête
```bash
curl -X POST http://localhost:8000/predict \
  -H "Content-Type: application/json" \
  -d '{
    "machine_id": "srv-01",
    "ram_pct": 65.0,
    "used_gb": 2.45,
    "available_gb": 1.35,
    "swap_pct": 35.0
  }'
```

## Réponse
```json
{
  "machine_id": "srv-01",
  "proba": 0.8234,
  "is_stress": 1,
  "alert_level": "STRESS",
  "ram_pct": 65.0,
  "swap_pct": 35.0,
  "message": "Détection ML (seuil 0.55)",
  "timestamp": "2026-08-12T19:00:00"
}
```

## Niveaux d'alerte
- 🔴 **CRITIQUE** : RAM > 90% ou Swap > 60% (règle métier)
- 🟠 **HAUTE** : RAM > 80% ou Swap > 50% (règle métier)
- 🟡 **STRESS** : ML détecte stress (proba >= 0.55)
- 🔵 **SUSPECT** : Proba >= 0.30
- 🟢 **NORMAL** : Tout va bien
- ⚪ **INIT** : Pas assez d'historique (besoin de 2 points)
