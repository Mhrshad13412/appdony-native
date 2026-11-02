#!/usr/bin/env python3
"""
ساخت popularity.json برای اپدونی.

۱) لیست پست‌ها رو از وردپرس اپدونی می‌خونه (فقط id و عنوان)
۲) اسم انگلیسی هر بازی رو درمیاره و از RAWG امتیاز شهرتش رو می‌گیره
۳) خروجی: popularity.json  ->  {"v":1,"scores":{"<postId>": <0..100>, ...}}

نتیجه‌ی RAWG توی scripts/rawg_cache.json ذخیره می‌شه؛ هر اسم فقط یک بار پرسیده می‌شه
و هر اجرا حداکثر MAX_RAWG_CALLS درخواست می‌زنه (سقف ماهانه‌ی رایگان RAWG: ۲۰٬۰۰۰).
"""
import difflib
import html
import json
import math
import os
import pathlib
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

SITE = "https://appdony.ir"
RAWG_KEY = os.environ.get("RAWG_KEY", "").strip()
MAX_RAWG_CALLS = int(os.environ.get("MAX_RAWG_CALLS", "500"))
RETRY_MISS_AFTER_DAYS = 60

ROOT = pathlib.Path(__file__).resolve().parent.parent
OUT = ROOT / "popularity.json"
CACHE = ROOT / "scripts" / "rawg_cache.json"
UA = "appdony-popularity/1.0 (+https://github.com/Mhrshad13412/appdony-native)"

DIGITS = {ord(c): str(i) for i, c in enumerate("۰۱۲۳۴۵۶۷۸۹")}
DIGITS.update({ord(c): str(i) for i, c in enumerate("٠١٢٣٤٥٦٧٨٩")})

EN_CUT = {"mod", "apk", "obb", "android", "ios", "hack", "hacked", "cracked", "patched", "data", "download"}
EN_PLATFORMS = {"android", "ios", "pc", "windows", "mac", "iphone", "ipad"}
EN_BREAK = {"-", "–", "—", "|", "/", "\\", "~", "•", ",", "،"}
EN_CONNECT = {"&", ":", "+", "'", "’", "!", ".", "·"}
EN_VERSION = re.compile(r"^(v\d+(\.\d+)*[a-z]?|\d+\.\d+\.\d+(\.\d+)*[a-z]?)$", re.I)
ARABIC = re.compile(r"[\u0600-\u06FF\u0750-\u077F\u08A0-\u08FF\uFB50-\uFDFF\uFE70-\uFEFF]")


def http_json(url, retries=3, timeout=40):
    last = None
    for i in range(retries):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA, "Accept": "application/json"})
            with urllib.request.urlopen(req, timeout=timeout) as r:
                return json.loads(r.read().decode("utf-8")), dict(r.headers)
        except urllib.error.HTTPError as e:
            if e.code in (400, 401, 403, 404, 429):
                raise
            last = e
        except Exception as e:  # noqa: BLE001
            last = e
        time.sleep(2 * (i + 1))
    raise last


def fetch_posts():
    posts, page, total_pages = [], 1, None
    while True:
        url = f"{SITE}/wp-json/wp/v2/posts?per_page=100&page={page}&_fields=id,title"
        try:
            data, headers = http_json(url)
        except urllib.error.HTTPError as e:
            if e.code == 400 and page > 1:  # بعد از آخرین صفحه
                break
            raise
        if not data:
            break
        posts.extend(data)
        if total_pages is None:
            tp = headers.get("X-WP-TotalPages") or headers.get("x-wp-totalpages")
            total_pages = int(tp) if tp and tp.isdigit() else None
        if total_pages and page >= total_pages:
            break
        page += 1
        if page > 300:
            break
    return posts


def english_name(title):
    """بخش انگلیسیِ اسم بازی (مثل فیلتر داخل اپ)؛ رشته‌ی خالی اگه نبود"""
    t = html.unescape(re.sub(r"<[^>]+>", " ", title))
    t = t.translate(DIGITS).replace("\u200c", " ").replace("\u00a0", " ").replace("…", " ")
    m = re.search(r"[“\"«„](.+?)[”\"»“]", t)
    if m:
        t = m.group(1)
    t = re.sub(r"[“”\"«»„‟()\[\]{}<>]", " ", t)
    tokens = t.split()

    runs, cur, i = [], [], 0

    def flush():
        nonlocal cur
        if cur:
            runs.append(cur)
        cur = []

    while i < len(tokens):
        tok = tokens[i]
        low = tok.lower()
        if ARABIC.search(tok) or low in EN_BREAK or low in EN_CUT or EN_VERSION.match(tok):
            flush()
        elif low == "for" and i + 1 < len(tokens) and tokens[i + 1].lower() in EN_PLATFORMS:
            flush()
            i += 1
        elif re.search(r"[A-Za-z0-9]", tok) or low in EN_CONNECT:
            cur.append(tok)
        else:
            flush()
        i += 1
    flush()

    best, best_letters = "", 0
    for run in runs:
        while run and run[0].lower() in EN_CONNECT:
            run = run[1:]
        while run and run[-1].lower() in EN_CONNECT and run[-1] not in ("!", "."):
            run = run[:-1]
        text = " ".join(run).strip(":&+,- ")
        letters = len(re.findall(r"[A-Za-z]", text))
        if letters >= 2 and letters > best_letters:
            best, best_letters = text, letters
    return best


