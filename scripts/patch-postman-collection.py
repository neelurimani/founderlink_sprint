#!/usr/bin/env python3
"""Patch FounderLink.postman_collection.json for JWT-derived user ids and investment auth."""
import json
from pathlib import Path

COL = Path(__file__).resolve().parent.parent / "postman" / "FounderLink.postman_collection.json"

JWT_DECL = [
    "function founderlinkJwtUserId(token) {",
    "    if (!token || typeof token !== 'string' || token.split('.').length < 2) return null;",
    "    try {",
    "        var b64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');",
    "        var pad = (4 - (b64.length % 4)) % 4;",
    "        var padded = b64 + (pad ? '===='.slice(0, pad) : '');",
    "        var payload = JSON.parse(atob(padded));",
    "        if (payload.userId != null) return String(payload.userId);",
    "        if (payload.sub != null) return String(payload.sub);",
    "    } catch (e) {}",
    "    return null;",
    "}",
]

PR_EXTRA = [
    "function founderlinkJwtUserId(token) {",
    "    if (!token || typeof token !== 'string' || token.split('.').length < 2) return null;",
    "    try {",
    "        var b64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');",
    "        var pad = (4 - (b64.length % 4)) % 4;",
    "        var padded = b64 + (pad ? '===='.slice(0, pad) : '');",
    "        var payload = JSON.parse(atob(padded));",
    "        if (payload.userId != null) return String(payload.userId);",
    "        if (payload.sub != null) return String(payload.sub);",
    "    } catch (e) {}",
    "    return null;",
    "}",
    "(function () {",
    "    var t = pm.environment.get('accessToken');",
    "    var uid = founderlinkJwtUserId(t);",
    "    if (uid) pm.environment.set('authUserId', uid);",
    "})();",
]

SET_FOUNDER = [
    "  const __uid = founderlinkJwtUserId(json.accessToken);",
    "  if (__uid) { pm.environment.set('authUserId', __uid); pm.environment.set('founderAuthUserId', __uid); }",
]
SET_INVESTOR = [
    "  const __uid = founderlinkJwtUserId(json.accessToken);",
    "  if (__uid) { pm.environment.set('authUserId', __uid); pm.environment.set('investorAuthUserId', __uid); }",
]
SET_COFOUNDER = [
    "  const __uid = founderlinkJwtUserId(json.accessToken);",
    "  if (__uid) { pm.environment.set('authUserId', __uid); pm.environment.set('cofounderAuthUserId', __uid); }",
]
SET_ADMIN = [
    "  const __uid = founderlinkJwtUserId(json.accessToken);",
    "  if (__uid) { pm.environment.set('authUserId', __uid); pm.environment.set('adminAuthUserId', __uid); }",
]
SET_GENERIC = [
    "  const __uid = founderlinkJwtUserId(json.accessToken);",
    "  if (__uid) pm.environment.set('authUserId', __uid);",
]


def ensure_jwt_decl_in_test(exec_lines):
    if any("founderlinkJwtUserId" in line for line in exec_lines):
        return exec_lines
    out = []
    for line in exec_lines:
        out.append(line)
        if line.strip() == "if (pm.response.code === 200) {":
            out.extend(JWT_DECL)
    return out


def insert_after_needle(exec_lines, needle, extra_lines):
    out = []
    inserted = False
    for line in exec_lines:
        out.append(line)
        if not inserted and needle in line:
            out.extend(extra_lines)
            inserted = True
    return out, inserted


def patch_request_by_name(items, name, callback):
    for it in items:
        if "item" in it:
            patch_request_by_name(it["item"], name, callback)
            continue
        if it.get("name") == name:
            callback(it)
            return True
    return False


def patch_test(items, name, needle, extra):
    def cb(node):
        for ev in node.get("event", []):
            if ev.get("listen") != "test":
                continue
            ex = ensure_jwt_decl_in_test(ev["script"]["exec"])
            ex, ok = insert_after_needle(ex, needle, extra)
            if ok:
                ev["script"]["exec"] = ex

    patch_request_by_name(items, name, cb)


def walk_mutate_strings(obj):
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k == "raw" and isinstance(v, str):
                obj[k] = v.replace("{{userId}}", "{{authUserId}}")
                if '"investorId": {{authUserId}}' in obj[k]:
                    obj[k] = obj[k].replace('"investorId": {{authUserId}}', '"investorId": {{investorAuthUserId}}')
            else:
                walk_mutate_strings(v)
    elif isinstance(obj, list):
        for x in obj:
            walk_mutate_strings(x)


