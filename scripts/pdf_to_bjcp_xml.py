#!/usr/bin/env python3
"""Convert BJCP-style PDF documents into schema-valid XML.

Usage examples:
    python scripts/pdf_to_bjcp_xml.py \
        --pdf db/2026_Guidelines_Mead-final.pdf \
        --schema db/xml/bjcp-styleguide.xsd \
        --output db/xml/bjcp-mead-2026_en.xml

The script is intentionally generic for BJCP-style PDFs that use section headings like:
    M1. Traditional Mead
    M1A. Dry Mead
    M3C. Fruit and Spice Mead
    etc.
"""

from __future__ import annotations

import argparse
import re
from pathlib import Path
from typing import Dict, List, Tuple
from xml.etree import ElementTree as ET

from pypdf import PdfReader

try:
    from lxml import etree as LET
except Exception:  # pragma: no cover
    LET = None


HEADING_RE = re.compile(
    r"^(?P<label>INTRODUCTION TO THE 2026 MEAD GUIDELINES|INTRODUCTION TO MEAD STYLES \(CATEGORIES M1-M4\)|"
    r"M[1-4](?:[A-F])?\.?\s+[A-Z0-9][A-Z0-9 /'&:-]*|M[1-4]\.?$)"
)

SECTION_HEADINGS = {
    "MI": "Introduction to the 2026 Mead Guidelines",
    "M1": "Traditional Mead",
    "M2": "Melomel",
    "M3": "Spiced Mead",
    "M4": "Specialty Mead",
}

SUBCATEGORY_LABELS = {
    "M1A": "Dry Mead",
    "M1B": "Semi-Sweet Mead",
    "M1C": "Sweet Mead",
    "M2A": "Cyser",
    "M2B": "Pyment",
    "M2C": "Berry Mead",
    "M2D": "Stone Fruit Mead",
    "M2E": "Other Fruit Mead",
    "M3A": "Metheglin",
    "M3B": "Vegetable Mead",
    "M3C": "Fruit and Spice Mead",
    "M4A": "Braggot",
    "M4B": "Bochet",
    "M4C": "Polish Mead",
    "M4D": "Wood-Aged Mead",
    "M4E": "Barrel-Aged Mead",
    "M4F": "Experimental Mead",
}


def normalize_line(raw: str) -> str:
    line = raw.replace("\xa0", " ")
    line = re.sub(r"\s+", " ", line)
    line = re.sub(r"\s+([.,;:])", r"\1", line)
    line = re.sub(r"\s+\d+$", "", line)
    line = re.sub(r"\s+[-.]{2,}\s*\d*$", "", line)
    line = re.sub(r"\s+\((?:\d+|\d+\s+of\s+\d+)\)$", "", line)
    return line.strip()


def extract_pdf_text(pdf_path: Path) -> str:
    reader = PdfReader(str(pdf_path))
    parts: List[str] = []
    for page in reader.pages:
        text = page.extract_text() or ""
        parts.append(text)
    return "\n".join(parts)


def strip_page_noise(text: str) -> str:
    lines = []
    for raw in text.splitlines():
        line = normalize_line(raw)
        if not line:
            lines.append("")
            continue

        if re.match(r"^(BJCP|BEER JUDGE CERTIFICATION PROGRAM|Mead Style Guidelines|Contents|Copyright|Updates available|Authored by|2026 Content|2026 Review|Proofreader|\d+)$", line, flags=re.I):
            continue
        if re.match(r"^BJCP Mead Style Guidelines.*\d+$", line, flags=re.I):
            continue
        if re.match(r"^\d+$", line):
            continue
        if re.match(r"^.*Page.*\d+.*$", line, flags=re.I):
            continue

        # Remove dotted page leaders such as:
        # "Aroma and Flavor........................................ 1"
        if re.match(r"^(?:Aroma and Flavor|Appearance|Mouthfeel|Overall Impression|Ingredients|Entry Instructions|INTRODUCTION TO THE 2026 MEAD GUIDELINES|INTRODUCTION TO MEAD STYLES \(CATEGORIES M1-M4\)|M[1-4](?:[A-F])?\.?\s+[A-Z0-9].*)\s*\.+\s*\d*$", line, flags=re.I):
            continue
        if re.search(r"\.{3,}\s*\d+$", line):
            line = re.sub(r"\.{3,}\s*\d+$", "", line)
        if re.search(r"\.{3,}\s*\d+\s*$", line):
            line = re.sub(r"\.{3,}\s*\d+\s*$", "", line)
        if line.endswith(".") and line.count(".") > 1 and re.search(r"\d$", line):
            line = re.sub(r"\.+\d*$", "", line)

        lines.append(line)
    return "\n".join(lines)


