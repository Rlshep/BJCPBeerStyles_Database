#!/usr/bin/env python3
"""Batch-convert BJCP PDF guideline files into XML using the BJCP schema.

Example:
    .venv/bin/python scripts/batch_convert_bjcp_pdfs.py \
        --input-dir db \
        --output-dir db/xml \
        --schema db/xml/bjcp-styleguide.xsd \
        --pattern "*Guidelines*.pdf"
"""

from __future__ import annotations

import argparse
from pathlib import Path
import sys

from pdf_to_bjcp_xml import build_document, validate_against_schema
from xml.etree import ElementTree as ET


def convert_one(pdf_path: Path, output_path: Path, xsd_path: Path, revision: str, validate: bool) -> bool:
    root = build_document(pdf_path, revision=revision)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    ET.indent(root, space="  ")
    ET.ElementTree(root).write(output_path, encoding="utf-8", xml_declaration=True)

    if validate:
        return validate_against_schema(output_path, xsd_path)
    return True


def main() -> int:
    parser = argparse.ArgumentParser(description="Batch-convert BJCP-style PDFs into XML using the shared schema.")
    parser.add_argument("--input-dir", type=Path, required=True, help="Folder containing BJCP PDF files")
    parser.add_argument("--output-dir", type=Path, required=True, help="Folder to write generated XML files")
    parser.add_argument("--schema", type=Path, required=True, help="Path to the XSD schema file")
    parser.add_argument("--pattern", default="*Guidelines*.pdf", help="Glob pattern for PDFs to convert")
    parser.add_argument("--revision", default="BJCP_2026", help="Revision value to write into the styleguide XML")
    parser.add_argument("--validate", action="store_true", help="Validate each file against the XSD")
    args = parser.parse_args()

    pdfs = sorted(args.input_dir.glob(args.pattern))
    if not pdfs:
        print(f"No PDF files matched pattern '{args.pattern}' under {args.input_dir}", file=sys.stderr)
        return 1

    ok = True
    for pdf in pdfs:
        output_name = pdf.name.replace(".pdf", ".xml")
        output_path = args.output_dir / output_name
        print(f"Converting {pdf} -> {output_path}")
        valid = convert_one(pdf, output_path, args.schema, args.revision, args.validate)
        if args.validate and not valid:
            print(f"Validation failed for {output_path}", file=sys.stderr)
            ok = False

    return 0 if ok else 1


if __name__ == "__main__":
    raise SystemExit(main())
