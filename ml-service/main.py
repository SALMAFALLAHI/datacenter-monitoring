from fastapi import FastAPI
from pydantic import BaseModel
from detector import predict, reset
import uvicorn

app = FastAPI(title="RAM Stress Detector API", version="1.0")

class Measurement(BaseModel):
    machine_id: str
    ram_pct: float
    used_gb: float
    available_gb: float
    swap_pct: float

@app.get("/health")
def health():
    return {"status": "ok", "model": "ram-stress-v1.0-conservateur"}

@app.post("/predict")
def predict_endpoint(m: Measurement):
    return predict(
        machine_id=m.machine_id,
        ram_pct=m.ram_pct,
        used_gb=m.used_gb,
        available_gb=m.available_gb,
        swap_pct=m.swap_pct,
    )

@app.post("/predict/batch")
def predict_batch_endpoint(measurements: list[Measurement]):
    return [predict(
        machine_id=m.machine_id,
        ram_pct=m.ram_pct,
        used_gb=m.used_gb,
        available_gb=m.available_gb,
        swap_pct=m.swap_pct,
    ) for m in measurements]

@app.post("/reset/{machine_id}")
def reset_endpoint(machine_id: str):
    return reset(machine_id)

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
