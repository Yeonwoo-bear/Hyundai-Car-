"""
일회성 변환 스크립트: 서울 열린데이터 GetParkInfo 덤프 -> app/src/main/assets/parking_coords.json

실시간 API(GetParkingInfo)에는 좌표가 없으므로, 주차장 정보 API(GetParkInfo)의 좌표를
PKLT_CD 기준으로 미리 추출해 앱 assets 에 넣는다.

사용법 (프로젝트 루트에서):
    python tools/build_parking_coords.py

입력:  tools/parkinfo/parkinfo_*.json  (GetParkInfo 응답 원본, 인증키 미포함)
출력:  app/src/main/assets/parking_coords.json
       [{"PKLT_CD": "...", "lat": 37.5, "lon": 126.9}, ...]

제외 규칙: 좌표가 비어 있거나 / 숫자로 파싱 불가 / 0 / 서울 범위 밖인 항목.
같은 PKLT_CD 가 여러 행이면 처음 나온 유효 좌표를 사용한다.
"""
import glob
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_GLOB = os.path.join(ROOT, "tools", "parkinfo", "parkinfo_*.json")
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "parking_coords.json")

LAT_RANGE = (37.4, 37.75)
LON_RANGE = (126.7, 127.3)


def to_float(v):
    try:
        f = float(str(v).strip())
    except (TypeError, ValueError):
        return None
    return f if f == f else None  # NaN 제외


def main():
    files = sorted(glob.glob(SRC_GLOB))
    if not files:
        sys.exit(f"입력 파일 없음: {SRC_GLOB}")

    coords = {}
    total_rows = 0
    for path in files:
        with open(path, encoding="utf-8") as f:
            body = json.load(f).get("GetParkInfo", {})
        rows = body.get("row", [])
        total_rows += len(rows)
        for r in rows:
            code = str(r.get("PKLT_CD", "")).strip()
            lat, lon = to_float(r.get("LAT")), to_float(r.get("LOT"))
            if not code or code in coords or lat is None or lon is None:
                continue
            if not (LAT_RANGE[0] <= lat <= LAT_RANGE[1] and LON_RANGE[0] <= lon <= LON_RANGE[1]):
                continue
            coords[code] = (round(lat, 6), round(lon, 6))

    out = [{"PKLT_CD": c, "lat": la, "lon": lo} for c, (la, lo) in sorted(coords.items())]
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        json.dump(out, f, ensure_ascii=False, separators=(",", ":"))
        f.write("\n")
    print(f"files={len(files)} rows={total_rows} unique_with_coords={len(out)} -> {os.path.relpath(OUT, ROOT)}")


if __name__ == "__main__":
    main()
