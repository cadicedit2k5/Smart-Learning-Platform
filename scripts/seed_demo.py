#!/usr/bin/env python3
"""
Seed every course JSON inside data/courses into Smart-Learning-Platform.

Expected:
    data/
      courses/
        database.json
        operating_system.json
        data_structures.json

Install:
    pip install requests

Environment:
    DEMO_EMAIL
    DEMO_PASSWORD

Optional:
    SYSTEM_API_URL   default http://localhost:8081
    CORE_API_URL     default http://localhost:8082

If your services use /api/v1, set the URLs including that prefix.
"""

from __future__ import annotations

import json
import os
import sys
from pathlib import Path
import mimetypes
import requests


DATA_DIR = Path(
    os.getenv(
        "DEMO_DATA_DIR",
        "../demo-data/courses",
    )
)

SYSTEM_API_URL = os.getenv(
    "SYSTEM_API_URL",
    "http://localhost:8081/api/v1",
).rstrip("/")

CORE_API_URL = os.getenv(
    "CORE_API_URL",
    "http://localhost:8082/api/v1",
).rstrip("/")

TIMEOUT = 60


class SeedError(RuntimeError):
    pass


def load_json(path: Path):
    try:
        return json.loads(
            path.read_text(
                encoding="utf-8"
            )
        )
    except Exception as exc:
        raise SeedError(
            f"Cannot read {path}: {exc}"
        ) from exc


def response_data(response):
    try:
        body = response.json()
    except ValueError as exc:
        raise SeedError(
            f"Non-JSON response "
            f"{response.status_code}: "
            f"{response.text[:500]}"
        ) from exc

    if not response.ok:
        raise SeedError(
            f"{response.request.method} "
            f"{response.url}\n"
            f"HTTP {response.status_code}\n"
            f"{json.dumps(body, ensure_ascii=False)}"
        )

    if "data" not in body:
        raise SeedError(
            f"Response has no data: {body}"
        )

    return body["data"]


def login(session, email, password):

    response = session.post(
        f"{SYSTEM_API_URL}/auth/login",
        json={
            "email": email,
            "password": password,
        },
        timeout=TIMEOUT,
    )

    data = response_data(response)

    token = data.get("accessToken")

    if not token:
        raise SeedError(
            "Login response has no "
            "accessToken"
        )

    session.headers.update({
        "Authorization":
            f"Bearer {token}",
        "Accept":
            "application/json",
    })


def create_course(
    session,
    payload,
    image_path=None,
):

    files = {
        "course": (
            None,
            json.dumps(
                payload,
                ensure_ascii=False,
            ),
            "application/json",
        )
    }

    image_file = None

    try:
        if image_path:
            if not image_path.exists():
                raise SeedError(
                    f"Course image not found: "
                    f"{image_path}"
                )

            content_type = (
                mimetypes.guess_type(
                    image_path.name
                )[0]
                or "application/octet-stream"
            )

            image_file = image_path.open("rb")

            files["image"] = (
                image_path.name,
                image_file,
                content_type,
            )

        response = session.post(
            f"{CORE_API_URL}/courses",
            files=files,
            timeout=TIMEOUT,
        )

        return response_data(response)

    finally:
        if image_file:
            image_file.close()

def publish_course(
    session,
    course_id,
):
    response = session.post(
        (
            f"{CORE_API_URL}/courses/"
            f"{course_id}/publish"
        ),
        timeout=TIMEOUT,
    )

    return response_data(response)

def create_chapter(
    session,
    course_id,
    payload,
):

    response = session.post(
        (
            f"{CORE_API_URL}/courses/"
            f"{course_id}/chapters"
        ),
        json=payload,
        timeout=TIMEOUT,
    )

    return response_data(response)


def create_topic(
    session,
    course_id,
    chapter_id,
    payload,
):

    response = session.post(
        (
            f"{CORE_API_URL}/courses/"
            f"{course_id}/chapters/"
            f"{chapter_id}/topics"
        ),
        json=payload,
        timeout=TIMEOUT,
    )

    return response_data(response)


def get_topics(
    session,
    course_id,
    chapter_id,
):

    response = session.get(
        (
            f"{CORE_API_URL}/courses/"
            f"{course_id}/chapters/"
            f"{chapter_id}/topics"
        ),
        timeout=TIMEOUT,
    )

    return response_data(response)


