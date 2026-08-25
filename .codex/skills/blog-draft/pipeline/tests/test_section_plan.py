from __future__ import annotations

import argparse
import json
from pathlib import Path
import sys
import tempfile
import unittest


PIPELINE_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(PIPELINE_DIR))

from intake_to_input import build_brief, read_base_brief, read_context, read_raw_text  # noqa: E402
from validate import validate_file  # noqa: E402


class SectionPlanInputTest(unittest.TestCase):
    def base_payload(self) -> dict:
        return {
            "brief_hash": "abcdef12",
            "brief": {
                "topic": "Section plan",
                "raw_text": "The observed decision failed once and changed the next attempt.",
                "intent": "Explain the decision",
                "audience": "Reviewers",
            },
            "created_at": "2026-08-25T15:00:00+09:00",
        }

    def plan_item(self, section_id: str = "s1") -> dict:
        return {
            "id": section_id,
            "heading_promise": "Why the decision changed",
            "purpose": "Show the observation that changed the decision",
            "materials": [
                {
                    "source": "raw_text",
                    "anchor": "decision failed once",
                    "role": "Evidence for the change",
                }
            ],
        }

    def validate_input(self, payload: dict) -> dict:
        with tempfile.TemporaryDirectory() as temp_dir:
            path = Path(temp_dir) / "input.json"
            path.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")
            return validate_file(path, artifact="input")

    def test_existing_input_without_section_plan_still_passes(self) -> None:
        result = self.validate_input(self.base_payload())

        self.assertEqual("PASS", result["status"])

    def test_connection_is_optional(self) -> None:
        payload = self.base_payload()
        payload["brief"]["section_plan"] = [self.plan_item()]

        result = self.validate_input(payload)

        self.assertEqual("PASS", result["status"])

    def test_declared_connection_must_be_non_blank_string(self) -> None:
        payload = self.base_payload()
        item = self.plan_item()
        item["connection_to_next"] = "   "
        payload["brief"]["section_plan"] = [item]

        result = self.validate_input(payload)

        self.assertEqual("REJECT", result["status"])
        self.assertTrue(any("connection_to_next" in error for error in result["errors"]))

    def test_spine_and_section_plan_cannot_coexist(self) -> None:
        payload = self.base_payload()
        payload["brief"]["spine"] = ["legacy order"]
        payload["brief"]["section_plan"] = [self.plan_item()]

        result = self.validate_input(payload)

        self.assertEqual("REJECT", result["status"])

    def test_section_ids_must_be_unique(self) -> None:
        payload = self.base_payload()
        payload["brief"]["section_plan"] = [self.plan_item(), self.plan_item()]

        result = self.validate_input(payload)

        self.assertIn("section_plan duplicate id: s1", result["errors"])

    def test_material_anchor_must_exist_in_selected_source(self) -> None:
        payload = self.base_payload()
        item = self.plan_item()
        item["materials"][0]["anchor"] = "an observation that was never supplied"
        payload["brief"]["section_plan"] = [item]

        result = self.validate_input(payload)

        self.assertEqual("REJECT", result["status"])
        self.assertTrue(any("anchor not found in raw_text" in error for error in result["errors"]))

    def test_context_file_accepts_section_plan(self) -> None:
        context = {"section_plan": [self.plan_item()]}
        with tempfile.TemporaryDirectory() as temp_dir:
            path = Path(temp_dir) / "context.json"
            path.write_text(json.dumps(context, ensure_ascii=False), encoding="utf-8")

            loaded = read_context(str(path))

        self.assertEqual(context, loaded)

    def test_base_input_preserves_brief_and_replaces_spine_with_section_plan(self) -> None:
        base = self.base_payload()
        base["brief"]["spine"] = ["legacy order"]
        base["brief"]["constraints"] = {"forbidden_phrases": ["internal token"]}
        context = {"section_plan": [self.plan_item()]}
        args = argparse.Namespace(
            raw_text_file=None,
            raw_text=None,
            topic=None,
            piece_type=None,
            intent=None,
            audience=None,
            target_length=None,
            tone=None,
            emphasis=None,
            must_include=None,
            avoid=None,
        )

        with tempfile.TemporaryDirectory() as temp_dir:
            path = Path(temp_dir) / "base.json"
            path.write_text(json.dumps(base, ensure_ascii=False), encoding="utf-8")
            base_brief = read_base_brief(str(path))

        raw_text = read_raw_text(args, base_brief)
        brief = build_brief(args, raw_text, context, base_brief)

        self.assertNotIn("spine", brief)
        self.assertEqual(context["section_plan"], brief["section_plan"])
        self.assertEqual(["internal token"], brief["constraints"]["forbidden_phrases"])
        self.assertEqual(base["brief"]["raw_text"], brief["raw_text"])


class SectionPlanCritiqueSchemaTest(unittest.TestCase):
    def test_section_reviews_are_part_of_critique_output(self) -> None:
        payload = {
            "brief_hash": "abcdef12",
            "iteration": "001",
            "summary": "The first section stays within its contract.",
            "strengths": [],
            "weaknesses": [],
            "revision_directions": [],
            "reader_risks": [],
            "section_reviews": [
                {
                    "section_id": "s1",
                    "actual_heading": "Why the decision changed",
                    "heading_match": True,
                    "purpose_match": True,
                    "material_use_match": True,
                    "out_of_scope_excerpts": [],
                    "connection_match": "not_declared",
                }
            ],
            "unsupported_claims": [],
            "suggestions": [],
        }
        with tempfile.TemporaryDirectory() as temp_dir:
            path = Path(temp_dir) / "critique.json"
            path.write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")
            result = validate_file(path, artifact="critique_output")

        self.assertEqual("PASS", result["status"])


if __name__ == "__main__":
    unittest.main()