def scan_sections(raw_text: str) -> Dict[str, List[str]]:
    text = strip_page_noise(raw_text)
    sections: Dict[str, List[str]] = {}
    current_id = None
    current_lines: List[str] = []

    def flush_current() -> None:
        nonlocal current_id, current_lines
        if current_id is not None:
            sections.setdefault(current_id, []).extend(current_lines)
            current_lines = []

    for line in text.splitlines():
        line = line.strip()
        if not line:
            if current_id is not None and current_lines:
                current_lines.append("")
            continue

        match = HEADING_RE.match(line)
        if match:
            label = match.group("label")
            if label.startswith("INTRODUCTION"):
                current_id = "MI"
            elif label.startswith("M1") and label.startswith("M1."):
                flush_current()
                current_id = "M1"
            elif label.startswith("M2") and label.startswith("M2."):
                flush_current()
                current_id = "M2"
            elif label.startswith("M3") and label.startswith("M3."):
                flush_current()
                current_id = "M3"
            elif label.startswith("M4") and label.startswith("M4."):
                flush_current()
                current_id = "M4"
            elif re.match(r"^M[1-4][A-F]", label):
                flush_current()
                current_id = label.split(".", 1)[0].strip() if "." in label else label
            else:
                current_id = label

            if current_id in ("MI", "M1", "M2", "M3", "M4"):
                current_lines = []
            else:
                current_lines = []
            continue

        if current_id is None:
            continue
        current_lines.append(line)

    flush_current()
    return sections


def clean_paragraphs(text_blocks: List[str]) -> str:
    para_parts: List[str] = []
    paragraph: List[str] = []
    for block in text_blocks:
        if not block:
            if paragraph:
                para_parts.append(" ".join(part for part in paragraph if part).strip())
                paragraph = []
            continue
        paragraph.append(block)
    if paragraph:
        para_parts.append(" ".join(part for part in paragraph if part).strip())

    joined = "\n\n".join(p for p in para_parts if p)
    return re.sub(r"\s+", " ", joined).strip()


def xml_escape(value: str) -> str:
    return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def make_notes_element(parent: ET.Element, text: str) -> None:
    notes = ET.SubElement(parent, "notes")
    notes.text = xml_escape(text)


def build_document(pdf_path: Path, revision: str = "BJCP_2026") -> ET.Element:
    raw_text = extract_pdf_text(pdf_path)
    sections = scan_sections(raw_text)

    root = ET.Element("styleguide", revision=revision, language="en")
    category_m = ET.SubElement(root, "category", {"id": "M"})
    ET.SubElement(category_m, "revision", {"number": "1"}).text = revision
    ET.SubElement(category_m, "name").text = "Mead 2026"

    intro_blocks = sections.get("MI", [])
    intro_text = clean_paragraphs(intro_blocks)
    mi = ET.SubElement(category_m, "subcategory", {"id": "MI"})
    ET.SubElement(mi, "name").text = SECTION_HEADINGS["MI"]
    if intro_text:
        make_notes_element(mi, intro_text)

    for section_id in ["M1", "M2", "M3", "M4"]:
        section_text = clean_paragraphs(sections.get(section_id, []))
        cat = ET.SubElement(category_m, "category", {"id": section_id})
        ET.SubElement(cat, "name").text = SECTION_HEADINGS[section_id]
        if section_text:
            make_notes_element(cat, section_text)

        for sub_id in sorted(
            [k for k in SUBCATEGORY_LABELS if k.startswith(section_id)],
            key=lambda k: (len(k), k),
        ):
            sub_text = clean_paragraphs(sections.get(sub_id, []))
            sub = ET.SubElement(cat, "subcategory", {"id": sub_id})
            ET.SubElement(sub, "name").text = SUBCATEGORY_LABELS[sub_id]
            if sub_text:
                make_notes_element(sub, sub_text)

    return root


def validate_against_schema(xml_path: Path, xsd_path: Path) -> bool:
    if LET is None:
        raise RuntimeError("lxml is required for schema validation; install it with: pip install lxml")
    xml_doc = LET.parse(str(xml_path))
    schema_doc = LET.parse(str(xsd_path))
    schema = LET.XMLSchema(schema_doc)
    is_valid = schema.validate(xml_doc)
    if not is_valid:
        print(schema.error_log)
    return is_valid


def main() -> int:
    parser = argparse.ArgumentParser(description="Convert BJCP PDF guideline files into XML using the styleguide schema.")
    parser.add_argument("--pdf", required=True, type=Path, help="Path to the input PDF")
    parser.add_argument("--schema", required=True, type=Path, help="Path to the schema XSD file")
    parser.add_argument("--output", required=True, type=Path, help="Path for the generated XML file")
    parser.add_argument("--revision", default="BJCP_2026", help="Revision value used in the root styleguide element")
    parser.add_argument("--validate", action="store_true", help="Validate the output against the given schema")
    args = parser.parse_args()

    root = build_document(args.pdf, revision=args.revision)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    ET.indent(root, space="  ")
    ET.ElementTree(root).write(args.output, encoding="utf-8", xml_declaration=True)

    if args.validate:
        valid = validate_against_schema(args.output, args.schema)
        if not valid:
            raise SystemExit(1)

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
