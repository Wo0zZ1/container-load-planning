def queue_order(container: dict) -> tuple[str, float]:
    # Контейнеры одной станции встают рядом, а тяжелые раньше легких
    # занимают свободную грузоподъемность
    return container["dest_station"], -container["gross_mass"]


def distribute_containers(
        containers: list[dict], platforms: list[dict],
        types: dict[str, dict], models: dict[str, dict],
) -> tuple[list[dict], list[dict]]:
    queue = sorted(containers, key=queue_order)
    loads = [{"platform_number": platform["number"],
              "model_code": platform["model_code"],
              "used_ft": 0, "mass": 0, "containers": []}
             for platform in platforms]
    rejected = []
    for container in queue:
        length = types[container["type_code"]]["length_ft"]
        mass = container["gross_mass"]
        for load in loads:
            model = models[load["model_code"]]
            if load["used_ft"] + length > model["length_ft"]:
                continue
            if load["mass"] + mass > model["load_capacity"]:
                continue
            load["used_ft"] += length
            load["mass"] += mass
            load["containers"].append(container)
            break
        else:
            rejected.append(container)
    return loads, rejected
