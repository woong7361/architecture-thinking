from __future__ import annotations

import importlib.util
import json
from pathlib import Path
import tempfile
import unittest


SKILL_DIR = Path(__file__).resolve().parent.parent


def load_module(name: str, path: Path):
    spec = importlib.util.spec_from_file_location(name, path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load module: {path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


prepare_review = load_module("speech_prepare_review", SKILL_DIR / "scripts" / "prepare_review.py")
aggregate_review = load_module("speech_aggregate_review", SKILL_DIR / "scripts" / "aggregate_review.py")
transcribe = load_module("speech_transcribe", SKILL_DIR / "scripts" / "transcribe.py")


class SpeechRehearsalPipelineTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.root = Path(self.temp_dir.name)
        self.transcript_path = self.root / "transcript.txt"
        self.transcript_path.write_text(
            "어 저는 그 문제를 설명합니다. 그, 다음은 결론입니다.",
            encoding="utf-8",
        )
        self.context = prepare_review.build_context(self.transcript_path, 30.0, None)

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def test_prepare_context_calculates_only_mechanical_metrics(self) -> None:
        self.assertEqual(8, self.context["metrics"]["whitespace_token_count"])
        self.assertEqual(30.0, self.context["metrics"]["duration_seconds"])
        self.assertEqual(16.0, self.context["metrics"]["tokens_per_minute"])
        self.assertFalse(self.context["presentation_plan"]["available"])
        self.assertEqual("t0001", self.context["transcript"]["tokens"][0]["id"])
        self.assertEqual("어", self.context["transcript"]["tokens"][0]["text"])

    def test_transcription_response_is_normalized_to_the_contract(self) -> None:
        transcript = transcribe.build_transcript(
            self.transcript_path,
            "gpt-transcribe",
            {"text": "발표 내용", "duration": 10.0, "segments": []},
        )

        transcribe.validate_transcript(transcript)
        self.assertEqual("발표 내용", transcript["text"])
        self.assertEqual(10.0, transcript["duration_seconds"])

    def test_aggregate_counts_ai_labels_without_reclassifying(self) -> None:
        delivery = self._delivery_review()
        logic = self._logic_review()
        aggregate_review.validate_schema(
            delivery,
            SKILL_DIR / "schemas" / "delivery-output.schema.json",
            "delivery review",
        )
        aggregate_review.validate_schema(
            logic,
            SKILL_DIR / "schemas" / "logic-output.schema.json",
            "logic review",
        )
        resources = aggregate_review.verify_resources(self.context)
        aggregate_review.verify_criterion_coverage(
            delivery,
            aggregate_review.rubric_criteria(resources["delivery"]["rubric"]),
            "delivery",
        )
        aggregate_review.verify_criterion_coverage(
            logic,
            aggregate_review.rubric_criteria(resources["logic"]["rubric"]),
            "logic",
        )
        aggregate_review.verify_token_evidence(self.context, delivery, logic)

        feedback = aggregate_review.build_feedback(self.context, delivery, logic)

        self.assertEqual(1, feedback["filler_summary"]["confirmed_filler_count"])
        self.assertEqual(1, feedback["filler_summary"]["lexical_count"])
        self.assertEqual(1, feedback["filler_summary"]["uncertain_count"])
        self.assertEqual(2.0, feedback["filler_summary"]["confirmed_fillers_per_minute"])
        self.assertEqual({"어": 1}, feedback["filler_summary"]["confirmed_filler_surfaces"])

    def test_overlapping_filler_annotations_are_rejected(self) -> None:
        delivery = self._delivery_review()
        duplicate = dict(delivery["filler_annotations"][0])
        duplicate["annotation_id"] = "f004"
        delivery["filler_annotations"].append(duplicate)

        with self.assertRaisesRegex(ValueError, "overlap"):
            aggregate_review.verify_token_evidence(self.context, delivery, self._logic_review())

    def test_scripts_do_not_duplicate_role_or_rubric_authority(self) -> None:
        script_text = "\n".join(path.read_text(encoding="utf-8") for path in (SKILL_DIR / "scripts").glob("*.py"))
        self.assertNotIn("You are a speech delivery diagnostician", script_text)
        self.assertNotIn("You are a skeptical senior audience member", script_text)

        rubric_paths = [
            SKILL_DIR / "references" / "rubrics" / "delivery.yaml",
            SKILL_DIR / "references" / "rubrics" / "logic.yaml",
        ]
        for rubric_path in rubric_paths:
            for criterion in aggregate_review.rubric_criteria(rubric_path):
                self.assertNotIn(criterion, script_text)

    def _delivery_review(self) -> dict:
        criteria = aggregate_review.rubric_criteria(
            SKILL_DIR / "references" / "rubrics" / "delivery.yaml"
        )
        return {
            "schema_version": 1,
            "reviewer": "delivery_reviewer",
            "overall_summary": "전달 방식 진단 요약",
            "filler_annotations": [
                {
                    "annotation_id": "f001",
                    "token_ids": ["t0001"],
                    "surface": "어",
                    "label": "filler",
                    "confidence": 0.95,
                    "evidence_quote": "어",
                    "reason": "의미 없이 발화를 시작함",
                },
                {
                    "annotation_id": "f002",
                    "token_ids": ["t0003"],
                    "surface": "그",
                    "label": "lexical",
                    "confidence": 0.9,
                    "evidence_quote": "그 문제를",
                    "reason": "대상을 한정함",
                },
                {
                    "annotation_id": "f003",
                    "token_ids": ["t0006"],
                    "surface": "그,",
                    "label": "uncertain",
                    "confidence": 0.5,
                    "evidence_quote": "그, 다음은",
                    "reason": "음성 정보 없이 망설임 여부를 확정하기 어려움",
                },
            ],
            "findings": [self._delivery_finding(criterion) for criterion in sorted(criteria)],
            "top_actions": ["다음 리허설 행동"],
            "limitations": ["텍스트만으로 음량을 평가하지 않음"],
        }

    def _delivery_finding(self, criterion: str) -> dict:
        return {
            "criterion": criterion,
            "status": "caution",
            "evidence": [{"token_ids": ["t0001"], "quote": "어"}],
            "diagnosis": "관찰 가능한 전달 특성이 있다.",
            "impact": "청자가 흐름을 놓칠 수 있다.",
            "action": "같은 구간을 다시 말해 본다.",
        }

    def _logic_review(self) -> dict:
        criteria = aggregate_review.rubric_criteria(
            SKILL_DIR / "references" / "rubrics" / "logic.yaml"
        )
        return {
            "schema_version": 1,
            "reviewer": "senior_logic_reviewer",
            "overall_summary": "논리 진단 요약",
            "findings": [self._logic_finding(criterion) for criterion in sorted(criteria)],
            "senior_questions": [
                {
                    "question": "이 주장의 적용 범위는 어디까지인가요?",
                    "why": "범위가 명시되지 않았다.",
                    "evidence": {"token_ids": ["t0003"], "quote": "그 문제를"},
                },
                {
                    "question": "결론을 뒷받침하는 근거는 무엇인가요?",
                    "why": "근거 연결을 확인해야 한다.",
                    "evidence": {"token_ids": ["t0003"], "quote": "그 문제를"},
                },
            ],
            "top_actions": ["근거 연결을 명시한다."],
            "limitations": ["제공된 transcript만 검토함"],
        }

    def _logic_finding(self, criterion: str) -> dict:
        return {
            "criterion": criterion,
            "status": "caution",
            "evidence": [{"token_ids": ["t0003"], "quote": "그 문제를"}],
            "observation": "문제가 언급되었다.",
            "inference": "설명의 대상이라고 추론된다.",
            "missing_evidence": "범위가 명시되지 않았다.",
            "diagnosis": "주장의 경계가 불분명하다.",
            "action": "적용 범위를 명시한다.",
        }


if __name__ == "__main__":
    unittest.main()