def normalize_course_data(data):

    # New generated format:
    #
    # {
    #   "source": {...},
    #   "course": {...},
    #   "chapter": {...},
    #   "topics": [...]
    # }
    image = data.get("image")

    source = data.get(
        "source",
        {},
    )

    course = data.get(
        "course"
    )

    if course is None:
        # Backward compatible with the old
        # voer_database_topics.json file.
        title = source.get(
            "collection_title"
        )

        if not title:
            raise SeedError(
                "Missing course title"
            )

        course = {
            "title": title,
            "description": None,
            "level": None,
            "visibility": "PUBLIC",
        }

    chapter = data.get(
        "chapter"
    )

    if chapter is None:
        chapter = {
            "title": "Nội dung khóa học",
            "description": None,
            "learningObjectives": None,
            "orderIndex": 0,
        }

    raw_topics = data.get(
        "topics",
        []
    )

    if not raw_topics:
        raise SeedError(
            f"Course "
            f"'{course['title']}' "
            f"has no topics"
        )

    topics = []

    for index, item in enumerate(
        raw_topics,
        start=1,
    ):

        if item.get("error"):
            raise SeedError(
                f"Extractor error in "
                f"topic #{index}: "
                f"{item['error']}"
            )

        # Supports both:
        # {"topic": {...}}
        # and direct {...}
        topic = item.get(
            "topic",
            item,
        )

        content = topic.get(
            "content"
        )

        if (
            not isinstance(content, dict)
            or content.get("type") != "doc"
            or not content.get("content")
        ):
            raise SeedError(
                f"Invalid content: "
                f"{topic.get('title')}"
            )

        topics.append({
            "title":
                topic["title"],

            "description":
                topic.get(
                    "description"
                ),

            "orderIndex":
                topic.get(
                    "orderIndex",
                    index,
                ),

            "estimatedMinutes":
                topic.get(
                    "estimatedMinutes"
                ),

            "content":
                content,
        })

    return (
        course,
        chapter,
        topics,
        image,
    )


def seed_course(
    session,
    path,
):

    raw = load_json(path)

    (
        course_payload,
        chapter_payload,
        topics,
        image,
    ) = normalize_course_data(
        raw
    )

    print(
        "\n================================"
    )

    image_path = None

    if image:
        image_path = (
            path.parent / image
        ).resolve()

    print(
        f"COURSE: "
        f"{course_payload['title']}"
    )

    created_course = create_course(
        session,
        course_payload,
        image_path,
    )

    course_id = created_course[
        "id"
    ]

    print(
        f"✓ Course created: "
        f"{course_id}"
    )

    created_chapter = (
        create_chapter(
            session,
            course_id,
            chapter_payload,
        )
    )

    chapter_id = created_chapter[
        "id"
    ]

    print(
        f"✓ Chapter created: "
        f"{created_chapter['title']}"
    )

    for index, topic in enumerate(
        topics,
        start=1,
    ):

        created = create_topic(
            session,
            course_id,
            chapter_id,
            topic,
        )

        print(
            f"  ✓ "
            f"{index:02d}/"
            f"{len(topics):02d} "
            f"{created['title']}"
        )

    actual = get_topics(
        session,
        course_id,
        chapter_id,
    )

    if len(actual) != len(topics):
        raise SeedError(
            f"Verify failed for "
            f"{course_payload['title']}: "
            f"expected "
            f"{len(topics)} topics, "
            f"got {len(actual)}"
        )

    print(
        f"✓ Verified "
        f"{len(actual)} topics"
    )

    published_course = publish_course(
        session,
        course_id,
    )

    print(
        f"✓ Course published: "
        f"{published_course['title']}"
    )

    return len(topics)


def main():

    email = 'lecturer01@example.com'

    password = '12345678'

    if not email or not password:
        print(
            "Missing DEMO_EMAIL or "
            "DEMO_PASSWORD",
            file=sys.stderr,
        )

        raise SystemExit(2)

    if not DATA_DIR.exists():
        print(
            f"Data directory not found: "
            f"{DATA_DIR}",
            file=sys.stderr,
        )

        raise SystemExit(2)

    files = sorted(
        DATA_DIR.glob("*.json")
    )

    if not files:
        print(
            f"No course JSON files in "
            f"{DATA_DIR}",
            file=sys.stderr,
        )

        raise SystemExit(2)

    session = requests.Session()

    print(
        f"Login: {email}"
    )

    login(
        session,
        email,
        password,
    )

    print(
        "✓ Login successful"
    )

    total_topics = 0

    for path in files:
        print(
            f"\nLoading: {path.name}"
        )

        total_topics += seed_course(
            session,
            path,
        )

    print(
        "\n================================"
    )

    print(
        "DEMO DATA READY"
    )

    print(
        "================================"
    )

    print(
        f"Courses: {len(files)}"
    )

    print(
        f"Topics : {total_topics}"
    )


if __name__ == "__main__":
    main()
