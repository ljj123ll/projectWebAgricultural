#!/usr/bin/env python3
"""Download better-matching product images and generate SQL updates."""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import urllib.parse
import urllib.request


DB_NAME = "nongnong_ecommerce"
DB_USER = "root"
MYSQL_PASSWORD = "798220040520ljj"
USER_AGENT = "codex-product-image-fix/1.0"

WORKSPACE = Path(r"c:\project\毕业设计\projectWebAgricultural")
UPLOAD_DIR = WORKSPACE / "agricultural" / "uploads"
SQL_OUTPUT = WORKSPACE / "agricultural" / "sql" / "fix_product_images_20260326.sql"
MANIFEST_OUTPUT = WORKSPACE / "agricultural" / "backups" / "product_images_manifest_20260326.json"


def run_mysql_query(sql: str) -> list[list[str]]:
    env = os.environ.copy()
    env["MYSQL_PWD"] = MYSQL_PASSWORD
    result = subprocess.run(
        [
            "mysql",
            "-h",
            "localhost",
            "-u",
            DB_USER,
            "-D",
            DB_NAME,
            "--default-character-set=utf8mb4",
            "--batch",
            "--raw",
            "--skip-column-names",
            "-e",
            sql,
        ],
        capture_output=True,
        text=True,
        encoding="utf-8",
        env=env,
        check=True,
    )
    rows = []
    for line in result.stdout.splitlines():
        rows.append(line.split("\t"))
    return rows


def http_download(url: str, target: Path) -> None:
    req = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(req, timeout=60) as response:
        payload = response.read()
    target.write_bytes(payload)


def escape_sql(value: str) -> str:
    return value.replace("\\", "\\\\").replace("'", "\\'")


def keyword_pack(product_name: str, category_id: int) -> tuple[list[str], list[str]]:
    name = product_name

    if "草莓" in name:
        return ["strawberry fruit", "strawberry basket"], ["strawberry close-up", "strawberry farm"]
    if "耙耙柑" in name or "柑" in name:
        return ["mandarin orange fruit", "tangerine fruit"], ["citrus fruit close-up", "orange basket"]
    if "血橙" in name:
        return ["blood orange fruit", "blood orange sliced"], ["orange fruit close-up", "blood orange"]
    if "蓝莓" in name:
        return ["blueberry fruit", "blueberry close-up"], ["blueberry basket", "blueberry fruit close-up"]
    if "芒果" in name:
        return ["mango fruit", "ripe mango"], ["mango sliced", "mango fruit close-up"]
    if "猕猴桃" in name:
        return ["kiwifruit fruit", "kiwi fruit sliced"], ["kiwifruit close-up", "kiwifruit vine"]
    if "柠檬" in name:
        return ["lemon fruit", "yellow lemon"], ["lemon sliced", "lemon tree fruit"]
    if "苹果" in name:
        return ["red apple fruit", "apple orchard"], ["apple sliced", "apple fruit close-up"]

    if "银耳" in name:
        return ["tremella fuciformis", "white fungus mushroom"], ["tremella dried", "white fungus close-up"]
    if "黑木耳" in name:
        return ["wood ear mushroom", "black fungus mushroom"], ["dried wood ear mushroom", "wood ear close-up"]
    if "香菇" in name:
        return ["shiitake mushroom dried", "shiitake mushroom"], ["dried shiitake close-up", "shiitake mushroom basket"]
    if "核桃" in name:
        return ["walnut kernel", "shelled walnuts"], ["walnut close-up", "walnut nuts"]
    if "牛肉干" in name:
        return ["beef jerky", "jerky snack"], ["dried beef jerky", "beef jerky close-up"]
    if "藏茶" in name or "茶" in name:
        return ["dark tea leaves", "tea leaves"], ["Chinese tea close-up", "tea leaves dry"]

    if "大米" in name:
        return ["rice grains", "rice harvest"], ["rice bowl grains", "rice close-up"]
    if "菜籽油" in name:
        return ["rapeseed oil bottle", "canola oil bottle"], ["cooking oil bottle", "rapeseed oil"]
    if "粉条" in name:
        return ["sweet potato noodles", "sweet potato vermicelli"], ["glass noodles", "sweet potato noodles close-up"]
    if "青稞面" in name:
        return ["barley flour", "barley grain"], ["flour bowl", "barley flour close-up"]
    if "黄豆" in name:
        return ["soybeans", "soy bean seeds"], ["soybeans close-up", "soybean harvest"]
    if "蜂蜜" in name:
        return ["honey jar", "raw honey"], ["honey dipper", "honey close-up"]

    if "鸡蛋" in name:
        return ["brown chicken eggs", "egg basket"], ["fresh eggs close-up", "hen eggs"]
    if "土鸡" in name or ("鸡" in name and "蛋" not in name):
        return ["free range chicken", "raw whole chicken"], ["chicken meat", "fresh chicken"]
    if "猪" in name:
        return ["raw pork shoulder", "fresh pork"], ["pork meat close-up", "raw pork"]
    if "鸭" in name:
        return ["raw duck meat", "fresh duck"], ["duck meat close-up", "duck raw meat"]
    if "牦牛" in name:
        return ["yak meat", "yak steak"], ["yak meat close-up", "beef tenderloin raw"]
    if "牛肉" in name:
        return ["raw beef meat", "beef slices raw"], ["beef close-up", "beef steak raw"]
    if "腊肠" in name:
        return ["Chinese sausage", "cured sausage"], ["sausage slices", "Chinese sausage close-up"]

    if category_id == 1:
        return ["fresh fruit", "fruit basket"], ["fruit close-up", "orchard fruit"]
    if category_id == 2:
        return ["dried food", "mushroom dried"], ["food close-up", "dry goods"]
    if category_id == 3:
        return ["grain food", "farm produce"], ["grain close-up", "farm ingredient"]
    return ["fresh meat", "meat product"], ["meat close-up", "raw meat"]


