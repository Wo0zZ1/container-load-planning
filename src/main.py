import sys
from datetime import datetime

from checks import check_containers
from output import print_plan, print_problems
from planner import distribute_containers
from stops import find_stop_changes
from storage import index_by, load_data, save_plan
from teu import calc_train_teu

USAGE = "usage: python main.py <data.json> <train_number> [plan.json]"


def train_platforms(data: dict[str, list[dict]], train_number: str,
                    platforms: dict[str, dict]) -> list[dict]:
    composition = [row for row in data["train_platforms"]
                   if row["train_number"] == train_number]
    composition.sort(key=lambda row: row["order_no"])
    return [platforms[row["platform_number"]] for row in composition]


def make_plan(train: dict, loads: list[dict]) -> tuple[dict, list[dict]]:
    created_at = datetime.now()
    plan = {"id": f"{train['number']}-{created_at:%Y%m%d%H%M}",
            "train_number": train["number"],
            "created_at": created_at.isoformat(timespec="minutes")}
    placements = [{"plan_id": plan["id"],
                   "container_number": container["number"],
                   "platform_number": load["platform_number"],
                   "slot_no": slot_no}
                  for load in loads
                  for slot_no, container in enumerate(load["containers"],
                                                      start=1)]
    return plan, placements


def main(args: list[str]) -> None:
    if len(args) not in (2, 3):
        sys.exit(USAGE)
    data = load_data(args[0])
    train = index_by(data["trains"], "number").get(args[1])
    if train is None:
        sys.exit(f"Поезд {args[1]} не найден в {args[0]}")
    types = index_by(data["container_types"], "code")
    models = index_by(data["platform_models"], "code")
    platforms = index_by(data["platforms"], "number")
    containers = [container for container in data["containers"]
                  if container["request_id"] == train["request_id"]]
    composition = train_platforms(data, train["number"], platforms)
    if not composition:
        sys.exit(f"В составе поезда {train['number']} нет платформ")

    problems = check_containers(containers, types)
    if problems:
        print_problems(problems)
        sys.exit(1)

    loads, rejected = distribute_containers(containers, composition,
                                            types, models)
    teu = calc_train_teu(loads, types, models)
    changes = find_stop_changes(loads, platforms, types)
    print_plan(train, loads, rejected, teu, changes)

    if len(args) == 3:
        save_plan(args[2], *make_plan(train, loads))
        print(f"План сохранен в {args[2]}")


if __name__ == "__main__":
    main(sys.argv[1:])