def main():
    data = json.loads(COL.read_text())

    # Keep in sync with postman/FounderLink.postman_collection.json info.description (gateway, idempotency, public GET /api/startups, status field).
    data["info"]["description"] = (
        "FounderLink API via API Gateway. See repo `postman/FounderLink.postman_collection.json` for full notes: "
        "idempotency exempt `/api/auth/**`, public GET startup catalog, `status` APPROVED/REJECTED for admin, etc."
    )

    for ev in data.get("event", []):
        if ev.get("listen") != "prerequest":
            continue
        ex = ev["script"]["exec"]
        new_ex = []
        for line in ex:
            new_ex.append(line)
            if "pm.environment.set('idempotencyKey'" in line:
                new_ex.append("")
                new_ex.extend(PR_EXTRA)
        ev["script"]["exec"] = new_ex
        break

    root = data["item"]

    patch_test(root, "Register (FOUNDER)", "pm.environment.set('accessToken', json.accessToken);", SET_FOUNDER)
    patch_test(root, "Register (INVESTOR)", "pm.environment.set('accessToken', json.accessToken);", SET_INVESTOR)
    patch_test(root, "Register (COFOUNDER)", "pm.environment.set('accessToken', json.accessToken);", SET_COFOUNDER)
    patch_test(root, "Register (ADMIN)", "pm.environment.set('accessToken', json.accessToken);", SET_ADMIN)
    patch_test(root, "Login (Admin)", "pm.environment.set('accessToken', json.accessToken);", SET_ADMIN)
    patch_test(root, "Login (Founder)", "pm.environment.set('accessToken', json.accessToken);", SET_FOUNDER)
    patch_test(root, "Login (Investor)", "pm.environment.set('accessToken', json.accessToken);", SET_INVESTOR)
    patch_test(root, "Login (Cofounder)", "pm.environment.set('accessToken', json.accessToken);", SET_COFOUNDER)
    patch_test(root, "Login (loginEmail & loginPassword)", "pm.environment.set('accessToken', json.accessToken);", SET_GENERIC)
    patch_test(root, "Refresh (uses refreshToken)", "pm.environment.set('accessToken', json.accessToken);", SET_GENERIC)
    patch_test(root, "Refresh (Admin — uses adminRefreshToken)", "pm.environment.set('adminAccessToken', json.accessToken);", SET_ADMIN)
    patch_test(root, "Refresh (Founder — uses founderRefreshToken)", "pm.environment.set('accessToken', json.accessToken);", SET_FOUNDER)
    patch_test(root, "Refresh (Investor — uses investorRefreshToken)", "pm.environment.set('accessToken', json.accessToken);", SET_INVESTOR)

    def patch_cofounder_refresh(node):
        for ev in node.get("event", []):
            if ev.get("listen") != "test":
                continue
            ex = ensure_jwt_decl_in_test(ev["script"]["exec"])
            ex, ok = insert_after_needle(ex, "pm.environment.set('accessToken', json.accessToken);", SET_COFOUNDER)
            if ok:
                ev["script"]["exec"] = ex

    patch_request_by_name(root, "Refresh (Cofounder — uses cofounderRefreshToken)", patch_cofounder_refresh)

    walk_mutate_strings(data)

    def set_investment_auth(node):
        node.setdefault("request", {})
        node["request"]["auth"] = {
            "type": "bearer",
            "bearer": [{"key": "token", "value": "{{investorAccessToken}}", "type": "string"}],
        }

    patch_request_by_name(root, "Create Investment (INVESTOR)", set_investment_auth)

    def set_get_investor_auth(node):
        node.setdefault("request", {})
        node["request"]["auth"] = {
            "type": "bearer",
            "bearer": [{"key": "token", "value": "{{investorAccessToken}}", "type": "string"}],
        }

    patch_request_by_name(root, "Get Investments By Investor Id", set_get_investor_auth)

    def patch_create_profile_final(node):
        for ev in node.get("event", []):
            if ev.get("listen") != "test":
                continue
            ev["script"]["exec"] = [
                "if (pm.response.code === 201 || pm.response.code === 200) {",
                "  const json = pm.response.json();",
                "  if (json && json.id) {",
                "    pm.environment.set('authUserId', String(json.id));",
                "    pm.environment.set('founderAuthUserId', String(json.id));",
                "  }",
                "}",
            ]

    patch_request_by_name(root, "Create User Profile", patch_create_profile_final)

    COL.write_text(json.dumps(data, indent=2) + "\n")
    print("Wrote", COL)


if __name__ == "__main__":
    main()
