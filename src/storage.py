import json
from pathlib import Path


def load_data(path: str) -> dict[str, list[dict]]:
    return json.loads(Path(path).read_text(encoding="utf-8"))


def index_by(rows: list[dict], key: str) -> dict[str, dict]:
    return {row[key]: row for row in rows}


def save_plan(path: str, plan: dict, placements: list[dict]) -> None:
    text = json.dumps({"load_plan": plan, "placements": placements},
                      ensure_ascii=False, indent=2)
    Path(path).write_text(text, encoding="utf-8")
