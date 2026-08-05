from __future__ import annotations

import argparse
import json
import subprocess
import sys
from collections import defaultdict
from decimal import Decimal
from pathlib import Path

from import_registration_20260801 import INPUT_PATH, build_breweries, load_rows


def decode_hex(value: str) -> str:
    if value in ("", "NULL", r"\N"):
        return ""
    return bytes.fromhex(value).decode("utf-8")


def mysql_rows(mysql: Path, password: str, database: str, sql: str) -> list[list[str]]:
    result = subprocess.run(
        [
            str(mysql),
            "--default-character-set=utf8mb4",
            "-h",
            "localhost",
            "-P",
            "3306",
            "-uroot",
            f"-p{password}",
            database,
            "-N",
            "-B",
            "-e",
            sql,
        ],
        check=True,
        capture_output=True,
    )
    return [line.decode("utf-8").split("\t") for line in result.stdout.splitlines() if line]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--mysql", type=Path, required=True)
    parser.add_argument("--password", required=True)
    parser.add_argument("--database", default="beer_competition")
    parser.add_argument("--input", type=Path, default=INPUT_PATH)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    expected_rows = load_rows(args.input)
    expected_by_uuid = {row["uuid"]: row for row in expected_rows}
    breweries = build_breweries(expected_rows)

    entry_sql = """
        SELECT HEX(e.uuid), e.id, e.category_id, HEX(e.name), HEX(e.style), e.abv,
               HEX(CAST(e.extra_fields_json AS CHAR)), HEX(l.short_code), HEX(b.company_name),
               HEX(b.phone), b.id, e.style_config_id, HEX(s.name), e.status,
               HEX(l.label_code), HEX(l.scan_token)
        FROM beer_entry e
        JOIN brewery b ON b.id = e.brewery_id
        LEFT JOIN entry_scan_label l ON l.beer_entry_id = e.id AND l.status = 'ACTIVE'
        LEFT JOIN competition_style_config s ON s.id = e.style_config_id
        WHERE e.competition_id = 1070 AND e.deleted_flag = 0
        ORDER BY e.id
    """
    actual_rows = mysql_rows(args.mysql, args.password, args.database, entry_sql)
    actual_by_uuid = {decode_hex(row[0]): row for row in actual_rows}

    differences: list[dict] = []
    field_difference_counts: defaultdict[str, int] = defaultdict(int)
    brand_actual_brewery_ids: defaultdict[str, set[int]] = defaultdict(set)

    def add_difference(source_row: int | None, field: str, expected, actual, severity: str = "error") -> None:
        field_difference_counts[field] += 1
        differences.append(
            {
                "source_row": source_row,
                "field": field,
                "expected": str(expected),
                "actual": str(actual),
                "severity": severity,
            }
        )

    for entry_uuid, expected in expected_by_uuid.items():
        actual = actual_by_uuid.get(entry_uuid)
        if actual is None:
            add_difference(expected["source_row"], "beer_entry", "存在", "缺失")
            continue
        actual_extra = json.loads(decode_hex(actual[6]))
        checks = {
            "category_id": (expected["category_id"], int(actual[2])),
            "name": (expected["name"], decode_hex(actual[3])),
            "style": (expected["style"], decode_hex(actual[4])),
            "abv": (expected["abv"], Decimal(actual[5])),
            "specialIngredients": (expected["special_ingredients"], actual_extra.get("specialIngredients", "")),
            "promotionAllowed": (expected["promotion_allowed"], actual_extra.get("custom_1785549677921_0089y", "")),
            "short_code": (expected["short_code"], decode_hex(actual[7])),
            "style_config_name": (expected["style"], decode_hex(actual[12])),
            "status": ("REGISTERED", actual[13]),
        }
        for field, (expected_value, actual_value) in checks.items():
            if expected_value != actual_value:
                add_difference(expected["source_row"], field, expected_value, actual_value)
        if not decode_hex(actual[14]):
            add_difference(expected["source_row"], "label_code", "非空", "空")
        if not decode_hex(actual[15]):
            add_difference(expected["source_row"], "scan_token", "非空", "空")

        expected_brand = expected["brand"]
        actual_brand = decode_hex(actual[8])
        brand_actual_brewery_ids[expected_brand].add(int(actual[10]))
        if expected_brand != actual_brand:
            add_difference(expected["source_row"], "brewery_company_name", expected_brand, actual_brand, "warning")

    for entry_uuid in sorted(set(actual_by_uuid) - set(expected_by_uuid)):
        add_difference(None, "unexpected_entry", "不存在", entry_uuid)

    normalized_extra_sql = """
        SELECT HEX(e.uuid), x.field_key, HEX(x.field_label), HEX(x.field_value)
        FROM beer_entry_extra_field x
        JOIN beer_entry e ON e.id = x.beer_entry_id
        WHERE e.competition_id = 1070 AND e.deleted_flag = 0
        ORDER BY e.id, x.field_key, x.id
    """
    normalized_extra_rows = mysql_rows(args.mysql, args.password, args.database, normalized_extra_sql)
    normalized_extra_by_entry: defaultdict[tuple[str, str], list[list[str]]] = defaultdict(list)
    for row in normalized_extra_rows:
        normalized_extra_by_entry[(decode_hex(row[0]), row[1])].append(row)
    for entry_uuid, expected in expected_by_uuid.items():
        expected_fields = {
            "specialIngredients": expected["special_ingredients"],
            "custom_1785549677921_0089y": expected["promotion_allowed"],
        }
        for field_key, expected_value in expected_fields.items():
            actual_fields = normalized_extra_by_entry.get((entry_uuid, field_key), [])
            if len(actual_fields) != 1:
                add_difference(expected["source_row"], f"normalized_extra_field[{field_key}]", 1, len(actual_fields))
                continue
            if not decode_hex(actual_fields[0][2]):
                add_difference(expected["source_row"], f"normalized_extra_label[{field_key}]", "非空", "空")
            actual_value = decode_hex(actual_fields[0][3])
            if expected_value != actual_value:
                add_difference(expected["source_row"], f"normalized_extra_value[{field_key}]", expected_value, actual_value)

    phone_expected_wechat: dict[str, str] = {}
    phone_expected_brand: dict[str, str] = {}
    for row in expected_rows:
        if row["phone"]:
            phone_expected_wechat.setdefault(row["phone"], row["wechat"])
            phone_expected_brand.setdefault(row["phone"], row["brand"])

    account_sql = """
        SELECT HEX(a.phone), HEX(a.wechat), HEX(a.display_name), a.brewery_id,
               HEX(b.company_name), HEX(b.contact_name), HEX(b.phone), HEX(b.wechat), a.status
        FROM portal_account a
        JOIN brewery b ON b.id = a.brewery_id
        WHERE a.brewery_id IN (
            SELECT DISTINCT brewery_id FROM beer_entry WHERE competition_id = 1070 AND deleted_flag = 0
        )
        ORDER BY a.phone
    """
    account_rows = mysql_rows(args.mysql, args.password, args.database, account_sql)
    account_by_phone = {decode_hex(row[0]): row for row in account_rows}
    for phone, expected_brand in phone_expected_brand.items():
        actual = account_by_phone.get(phone)
        if actual is None:
            add_difference(None, "portal_account", f"手机号 {phone} 存在账号", "缺失")
            continue
        actual_wechat = decode_hex(actual[1])
        expected_wechat = phone_expected_wechat[phone]
        if expected_wechat != actual_wechat:
            add_difference(None, f"portal_account.wechat[{phone}]", expected_wechat, actual_wechat, "warning")
        actual_brand = decode_hex(actual[4])
        if expected_brand != actual_brand:
            add_difference(None, f"portal_account.brewery[{phone}]", expected_brand, actual_brand, "warning")
        actual_display_name = decode_hex(actual[2])
        if expected_brand != actual_display_name:
            add_difference(None, f"portal_account.display_name[{phone}]", expected_brand, actual_display_name, "warning")
        if actual[8] != "1":
            add_difference(None, f"portal_account.status[{phone}]", "1", actual[8])

    brewery_sql = """
        SELECT b.id, HEX(b.company_name), HEX(b.contact_name), HEX(b.phone), HEX(b.wechat)
        FROM brewery b
        WHERE b.id IN (
            SELECT DISTINCT brewery_id FROM beer_entry WHERE competition_id = 1070 AND deleted_flag = 0
        )
        ORDER BY b.id
    """
    brewery_rows = mysql_rows(args.mysql, args.password, args.database, brewery_sql)
    brewery_by_id = {int(row[0]): row for row in brewery_rows}
    for brand, expected in breweries.items():
        ids = brand_actual_brewery_ids.get(brand, set())
        if len(ids) != 1:
            add_difference(None, f"brewery_id[{brand}]", "唯一", sorted(ids))
            continue
        actual = brewery_by_id[next(iter(ids))]
        brewery_checks = {
            f"brewery.company_name[{brand}]": (brand, decode_hex(actual[1])),
            f"brewery.contact_name[{brand}]": (expected["contact_name"], decode_hex(actual[2])),
            f"brewery.phone[{brand}]": (expected["phones"][0] if expected["phones"] else "", decode_hex(actual[3])),
            f"brewery.wechat[{brand}]": (expected["wechat"], decode_hex(actual[4])),
        }
        for field, (expected_value, actual_value) in brewery_checks.items():
            if expected_value != actual_value:
                add_difference(None, field, expected_value, actual_value, "warning")

    actual_brewery_ids = {int(row[10]) for row in actual_rows}
    no_phone_accounts = [row for row in account_rows if int(row[3]) in actual_brewery_ids and decode_hex(row[0]) == ""]
    if no_phone_accounts:
        add_difference(None, "no_phone_account", "0", len(no_phone_accounts))

    style_names = {row["style"] for row in expected_rows}
    report = {
        "summary": {
            "expected_entries": len(expected_rows),
            "actual_entries": len(actual_rows),
            "expected_breweries": len(breweries),
            "actual_breweries": len(actual_brewery_ids),
            "expected_phone_accounts": len(phone_expected_brand),
            "actual_phone_accounts": len(account_rows),
            "expected_styles": len(style_names),
            "expected_normalized_extra_fields": len(expected_rows) * 2,
            "actual_normalized_extra_fields": len(normalized_extra_rows),
            "error_count": sum(item["severity"] == "error" for item in differences),
            "warning_count": sum(item["severity"] == "warning" for item in differences),
            "field_difference_counts": dict(sorted(field_difference_counts.items())),
        },
        "differences": differences,
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report["summary"], ensure_ascii=False, indent=2))
    for item in differences:
        print(json.dumps(item, ensure_ascii=False))
    if report["summary"]["error_count"]:
        sys.exit(2)


if __name__ == "__main__":
    main()
