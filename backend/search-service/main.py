from fastapi import FastAPI

app = FastAPI(title="PetRadar Search Service")


@app.get("/health")
def health():
    return {"status": "UP"}


@app.get("/search")
def search(q: str):
    # TODO: RAG 기반 검색 구현
    return {"query": q, "results": []}
