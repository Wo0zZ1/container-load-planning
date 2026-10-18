def check_containers(containers: list[dict],
                     types: dict[str, dict]) -> list[str]:
    problems = []
    for container in containers:
        number = container["number"]
        type_code = container["type_code"]
        container_type = types.get(type_code)
        mass = container.get("gross_mass")
        if container_type is None:
            problems.append(f"{number}: неизвестный типоразмер {type_code}")
        if not container.get("dest_station"):
            problems.append(f"{number}: не указана станция назначения")
        if not mass:
            problems.append(f"{number}: не указана масса брутто")
        elif container_type and mass > container_type["max_gross_mass"]:
            problems.append(f"{number}: масса брутто {mass} т больше "
                            f"допустимой для типоразмера {type_code}")
    return problems
