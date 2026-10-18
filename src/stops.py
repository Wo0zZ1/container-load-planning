from collections import Counter


def find_stop_changes(loads: list[dict], platforms: dict[str, dict],
                      types: dict[str, dict]) -> list[dict]:
    changes = []
    for load in loads:
        if not load["containers"]:
            continue
        lengths = sorted((types[container["type_code"]]["length_ft"]
                          for container in load["containers"]), reverse=True)
        current = platforms[load["platform_number"]]["stops_layout"]
        slots = Counter(int(length) for length in current.split("+"))
        # Упоры не переставляют, если под каждый контейнер уже есть
        # позиция его длины
        if Counter(lengths) - slots:
            changes.append({"platform_number": load["platform_number"],
                            "current": current,
                            "required": "+".join(map(str, lengths))})
    return changes
