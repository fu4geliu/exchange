from fastapi import FastAPI

app = FastAPI(title="market-service")


@app.get("/market/health")
def health():
    return {"success": True, "message": "market-service ok"}
