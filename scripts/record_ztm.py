import gzip
import json
import os
import sys
import time
import urllib.request
from datetime import datetime, timezone

URL = "https://ckan2.multimediagdansk.pl/gpsPositions?v=2"
SECONDS_BETWEEN_FETCHES = 20
REQUEST_TIMEOUT = 10  # seconds to wait for the answer
DEFAULT_FOLDER = "fixtures/ztm"


def get_folder():
    if len(sys.argv) > 1:
        return sys.argv[1]
    folder_from_env = os.environ.get("ZTM_OUTPUT_DIR", "")
    if folder_from_env != "":
        return folder_from_env
    return DEFAULT_FOLDER


def log(message):
    clock = time.strftime("%H:%M:%S")
    print(clock, message, flush=True)


def download():
    request = urllib.request.Request(URL)
    request.add_header("User-Agent", "BusRadar-recorder")
    with urllib.request.urlopen(request, timeout=REQUEST_TIMEOUT) as response:
        text = response.read().decode("utf-8")
    return json.loads(text)


def make_line(fetched_at, data):
    record = {
        "fetchedAt": fetched_at.strftime("%Y-%m-%dT%H:%M:%SZ"),
        "body": data,
    }
    return json.dumps(record, ensure_ascii=False) + "\n"


def save(folder, fetched_at, line):
    local_time = fetched_at.astimezone()
    file_name = local_time.strftime("%Y-%m-%d-%H") + ".jsonl.gz"
    path = os.path.join(folder, file_name)
    with gzip.open(path, "ab") as file:
        file.write(line.encode("utf-8"))
    return file_name


def main():
    folder = get_folder()
    os.makedirs(folder, exist_ok=True)
    log("recording to " + folder + " (press Ctrl+C to stop)")

    try:
        while True:
            started = time.time()
            fetched_at = datetime.now(timezone.utc)

            try:
                data = download()
                line = make_line(fetched_at, data)
                file_name = save(folder, fetched_at, line)
                log(f"saved {len(line)} characters to {file_name}")
            except Exception as error:
                log(f"error: {error}")

            seconds_used = time.time() - started
            if seconds_used < SECONDS_BETWEEN_FETCHES:
                time.sleep(SECONDS_BETWEEN_FETCHES - seconds_used)
    except KeyboardInterrupt:
        log("stopped")


if __name__ == "__main__":
    main()
