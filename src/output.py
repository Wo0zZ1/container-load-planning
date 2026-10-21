from constants import NORM_PERCENT


def number(value: float) -> str:
    return f"{value:g}".replace(".", ",")


def describe(container: dict) -> str:
    return (f"{container['number']} {container['type_code']}, "
            f"{number(container['gross_mass'])} т, "
            f"{container['dest_station']}")


def print_problems(problems: list[str]) -> None:
    print("В данных контейнеров есть ошибки, план не сформирован:")
    for problem in problems:
        print(f"  {problem}")


def print_plan(train: dict, loads: list[dict], rejected: list[dict],
               teu: dict, changes: list[dict]) -> None:
    print(f"План погрузки поезда {train['number']}, "
          f"отправление {train['departure_date']}")
    print()
    for order_no, load in enumerate(loads, start=1):
        print(f"{order_no}. Платформа {load['platform_number']} "
              f"({load['model_code']}): масса {number(load['mass'])} т, "
              f"{number(load['teu'])} ДФЭ")
        for slot_no, container in enumerate(load["containers"], start=1):
            print(f"   {slot_no}) {describe(container)}")
    print()
    fill = f"{teu['fill_percent']:.1f}".replace(".", ",")
    print(f"Загружено {number(teu['loaded_teu'])} ДФЭ "
          f"из {number(teu['capacity_teu'])}, заполнение {fill}%")
    if teu["norm_met"]:
        print(f"Норма {NORM_PERCENT}% выполнена")
    else:
        print(f"Норма {NORM_PERCENT}% не выполнена, не хватает "
              f"{number(round(teu['lack_teu'], 1))} ДФЭ")
    print()
    if changes:
        print("Перестановка упоров:")
    else:
        print("Перестановка упоров не нужна")
    for change in changes:
        print(f"  платформа {change['platform_number']}: "
              f"{change['current']} -> {change['required']}")
    print("Не вошли в поезд:" if rejected else "Все контейнеры вошли в поезд")
    for container in rejected:
        print(f"  {describe(container)}")
