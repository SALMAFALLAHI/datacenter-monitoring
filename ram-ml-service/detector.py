"""
Détecteur de stress RAM - Sans data leakage
Maintient l'historique par machine_id pour calculer les features.
"""
import pickle
import numpy as np
import pandas as pd
from collections import defaultdict
from datetime import datetime
from typing import Dict, List, Optional

# Charger le modèle
with open("model.pkl", "rb") as f:
    MODEL_DATA = pickle.load(f)

MODEL = MODEL_DATA["model"]
SCALER = MODEL_DATA["scaler"]
FEATURE_COLS = MODEL_DATA["feature_cols"]
THRESHOLD = MODEL_DATA["threshold"]

# Historique par machine: liste de dicts {"timestamp", "ram_pct", "used_gb", "available_gb", "swap_pct"}
HISTORY: Dict[str, List[dict]] = defaultdict(list)
MAX_HISTORY = 20  # garder les 20 derniers points


def _compute_features(history: List[dict]) -> pd.DataFrame:
    """Calcule les features sur l'historique passé (pas de fuite)."""
    df = pd.DataFrame(history)
    df = df.sort_values("timestamp").reset_index(drop=True)

    # Features de base
    df["used_to_available_ratio"] = df["used_gb"] / (df["available_gb"] + 0.001)
    df["ram_pct_diff"] = df["ram_pct"].diff()
    df["ram_pct_diff_abs"] = df["ram_pct_diff"].abs()
    df["swap_pct_diff"] = df["swap_pct"].diff()
    df["swap_pct_diff_abs"] = df["swap_pct_diff"].abs()

    # Rolling passé uniquement (shift 1)
    for w in [3, 5, 10]:
        df[f"ram_pct_roll_mean_{w}"] = df["ram_pct"].shift(1).rolling(window=w, min_periods=1).mean()
        df[f"ram_pct_roll_std_{w}"] = df["ram_pct"].shift(1).rolling(window=w, min_periods=1).std()
        df[f"ram_pct_roll_max_{w}"] = df["ram_pct"].shift(1).rolling(window=w, min_periods=1).max()
        df[f"ram_pct_roll_min_{w}"] = df["ram_pct"].shift(1).rolling(window=w, min_periods=1).min()
        df[f"swap_pct_roll_mean_{w}"] = df["swap_pct"].shift(1).rolling(window=w, min_periods=1).mean()
        df[f"swap_pct_roll_std_{w}"] = df["swap_pct"].shift(1).rolling(window=w, min_periods=1).std()

    df["ram_pct_lag1"] = df["ram_pct"].shift(1)
    df["ram_pct_lag2"] = df["ram_pct"].shift(2)
    df["swap_pct_lag1"] = df["swap_pct"].shift(1)

    df["ram_pct_zscore_10"] = (df["ram_pct"] - df["ram_pct_roll_mean_10"]) / (df["ram_pct_roll_std_10"] + 0.001)

    # Dernier point uniquement
    last = df.iloc[[-1]].copy()
    return last[FEATURE_COLS]


def predict(machine_id: str, ram_pct: float, used_gb: float,
            available_gb: float, swap_pct: float) -> dict:
    """
    Ajoute une mesure et prédit.
    Retourne INIT si pas assez d'historique (< 2 points).
    """
    now = datetime.utcnow()
    entry = {
        "timestamp": now,
        "ram_pct": ram_pct,
        "used_gb": used_gb,
        "available_gb": available_gb,
        "swap_pct": swap_pct,
    }
    HISTORY[machine_id].append(entry)
    if len(HISTORY[machine_id]) > MAX_HISTORY:
        HISTORY[machine_id].pop(0)

    # Règles métier prioritaires
    if ram_pct > 90 or swap_pct > 60:
        return {
            "machine_id": machine_id,
            "proba": 1.0,
            "is_stress": 1,
            "alert_level": "CRITIQUE",
            "ram_pct": ram_pct,
            "swap_pct": swap_pct,
            "message": "RAM ou swap critique (règle métier)",
            "timestamp": now.isoformat(),
        }
    if ram_pct > 80 or swap_pct > 50:
        return {
            "machine_id": machine_id,
            "proba": 0.95,
            "is_stress": 1,
            "alert_level": "HAUTE",
            "ram_pct": ram_pct,
            "swap_pct": swap_pct,
            "message": "RAM ou swap élevé (règle métier)",
            "timestamp": now.isoformat(),
        }

    # Besoin d'au moins 2 points pour les diff
    if len(HISTORY[machine_id]) < 2:
        return {
            "machine_id": machine_id,
            "proba": 0.0,
            "is_stress": 0,
            "alert_level": "INIT",
            "ram_pct": ram_pct,
            "swap_pct": swap_pct,
            "message": "Collecte d'historique en cours...",
            "timestamp": now.isoformat(),
        }

    features_df = _compute_features(HISTORY[machine_id])
    features = SCALER.transform(features_df.values)
    proba = float(MODEL.predict_proba(features)[0][1])
    is_stress = 1 if proba >= THRESHOLD else 0

    if proba >= THRESHOLD:
        alert_level = "STRESS"
    elif proba >= 0.30:
        alert_level = "SUSPECT"
    else:
        alert_level = "NORMAL"

    return {
        "machine_id": machine_id,
        "proba": round(proba, 4),
        "is_stress": is_stress,
        "alert_level": alert_level,
        "ram_pct": ram_pct,
        "swap_pct": swap_pct,
        "message": f"Détection ML (seuil {THRESHOLD})",
        "timestamp": now.isoformat(),
    }


def reset(machine_id: str):
    """Vide l'historique d'une machine."""
    if machine_id in HISTORY:
        HISTORY[machine_id] = []
    return {"status": "reset", "machine_id": machine_id}
