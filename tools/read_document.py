#!/usr/bin/env python3
"""Extract text from DOCX and PDF files."""

from __future__ import annotations

import argparse
from pathlib import Path
import sys

import docx
import fitz
import pdfplumber
from pypdf import PdfReader


def read_docx(path: Path) -> str:
    document = docx.Document(path)
    chunks: list[str] = []

    for paragraph in document.paragraphs:
        text = paragraph.text.strip()
        if text:
            chunks.append(text)

    for table_index, table in enumerate(document.tables, start=1):
        chunks.append(f"[Table {table_index}]")
        for row in table.rows:
            cells = [cell.text.strip() for cell in row.cells]
            if any(cells):
                chunks.append(" | ".join(cells))

    return "\n".join(chunks).strip()


def read_pdf_with_fitz(path: Path) -> str:
    chunks: list[str] = []
    with fitz.open(path) as pdf:
        for page_number, page in enumerate(pdf, start=1):
            text = page.get_text("text").strip()
            if text:
                chunks.append(f"[Page {page_number}]")
                chunks.append(text)
    return "\n\n".join(chunks).strip()


def read_pdf_with_pdfplumber(path: Path) -> str:
    chunks: list[str] = []
    with pdfplumber.open(path) as pdf:
        for page_number, page in enumerate(pdf.pages, start=1):
            text = (page.extract_text() or "").strip()
            if text:
                chunks.append(f"[Page {page_number}]")
                chunks.append(text)
    return "\n\n".join(chunks).strip()


def read_pdf_with_pypdf(path: Path) -> str:
    chunks: list[str] = []
    reader = PdfReader(path)
    for page_number, page in enumerate(reader.pages, start=1):
        text = (page.extract_text() or "").strip()
        if text:
            chunks.append(f"[Page {page_number}]")
            chunks.append(text)
    return "\n\n".join(chunks).strip()


def read_pdf(path: Path) -> str:
    errors: list[str] = []

    for reader_name, reader_func in (
        ("PyMuPDF", read_pdf_with_fitz),
        ("pdfplumber", read_pdf_with_pdfplumber),
        ("pypdf", read_pdf_with_pypdf),
    ):
        try:
            text = reader_func(path)
            if text:
                return text
            errors.append(f"{reader_name}: extracted empty text")
        except Exception as exc:  # pragma: no cover - fallback chain
            errors.append(f"{reader_name}: {exc}")

    raise RuntimeError("Unable to extract text from PDF. " + "; ".join(errors))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Read DOCX or PDF files and print extracted text."
    )
    parser.add_argument("file", help="Path to a .docx or .pdf file")
    parser.add_argument(
        "-o",
        "--output",
        help="Optional output text file path. If omitted, prints to stdout.",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    path = Path(args.file).expanduser().resolve()

    if not path.is_file():
        print(f"File not found: {path}", file=sys.stderr)
        return 1

    suffix = path.suffix.lower()
    if suffix == ".docx":
        text = read_docx(path)
    elif suffix == ".pdf":
        text = read_pdf(path)
    else:
        print("Only .docx and .pdf files are supported.", file=sys.stderr)
        return 1

    if not text:
        print("No readable text was extracted from the document.", file=sys.stderr)
        return 2

    if args.output:
        output_path = Path(args.output).expanduser().resolve()
        output_path.write_text(text, encoding="utf-8")
    else:
        print(text)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
