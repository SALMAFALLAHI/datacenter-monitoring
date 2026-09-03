import requests

URL = "http://localhost:8000"

def test():
    # Health
    print("Health:", requests.get(f"{URL}/health").json())

    # Envoyer 5 mesures normales
    for i in range(5):
        r = requests.post(f"{URL}/predict", json={
            "machine_id": "srv-test",
            "ram_pct": 30.0 + i*2,
            "used_gb": 1.1 + i*0.05,
            "available_gb": 2.6 - i*0.05,
            "swap_pct": 5.0 + i
        })
        print(f"Normal {i+1}:", r.json()["alert_level"], "proba:", r.json()["proba"])

    # Envoyer mesure stress
    r = requests.post(f"{URL}/predict", json={
        "machine_id": "srv-test",
        "ram_pct": 85.0,
        "used_gb": 3.2,
        "available_gb": 0.5,
        "swap_pct": 55.0
    })
    print("Stress:", r.json()["alert_level"], "proba:", r.json()["proba"])

    # Reset
    print("Reset:", requests.post(f"{URL}/reset/srv-test").json())

if __name__ == "__main__":
    test()
