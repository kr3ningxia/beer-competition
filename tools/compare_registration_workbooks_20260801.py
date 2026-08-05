from __future__ import annotations

import argparse
import json
from pathlib import Path

import openpyxl


FIELD_COLUMNS = {
    "status": 2,
    "contact_name": 3,
    "wechat": 4,
    "phone": 5,
    "brand": 6,
    "group": 7,
    "name": 8,
    "style": 9,
    "abv": 10,
    "special": 11,
    "promotion": 12,
    "work_code": 13,
}


def clean(value: object) -> str:
    if value is None:
        return ""
    if isinstance(value, float) and value.is_integer():
        return str(int(value))
    return str(value).strip()


def load_paid_rows(path: Path) -> dict[str, dict[str, str | int]]:
    # 部分外部导出的工作簿在只读模式下不会正确返回共享字符串，使用普通模式读取。
    workbook = openpyxl.load_workbook(path, data_only=False, read_only=False)
    sheet = workbook.active
    rows: dict[str, dict[str, str | int]] = {}
    for row_number, values in enumerate(sheet.iter_rows(min_row=3, values_only=True), start=3):
        if clean(values[FIELD_COLUMNS["status"] - 1]) != "已付款":
            continue
        item: dict[str, str | int] = {"source_row": row_number}
        for field, column in FIELD_COLUMNS.items():
            item[field] = clean(values[column - 1] if column <= len(values) else None)
        work_code = str(item["work_code"])
        if not work_code:
            raise ValueError(f"{path} 第 {row_number} 行缺少作品编号")
        if work_code in rows:
            raise ValueError(f"{path} 作品编号重复：{work_code}")
        rows[work_code] = item
    return rows


def compare(old_path: Path, new_path: Path) -> dict[str, object]:
    old_rows = load_paid_rows(old_path)
    new_rows = load_paid_rows(new_path)
    compared_fields = [field for field in FIELD_COLUMNS if field not in {"status", "work_code"}]
    changes = []
    phone_additions = []
    for work_code in sorted(old_rows.keys() & new_rows.keys()):
        old_row = old_rows[work_code]
        new_row = new_rows[work_code]
        field_changes = {
            field: {"old": old_row[field], "new": new_row[field]}
            for field in compared_fields
            if old_row[field] != new_row[field]
        }
        if field_changes:
            item = {
                "work_code": work_code,
                "old_source_row": old_row["source_row"],
                "new_source_row": new_row["source_row"],
                "brand": new_row["brand"],
                "name": new_row["name"],
                "changes": field_changes,
            }
            changes.append(item)
            if "phone" in field_changes and not field_changes["phone"]["old"] and field_changes["phone"]["new"]:
                phone_additions.append(item)

    return {
        "old_file": str(old_path),
        "new_file": str(new_path),
        "old_paid_rows": len(old_rows),
        "new_paid_rows": len(new_rows),
        "removed_work_codes": sorted(old_rows.keys() - new_rows.keys()),
        "added_work_codes": sorted(new_rows.keys() - old_rows.keys()),
        "changed_records": changes,
        "phone_additions": phone_additions,
        "old_blank_phone_records": sum(not row["phone"] for row in old_rows.values()),
        "new_blank_phone_records": sum(not row["phone"] for row in new_rows.values()),
        "old_unique_phones": len({str(row["phone"]) for row in old_rows.values() if row["phone"]}),
        "new_unique_phones": len({str(row["phone"]) for row in new_rows.values() if row["phone"]}),
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="按作品编号对比两份报名表中的已付款记录")
    parser.add_argument("old", type=Path)
    parser.add_argument("new", type=Path)
    args = parser.parse_args()
    print(json.dumps(compare(args.old, args.new), ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