def build_loremflickr_url(term: str, width: int, height: int, lock: int) -> str:
    keywords = ",".join(part for part in re.split(r"\s+", term.strip()) if part)
    return f"https://loremflickr.com/{width}/{height}/{urllib.parse.quote(keywords)}?lock={lock}"


def download_set(product_id: int, product_name: str, terms: list[str], role: str, limit: int) -> tuple[list[str], list[dict]]:
    urls: list[str] = []
    manifest: list[dict] = []
    width, height = (900, 900) if role == "main" else (1200, 800)

    for index, term in enumerate(terms[:limit], start=1):
        source_url = build_loremflickr_url(
            term,
            width=width,
            height=height,
            lock=product_id * 100 + index + (0 if role == "main" else 50),
        )
        file_name = f"product-{product_id}-{role}-{index}.jpg"
        local_path = UPLOAD_DIR / file_name
        try:
            http_download(source_url, local_path)
        except Exception:
            continue
        urls.append(f"/uploads/{file_name}")
        manifest.append(
            {
                "product_id": product_id,
                "product_name": product_name,
                "role": role,
                "index": index,
                "term": term,
                "source_url": source_url,
                "local_path": str(local_path),
            }
        )
        time.sleep(0.2)
    return urls, manifest


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Fix product images by downloading better-matching assets.")
    parser.add_argument(
        "--ids",
        help="Optional comma-separated product IDs to process.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    UPLOAD_DIR.mkdir(parents=True, exist_ok=True)
    SQL_OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST_OUTPUT.parent.mkdir(parents=True, exist_ok=True)

    rows = run_mysql_query(
        "SELECT id, product_name, category_id FROM product_info WHERE delete_flag=0 ORDER BY id"
    )
    selected_ids = None
    if args.ids:
        selected_ids = {int(item.strip()) for item in args.ids.split(",") if item.strip()}
        rows = [row for row in rows if int(row[0]) in selected_ids]

    sql_lines = [
        "USE nongnong_ecommerce;",
        "START TRANSACTION;",
    ]
    manifest: list[dict] = []

    for row in rows:
        product_id = int(row[0])
        product_name = row[1]
        category_id = int(row[2])

        main_terms, detail_terms = keyword_pack(product_name, category_id)
        main_urls, main_manifest = download_set(product_id, product_name, main_terms, "main", 2)
        detail_urls, detail_manifest = download_set(product_id, product_name, detail_terms, "detail", 2)

        # If detail search is sparse, reuse main assets so details are at least product-consistent.
        if len(detail_urls) < 2:
            for url in main_urls:
                if url not in detail_urls:
                    detail_urls.append(url)
                if len(detail_urls) >= 2:
                    break

        if not main_urls:
            raise RuntimeError(f"Could not find any image for product {product_id} {product_name}")

        product_img = ",".join(main_urls)
        product_detail_img = ",".join(detail_urls or main_urls)

        sql_lines.append(
            "UPDATE product_info "
            f"SET product_img='{escape_sql(product_img)}', "
            f"product_detail_img='{escape_sql(product_detail_img)}' "
            f"WHERE id={product_id};"
        )

        manifest.extend(main_manifest)
        manifest.extend(detail_manifest)
        print(f"{product_id}: {product_name} -> {len(main_urls)} main, {len(detail_urls)} detail")

    sql_lines.extend(["COMMIT;", ""])
    SQL_OUTPUT.write_text("\n".join(sql_lines), encoding="utf-8")
    MANIFEST_OUTPUT.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"SQL written to {SQL_OUTPUT}")
    print(f"Manifest written to {MANIFEST_OUTPUT}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
