from __future__ import annotations

import argparse
import hashlib
import json
import mimetypes
import os
from pathlib import Path
import sys
import urllib.error
import urllib.request
import uuid

from jsonschema import Draft202012Validator


API_URL = "https://api.openai.com/v1/audio/transcriptions"
DEFAULT_MODEL = "gpt-transcribe"
DEFAULT_TIMEOUT_SECONDS = 300
SUPPORTED_SUFFIXES = {".flac", ".mp3", ".mp4", ".mpeg", ".mpga", ".m4a", ".ogg", ".wav", ".webm"}
TRANSCRIPT_SCHEMA_PATH = Path(__file__).resolve().parent.parent / "schemas" / "transcript.schema.json"


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def multipart_body(fields: dict[str, str], file_path: Path) -> tuple[bytes, str]:
    boundary = f"----speech-rehearsal-{uuid.uuid4().hex}"
    newline = b"\r\n"
    chunks: list[bytes] = []

    for name, value in fields.items():
        chunks.extend(
            [
                f"--{boundary}".encode(),
                f'Content-Disposition: form-data; name="{name}"'.encode(),
                b"",
                value.encode("utf-8"),
            ]
        )

    media_type = mimetypes.guess_type(file_path.name)[0] or "application/octet-stream"
    chunks.extend(
        [
            f"--{boundary}".encode(),
            f'Content-Disposition: form-data; name="file"; filename="{file_path.name}"'.encode("utf-8"),
            f"Content-Type: {media_type}".encode(),
            b"",
            file_path.read_bytes(),
            f"--{boundary}--".encode(),
            b"",
        ]
    )
    return newline.join(chunks), boundary


def request_transcription(
    media_path: Path,
    api_key: str,
    model: str,
    language: str | None,
    timeout_seconds: int,
) -> dict:
    fields = {"model": model, "response_format": "json"}
    if language:
        fields["language"] = language

    body, boundary = multipart_body(fields, media_path)
    request = urllib.request.Request(
        API_URL,
        data=body,
        method="POST",
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": f"multipart/form-data; boundary={boundary}",
        },
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout_seconds) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        detail = error.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"transcription request failed with HTTP {error.code}: {detail}") from error


def normalize_segments(raw_segments: object) -> list[dict]:
    if not isinstance(raw_segments, list):
        return []
    segments = []
    for item in raw_segments:
        if not isinstance(item, dict):
            continue
        segments.append(
            {
                "text": str(item.get("text", "")),
                "start_seconds": item.get("start_seconds", item.get("start")),
                "end_seconds": item.get("end_seconds", item.get("end")),
            }
        )
    return segments


def build_transcript(media_path: Path, model: str, response: dict) -> dict:
    text = response.get("text")
    if not isinstance(text, str) or not text.strip():
        raise ValueError("transcription response did not contain non-empty text")

    duration = response.get("duration")
    if not isinstance(duration, (int, float)) or isinstance(duration, bool) or duration <= 0:
        duration = None

    return {
        "schema_version": 1,
        "text": text.strip(),
        "duration_seconds": duration,
        "segments": normalize_segments(response.get("segments")),
        "source": {
            "path": str(media_path.resolve()),
            "sha256": sha256_file(media_path),
        },
        "provider": "openai",
        "model": model,
    }


def write_json(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def validate_transcript(value: dict) -> None:
    schema = json.loads(TRANSCRIPT_SCHEMA_PATH.read_text(encoding="utf-8"))
    errors = sorted(Draft202012Validator(schema).iter_errors(value), key=lambda error: list(error.path))
    if errors:
        details = "; ".join(error.message for error in errors[:5])
        raise ValueError(f"transcript schema validation failed: {details}")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Transcribe one complete media file with the OpenAI audio transcription endpoint.")
    parser.add_argument("media", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--model", default=DEFAULT_MODEL)
    parser.add_argument("--language")
    parser.add_argument("--timeout-seconds", type=int, default=DEFAULT_TIMEOUT_SECONDS)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    media_path = args.media.resolve()
    if not media_path.is_file():
        raise FileNotFoundError(f"media file not found: {media_path}")
    if media_path.suffix.lower() not in SUPPORTED_SUFFIXES:
        raise ValueError(f"unsupported media suffix: {media_path.suffix}")
    if args.output.exists():
        raise FileExistsError(f"refusing to overwrite existing output: {args.output}")

    api_key = os.environ.get("OPENAI_API_KEY")
    if not api_key:
        raise RuntimeError("OPENAI_API_KEY is not set; provide a transcript or set the user-owned environment variable")

    response = request_transcription(media_path, api_key, args.model, args.language, args.timeout_seconds)
    transcript = build_transcript(media_path, args.model, response)
    validate_transcript(transcript)
    write_json(args.output.resolve(), transcript)
    print(args.output.resolve())
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as error:
        print(f"ERROR: {error}", file=sys.stderr)
        raise SystemExit(1)
