#!/usr/bin/env python3
"""
Seed demo users into Smart-Learning-Platform system service.

Install:
    pip install requests

Environment:
    SEED_ADMIN_EMAIL
    SEED_ADMIN_PASSWORD

Optional:
    SYSTEM_API_URL
    default: http://localhost:8081/api/v1

Run from project root:
    python scripts/seed_system.py
"""

from __future__ import annotations

import json
import os
import sys
from pathlib import Path

import requests

BASE_DIR = Path(__file__).resolve().parents[1]

USERS_FILE = BASE_DIR / "demo-data" / "system" / "user.json"

SYSTEM_API_URL = os.getenv(
    "SYSTEM_API_URL",
    "http://localhost:8081/api/v1",
).rstrip("/")

TIMEOUT = 60


class SeedError(RuntimeError):
    pass


def response_data(response):
    if not response.ok:
        try:
            body = response.json()
            detail = json.dumps(
                body,
                ensure_ascii=False,
            )
        except ValueError:
            detail = (
                response.text.strip()
                or "(empty response body)"
            )

        raise SeedError(
            f"{response.request.method} "
            f"{response.url}\n"
            f"HTTP {response.status_code}\n"
            f"{detail}"
        )

    try:
        body = response.json()
    except ValueError as exc:
        raise SeedError(
            f"Non-JSON response "
            f"{response.status_code}: "
            f"{response.text[:500]}"
        ) from exc

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
            "Login response has no accessToken"
        )

    session.headers.update({
        "Authorization": f"Bearer {token}",
        "Accept": "application/json",
    })


def create_user(session, user):
    # Endpoint consumes multipart/form-data.
    files = {
        "email": (
            None,
            user["email"],
        ),
        "password": (
            None,
            user["password"],
        ),
        "fullName": (
            None,
            user["fullName"],
        ),
        "roleCode": (
            None,
            user["roleCode"],
        ),
    }

    avatar_file = None

    try:
        avatar = user.get("avatar")

        if avatar:
            avatar_path = (
                DATA_FILE.parent / avatar
            ).resolve()

            if not avatar_path.exists():
                raise SeedError(
                    f"Avatar not found: "
                    f"{avatar_path}"
                )

            avatar_file = avatar_path.open("rb")

            files["avatar"] = (
                avatar_path.name,
                avatar_file,
            )

        response = session.post(
            f"{SYSTEM_API_URL}/admin/users",
            files=files,
            timeout=TIMEOUT,
        )

        # Re-running the seed should not stop
        # just because a user already exists.
        if response.status_code == 409:
            print(
                f"  - Skip existing: "
                f"{user['email']}"
            )
            return None

        return response_data(response)

    finally:
        if avatar_file:
            avatar_file.close()


def main():
    admin_email = os.getenv(
        "SEED_ADMIN_EMAIL"
    )
    admin_password = os.getenv(
        "SEED_ADMIN_PASSWORD"
    )

    if not admin_email or not admin_password:
        print(
            "Missing SEED_ADMIN_EMAIL or "
            "SEED_ADMIN_PASSWORD",
            file=sys.stderr,
        )
        raise SystemExit(2)

    if not DATA_FILE.exists():
        print(
            f"Seed file not found: "
            f"{DATA_FILE}",
            file=sys.stderr,
        )
        raise SystemExit(2)

    data = json.loads(
        DATA_FILE.read_text(
            encoding="utf-8"
        )
    )

    users = data.get(
        "users",
        []
    )

    if not users:
        raise SeedError(
            "No users in seed file"
        )

    session = requests.Session()

    print(
        f"Admin login: {admin_email}"
    )

    login(
        session,
        admin_email,
        admin_password,
    )

    print(
        "✓ Admin login successful"
    )

    created = 0
    skipped = 0

    for index, user in enumerate(
        users,
        start=1,
    ):
        print(
            f"[{index:02d}/{len(users):02d}] "
            f"{user['roleCode']} "
            f"{user['email']}"
        )

        result = create_user(
            session,
            user,
        )

        if result is None:
            skipped += 1
        else:
            created += 1
            print(
                f"  ✓ Created: "
                f"{result['fullName']}"
            )

    print(
        "\n============================"
    )
    print(
        "SYSTEM SEED COMPLETE"
    )
    print(
        "============================"
    )
    print(
        f"Created : {created}"
    )
    print(
        f"Skipped : {skipped}"
    )
    print(
        f"Total   : {len(users)}"
    )


if __name__ == "__main__":
    main()
