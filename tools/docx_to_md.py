#!/usr/bin/env python3
"""Convert DOCX documents to Markdown."""

from __future__ import annotations

import argparse
from pathlib import Path
import re
import zipfile

import docx
from docx.document import Document as DocxDocument
from docx.oxml.table import CT_Tbl
from docx.oxml.text.paragraph import CT_P
from docx.table import Table
from docx.text.paragraph import Paragraph


HEADING_PATTERN = re.compile(r"(heading|标题)\s*([1-6])", re.IGNORECASE)
LIST_PATTERN = re.compile(r"^(\d+[\.\)]|[一二三四五六七八九十]+[、.]|[-*•])\s+")


def format_markdown_table(rows: list[list[str]]) -> str:
    normalized = [[(cell or "").replace("\n", " ").strip() for cell in row] for row in rows]
    normalized = [row for row in normalized if any(row)]
    if not normalized:
        return ""

    deduped: list[list[str]] = []
    for row in normalized:
        non_empty = [cell for cell in row if cell]
        if len(non_empty) > 1 and len(set(non_empty)) == 1:
            deduped.append([non_empty[0]] + [""] * (len(row) - 1))
        else:
            deduped.append(row)
    normalized = deduped

    width = max(len(row) for row in normalized)
    padded = [row + [""] * (width - len(row)) for row in normalized]

    if len(padded) == 1:
        padded.append([""] * width)

    lines = [
        "| " + " | ".join(padded[0]) + " |",
        "| " + " | ".join(["---"] * width) + " |",
    ]
    for row in padded[1:]:
        lines.append("| " + " | ".join(row) + " |")
    return "\n".join(lines)


def paragraph_to_md(paragraph: docx.text.paragraph.Paragraph) -> str:
    text = paragraph.text.strip()
    if not text:
        return ""

    style_name = paragraph.style.name if paragraph.style else ""
    match = HEADING_PATTERN.search(style_name)
    if match:
        level = int(match.group(2))
        return f"{'#' * level} {text}"

    if LIST_PATTERN.match(text):
        return text

    return text


def extract_docx_image_notes(path: Path) -> list[str]:
    notes: list[str] = []
    with zipfile.ZipFile(path) as archive:
        image_names = [
            name for name in archive.namelist() if name.startswith("word/media/")
        ]
        for index, _ in enumerate(image_names, start=1):
            notes.append(f"![文档图片 {index}](images/{path.stem}_image_{index}.png)")
    return notes


def iter_block_items(parent: DocxDocument):
    for child in parent.element.body.iterchildren():
        if isinstance(child, CT_P):
            yield Paragraph(child, parent)
        elif isinstance(child, CT_Tbl):
            yield Table(child, parent)


def convert_docx_to_markdown(input_path: Path) -> str:
    document = docx.Document(input_path)
    chunks: list[str] = []

    table_index = 1
    for item in iter_block_items(document):
        if isinstance(item, Paragraph):
            block = paragraph_to_md(item)
            if block:
                chunks.append(block)
        elif isinstance(item, Table):
            rows = []
            for row in item.rows:
                rows.append([cell.text.strip() for cell in row.cells])
            markdown = format_markdown_table(rows)
            if markdown:
                chunks.append(f"[表格 {table_index}]")
                chunks.append(markdown)
                table_index += 1

    image_notes = extract_docx_image_notes(input_path)
    if image_notes:
        chunks.append("\n[文档中的图片占位]\n")
        chunks.extend(image_notes)

    cleaned: list[str] = []
    previous_blank = False
    for chunk in chunks:
        chunk = chunk.strip()
        if not chunk:
            if not previous_blank:
                cleaned.append("")
            previous_blank = True
            continue
        cleaned.append(chunk)
        previous_blank = False

    return "\n\n".join(cleaned).strip() + "\n"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Convert a DOCX file to Markdown.")
    parser.add_argument("input", help="Path to the source DOCX file")
    parser.add_argument("output", help="Path to the destination Markdown file")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    input_path = Path(args.input).expanduser().resolve()
    output_path = Path(args.output).expanduser().resolve()

    if not input_path.is_file():
        raise SystemExit(f"Input file not found: {input_path}")

    markdown = convert_docx_to_markdown(input_path)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(markdown, encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
