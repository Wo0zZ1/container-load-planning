from constants import (HEAVY_MASS_T, LONG_CONTAINER_FT, LONG_PLATFORM_M,
                       NORM_PERCENT, SHORT_PLATFORM_M)


def container_teu(length_ft: int, gross_mass: float,
                  platform_length_m: float) -> float:
    if length_ft == LONG_CONTAINER_FT:
        return 2
    if gross_mass <= HEAVY_MASS_T:
        return 1
    # Тяжелый 20-футовый контейнер занимает грузоподъемность
    # за полтора или два места
    if platform_length_m > LONG_PLATFORM_M:
        return 2
    if platform_length_m >= SHORT_PLATFORM_M:
        return 1.5
    return 1


def calc_train_teu(loads: list[dict], types: dict[str, dict],
                   models: dict[str, dict]) -> dict:
    loaded = 0
    capacity = 0
    for load in loads:
        model = models[load["model_code"]]
        platform_teu = 0.0
        for container in load["containers"]:
            length = types[container["type_code"]]["length_ft"]
            platform_teu += container_teu(length, container["gross_mass"],
                                          model["length_m"])
        load["teu"] = min(platform_teu, model["max_teu"])
        loaded += load["teu"]
        capacity += model["max_teu"]
    fill = loaded / capacity * 100
    lack = max(0, capacity * NORM_PERCENT / 100 - loaded)
    return {"loaded_teu": loaded, "capacity_teu": capacity,
            "fill_percent": fill, "norm_met": fill >= NORM_PERCENT,
            "lack_teu": lack}