def norm(s):
    return re.sub(r"[^a-z0-9]", "", s.lower())


def rawg_lookup(name):
    q = urllib.parse.urlencode({"key": RAWG_KEY, "search": name, "search_precise": "true", "page_size": 5})
    data, _ = http_json(f"https://api.rawg.io/api/games?{q}", retries=2)
    want = norm(name)
    best, best_ratio = None, 0.0
    for g in data.get("results", []):
        got = norm(g.get("name", ""))
        if not got:
            continue
        ratio = difflib.SequenceMatcher(None, want, got).ratio()
        if ratio > best_ratio or (ratio == best_ratio and g.get("added", 0) > (best or {}).get("added", 0)):
            best, best_ratio = g, ratio
    if not best or best_ratio < 0.82:
        return {"miss": True, "t": int(time.time())}
    return {
        "rawg": best.get("name"),
        "added": int(best.get("added") or 0),
        "rc": int(best.get("ratings_count") or 0),
        "mc": best.get("metacritic"),
        "t": int(time.time()),
    }


def score_of(entry):
    added = int(entry.get("added") or 0)
    if added <= 0:
        return 0
    mc = entry.get("mc") or 0
    s = math.log10(1 + added) / 4.3 * 85 + 15 * (mc / 100.0)
    return int(round(min(100, s)))


def main():
    if not RAWG_KEY:
        print("RAWG_KEY خالیه؛ توی Secrets ریپو بذارش.", file=sys.stderr)
        return 1

    cache = json.loads(CACHE.read_text("utf-8")) if CACHE.exists() else {}

    print("در حال خواندن پست‌ها از", SITE)
    posts = fetch_posts()
    print("تعداد پست:", len(posts))
    if not posts:
        print("هیچ پستی نیومد؛ خروجی عوض نمی‌شه.", file=sys.stderr)
        return 1

    posts.sort(key=lambda p: p["id"], reverse=True)  # جدیدترها اول
    names = {}  # کلید -> اسم
    post_key = {}
    for p in posts:
        nm = english_name(p["title"]["rendered"])
        k = norm(nm)
        if len(k) < 3:
            continue
        post_key[p["id"]] = k
        names.setdefault(k, nm)

    now = time.time()
    todo = []
    for k, nm in names.items():
        e = cache.get(k)
        if e is None or (e.get("miss") and now - e.get("t", 0) > RETRY_MISS_AFTER_DAYS * 86400):
            todo.append((k, nm))

    print(f"اسم یکتا: {len(names)} | جدید برای پرسیدن: {len(todo)} | سقف این اجرا: {MAX_RAWG_CALLS}")
    calls = 0
    for k, nm in todo[:MAX_RAWG_CALLS]:
        try:
            cache[k] = rawg_lookup(nm)
            calls += 1
        except urllib.error.HTTPError as e:
            if e.code in (401, 403):
                print("کلید RAWG قبول نشد (", e.code, ")", file=sys.stderr)
                break
            if e.code == 429:
                print("سقف درخواست RAWG پر شد؛ بقیه فردا.")
                break
            print("خطا برای", nm, e.code)
        except Exception as e:  # noqa: BLE001
            print("خطا برای", nm, e)
        time.sleep(0.25)
    print("درخواست‌های انجام‌شده:", calls)

    scores = {}
    for pid, k in post_key.items():
        e = cache.get(k)
        if e:
            s = score_of(e)
            if s > 0:
                scores[str(pid)] = s

    CACHE.write_text(json.dumps(cache, ensure_ascii=False, sort_keys=True, separators=(",", ":")), "utf-8")
    OUT.write_text(json.dumps({"v": 1, "scores": scores}, sort_keys=True, separators=(",", ":")), "utf-8")
    print("امتیاز برای", len(scores), "پست نوشته شد.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
